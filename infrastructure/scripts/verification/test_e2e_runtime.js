const http = require('http');

const tests = [
  { name: 'Frontend Web App', url: 'http://localhost:5173/', expectedStatus: 200 },
  { name: 'Auth Service', url: 'http://localhost:8081/api/v1/auth/health', expectedStatus: 200, schema: 'auth_schema' },
  { name: 'Commerce Service', url: 'http://localhost:8082/api/v1/commerce/health', expectedStatus: 200, schema: 'commerce_schema' },
  { name: 'Customer & Ledger Service', url: 'http://localhost:8083/api/v1/customers/health', expectedStatus: 200, schema: 'customer_schema' },
  { name: 'Inventory Service', url: 'http://localhost:8084/api/v1/inventory/health', expectedStatus: 200, schema: 'inventory_schema' },
  { name: 'Procurement Service', url: 'http://localhost:8085/api/v1/procurement/health', expectedStatus: 200, schema: 'procurement_schema' },
  { name: 'Production Service', url: 'http://localhost:8086/api/v1/production/health', expectedStatus: 200, schema: 'production_schema' },
  { name: 'Finance Service', url: 'http://localhost:8087/api/v1/finance/health', expectedStatus: 200, schema: 'finance_schema' },
  { name: 'Notification Service', url: 'http://localhost:8088/api/v1/notifications/health', expectedStatus: 200, schema: 'notification_schema' }
];

function runTest(t) {
  return new Promise((resolve) => {
    const start = Date.now();
    const req = http.get(t.url, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        const latency = Date.now() - start;
        let data = null;
        try { data = JSON.parse(body); } catch(e) {}

        const isOk = res.statusCode === t.expectedStatus && (!t.schema || (data && data.data && data.data.schema === t.schema));
        resolve({
          name: t.name,
          url: t.url,
          statusCode: res.statusCode,
          latency,
          schema: data?.data?.schema,
          status: data?.data?.status || 'OK',
          passed: isOk
        });
      });
    });
    req.on('error', (err) => resolve({ name: t.name, url: t.url, passed: false, error: err.message }));
  });
}

async function main() {
  console.log('=== RUNNING COMPLETE RUNTIME INTEGRATION TEST MATRIX ===');
  let allPass = true;
  for (const t of tests) {
    const res = await runTest(t);
    if (res.passed) {
      console.log(`[PASS] ${res.name.padEnd(28)} | HTTP ${res.statusCode} | Status: ${res.status} | Schema: ${res.schema || 'N/A'} | Latency: ${res.latency}ms`);
    } else {
      console.error(`[FAIL] ${res.name.padEnd(28)} | Error: ${res.error || res.statusCode}`);
      allPass = false;
    }
  }

  console.log('========================================================');
  if (allPass) {
    console.log('RESULT: 100% OF ALL RUNTIME ENDPOINTS PASSED SUCCESSFULLY!');
    process.exit(0);
  } else {
    console.error('RESULT: Runtime endpoint verification failed.');
    process.exit(1);
  }
}

main().catch(console.error);
