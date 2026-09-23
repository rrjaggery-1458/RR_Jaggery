const { spawn } = require('child_process');
const http = require('http');
const path = require('path');
const fs = require('fs');

const services = [
  { name: 'auth-service', port: 8081, jar: path.resolve('services/auth-service/target/auth-service-1.0.0-SNAPSHOT.jar'), path: '/api/v1/auth/health' },
  { name: 'commerce-service', port: 8082, jar: path.resolve('services/commerce-service/target/commerce-service-1.0.0-SNAPSHOT.jar'), path: '/api/v1/commerce/health' },
  { name: 'customer-ledger-service', port: 8083, jar: path.resolve('services/customer-ledger-service/target/customer-ledger-service-1.0.0-SNAPSHOT.jar'), path: '/api/v1/customers/health' },
  { name: 'inventory-service', port: 8084, jar: path.resolve('services/inventory-service/target/inventory-service-1.0.0-SNAPSHOT.jar'), path: '/api/v1/inventory/health' },
  { name: 'procurement-service', port: 8085, jar: path.resolve('services/procurement-service/target/procurement-service-1.0.0-SNAPSHOT.jar'), path: '/api/v1/procurement/health' },
  { name: 'production-service', port: 8086, jar: path.resolve('services/production-service/target/production-service-1.0.0-SNAPSHOT.jar'), path: '/api/v1/production/health' },
  { name: 'finance-service', port: 8087, jar: path.resolve('services/finance-service/target/finance-service-1.0.0-SNAPSHOT.jar'), path: '/api/v1/finance/health' },
  { name: 'notification-service', port: 8088, jar: path.resolve('services/notification-service/target/notification-service-1.0.0-SNAPSHOT.jar'), path: '/api/v1/notifications/health' }
];

function checkHealth(svc) {
  return new Promise((resolve) => {
    const req = http.get({
      hostname: 'localhost',
      port: svc.port,
      path: svc.path,
      timeout: 2000
    }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          const json = JSON.parse(body);
          if (res.statusCode === 200 && json.success === true) {
            resolve({ ok: true, data: json.data });
          } else {
            resolve({ ok: false, error: `HTTP ${res.statusCode}: ${body}` });
          }
        } catch (e) {
          resolve({ ok: false, error: e.message });
        }
      });
    });

    req.on('error', (e) => resolve({ ok: false, error: e.message }));
    req.on('timeout', () => { req.destroy(); resolve({ ok: false, error: 'Timeout' }); });
  });
}

async function main() {
  console.log('=== RR JAGGERY TRADERS — BACKEND RUNNER ===');
  const procs = [];

  for (const svc of services) {
    // Check if already running
    const health = await checkHealth(svc);
    if (health.ok) {
      console.log(`[ALREADY RUNNING] ${svc.name} on port ${svc.port}`);
      continue;
    }

    console.log(`[STARTING] ${svc.name} on port ${svc.port}...`);
    const child = spawn('java', ['-Xms64m', '-Xmx128m', '-XX:TieredStopAtLevel=1', '-jar', svc.jar], {
      stdio: 'pipe',
      detached: false
    });

    child.stdout.on('data', (d) => {
      const s = d.toString();
      if (s.includes('Started ') || s.includes('Tomcat started on port')) {
        console.log(`  -> ${svc.name}: Started successfully.`);
      }
    });

    child.stderr.on('data', (d) => {
      const s = d.toString();
      if (s.includes('Exception') || s.includes('ERROR')) {
        console.error(`  -> [ERROR] ${svc.name}: ${s.trim()}`);
      }
    });

    procs.push({ name: svc.name, proc: child });
    // Small stagger between JVM boots
    await new Promise(r => setTimeout(r, 1200));
  }

  console.log('\nWaiting for all 8 services to report UP...');
  let allHealthy = false;
  let attempts = 0;

  while (!allHealthy && attempts < 25) {
    attempts++;
    await new Promise(r => setTimeout(r, 2000));
    let healthyCount = 0;

    for (const svc of services) {
      const h = await checkHealth(svc);
      if (h.ok) {
        healthyCount++;
      }
    }

    console.log(`Check ${attempts}: ${healthyCount} / ${services.length} services healthy`);
    if (healthyCount === services.length) {
      allHealthy = true;
    }
  }

  console.log('\n=== FINAL HEALTH CHECK MATRIX ===');
  for (const svc of services) {
    const h = await checkHealth(svc);
    if (h.ok) {
      console.log(`[PASS] ${svc.name.padEnd(26)} (:${svc.port}) -> Status: ${h.data.status} | Schema: ${h.data.schema} | Version: ${h.data.version}`);
    } else {
      console.log(`[FAIL] ${svc.name.padEnd(26)} (:${svc.port}) -> Error: ${h.error}`);
    }
  }

  if (allHealthy) {
    console.log('\nALL 8 SPRING BOOT SERVICES ARE RUNNING AND HEALTHY!');
  } else {
    console.log('\nSome services did not reach healthy state.');
  }

  // Keep script alive to hold child processes
  process.on('SIGINT', () => {
    console.log('\nStopping all services...');
    procs.forEach(p => p.proc.kill());
    process.exit(0);
  });
}

main().catch(console.error);
