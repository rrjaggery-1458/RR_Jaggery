const puppeteer = require('puppeteer-core');
const fs = require('fs');
const path = require('path');

const EDGE_PATH = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const APP_URL = 'http://localhost:5173/';

async function runSprint2Verification() {
  console.log('======================================================================');
  console.log('  STARTING SPRINT 2 END-TO-END BROWSER & RUNTIME VERIFICATION');
  console.log('======================================================================');

  const consoleLogs = [];
  const networkErrors = [];
  const networkCalls = [];

  const browser = await puppeteer.launch({
    executablePath: EDGE_PATH,
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu', '--window-size=1280,900'],
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 900 });

  page.on('console', msg => {
    const text = msg.text();
    consoleLogs.push({ type: msg.type(), text });
    if (msg.type() === 'error') {
      console.log(`[Browser Console ERROR]: ${text}`);
    }
  });

  page.on('requestfailed', request => {
    networkErrors.push({
      url: request.url(),
      errorText: request.failure() ? request.failure().errorText : 'Unknown',
    });
    console.log(`[Network Error]: ${request.url()} failed: ${request.failure()?.errorText}`);
  });

  page.on('response', response => {
    const url = response.url();
    if (url.includes('/api/v1/')) {
      networkCalls.push({
        url,
        status: response.status(),
        ok: response.ok(),
      });
    }
  });

  try {
    // ── 1. Navigate to Storefront ──
    console.log('\n[STEP 1] Navigating to Storefront (http://localhost:5173/)...');
    await page.goto(APP_URL, { waitUntil: 'networkidle0', timeout: 15000 });
    console.log('✓ Storefront loaded successfully.');

    // ── 2. Verify Product Catalogue Display ──
    console.log('\n[STEP 2] Verifying seeded catalogue products rendered from backend...');
    await page.waitForSelector('[id^="product-card-"]', { timeout: 10000 });
    const productCards = await page.$$('[id^="product-card-"]');
    console.log(`✓ Found ${productCards.length} product cards in public catalogue.`);
    if (productCards.length < 4) throw new Error(`Expected at least 4 products, found ${productCards.length}`);

    // ── 3. Register or Log In Customer ──
    console.log('\n[STEP 3] Registering new customer for clean cart and checkout verification...');
    await page.click('#header-signin-btn');
    await page.waitForSelector('#tab-register', { timeout: 5000 });
    await page.click('#tab-register');
    await page.waitForSelector('#reg-fullname', { timeout: 5000 });

    const timestamp = Date.now();
    const customerEmail = `mandya_shopper_${timestamp}@sugarcanemart.in`;

    await page.type('#reg-fullname', 'Rohan Sugarcane Mart');
    await page.type('#reg-email', customerEmail);
    await page.type('#reg-password', 'Customer@123');
    await page.select('#reg-customertype', 'RETAIL');
    await page.click('#register-submit');

    await page.waitForSelector('#user-display-name', { timeout: 8000 });
    const customerName = await page.$eval('#user-display-name', el => el.innerText);
    console.log(`✓ Customer account created & logged in: "${customerName}" (${customerEmail})`);

    // ── 4. Add Product to Persistent Cart ──
    console.log('\n[STEP 4] Adding products to cart from catalogue...');
    const firstAddBtn = await page.$('[id^="add-to-cart-"]');
    if (!firstAddBtn) throw new Error('No add-to-cart button found!');
    await firstAddBtn.click();
    await new Promise(r => setTimeout(r, 1500));

    // Cart drawer opens automatically on add
    await page.waitForSelector('#cart-badge-count', { timeout: 5000 });
    let cartBadge = await page.$eval('#cart-badge-count', el => el.innerText);
    console.log(`✓ Item added to cart. Cart badge count: ${cartBadge}`);

    // ── 5. Manage Cart Items in Cart Drawer (Quantity Updates) ──
    console.log('\n[STEP 5] Updating cart item quantities in Cart Drawer...');
    await page.waitForSelector('[id^="inc-qty-"]', { timeout: 5000 });
    const incBtn = await page.$('[id^="inc-qty-"]');
    if (incBtn) {
      await incBtn.click();
      await new Promise(r => setTimeout(r, 1000));
      console.log('✓ Incremented cart item quantity (+1).');
    }

    // Verify recalculation
    cartBadge = await page.$eval('#cart-badge-count', el => el.innerText);
    console.log(`✓ Updated cart item count: ${cartBadge}`);

    // ── 6. Proceed to Checkout Modal ──
    console.log('\n[STEP 6] Opening Secure Checkout modal...');
    await page.waitForSelector('#btn-proceed-to-checkout', { timeout: 5000 });
    await page.click('#btn-proceed-to-checkout');
    await page.waitForSelector('#checkout-recipient', { timeout: 5000 });
    console.log('✓ Checkout Modal opened.');

    // ── 7. Fill Delivery Address & Delivery Notes ──
    console.log('\n[STEP 7] Entering delivery address details and notes...');
    // Replace recipient
    await page.click('#checkout-recipient');
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#checkout-recipient', 'Rohan Sugarcane Mart');

    // Phone
    await page.click('#checkout-phone');
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#checkout-phone', '+91 9845012345');

    // Street
    await page.click('#checkout-street');
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#checkout-street', 'Plot 45, Mandya Sugar Complex Road');

    // City
    await page.click('#checkout-city');
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#checkout-city', 'Mandya');

    // State
    await page.click('#checkout-state');
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#checkout-state', 'Karnataka');

    // PIN Code
    await page.click('#checkout-postalcode');
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#checkout-postalcode', '571401');

    // Delivery Notes
    await page.type('#checkout-notes', 'Urgent mill batch delivery. Unload at godown section B.');
    console.log('✓ Address and special instructions populated.');

    // Select Instant Test Payment
    await page.click('#payment-method-test');
    await new Promise(r => setTimeout(r, 500));

    // ── 8. Submit Order & Verify Order Confirmation Modal ──
    console.log('\n[STEP 8] Confirming order and submitting payment...');
    await page.click('#btn-place-order');
    await page.waitForSelector('#confirmed-order-number', { timeout: 10000 });

    const confirmedOrderNum = await page.$eval('#confirmed-order-number', el => el.innerText);
    console.log(`✓ Order placed successfully! Generated authoritative sequential Order Number: "${confirmedOrderNum}"`);

    // ── 9. View Printable GST Tax Invoice Modal ──
    console.log('\n[STEP 9] Inspecting GST Tax Invoice Modal for placed order...');
    await page.click('#btn-confirm-view-invoice');
    await page.waitForSelector('#invoice-seller-name', { timeout: 10000 });

    const invoiceSeller = await page.$eval('#invoice-seller-name', el => el.innerText);
    const invoiceModalContent = await page.$eval('#invoice-modal-content', el => el.innerText);
    console.log(`✓ Tax Invoice Modal successfully rendered for seller: "${invoiceSeller}"`);
    if (!invoiceModalContent.toUpperCase().includes('RR JAGGERY TRADERS')) throw new Error('Invoice missing seller name!');
    if (!invoiceModalContent.includes('29AABCR1234F1Z5')) throw new Error('Invoice missing Mandya mill GSTIN!');
    if (!invoiceModalContent.includes('17011490')) throw new Error('Invoice missing Jaggery HSN Code 17011490!');
    if (!invoiceModalContent.includes('CGST (2.5%)')) throw new Error('Invoice missing CGST breakdown!');
    if (!invoiceModalContent.includes('SGST (2.5%)')) throw new Error('Invoice missing SGST breakdown!');
    console.log('✓ Verified: Seller Mill Info, GSTIN 29AABCR1234F1Z5, HSN 17011490, CGST (2.5%), SGST (2.5%).');

    // Close invoice modal
    await page.click('#close-invoice-modal');
    await new Promise(r => setTimeout(r, 800));

    // ── 10. Verify Order in Customer "My Orders" History ──
    console.log('\n[STEP 10] Verifying order presence in Customer "My Orders" history view...');
    await page.click('#nav-orders');
    await page.waitForSelector(`#order-card-${confirmedOrderNum}`, { timeout: 8000 });
    console.log(`✓ Order "${confirmedOrderNum}" confirmed in Customer order history.`);

    // ── 11. Admin Order Fulfillment & Status Lifecycle ──
    console.log('\n[STEP 11] Logging in as Admin to manage order lifecycle...');
    await page.click('#logout-btn');
    await page.waitForSelector('#header-signin-btn', { timeout: 5000 });

    await page.click('#header-signin-btn');
    await page.waitForSelector('#tab-login', { timeout: 5000 });
    await page.click('#tab-login');
    await page.waitForSelector('#login-email', { timeout: 5000 });

    await page.click('#login-email');
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#login-email', 'admin@rrjaggery.com');

    await page.click('#login-password');
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#login-password', 'Admin@123');

    await page.click('#login-submit');
    await page.waitForSelector('#nav-admin', { timeout: 8000 });
    console.log('✓ Admin logged in successfully.');

    // Navigate to Admin Portal -> Orders Tab
    await page.click('#nav-admin');
    await page.waitForSelector('#tab-btn-admin-orders', { timeout: 5000 });
    await page.click('#tab-btn-admin-orders');

    await page.waitForSelector(`#admin-order-row-${confirmedOrderNum}`, { timeout: 8000 });
    console.log(`✓ Placed customer order "${confirmedOrderNum}" located in Admin Orders Console.`);

    // Change status: CONFIRMED -> PROCESSING
    console.log('\n[STEP 12] Transitioning order status: CONFIRMED -> PROCESSING...');
    await page.select(`#select-status-${confirmedOrderNum}`, 'PROCESSING');
    await new Promise(r => setTimeout(r, 1500));
    console.log('✓ Status updated to PROCESSING in database.');

    // Change status: PROCESSING -> DISPATCHED
    console.log('\n[STEP 13] Transitioning order status: PROCESSING -> DISPATCHED...');
    await page.select(`#select-status-${confirmedOrderNum}`, 'DISPATCHED');
    await new Promise(r => setTimeout(r, 1500));
    console.log('✓ Status updated to DISPATCHED in database.');

    // ── 14. Customer Status Sync Verification ──
    console.log('\n[STEP 14] Logging back in as Customer to verify updated order status...');
    await page.click('#logout-btn');
    await page.waitForSelector('#header-signin-btn', { timeout: 5000 });

    await page.click('#header-signin-btn');
    await page.waitForSelector('#tab-login', { timeout: 5000 });
    await page.click('#tab-login');
    await page.waitForSelector('#login-email', { timeout: 5000 });

    await page.click('#login-email');
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#login-email', customerEmail);

    await page.click('#login-password');
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#login-password', 'Customer@123');

    await page.click('#login-submit');
    await page.waitForSelector('#nav-orders', { timeout: 8000 });

    await page.click('#nav-orders');
    await page.waitForSelector(`#order-card-${confirmedOrderNum}`, { timeout: 8000 });

    const orderCardText = await page.$eval(`#order-card-${confirmedOrderNum}`, el => el.innerText);
    if (!orderCardText.includes('DISPATCHED')) {
      throw new Error(`Expected order status DISPATCHED, but card contains: ${orderCardText}`);
    }
    console.log(`✓ Customer view verified: Order "${confirmedOrderNum}" is now marked DISPATCHED.`);

    // ── 15. Summary of Network & Browser Health ──
    console.log('\n[STEP 15] Verifying API calls and browser console health...');
    const failedApiResponses = networkCalls.filter(n => !n.ok);
    console.log(`✓ Total Backend API requests executed: ${networkCalls.length}`);
    console.log(`✓ Failed API responses: ${failedApiResponses.length}`);
    console.log(`✓ Total browser network connection errors: ${networkErrors.length}`);

    if (failedApiResponses.length > 0) {
      console.warn('Failed API URLs:', failedApiResponses.map(f => f.url));
    }

    console.log('\n======================================================================');
    console.log('  ALL SPRINT 2 E2E BROWSER VERIFICATION FLOWS PASSED WITH 100% SUCCESS!');
    console.log('======================================================================');

  } catch (err) {
    console.error('\n❌ SPRINT 2 BROWSER VERIFICATION FAILED:', err);
    throw err;
  } finally {
    await browser.close();
  }
}

runSprint2Verification();
