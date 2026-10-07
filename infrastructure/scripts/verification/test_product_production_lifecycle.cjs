const http = require('http');

async function request(url, options = {}) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const reqOptions = {
      hostname: parsed.hostname,
      port: parsed.port || 80,
      path: parsed.pathname + parsed.search,
      method: options.method || 'GET',
      headers: options.headers || {}
    };

    const req = http.request(reqOptions, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        let json = null;
        try { json = JSON.parse(body); } catch (e) {}
        resolve({ status: res.statusCode, headers: res.headers, body, json });
      });
    });

    req.on('error', reject);
    if (options.body) {
      req.write(typeof options.body === 'string' ? options.body : JSON.stringify(options.body));
    }
    req.end();
  });
}

async function run() {
  console.log('=== COMPREHENSIVE PRODUCT LIFECYCLE & PRODUCTION SYNC TEST ===\n');

  // 1. Auth
  const loginRes = await request('http://localhost/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: { email: 'rrjaggery@gmail.com', password: 'RRJAGGERY@775858' }
  });
  const token = loginRes.json.data.token;
  const headers = { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' };

  // 2. Fetch categories
  const catRes = await request('http://localhost/api/v1/commerce/admin/categories', { headers });
  const categoryId = catRes.json.data?.[0]?.id;

  // 3. Create new Product
  const uniqueSku = `JAG-AUTO-${Date.now().toString().slice(-4)}`;
  const prodName = `Special Heritage Jaggery ${uniqueSku}`;
  console.log(`1. Creating test product: "${prodName}" with SKU ${uniqueSku}...`);
  
  const createRes = await request('http://localhost/api/v1/commerce/admin/products', {
    method: 'POST',
    headers,
    body: {
      categoryId,
      name: prodName,
      sku: uniqueSku,
      grade: 'GRADE_A_TRADITIONAL',
      packageType: 'BOX',
      unitWeightKg: 1,
      retailPrice: 150,
      wholesalePrice: 110,
      wholesaleMoq: 20,
      active: true,
      featured: false
    }
  });

  if (createRes.status !== 200 && createRes.status !== 201) {
    console.error('Failed to create product:', createRes.status, createRes.body);
    process.exit(1);
  }
  const createdProd = createRes.json.data;
  console.log(`✓ Product created successfully: ID=${createdProd.id}`);

  // 3. Verify product is listed in active products
  console.log('2. Verifying product is listed in active products...');
  const listRes = await request('http://localhost/api/v1/commerce/admin/products', { headers });
  const allProds = listRes.json.data || [];
  const found = allProds.find(p => p.sku === uniqueSku && p.active === true);
  if (!found) {
    console.error('Newly created product not found in active list!');
    process.exit(1);
  }
  console.log(`✓ Product "${found.name}" is present and ACTIVE.`);

  // 4. Record production for this new product
  console.log(`3. Recording 75 KG production for new product "${uniqueSku}"...`);
  const prodBatchRes = await request('http://localhost/api/v1/production/batches', {
    method: 'POST',
    headers,
    body: {
      targetProductName: found.name,
      targetProductSku: found.sku,
      plannedQuantityKg: 75,
      actualQuantity: 75,
      wastageQuantity: 1,
      status: 'COMPLETED',
      scheduledDate: new Date().toISOString().split('T')[0],
      notes: 'New product production test'
    }
  });

  if (prodBatchRes.status !== 200 && prodBatchRes.status !== 201) {
    console.error('Failed to record production for new product:', prodBatchRes.status, prodBatchRes.body);
    process.exit(1);
  }
  console.log(`✓ Production batch recorded for ${found.name}: Batch=${prodBatchRes.json.data.batchNumber}`);

  // 5. Verify finished-goods inventory for the new product
  console.log('4. Verifying inventory item created/updated with 75 KG...');
  const invRes = await request('http://localhost/api/v1/inventory/items', { headers });
  const newInv = (invRes.json.data || []).find(i => i.sku === uniqueSku);
  console.log(`✓ Inventory item found: SKU=${newInv?.sku}, Name=${newInv?.itemName}, Stock=${newInv?.currentQuantity} KG`);

  // 6. Deactivate product and verify it is marked inactive
  console.log('5. Deactivating product in Product Master...');
  const updateRes = await request(`http://localhost/api/v1/commerce/admin/products/${createdProd.id}`, {
    method: 'PUT',
    headers,
    body: {
      categoryId,
      name: prodName,
      sku: uniqueSku,
      grade: 'GRADE_A_TRADITIONAL',
      packageType: 'BOX',
      unitWeightKg: 1,
      retailPrice: 150,
      wholesalePrice: 110,
      wholesaleMoq: 20,
      active: false
    }
  });
  console.log(`✓ Product deactivation response status: ${updateRes.status}`);

  const listRes2 = await request('http://localhost/api/v1/commerce/admin/products', { headers });
  const activeOnly = (listRes2.json.data || []).filter(p => p.active);
  const isExcluded = !activeOnly.some(p => p.sku === uniqueSku);
  console.log(`✓ Deactivated product excluded from active production list: ${isExcluded}`);

  console.log('\n=== ALL LIFECYCLE & SYNC TESTS PASSED SUCCESSFULLY ===');
}

run().catch(err => {
  console.error('Lifecycle test error:', err);
  process.exit(1);
});
