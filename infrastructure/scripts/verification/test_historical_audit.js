const crypto = require('crypto');
const http = require('http');

const JWT_SECRET = process.env.JWT_SECRET || (() => { throw new Error('JWT_SECRET env var is required to run this verification script'); })();

function base64url(input) { return Buffer.from(input).toString('base64url'); }
function createToken(userId, email, roles) {
  const encHeader = base64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const encPayload = base64url(JSON.stringify({ sub: userId, email, roles, exp: Math.floor(Date.now()/1000) + 86400 }));
  const sig = crypto.createHmac('sha256', Buffer.from(JWT_SECRET)).update(encHeader + '.' + encPayload).digest('base64url');
  return encHeader + '.' + encPayload + '.' + sig;
}

const adminToken = createToken('e1f48826-ced4-416b-81f9-8efa89f8f500', 'rrjaggery@gmail.com', ['ROLE_ADMIN']);
const managerToken = createToken('c83972e4-d50e-443e-8018-a462b09ce25c', 'manager@example.com', ['ROLE_MANAGER']);

function request(options, data = null) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => (body += chunk));
      res.on('end', () => {
        try { resolve({ status: res.statusCode, body: body ? JSON.parse(body) : null }); }
        catch (e) { resolve({ status: res.statusCode, body: null, raw: body }); }
      });
    });
    req.on('error', reject);
    if (data) req.write(typeof data === 'string' ? data : JSON.stringify(data));
    req.end();
  });
}

async function testHistoricalIntegrity() {
  console.log('Testing Form Immutability & Historical Audit Integrity...');

  // 1. Admin adds a test field to EXPENSE
  const timestamp = Date.now();
  const fieldKey = `auditField_${timestamp}`;
  const addFieldRes = await request({
    host: 'localhost', port: 8087, path: '/api/v1/finance/forms/definitions/EXPENSE/fields',
    method: 'POST', headers: { Authorization: `Bearer ${adminToken}`, 'Content-Type': 'application/json' }
  }, {
    fieldKey: fieldKey,
    label: `Audit Note ${timestamp}`,
    fieldType: 'TEXT',
    required: false,
    displayOrder: 25
  });
  const fieldId = addFieldRes.body.data.id;
  console.log('1. Custom Field created:', fieldId, fieldKey);

  // 2. Manager creates an Operating Expense record
  const expRes = await request({
    host: 'localhost', port: 8087, path: '/api/v1/finance/expenses',
    method: 'POST', headers: { Authorization: `Bearer ${managerToken}`, 'Content-Type': 'application/json' }
  }, {
    category: 'Fuel / Diesel',
    amount: 1500.00,
    paymentMethod: 'CASH',
    expenseDate: '2026-09-30',
    payeeVendor: 'Kolhapur Diesel Pump',
    description: 'Diesel for secondary boiler test'
  });
  const expenseId = expRes.body.data.id;
  console.log('2. Operating Expense created, ID:', expenseId);

  // 3. Manager submits supplemental form data using the custom field
  const subRes = await request({
    host: 'localhost', port: 8087, path: '/api/v1/finance/forms/submissions',
    method: 'POST', headers: { Authorization: `Bearer ${managerToken}`, 'Content-Type': 'application/json' }
  }, {
    formKey: 'EXPENSE',
    recordType: 'EXPENSE',
    recordId: expenseId,
    answers: { [fieldKey]: 'Historical Shift Value 9988' }
  });
  console.log('3. Supplemental submission saved, ID:', subRes.body.data.id);

  // 4. Admin deactivates the field
  const deactRes = await request({
    host: 'localhost', port: 8087, path: `/api/v1/finance/forms/fields/${fieldId}/status`,
    method: 'PATCH', headers: { Authorization: `Bearer ${adminToken}`, 'Content-Type': 'application/json' }
  }, { active: false });
  console.log('4. Field deactivated (active=false):', deactRes.body.data.active === false);

  // 5. Read old submission
  const readSub = await request({
    host: 'localhost', port: 8087,
    path: `/api/v1/finance/forms/submissions?recordType=EXPENSE&recordId=${expenseId}`,
    method: 'GET', headers: { Authorization: `Bearer ${managerToken}` }
  });
  const subData = readSub.body.data[0];
  console.log('5. Read old submission verification:');
  console.log('   Submission Record ID:', subData.recordId);
  console.log('   Answers:', JSON.stringify(subData.answers));
  console.log('   Submitted Value for deactivated field:', subData.answers[fieldKey]);

  if (subData.answers[fieldKey] === 'Historical Shift Value 9988') {
    console.log('\n✓ HISTORICAL AUDIT VERIFICATION PASSED: Deactivated field values remain 100% immutable and readable.');
  } else {
    console.error('\n✗ HISTORICAL AUDIT VERIFICATION FAILED.');
    process.exit(1);
  }
}

testHistoricalIntegrity().catch(err => {
  console.error('Test error:', err);
  process.exit(1);
});
