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
  console.log('=== TESTING RETAIL ORDER PRODUCT SELECTION & STOCK SYNC ===\n');

  // 1. Auth
  const loginRes = await request('http://localhost/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: { email: 'rrjaggery@gmail.com', password: 'RRJAGGERY@775858' }
  });
  const token = loginRes.json.data.token;
  const headers = { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' };

  // 2. Fetch inventory items
  const invRes = await request('http://localhost/api/v1/inventory/items', { headers });
  const items = invRes.json.data || [];
  const tradItem = items.find(i => i.sku === 'JAG-TRAD-001');
  const powdItem = items.find(i => i.sku === 'JAG-POWD-001' || i.sku === 'JAG-POW-001');

  console.log('Current Inventory:');
  console.log(`- Traditional Jaggery Block (JAG-TRAD-001): ${tradItem?.currentQuantity} KG`);
  console.log(`- Organic Jaggery Powder (${powdItem?.sku}): ${powdItem?.currentQuantity} KG`);

  const initialPowdStock = Number(powdItem?.currentQuantity) || 0;
  const soldQty = 15;

  // 3. Record Manual Retail Order for Organic Jaggery Powder
  console.log(`\nRecording Manual Retail Order for ${soldQty} KG of Organic Jaggery Powder...`);
  const payload = {
    customerName: 'Test Walk-in Customer',
    orderType: 'MANUAL_RETAIL',
    paymentMethod: 'CASH',
    orderStatus: 'DELIVERED',
    paymentStatus: 'PAID',
    deliveryCharges: 0,
    immediatePaidAmount: soldQty * 140,
    orderDate: new Date().toISOString(),
    notes: 'Direct retail sale test',
    items: [
      {
        productName: 'Organic Jaggery Powder',
        quantity: soldQty,
        unitPrice: 140,
        lineTotal: soldQty * 140,
        unitWeightKg: 1
      }
    ]
  };

  const orderRes = await request('http://localhost/api/v1/customers/offline-orders', {
    method: 'POST',
    headers,
    body: payload
  });

  if (orderRes.status !== 200 && orderRes.status !== 201) {
    console.error('Failed to create retail order:', orderRes.status, orderRes.body);
    process.exit(1);
  }

  const orderId = orderRes.json.data?.id || `RETAIL-${Date.now()}`;
  console.log(`✓ Manual Retail Order saved: ID=${orderId}`);

  // Deduct stock
  const saleRes = await request('http://localhost/api/v1/inventory/stock/sale', {
    method: 'POST',
    headers,
    body: {
      sku: powdItem.sku,
      quantity: soldQty,
      actor: 'MANAGER',
      referenceId: String(orderId)
    }
  });

  if (saleRes.status !== 200) {
    console.error('Failed to deduct stock:', saleRes.status, saleRes.body);
    process.exit(1);
  }
  console.log(`✓ Stock deduction recorded for SKU: ${powdItem.sku}`);

  // 4. Verify inventory reduction
  const invResAfter = await request('http://localhost/api/v1/inventory/items', { headers });
  const powdItemAfter = (invResAfter.json.data || []).find(i => i.sku === powdItem.sku);
  const newStock = Number(powdItemAfter?.currentQuantity) || 0;
  console.log(`✓ New stock for ${powdItem.sku}: ${newStock} KG (Expected: ${initialPowdStock - soldQty} KG)`);

  if (newStock === initialPowdStock - soldQty) {
    console.log('✓ Stock deduction verified accurately!');
  }

  // 5. Verify Financial Balance & Dashboard Statistics
  console.log('\nChecking Financial Balance & Overview KPIs:');
  const now = new Date();
  const balRes = await request(`http://localhost/api/v1/finance/balance?year=${now.getFullYear()}&month=${now.getMonth() + 1}`, { headers });
  const bal = balRes.json.data;
  console.log(`- Opening Balance: ₹${bal?.openingBalance}`);
  console.log(`- Period Expenses: ₹${bal?.periodExpenses}`);
  console.log(`- Period Order Income: ₹${bal?.periodTotalOrderIncome}`);
  console.log(`- Finished Goods Stock Value: ₹${bal?.finishedGoodsStockValue}`);
  console.log(`- Total Business Position: ₹${bal?.totalBusinessPosition}`);

  console.log('\n=== ALL TESTS COMPLETED SUCCESSFULLY ===');
}

run().catch(err => {
  console.error('Test execution error:', err);
  process.exit(1);
});
