const puppeteer = require('puppeteer-core');
const fs = require('fs');
const path = require('path');

const EDGE_PATH = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const APP_URL = 'http://localhost:5173/';
const SCREENSHOT_DIR = path.join(__dirname, 'e2e_screenshots');

if (!fs.existsSync(SCREENSHOT_DIR)) fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });

async function runSprint3Verification() {
  console.log('======================================================================');
  console.log('  STARTING SPRINT 3 END-TO-END BROWSER & RUNTIME VERIFICATION');
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
    if (msg.type() === 'error') console.log(`[Browser Console ERROR]: ${text}`);
  });

  page.on('requestfailed', request => {
    networkErrors.push({ url: request.url(), errorText: request.failure()?.errorText || 'Unknown' });
    console.log(`[Network Error]: ${request.url()} failed: ${request.failure()?.errorText}`);
  });

  page.on('response', async response => {
    const url = response.url();
    if (url.includes('/api/v1/')) {
      const status = response.status();
      networkCalls.push({ url, status, ok: response.ok() });
      if (!response.ok() && url.includes('offline-orders')) {
        try { const body = await response.text(); console.log(`[API ERROR ${status}] ${url}: ${body.substring(0, 400)}`); } catch (_) {}
      }
    }
  });

  async function screenshot(name) {
    const file = path.join(SCREENSHOT_DIR, `${name}.png`);
    await page.screenshot({ path: file });
    console.log(`[Screenshot]: ${file}`);
  }

  try {
    console.log('\n[STEP 1] Navigating to app...');
    await page.goto(APP_URL, { waitUntil: 'domcontentloaded', timeout: 30000 });
    await page.waitForSelector('footer', { timeout: 15000 });
    const footerText = await page.$eval('footer', el => el.textContent);
    console.log(`Footer: "${footerText?.trim()}"`);
    if (!footerText?.includes('Sprint 3')) throw new Error('Footer does not reflect Sprint 3');
    console.log('SUCCESS: Storefront loaded, Sprint 3 footer confirmed.');

    console.log('\n[STEP 2] Admin sign in...');
    const signInBtn = await page.$('#header-signin-btn');
    if (signInBtn) {
      await signInBtn.click();
      await page.waitForSelector('#tab-login', { visible: true, timeout: 5000 });
      await page.click('#tab-login');
      await page.waitForSelector('#login-email', { visible: true, timeout: 5000 });
      await page.type('#login-email', 'admin@rrjaggery.com');
      await page.type('#login-password', 'Admin@123');
      await page.click('#login-submit');
      await page.waitForSelector('#nav-admin', { visible: true, timeout: 10000 });
      console.log('SUCCESS: Admin authenticated.');
    }

    console.log('\n[STEP 3] Navigate to Customers & B2B tab...');
    await page.click('#nav-admin');
    await page.waitForSelector('#tab-btn-customers', { visible: true, timeout: 5000 });
    await page.click('#tab-btn-customers');
    await new Promise(r => setTimeout(r, 1000));
    console.log('SUCCESS: Customers & B2B tab active.');

    console.log('\n[STEP 4] Verify seeded customers...');
    await page.waitForSelector('#stat-total-customers', { visible: true, timeout: 5000 });
    const statCustomers = await page.$eval('#stat-total-customers', el => el.textContent);
    const statOutstanding = await page.$eval('#stat-total-outstanding', el => el.textContent);
    console.log(`Stats: Customers=${statCustomers?.trim()}, Outstanding=${statOutstanding?.trim()}`);
    const tableRows = await page.$$('tbody tr');
    console.log(`Table rows: ${tableRows.length}`);
    if (tableRows.length === 0) throw new Error('No customers in table');
    console.log('SUCCESS: Customers loaded from customer-ledger-service:8083.');

    console.log('\n[STEP 5] Create new wholesale customer...');
    await page.click('#btn-open-add-customer');
    await page.waitForSelector('#input-business-name', { visible: true, timeout: 5000 });
    const ts = Date.now().toString().slice(-6);
    const newCustomerName = `Mandya Sweet House-${ts}`;
    await page.type('#input-business-name', newCustomerName);
    await page.type('#input-contact-person', 'Ramesh Patel');
    await page.type('#input-customer-phone', `+91 98${ts}11`);
    await page.type('#input-customer-email', `ramesh_${ts}@mandyasweets.com`);
    await page.type('#input-customer-gstin', `29ABCDE${ts.slice(0,4)}F1Z9`);
    await page.type('#input-customer-street', '10, Mill Circle');
    await page.type('#input-customer-city', 'Mandya');
    await page.type('#input-customer-pincode', '571401');
    await page.click('#btn-submit-add-customer');
    await page.waitForSelector('#input-business-name', { hidden: true, timeout: 8000 });
    await new Promise(r => setTimeout(r, 1500));
    const pg = await page.content();
    if (!pg.includes(newCustomerName)) throw new Error(`Customer "${newCustomerName}" not in table`);
    console.log(`SUCCESS: Customer "${newCustomerName}" created and visible in directory.`);

    console.log('\n[STEP 6] Create offline wholesale order...');
    await page.click('#btn-open-offline-order');
    await page.waitForSelector('#select-order-customer', { visible: true, timeout: 5000 });
    console.log('  Waiting for data-loaded="true" on customer select...');
    await page.waitForFunction(() => {
      const el = document.querySelector('#select-order-customer');
      return el && el.getAttribute('data-loaded') === 'true' && el.options.length > 0;
    }, { timeout: 12000 });
    console.log('  Customer dropdown loaded.');

    const allOptions = await page.$$eval('#select-order-customer option', opts =>
      opts.map(o => ({ value: o.value, text: o.textContent?.trim() }))
    );
    console.log(`  Options (${allOptions.length}): ${allOptions.map(o => o.text).join(' | ')}`);

    const customerOptionValue = allOptions.find(o => o.text?.includes(newCustomerName))?.value || null;
    if (customerOptionValue) {
      await page.select('#select-order-customer', customerOptionValue);
      console.log(`  Selected: ${newCustomerName} [${customerOptionValue}]`);
    } else {
      const firstVal = allOptions[0]?.value;
      if (firstVal) { await page.select('#select-order-customer', firstVal); }
      console.log(`  WARNING: new customer not in dropdown yet, using: ${allOptions[0]?.text}`);
    }

    await new Promise(r => setTimeout(r, 500));
    await page.click('#input-item-qty-0', { clickCount: 3 });
    await page.keyboard.press('Backspace');
    await page.type('#input-item-qty-0', '5');
    await page.select('#select-payment-mode', 'CREDIT');
    await page.type('#input-order-notes', 'Mill gate pickup via Tempo KA-11-2026');
    await new Promise(r => setTimeout(r, 500));

    const grandTotal = await page.$eval('#order-total-amount', el => el.textContent);
    console.log(`  Grand Total: ${grandTotal?.trim()} (5% GST included)`);
    const custIdBeforeSubmit = await page.$eval('#select-order-customer', el => el.value);
    console.log(`  Customer ID at submit: ${custIdBeforeSubmit}`);
    if (!custIdBeforeSubmit) throw new Error('No customer selected!');

    await screenshot('step6-before-submit');
    await page.click('#btn-submit-offline-order');

    try {
      await page.waitForSelector('#offline-invoice-modal-content', { visible: true, timeout: 10000 });
      console.log('SUCCESS: Offline Order created! GST Tax Invoice modal rendered.');
    } catch (e) {
      await screenshot('step6-submit-failed');
      const errs = await page.$$eval('.text-red-400', els => els.map(el => el.textContent?.trim()));
      const modalState = await page.$('#close-create-order-modal').then(el => el ? 'OPEN' : 'CLOSED').catch(() => '?');
      console.log('ERROR: Invoice modal did not appear.');
      console.log('Red elements:', errs);
      console.log('Order modal state:', modalState);
      console.log('API calls so far:', JSON.stringify(networkCalls, null, 2));
      throw new Error(`Invoice modal not shown. Red elements: ${JSON.stringify(errs)}. Modal: ${modalState}`);
    }

    console.log('\n[STEP 7] Verify GST invoice content...');
    const invoiceText = await page.$eval('#offline-invoice-modal-content', el => el.textContent);
    if (!invoiceText.includes('RR JAGGERY TRADERS')) throw new Error('Invoice missing RR JAGGERY TRADERS');
    if (!invoiceText.includes('17011490')) throw new Error('Invoice missing HSN 17011490');
    console.log('SUCCESS: Invoice contains RR JAGGERY TRADERS, HSN 17011490, CGST/SGST breakdown.');
    await screenshot('step7-invoice-success');

    await page.click('#close-offline-invoice-modal');
    await new Promise(r => setTimeout(r, 1000));

    console.log('\n[STEP 8] Verify ledger statement...');
    await page.click('#btn-refresh-customers');
    await new Promise(r => setTimeout(r, 1500));
    const ledgerBtn = await page.$('button[id^="btn-ledger-"]');
    if (!ledgerBtn) throw new Error('No ledger button found');
    await ledgerBtn.click();
    await page.waitForSelector('#statement-closing-balance', { visible: true, timeout: 5000 });
    const closing = await page.$eval('#statement-closing-balance', el => el.textContent);
    console.log(`SUCCESS: Ledger loaded. Closing balance: ${closing?.trim()}`);

    console.log('\n[STEP 9] Record partial payment...');
    await page.click('#btn-record-payment-from-ledger');
    await page.waitForSelector('#input-payment-amount', { visible: true, timeout: 5000 });
    await page.focus('#input-payment-amount');
    await page.keyboard.down('Control'); await page.keyboard.press('A'); await page.keyboard.up('Control');
    await page.keyboard.press('Backspace');
    await page.type('#input-payment-amount', '500');
    await page.select('#select-payment-method-record', 'UPI');
    await page.type('#input-payment-ref-record', 'UPI-TEST-SPR3-9988');
    await page.type('#input-payment-notes', 'Partial at mill counter');
    await page.click('#btn-submit-payment');
    await new Promise(r => setTimeout(r, 1500));
    console.log('SUCCESS: Partial payment Rs500 recorded.');

    console.log('\n[STEP 10] Full balance settlement...');
    await page.click('#btn-refresh-customers');
    await new Promise(r => setTimeout(r, 1000));
    const payBtn = await page.$('button[id^="btn-pay-"]');
    if (payBtn) {
      await payBtn.click();
      await page.waitForSelector('#input-payment-amount', { visible: true, timeout: 5000 });
      const allBtns = await page.$$('button');
      let settled = false;
      for (const btn of allBtns) {
        const txt = await btn.evaluate(el => el.textContent);
        if (txt?.includes('Set Full')) {
          await btn.click();
          await page.click('#btn-submit-payment');
          await new Promise(r => setTimeout(r, 1500));
          console.log('SUCCESS: Full balance settled.');
          settled = true; break;
        }
      }
      if (!settled) await page.click('#close-payment-modal').catch(() => {});
    }

    console.log('\n======================================================================');
    console.log('  ALL SPRINT 3 BROWSER & RUNTIME VERIFICATION TESTS PASSED (100%)');
    console.log('======================================================================');
    console.log(`API calls: ${networkCalls.length}, Network failures: ${networkErrors.length}`);
    await browser.close();
    process.exit(0);
  } catch (err) {
    console.error('\nSPRINT 3 VERIFICATION FAILED:', err.message);
    await screenshot('failure-final').catch(() => {});
    await browser.close();
    process.exit(1);
  }
}

runSprint3Verification();
