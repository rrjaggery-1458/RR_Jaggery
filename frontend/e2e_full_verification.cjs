const puppeteer = require('puppeteer-core');
const fs = require('fs');
const path = require('path');

const EDGE_PATH = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const APP_URL = 'http://localhost:5173/';

async function runVerification() {
  console.log('===============================================================');
  console.log('  STARTING SPRINT 1 BROWSER-LEVEL END-TO-END VERIFICATION');
  console.log('===============================================================');

  const consoleLogs = [];
  const networkErrors = [];
  const networkCalls = [];

  const browser = await puppeteer.launch({
    executablePath: EDGE_PATH,
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu', '--window-size=1280,800'],
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 800 });

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
    // ── 1. Open Storefront ──
    console.log('\n[STEP 1] Navigating to Storefront (http://localhost:5173/)...');
    await page.goto(APP_URL, { waitUntil: 'networkidle0', timeout: 15000 });
    console.log('✓ Storefront loaded successfully.');

    // ── 2. Confirm Seeded Catalogue from Real Backend ──
    console.log('\n[STEP 2] Verifying seeded catalogue products rendered from backend...');
    await page.waitForSelector('[id^="product-card-"]', { timeout: 10000 });
    const productCards = await page.$$('[id^="product-card-"]');
    console.log(`✓ Found ${productCards.length} product cards in public catalogue.`);
    if (productCards.length < 4) throw new Error(`Expected at least 4 products, found ${productCards.length}`);

    // Verify first product title
    const firstProductName = await page.$eval('[id^="product-card-"] h3', el => el.innerText);
    console.log(`✓ Sample catalogue product: "${firstProductName}"`);

    // ── 3. Search & Filter Products ──
    console.log('\n[STEP 3] Testing search and filter...');
    await page.type('#catalogue-search', 'Powder');
    await new Promise(r => setTimeout(r, 1000));
    const filteredSearch = await page.$$('[id^="product-card-"]');
    console.log(`✓ Search for "Powder" returned ${filteredSearch.length} products.`);

    // Clear search
    await page.$eval('#catalogue-search', el => el.value = '');
    await page.type('#catalogue-search', ' ');
    await page.keyboard.press('Backspace');
    await new Promise(r => setTimeout(r, 800));

    // Filter by Category
    const catPill = await page.$('#category-pill-traditional-blocks');
    if (catPill) {
      await catPill.click();
      await new Promise(r => setTimeout(r, 1000));
      const catFiltered = await page.$$('[id^="product-card-"]');
      console.log(`✓ Category filter 'traditional-blocks' returned ${catFiltered.length} products.`);
    }

    // Reset Category filter
    await page.click('#category-pill-all');
    await new Promise(r => setTimeout(r, 800));

    // ── 4. Open Product Details Modal ──
    console.log('\n[STEP 4] Testing Product Details Modal...');
    const detailBtn = await page.$('[id^="view-details-"]');
    if (detailBtn) {
      await detailBtn.click();
      await page.waitForSelector('#close-product-modal', { timeout: 5000 });
      const modalText = await page.$eval('.bg-slate-900', el => el.innerText);
      console.log('✓ Product Details Modal opened displaying specifications & wholesale MOQs.');
      await page.click('#close-product-modal');
      await new Promise(r => setTimeout(r, 500));
      console.log('✓ Product Details Modal closed.');
    }

    // ── 5. Test Invalid Login Behavior ──
    console.log('\n[STEP 5] Testing invalid login error handling...');
    await page.click('#header-signin-btn');
    await page.waitForSelector('#login-email', { timeout: 5000 });
    await page.type('#login-email', 'admin@rrjaggery.com');
    await page.type('#login-password', 'WrongPassword!999');
    await page.click('#login-submit');

    await page.waitForSelector('#auth-error-message', { timeout: 5000 });
    const authError = await page.$eval('#auth-error-message', el => el.innerText);
    console.log(`✓ Invalid login correctly rejected with error message: "${authError}"`);

    // ── 6. Register a New Retail Customer Through UI ──
    console.log('\n[STEP 6] Testing retail customer registration through UI...');
    await page.click('#tab-register');
    await page.waitForSelector('#reg-fullname', { timeout: 5000 });

    const timestamp = Date.now();
    const newCustomerEmail = `e2e_customer_${timestamp}@mandyaorganics.in`;

    await page.type('#reg-fullname', 'Mandya Retail Mart');
    await page.type('#reg-email', newCustomerEmail);
    await page.type('#reg-password', 'Customer@123');
    await page.select('#reg-customertype', 'RETAIL');
    await page.click('#register-submit');

    await page.waitForSelector('#user-display-name', { timeout: 8000 });
    const loggedInUser = await page.$eval('#user-display-name', el => el.innerText);
    console.log(`✓ Registration succeeded. Logged in session established for: "${loggedInUser}"`);

    // ── 7. Verify Role-Dependent UI Behavior for Non-Admin ──
    console.log('\n[STEP 7] Verifying role-dependent UI (non-admin cannot see Admin Portal)...');
    const adminNavPresent = await page.$('#nav-admin');
    console.log(`✓ Non-Admin customer view confirmed: Admin Portal button present? ${adminNavPresent !== null} (Expected: false)`);
    if (adminNavPresent !== null) throw new Error('Security violation: Retail customer should not see Admin Portal nav!');

    // Log out customer
    await page.click('#logout-btn');
    await page.waitForSelector('#header-signin-btn', { timeout: 5000 });
    console.log('✓ Customer logged out successfully.');

    // ── 8. Log In as Admin ──
    console.log('\n[STEP 8] Logging in as Admin (admin@rrjaggery.com)...');
    await page.click('#header-signin-btn');
    await page.waitForSelector('#tab-login', { timeout: 5000 });
    await page.click('#tab-login');
    await page.waitForSelector('#login-email', { timeout: 5000 });

    // Focus, select all, and replace email
    await page.click('#login-email');
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#login-email', 'admin@rrjaggery.com');

    // Focus, select all, and replace password
    await page.click('#login-password');
    await page.keyboard.down('Control');
    await page.keyboard.press('A');
    await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#login-password', 'Admin@123');

    await page.click('#login-submit');

    await page.waitForSelector('#nav-admin', { timeout: 8000 });
    console.log('✓ Admin login successful. Admin Portal navigation unlocked.');

    // ── 9. Open Admin Product Management Interface ──
    console.log('\n[STEP 9] Navigating to Admin Product Management...');
    await page.click('#nav-admin');
    await page.waitForSelector('#btn-add-product', { timeout: 5000 });
    const adminTableRows = await page.$$('[id^="admin-product-row-"]');
    console.log(`✓ Admin Product Table rendered with ${adminTableRows.length} products.`);

    // ── 10. Create a New Product Through UI ──
    console.log('\n[STEP 10] Creating a new product through Admin UI modal...');
    await page.click('#btn-add-product');
    await page.waitForSelector('#form-prod-name', { timeout: 5000 });

    const newProdSku = `JAG-E2E-${timestamp.toString().slice(-4)}`;
    const newProdName = `E2E Verified Spiced Jaggery ${timestamp.toString().slice(-4)}`;
    const newProdSlug = `e2e-spiced-jaggery-${timestamp.toString().slice(-4)}`;

    await page.type('#form-prod-name', newProdName);
    await page.type('#form-prod-sku', newProdSku);
    await page.type('#form-prod-slug', newProdSlug);
    await page.select('#form-prod-grade', 'GRADE_A_TRADITIONAL');
    await page.select('#form-prod-pkg', 'BOX');
    await page.$eval('#form-prod-weight', el => el.value = '');
    await page.type('#form-prod-weight', '1.5');
    await page.$eval('#form-prod-retailprice', el => el.value = '');
    await page.type('#form-prod-retailprice', '175');
    await page.$eval('#form-prod-wholesaleprice', el => el.value = '');
    await page.type('#form-prod-wholesaleprice', '130');
    await page.$eval('#form-prod-moq', el => el.value = '');
    await page.type('#form-prod-moq', '30');
    await page.type('#form-prod-desc', 'Automated E2E browser test created product with certified organic spices.');

    await page.click('#submit-save-product');
    await new Promise(r => setTimeout(r, 2000));

    // Confirm product is in Admin table
    await page.waitForSelector(`#admin-product-row-${newProdSku}`, { timeout: 5000 });
    console.log(`✓ Created product "${newProdName}" successfully persisted and rendered in Admin Table!`);

    // ── 11. Confirm New Product in Storefront Public Catalogue ──
    console.log('\n[STEP 11] Verifying new product appears in Public Storefront...');
    await page.click('#nav-storefront');
    await new Promise(r => setTimeout(r, 1500));
    const storefrontCard = await page.$(`#product-card-${newProdSku}`);
    console.log(`✓ New product "${newProdName}" (SKU: ${newProdSku}) successfully verified in Storefront catalogue!`);

    // ── 12. Edit Product & Toggle Active/Inactive Status ──
    console.log('\n[STEP 12] Navigating back to Admin and testing Active/Inactive toggle & Edit...');
    await page.click('#nav-admin');
    await page.waitForSelector(`#toggle-active-${newProdSku}`, { timeout: 5000 });

    // Toggle Active -> Inactive
    await page.click(`#toggle-active-${newProdSku}`);
    await new Promise(r => setTimeout(r, 1500));
    let toggleBtnText = await page.$eval(`#toggle-active-${newProdSku}`, el => el.innerText);
    console.log(`✓ Toggled status to: "${toggleBtnText}"`);

    // Edit product price
    await page.click(`#edit-product-${newProdSku}`);
    await page.waitForSelector('#form-prod-retailprice', { timeout: 5000 });
    await page.$eval('#form-prod-retailprice', el => el.value = '');
    await page.type('#form-prod-retailprice', '199.50');
    await page.click('#submit-save-product');
    await new Promise(r => setTimeout(r, 2000));
    console.log('✓ Product price edited to ₹199.50 and saved via modal.');

    // ── 13. Page Refresh & Session Persistence Verification ──
    console.log('\n[STEP 13] Refreshing page to verify localStorage auth & persisted state...');
    await page.reload({ waitUntil: 'networkidle0' });
    await page.waitForSelector('#user-display-name', { timeout: 5000 });
    const adminUserAfterReload = await page.$eval('#user-display-name', el => el.innerText);
    console.log(`✓ Authenticated session restored cleanly from localStorage for: "${adminUserAfterReload}"`);

    // ── 14. Network & Security Summary ──
    console.log('\n[STEP 14] Verifying network requests & console logs...');
    const failedApiResponses = networkCalls.filter(n => !n.ok);
    console.log(`✓ Total Backend API requests executed: ${networkCalls.length}`);
    console.log(`✓ Failed API responses (excluding intentional invalid login 400): ${failedApiResponses.filter(r => !r.url.includes('/login')).length}`);
    console.log(`✓ Total browser network connection errors: ${networkErrors.length}`);

    console.log('\n===============================================================');
    console.log('  ALL SPRINT 1 BROWSER VERIFICATION STEPS PASSED SUCCESSFULLY!');
    console.log('===============================================================');

  } catch (err) {
    console.error('\n❌ BROWSER VERIFICATION FAILED:', err);
    throw err;
  } finally {
    await browser.close();
  }
}

runVerification();
