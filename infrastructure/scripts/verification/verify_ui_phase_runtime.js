const http = require('http');
const crypto = require('crypto');

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

async function verifyAll() {
  console.log('======================================================================');
  console.log('RR JAGGERY TRADERS — PRE-SPRINT 8 UI PHASE RUNTIME INTEGRITY SUITE');
  console.log('======================================================================\n');

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

  // 1. Gateway Routing Check (Port 80)
  console.log('── 1. Gateway Routing Verification (Port 80) ──');
  const gwRoot = await request({ host: 'localhost', port: 80, path: '/', method: 'GET' });
  assert(gwRoot.status === 200, 'Gateway serves index.html at http://localhost/ (200 OK)');

  // 2. Dynamic Form Builder: 7 Business Domains via Gateway
  console.log('\n── 2. Dynamic Form Builder — 7 Supported Domains via Gateway ──');
  const domains = [
    { key: 'EXPENSE', svc: 'finance' },
    { key: 'INVESTMENT', svc: 'finance' },
    { key: 'STAFF', svc: 'finance' },
    { key: 'ATTENDANCE', svc: 'finance' },
    { key: 'PAYROLL', svc: 'finance' },
    { key: 'WHOLESALE_ORDER', svc: 'customers' },
    { key: 'PRODUCTION', svc: 'production' },
  ];

  for (const d of domains) {
    const res = await request({
      host: 'localhost',
      port: 80,
      path: `/api/v1/${d.svc}/forms/definitions/${d.key}`,
      method: 'GET',
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    assert(res.status === 200 && res.body?.data?.formKey === d.key, `Admin loads form definition for domain ${d.key} via /api/v1/${d.svc}/ (200 OK)`);
  }

  // 3. Section 22 Dynamic Form Runtime Test:
  // Admin creates "UI Verification Reference" test field -> Manager consumes -> Submit -> Persist -> Admin deactivates -> Verify immutability
  console.log('\n── 3. Section 22 Dynamic Form Runtime Test ──');
  const testFieldKey = `ui_verif_${Date.now()}`;
  const addFieldRes = await request(
    {
      host: 'localhost',
      port: 80,
      path: '/api/v1/finance/forms/definitions/EXPENSE/fields',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${adminToken}`,
      },
    },
    {
      fieldKey: testFieldKey,
      label: 'UI Verification Reference',
      fieldType: 'TEXT',
      required: false,
      placeholder: 'Enter verification ref...',
      helpText: 'Automated UI test field',
      displayOrder: 99,
    }
  );
  assert(addFieldRes.status === 201, `Admin created test field "${testFieldKey}" in EXPENSE (201 Created)`);
  const createdFieldId = addFieldRes.body?.data?.id;

  // Manager reads active EXPENSE form definition
  const mgrFormRes = await request({
    host: 'localhost',
    port: 80,
    path: '/api/v1/finance/forms/definitions/EXPENSE',
    method: 'GET',
    headers: { Authorization: `Bearer ${managerToken}` },
  });
  const foundInMgr = mgrFormRes.body?.data?.fields?.some((f) => f.fieldKey === testFieldKey && f.active);
  assert(foundInMgr, 'Manager receives new dynamic field "UI Verification Reference" in active definition');

  // Manager creates Operating Expense with dynamic answer
  const testVal = `RUNTIME-VAL-${Date.now()}`;
  const expenseRes = await request(
    {
      host: 'localhost',
      port: 80,
      path: '/api/v1/finance/expenses',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${managerToken}`,
      },
    },
    {
      category: 'Fuel / Diesel',
      amount: 1750.0,
      paymentMethod: 'CASH',
      expenseDate: new Date().toISOString().split('T')[0],
      payeeVendor: 'Mandya Petroleum',
      description: 'Boiler fuel test for dynamic runtime verification',
    }
  );
  assert(expenseRes.status === 201, 'Manager submitted Operating Expense (201 Created)');
  const expenseId = expenseRes.body?.data?.id;

  // Submit supplemental answers
  const subRes = await request(
    {
      host: 'localhost',
      port: 80,
      path: '/api/v1/finance/forms/submissions',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${managerToken}`,
      },
    },
    {
      formKey: 'EXPENSE',
      recordType: 'EXPENSE',
      recordId: expenseId,
      answers: {
        [testFieldKey]: testVal,
      },
    }
  );
  assert(subRes.status === 201, 'Manager submitted dynamic form answers for record (201 Created)');

  // Admin deactivates test field
  const deactRes = await request(
    {
      host: 'localhost',
      port: 80,
      path: `/api/v1/finance/forms/fields/${createdFieldId}/status`,
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${adminToken}`,
      },
    },
    { active: false }
  );
  assert(deactRes.status === 200 && deactRes.body?.data?.active === false, 'Admin deactivated test field (active=false)');

  // Manager reads form definition again -> field is no longer presented as active for new forms
  const mgrFormRes2 = await request({
    host: 'localhost',
    port: 80,
    path: '/api/v1/finance/forms/definitions/EXPENSE',
    method: 'GET',
    headers: { Authorization: `Bearer ${managerToken}` },
  });
  const stillActive = mgrFormRes2.body?.data?.fields?.some((f) => f.fieldKey === testFieldKey && f.active);
  assert(!stillActive, 'Deactivated field is NOT presented in active form definition for new entries');

  // Historical submission check -> answers still completely readable
  const histRes = await request({
    host: 'localhost',
    port: 80,
    path: `/api/v1/finance/forms/submissions?recordType=EXPENSE&recordId=${expenseId}`,
    method: 'GET',
    headers: { Authorization: `Bearer ${adminToken}` },
  });
  const subRecord = histRes.body?.data?.[0];
  const savedVal = subRecord?.answers?.[testFieldKey];
  assert(savedVal === testVal, `Historical record still retains custom answer "${savedVal}" after field deactivation`);

  // 4. Initial Investment End-to-End Test (Section 7)
  console.log('\n── 4. Capital Investments UI & Accounting Isolation ──');
  const createInvRes = await request(
    {
      host: 'localhost',
      port: 80,
      path: '/api/v1/finance/investments',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${adminToken}`,
      },
    },
    {
      investorName: 'Ramesh Patil (Managing Partner)',
      investmentType: 'OWNER_CAPITAL',
      amount: 750000.0,
      investmentDate: new Date().toISOString().split('T')[0],
      details: 'Initial equity injection for sugar processing mill',
    }
  );
  assert(createInvRes.status === 201, 'Admin created Capital Investment of ₹7,50,000 (201 Created)');
  const createdInvId = createInvRes.body?.data?.id;

  // Void Investment
  const voidRes = await request(
    {
      host: 'localhost',
      port: 80,
      path: `/api/v1/finance/investments/${createdInvId}/void`,
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${adminToken}`,
      },
    },
    {
      reason: 'Audit reconciliation correction',
    }
  );
  assert(voidRes.status === 200 && voidRes.body?.data?.status === 'VOIDED', 'Admin voided investment with audit log (Status: VOIDED)');

  // 5. Customer Isolation & RBAC Security Verification (Section 9)
  console.log('\n── 5. Customer RBAC Isolation ──');
  const custBuilder = await request(
    {
      host: 'localhost',
      port: 80,
      path: '/api/v1/finance/forms/definitions/EXPENSE/fields',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${customerToken}`,
      },
    },
    { fieldKey: 'hacked', label: 'Hacked', fieldType: 'TEXT' }
  );
  assert(custBuilder.status === 403, 'Customer CANNOT access Dynamic Form Builder (403 Forbidden)');

  const custInv = await request({
    host: 'localhost',
    port: 80,
    path: '/api/v1/finance/investments',
    method: 'GET',
    headers: { Authorization: `Bearer ${customerToken}` },
  });
  assert(custInv.status === 403, 'Customer CANNOT access Capital Investments (403 Forbidden)');

  const custExp = await request({
    host: 'localhost',
    port: 80,
    path: '/api/v1/finance/expenses',
    method: 'GET',
    headers: { Authorization: `Bearer ${customerToken}` },
  });
  assert(custExp.status === 403, 'Customer CANNOT access Operating Expenses (403 Forbidden)');

  // 6. Manager Isolation from Admin Features
  console.log('\n── 6. Manager RBAC Isolation ──');
  const mgrBuilder = await request(
    {
      host: 'localhost',
      port: 80,
      path: '/api/v1/finance/forms/definitions/EXPENSE/fields',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${managerToken}`,
      },
    },
    { fieldKey: 'mgr_field', label: 'Manager Field', fieldType: 'TEXT' }
  );
  assert(mgrBuilder.status === 403, 'Manager CANNOT add/modify Dynamic Form fields (403 Forbidden)');

  const mgrInv = await request({
    host: 'localhost',
    port: 80,
    path: '/api/v1/finance/investments',
    method: 'GET',
    headers: { Authorization: `Bearer ${managerToken}` },
  });
  assert(mgrInv.status === 403, 'Manager CANNOT list or access Capital Investments (403 Forbidden)');

  console.log('\n======================================================================');
  console.log(`TOTAL RUNTIME CHECKS: ${passed + failed} | PASSED: ${passed} | FAILED: ${failed}`);
  console.log('======================================================================');

  if (failed > 0) {
    process.exit(1);
  }
}

verifyAll().catch((err) => {
  console.error('Fatal error during verification:', err);
  process.exit(1);
});
