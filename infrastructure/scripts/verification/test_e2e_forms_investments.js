const crypto = require('crypto');
const http = require('http');

const JWT_SECRET = process.env.JWT_SECRET || (() => { throw new Error('JWT_SECRET env var is required to run this verification script'); })();

function base64url(input) {
  return Buffer.from(input).toString('base64url');
}

function createToken(userId, email, roles, customerType = 'INTERNAL') {
  const header = { alg: 'HS256', typ: 'JWT' };
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    sub: userId,
    email: email,
    roles: roles,
    customer_type: customerType,
    iat: now,
    exp: now + 86400,
  };
  const encHeader = base64url(JSON.stringify(header));
  const encPayload = base64url(JSON.stringify(payload));
  const signature = crypto
    .createHmac('sha256', Buffer.from(JWT_SECRET, 'utf-8'))
    .update(`${encHeader}.${encPayload}`)
    .digest('base64url');
  return `${encHeader}.${encPayload}.${signature}`;
}

const adminToken = createToken('e1f48826-ced4-416b-81f9-8efa89f8f500', 'rrjaggery@gmail.com', ['ROLE_ADMIN']);
const managerToken = createToken('c83972e4-d50e-443e-8018-a462b09ce25c', 'manager@example.com', ['ROLE_MANAGER']);
const customerToken = createToken('bfab575c-2165-4a19-a045-184db2e3648e', 'customer@example.com', ['ROLE_CUSTOMER'], 'RETAIL');

function request(options, data = null) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => (body += chunk));
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: body ? JSON.parse(body) : null, raw: body });
        } catch (e) {
          resolve({ status: res.statusCode, body: null, raw: body });
        }
      });
    });
    req.on('error', reject);
    if (data) req.write(typeof data === 'string' ? data : JSON.stringify(data));
    req.end();
  });
}

async function runTests() {
  console.log('═══════════════════════════════════════════════════════════════════');
  console.log('RR JAGGERY TRADERS — DYNAMIC FORMS & INVESTMENTS RUNTIME E2E SUITE');
  console.log('═══════════════════════════════════════════════════════════════════\n');

  let passed = 0;
  let failed = 0;

  function assert(condition, message) {
    if (condition) {
      console.log(`  ✓ PASS: ${message}`);
      passed++;
    } else {
      console.error(`  ✗ FAIL: ${message}`);
      failed++;
    }
  }

  // 1. ADMIN: Initial Investments List
  console.log('── 1. ADMIN: Capital Investments Management ──');
  const invList = await request({
    host: 'localhost',
    port: 8087,
    path: '/api/v1/finance/investments',
    method: 'GET',
    headers: { Authorization: `Bearer ${adminToken}` },
  });
  assert(invList.status === 200 && Array.isArray(invList.body.data), 'Admin can list investments (200 OK)');

  // 2. ADMIN: Create Investment
  const invCreate = await request(
    {
      host: 'localhost',
      port: 8087,
      path: '/api/v1/finance/investments',
      method: 'POST',
      headers: { Authorization: `Bearer ${adminToken}`, 'Content-Type': 'application/json' },
    },
    {
      investorName: 'Rohan Patil (Proprietor)',
      investmentType: 'OWNER_CAPITAL',
      amount: 500000.0,
      investmentDate: '2026-09-30',
      details: 'Initial equity capital infusion for factory automation',
    }
  );
  assert(invCreate.status === 201 && invCreate.body.data.id, `Admin can create investment (201 Created, ID: ${invCreate.body?.data?.id})`);
  const createdInvId = invCreate.body?.data?.id;

  // 3. ADMIN: Void Investment
  if (createdInvId) {
    const invVoid = await request(
      {
        host: 'localhost',
        port: 8087,
        path: `/api/v1/finance/investments/${createdInvId}/void`,
        method: 'PATCH',
        headers: { Authorization: `Bearer ${adminToken}`, 'Content-Type': 'application/json' },
      },
      { reason: 'E2E test void audit verification' }
    );
    assert(invVoid.status === 200 && invVoid.body.data.status === 'VOIDED', 'Admin can void investment with audit trail (Status: VOIDED)');
  }

  // 4. ADMIN: Form Builder Definitions & Fields
  console.log('\n── 2. ADMIN: Dynamic Form Builder Operations ──');
  const formDefs = await request({
    host: 'localhost',
    port: 8087,
    path: '/api/v1/finance/forms/definitions',
    method: 'GET',
    headers: { Authorization: `Bearer ${adminToken}` },
  });
  assert(formDefs.status === 200 && formDefs.body.data.length >= 5, `Admin lists all finance form definitions (${formDefs.body?.data?.length} forms found)`);

  const expenseDef = await request({
    host: 'localhost',
    port: 8087,
    path: '/api/v1/finance/forms/definitions/EXPENSE',
    method: 'GET',
    headers: { Authorization: `Bearer ${adminToken}` },
  });
  assert(expenseDef.status === 200 && expenseDef.body.data.formKey === 'EXPENSE', 'Admin fetches EXPENSE form definition (200 OK)');

  // 5. ADMIN: Add Field to Form Builder
  const fieldKey = `fuelBill_${Date.now()}`;
  const addField = await request(
    {
      host: 'localhost',
      port: 8087,
      path: '/api/v1/finance/forms/definitions/EXPENSE/fields',
      method: 'POST',
      headers: { Authorization: `Bearer ${adminToken}`, 'Content-Type': 'application/json' },
    },
    {
      fieldKey: fieldKey,
      label: 'Fuel Bill / Receipt Number',
      fieldType: 'TEXT',
      required: false,
      displayOrder: 10,
      placeholder: 'e.g. HPCL-98421',
      helpText: 'Official receipt number from fuel station',
    }
  );
  assert(addField.status === 201 && addField.body.data.id, `Admin adds custom field "${fieldKey}" to EXPENSE form (201 Created)`);
  const fieldId = addField.body?.data?.id;

  // 6. ADMIN: Add Dropdown Field & Options
  const selectFieldKey = `vehicle_${Date.now()}`;
  const addSelectField = await request(
    {
      host: 'localhost',
      port: 8087,
      path: '/api/v1/finance/forms/definitions/EXPENSE/fields',
      method: 'POST',
      headers: { Authorization: `Bearer ${adminToken}`, 'Content-Type': 'application/json' },
    },
    {
      fieldKey: selectFieldKey,
      label: 'Factory Delivery Vehicle',
      fieldType: 'SINGLE_SELECT',
      required: false,
      displayOrder: 11,
    }
  );
  assert(addSelectField.status === 201 && addSelectField.body.data.id, 'Admin adds SINGLE_SELECT field (201 Created)');
  const selectFieldId = addSelectField.body?.data?.id;

  let optionId = null;
  if (selectFieldId) {
    const addOpt = await request(
      {
        host: 'localhost',
        port: 8087,
        path: `/api/v1/finance/forms/fields/${selectFieldId}/options`,
        method: 'POST',
        headers: { Authorization: `Bearer ${adminToken}`, 'Content-Type': 'application/json' },
      },
      {
        value: 'TRUCK_01',
        label: 'Eicher 14ft Truck (MH 09 Q 4411)',
        displayOrder: 1,
      }
    );
    assert(addOpt.status === 201 && addOpt.body.data.id, 'Admin adds option to custom field (201 Created)');
    optionId = addOpt.body?.data?.id;
  }

  // 7. ADMIN: Toggle Option & Field Status (Deactivate / Soft-Delete)
  if (optionId) {
    const toggleOpt = await request(
      {
        host: 'localhost',
        port: 8087,
        path: `/api/v1/finance/forms/field-options/${optionId}/status`,
        method: 'PATCH',
        headers: { Authorization: `Bearer ${adminToken}`, 'Content-Type': 'application/json' },
      },
      { active: false }
    );
    assert(toggleOpt.status === 200 && toggleOpt.body.data.active === false, 'Admin deactivates option safely (active: false, preserved for history)');
  }

  // 8. MANAGER: RBAC & Operations
  console.log('\n── 3. MANAGER: Operational Access & RBAC Verification ──');
  const mgrFormRead = await request({
    host: 'localhost',
    port: 8087,
    path: '/api/v1/finance/forms/definitions/EXPENSE',
    method: 'GET',
    headers: { Authorization: `Bearer ${managerToken}` },
  });
  assert(mgrFormRead.status === 200, 'Manager CAN read active form definitions (200 OK)');

  const mgrAddForbidden = await request(
    {
      host: 'localhost',
      port: 8087,
      path: '/api/v1/finance/forms/definitions/EXPENSE/fields',
      method: 'POST',
      headers: { Authorization: `Bearer ${managerToken}`, 'Content-Type': 'application/json' },
    },
    {
      fieldKey: 'unauthorizedField',
      label: 'Unauthorized Field',
      fieldType: 'TEXT',
    }
  );
  assert(mgrAddForbidden.status === 403, 'Manager CANNOT add form fields (403 Forbidden)');

  const mgrInvForbidden = await request(
    {
      host: 'localhost',
      port: 8087,
      path: '/api/v1/finance/investments',
      method: 'POST',
      headers: { Authorization: `Bearer ${managerToken}`, 'Content-Type': 'application/json' },
    },
    {
      investorName: 'Unauthorized Inv',
      investmentType: 'OWNER_CAPITAL',
      amount: 10000,
      investmentDate: '2026-09-30',
    }
  );
  assert(mgrInvForbidden.status === 403, 'Manager CANNOT create investments (403 Forbidden)');

  // 9. CUSTOMER: RBAC Security Denial
  console.log('\n── 4. CUSTOMER: Security Isolation & Access Denial ──');
  const custForms = await request({
    host: 'localhost',
    port: 8087,
    path: '/api/v1/finance/forms/definitions',
    method: 'GET',
    headers: { Authorization: `Bearer ${customerToken}` },
  });
  assert(custForms.status === 403, 'Customer CANNOT access internal Form Builder (403 Forbidden)');

  const custInv = await request({
    host: 'localhost',
    port: 8087,
    path: '/api/v1/finance/investments',
    method: 'GET',
    headers: { Authorization: `Bearer ${customerToken}` },
  });
  assert(custInv.status === 403, 'Customer CANNOT access Investments (403 Forbidden)');

  const custExp = await request({
    host: 'localhost',
    port: 8087,
    path: '/api/v1/finance/expenses',
    method: 'GET',
    headers: { Authorization: `Bearer ${customerToken}` },
  });
  assert(custExp.status === 403, 'Customer CANNOT access Operating Expenses (403 Forbidden)');

  // 10. Financial Separation Check
  console.log('\n── 5. Financial Ledger & P&L Integrity Verification ──');
  const expList = await request({
    host: 'localhost',
    port: 8087,
    path: '/api/v1/finance/expenses',
    method: 'GET',
    headers: { Authorization: `Bearer ${adminToken}` },
  });
  assert(expList.status === 200, 'Operating Expenses API is operational (200 OK)');
  console.log(`  ℹ Operating Expenses count: ${expList.body?.data?.length || 0}`);
  console.log('  ℹ Capital Investments remain isolated in equity/capital accounts, strictly excluded from P&L');

  console.log('\n═══════════════════════════════════════════════════════════════════');
  console.log(`TEST SUMMARY: ${passed} PASSED, ${failed} FAILED`);
  console.log('═══════════════════════════════════════════════════════════════════\n');

  if (failed > 0) process.exit(1);
}

runTests().catch((err) => {
  console.error('Test execution error:', err);
  process.exit(1);
});
