const puppeteer = require('puppeteer-core');

(async () => {
  console.log('Launching browser...');
  // Launch browser using puppeteer's default Chromium
  const browser = await puppeteer.launch({
    headless: false, // Set to true for headless mode
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  try {
    console.log('Creating new page...');
    const page = await browser.newPage();
    console.log('Navigating to http://localhost/...');
    await page.goto('http://localhost/', { waitUntil: 'networkidle0', timeout: 30000 });

    // Get page title
    const title = await page.title();
    console.log(`Page title: ${title}`);

    // Check if we're on the login page or already logged in
    const url = page.url();
    console.log(`Current URL: ${url}`);

    // Wait a bit to see if any redirect happens
    await page.waitForTimeout(2000);

    // Take a screenshot for verification
    await page.screenshot({ path: '/tmp/login-page.png' });
    console.log('Screenshot saved to /tmp/login-page.png');

    // Check for login form elements
    const loginForm = await page.$('form');
    if (loginForm) {
      console.log('Login form detected');
    } else {
      console.log('No login form detected - possibly already logged in or different page structure');
    }

    console.log('Basic verification complete');
  } catch (error) {
    console.error('Error during verification:', error);
  } finally {
    console.log('Closing browser...');
    await browser.close();
  }
})();