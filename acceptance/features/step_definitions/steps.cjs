const assert = require("node:assert/strict");
const { Given, Then, When } = require("@cucumber/cucumber");
const runtime = require("../../runtime.cjs");

Given("I begin a recorded demo", async function () {
  await runtime.beginRecording(this);
});

Then("I finish the recorded demo", async function () {
  await runtime.finishRecording(this);
});

When("I narrate in {string} for at least {int} seconds:", async function (language, seconds, text) {
  await runtime.narrate(language, seconds, text);
});

When("I wait for {int} seconds", async function (seconds) {
  await runtime.sleep(seconds * 1000);
});

Given("I open the web application at path {string}", async function (pathname) {
  await runtime.openWeb(this, pathname);
});

Given("the next page load reports degraded API health", async function () {
  await runtime.mockInitialHealth(this, "degraded");
});

Given("the next page load cannot reach API health", async function () {
  await runtime.mockInitialHealth(this, "unavailable");
});

Given("I sign in to System Monitor with configured UAT credentials", async function () {
  const username = process.env.SYSTEM_MONITOR_UAT_USERNAME;
  const password = process.env.SYSTEM_MONITOR_UAT_PASSWORD;
  assert.ok(username && password, "SYSTEM_MONITOR_UAT_USERNAME and SYSTEM_MONITOR_UAT_PASSWORD are required");
  await runtime.openWeb(this, "/login");
  await runtime.replaceCss(this, '.login-container input[type="text"]', username);
  await runtime.replaceCss(this, '.login-container input[type="password"]', password);
  await runtime.clickCss(this, '.login-container button[type="submit"]');
  await runtime.waitPath(this, "/daily-monitor");
});

Then("the web page title contains {string}", async function (expected) {
  assert.ok((await (await runtime.ensureBrowser(this)).getTitle()).includes(expected));
});

Then("the web path eventually ends with {string}", async function (suffix) {
  await runtime.waitPath(this, suffix);
});

Then("CSS {string} is visible", async function (selector) {
  await runtime.cssElement(this, selector);
});

Then("CSS {string} has text {string}", async function (selector, expected) {
  assert.equal(await (await runtime.cssElement(this, selector)).getText(), expected);
});

Then("CSS {string} contains text {string}", async function (selector, expected) {
  assert.ok((await (await runtime.cssElement(this, selector)).getText()).includes(expected));
});

Then("CSS {string} eventually contains text {string}", async function (selector, expected) {
  await runtime.waitCssText(this, selector, expected, true);
});

Then("CSS {string} eventually excludes text {string}", async function (selector, expected) {
  await runtime.waitCssText(this, selector, expected, false);
});

Then("CSS {string} contains environment variable {string}", async function (selector, name) {
  const expected = process.env[name];
  assert.ok(expected, `Required environment variable is missing: ${name}`);
  assert.ok((await (await runtime.cssElement(this, selector)).getText()).includes(expected));
});

Then("CSS {string} has value {string}", async function (selector, expected) {
  assert.equal(await (await runtime.cssElement(this, selector, false)).getAttribute("value"), expected);
});

Then("CSS {string} has attribute {string} equal to {string}", async function (selector, attribute, expected) {
  assert.equal(await (await runtime.cssElement(this, selector, false)).getAttribute(attribute), expected);
});

Then("at least {int} elements match CSS {string}", async function (minimum, selector) {
  await runtime.waitCssCount(this, selector, minimum, false);
});

Then("exactly {int} elements match CSS {string}", async function (expected, selector) {
  await runtime.waitCssCount(this, selector, expected, true);
});

Then("no elements match CSS {string}", async function (selector) {
  await runtime.waitCssCount(this, selector, 0, true);
});

When("I click CSS {string}", async function (selector) {
  await runtime.clickCss(this, selector);
});

When("I replace CSS {string} with {string}", async function (selector, value) {
  await runtime.replaceCss(this, selector, value);
});

When("I replace CSS {string} with:", async function (selector, value) {
  await runtime.replaceCss(this, selector, value);
});

When("I replace CSS {string} with environment variable {string}", async function (selector, name) {
  const value = process.env[name];
  assert.ok(value, `Required environment variable is missing: ${name}`);
  await runtime.replaceCss(this, selector, value);
});

When("I choose value {string} in CSS {string}", async function (value, selector) {
  await runtime.chooseValue(this, selector, value);
});

When("I choose value from environment variable {string} in CSS {string}", async function (name, selector) {
  const value = process.env[name];
  assert.ok(value, `Required environment variable is missing: ${name}`);
  await runtime.chooseValue(this, selector, value);
});

When("I reload the web page", async function () {
  await (await runtime.ensureBrowser(this)).navigate().refresh();
});

When("I set the browser network offline", async function () {
  await runtime.setNetworkOffline(this, true);
});

When("I restore the browser network", async function () {
  await runtime.setNetworkOffline(this, false);
});

When("I block browser requests matching {string}", async function (pattern) {
  await runtime.setBlockedUrls(this, [pattern]);
});

When("I unblock all browser requests", async function () {
  await runtime.setBlockedUrls(this, []);
});

When("I resize the browser to {int} by {int}", async function (width, height) {
  await runtime.setViewport(this, width, height);
});

When("I select only the monitor key from environment variable {string}", async function (name) {
  await runtime.selectOnlyCheckboxByLabel(this, name);
});

When("I deselect every monitor key", async function () {
  await runtime.deselectAllMonitorKeys(this);
});

When("I remember resource requests containing {string} as {string}", async function (fragment, name) {
  await runtime.rememberResourceCount(this, fragment, name);
});

Then("resource requests containing {string} eventually increase from {string}", async function (fragment, name) {
  await runtime.waitResourceCountIncrease(this, fragment, name);
});

Then("JavaScript expression {string} returns true", async function (expression) {
  assert.equal(await (await runtime.ensureBrowser(this)).executeScript(`return Boolean(${expression});`), true);
});

Then("JavaScript expression {string} eventually returns true", async function (expression) {
  await runtime.waitJavaScript(this, expression);
});

Then("local storage item {string} is present", async function (name) {
  assert.ok(await (await runtime.ensureBrowser(this)).executeScript("return localStorage.getItem(arguments[0]);", name));
});

Then("local storage item {string} is absent", async function (name) {
  assert.equal(await (await runtime.ensureBrowser(this)).executeScript("return localStorage.getItem(arguments[0]);", name), null);
});

Then("the monitor chart contains normal warning and critical colors", async function () {
  const result = await (await runtime.ensureBrowser(this)).executeScript(`
    const canvas = document.querySelector('.graph-container canvas');
    if (!canvas) return false;
    const pixels = canvas.getContext('2d').getImageData(0, 0, canvas.width, canvas.height).data;
    let green = false, orange = false, red = false;
    for (let index = 0; index < pixels.length; index += 4) {
      const r = pixels[index], g = pixels[index + 1], b = pixels[index + 2], a = pixels[index + 3];
      if (a && r < 30 && g >= 100 && b < 30) green = true;
      if (a && r >= 220 && g >= 110 && g <= 190 && b < 40) orange = true;
      if (a && r >= 220 && g < 40 && b < 40) red = true;
    }
    return green && orange && red;
  `);
  assert.equal(result, true, "Expected green, orange, and red chart points; verify the UAT fixture spans every range");
});
