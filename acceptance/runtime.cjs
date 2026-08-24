const { execFileSync, spawnSync } = require("node:child_process");
const { resolve4 } = require("node:dns/promises");
const { existsSync, mkdirSync, readFileSync, writeFileSync } = require("node:fs");
const path = require("node:path");
const { Builder, By, Select, until } = require("selenium-webdriver");
const chrome = require("selenium-webdriver/chrome");

const acceptanceDirectory = __dirname;
const config = JSON.parse(readFileSync(path.join(acceptanceDirectory, "project.json"), "utf8"));
const sleep = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));
const timeoutMs = () => Number(process.env.ACCEPTANCE_TIMEOUT_SECONDS || config.timeoutSeconds || 45) * 1000;

function slug(value) {
  return String(value).toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "").slice(0, 80) || "scenario";
}

async function ensureBrowser(world) {
  if (world.driver) return world.driver;
  const options = new chrome.Options();
  const base = new URL(process.env.UAT_BASE_URL || config.baseUrl);
  const mappings = ["MAP localhost 127.0.0.1"];
  for (const host of [...new Set([base.hostname, ...(config.ipv4Hosts || [])])]) {
    if (/^(?:\d{1,3}\.){3}\d{1,3}$/.test(host) || host === "localhost") continue;
    if (!/^[a-z0-9.-]+$/i.test(host)) throw new Error(`Unsafe host for IPv4 mapping: ${host}`);
    const addresses = await resolve4(host);
    if (!addresses.length) throw new Error(`No IPv4 address found for ${host}`);
    mappings.push(`MAP ${host} ${addresses[0]}`);
  }
  options.addArguments(
    "--window-size=1440,1000",
    "--disable-dev-shm-usage",
    "--no-sandbox",
    "--remote-debugging-pipe",
    "--disable-ipv6",
    `--host-resolver-rules=${mappings.join(", ")}`,
  );
  if (config.acceptInsecureTls) options.addArguments("--ignore-certificate-errors");
  if (process.env.ACCEPTANCE_HEADLESS === "1") options.addArguments("--headless=new", "--disable-gpu");
  if (process.env.CHROME_BINARY) options.setChromeBinaryPath(process.env.CHROME_BINARY);
  const driverBinary = process.env.CHROMEDRIVER_PATH || (existsSync("/usr/bin/chromedriver") ? "/usr/bin/chromedriver" : "");
  const builder = new Builder().forBrowser("chrome").setChromeOptions(options);
  if (driverBinary) builder.setChromeService(new chrome.ServiceBuilder(driverBinary));
  world.driver = await builder.build();
  await world.driver.manage().setTimeouts({ pageLoad: timeoutMs(), script: timeoutMs() });
  return world.driver;
}

async function openWeb(world, pathname) {
  const driver = await ensureBrowser(world);
  await driver.get(new URL(pathname, process.env.UAT_BASE_URL || config.baseUrl).href);
}

async function cssElement(world, selector, visible = true) {
  const driver = await ensureBrowser(world);
  const element = await driver.wait(until.elementLocated(By.css(selector)), timeoutMs(), `CSS not found: ${selector}`);
  if (visible) await driver.wait(until.elementIsVisible(element), timeoutMs(), `CSS not visible: ${selector}`);
  return element;
}

async function clickCss(world, selector) {
  const driver = await ensureBrowser(world);
  const element = await cssElement(world, selector);
  await driver.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
  await driver.wait(until.elementIsEnabled(element), timeoutMs());
  await element.click();
}

async function replaceCss(world, selector, value) {
  const driver = await ensureBrowser(world);
  const element = await cssElement(world, selector);
  await driver.executeScript(`
    const element = arguments[0];
    const value = String(arguments[1]);
    const prototype = element instanceof HTMLTextAreaElement
      ? HTMLTextAreaElement.prototype
      : HTMLInputElement.prototype;
    const setter = Object.getOwnPropertyDescriptor(prototype, "value").set;
    setter.call(element, value);
    element.dispatchEvent(new Event("input", { bubbles: true }));
    element.dispatchEvent(new Event("change", { bubbles: true }));
  `, element, value);
}

async function chooseValue(world, selector, value) {
  const select = new Select(await cssElement(world, selector));
  try {
    await select.selectByValue(value);
  } catch {
    await select.selectByVisibleText(value);
  }
}

async function waitCssText(world, selector, expected, contains = true) {
  const driver = await ensureBrowser(world);
  await driver.wait(async () => {
    try {
      const text = await (await cssElement(world, selector, false)).getText();
      return contains ? text.includes(expected) : !text.includes(expected);
    } catch {
      return !contains;
    }
  }, timeoutMs(), `CSS ${selector} did not ${contains ? "contain" : "exclude"} ${expected}`);
}

async function waitCssCount(world, selector, expected, exact) {
  const driver = await ensureBrowser(world);
  await driver.wait(async () => {
    const count = (await driver.findElements(By.css(selector))).length;
    return exact ? count === expected : count >= expected;
  }, timeoutMs(), `Unexpected element count for ${selector}`);
}

async function waitJavaScript(world, expression) {
  const driver = await ensureBrowser(world);
  await driver.wait(async () => Boolean(await driver.executeScript(`return Boolean(${expression});`)), timeoutMs(), `Expression stayed false: ${expression}`);
}

async function waitPath(world, suffix) {
  const driver = await ensureBrowser(world);
  await driver.wait(async () => new URL(await driver.getCurrentUrl()).pathname.endsWith(suffix), timeoutMs(), `Path did not end with ${suffix}`);
}

async function setNetworkOffline(world, offline) {
  const driver = await ensureBrowser(world);
  if (offline) {
    await driver.setNetworkConditions({ offline: true, latency: 0, download_throughput: 0, upload_throughput: 0 });
  } else {
    await driver.deleteNetworkConditions();
  }
}

async function setBlockedUrls(world, urls) {
  const driver = await ensureBrowser(world);
  await driver.sendDevToolsCommand("Network.enable", {});
  await driver.sendDevToolsCommand("Network.setBlockedURLs", { urls });
}

async function setViewport(world, width, height) {
  await (await ensureBrowser(world)).manage().window().setRect({ width, height });
  await sleep(300);
}

async function mockInitialHealth(world, mode) {
  if (!["degraded", "unavailable"].includes(mode)) throw new Error(`Unknown health mode: ${mode}`);
  const driver = await ensureBrowser(world);
  const source = mode === "degraded"
    ? `
      const nativeFetch = window.fetch.bind(window);
      window.fetch = (input, options) => {
        const target = typeof input === 'string' ? input : input.url;
        if (target === '/actuator/health') {
          return Promise.resolve(new Response(JSON.stringify({ status: 'DOWN' }), {
            status: 503,
            statusText: 'Service Unavailable',
            headers: { 'Content-Type': 'application/json' },
          }));
        }
        return nativeFetch(input, options);
      };
    `
    : `
      const nativeFetch = window.fetch.bind(window);
      window.fetch = (input, options) => {
        const target = typeof input === 'string' ? input : input.url;
        if (target === '/actuator/health') return Promise.reject(new TypeError('Failed to fetch'));
        return nativeFetch(input, options);
      };
    `;
  await driver.sendDevToolsCommand("Page.addScriptToEvaluateOnNewDocument", { source });
}

async function selectOnlyCheckboxByLabel(world, environmentName) {
  const value = process.env[environmentName];
  if (!value) throw new Error(`Required environment variable is missing: ${environmentName}`);
  const driver = await ensureBrowser(world);
  const selected = await driver.executeScript(`
    const wanted = String(arguments[0]);
    const boxes = [...document.querySelectorAll('.keys-checkboxes input[type="checkbox"]')];
    const target = boxes.find((box) => box.closest('label')?.textContent.trim() === wanted);
    if (!target) return false;
    for (const box of boxes) if (box.checked !== (box === target)) box.click();
    return true;
  `, value);
  if (!selected) throw new Error(`Monitor key was not found: ${value}`);
}

async function deselectAllMonitorKeys(world) {
  const driver = await ensureBrowser(world);
  await driver.executeScript(`
    for (const box of document.querySelectorAll('.keys-checkboxes input[type="checkbox"]')) {
      if (box.checked) box.click();
    }
  `);
}

async function rememberResourceCount(world, fragment, name) {
  const driver = await ensureBrowser(world);
  const count = await driver.executeScript(
    "return performance.getEntriesByType('resource').filter((entry) => entry.name.includes(arguments[0])).length;",
    fragment,
  );
  world.memory.set(name, count);
}

async function waitResourceCountIncrease(world, fragment, name) {
  if (!world.memory.has(name)) throw new Error(`No remembered value named ${name}`);
  const before = world.memory.get(name);
  const driver = await ensureBrowser(world);
  await driver.wait(async () => {
    const count = await driver.executeScript(
      "return performance.getEntriesByType('resource').filter((entry) => entry.name.includes(arguments[0])).length;",
      fragment,
    );
    return count > before;
  }, timeoutMs(), `Resource count did not increase for ${fragment}`);
}

function runExternalCommand(variable, extraEnvironment = {}) {
  const executable = process.env[variable];
  if (!executable) return false;
  const result = spawnSync(executable, [], {
    env: { ...process.env, ...extraEnvironment },
    encoding: "utf8",
    stdio: "inherit",
  });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`${variable} exited with status ${result.status}`);
  return true;
}

async function beginRecording(world) {
  runExternalCommand("DEMO_RECORD_START_COMMAND", {
    DEMO_SUITE: process.env.ACCEPTANCE_SUITE,
    DEMO_REPOSITORY: config.repository,
  });
  world.demoRecording = true;
}

async function finishRecording(world) {
  if (!world.demoRecording) return;
  runExternalCommand("DEMO_RECORD_STOP_COMMAND", {
    DEMO_SUITE: process.env.ACCEPTANCE_SUITE,
    DEMO_REPOSITORY: config.repository,
  });
  world.demoRecording = false;
}

async function narrate(language, minimumSeconds, text) {
  const started = Date.now();
  const invoked = runExternalCommand("DEMO_TTS_COMMAND", {
    DEMO_TTS_LANGUAGE: language,
    DEMO_TTS_TEXT: text,
    DEMO_TTS_MIN_SECONDS: String(minimumSeconds),
  });
  if (!invoked) process.stdout.write(`NARRATION [${language}, >=${minimumSeconds}s]: ${text}\n`);
  const remaining = minimumSeconds * 1000 - (Date.now() - started);
  if (remaining > 0) await sleep(remaining);
}

async function captureScenario(world, scenarioName) {
  if (!world.driver || !process.env.ACCEPTANCE_REPORT_DIR) return;
  mkdirSync(process.env.ACCEPTANCE_REPORT_DIR, { recursive: true });
  try {
    writeFileSync(
      path.join(process.env.ACCEPTANCE_REPORT_DIR, `${slug(scenarioName)}.png`),
      await world.driver.takeScreenshot(),
      "base64",
    );
  } catch (error) {
    process.stderr.write(`Could not capture screenshot: ${error.message}\n`);
  }
}

async function closeWorld(world) {
  await finishRecording(world);
  if (world.driver) {
    await world.driver.quit();
    world.driver = null;
  }
}

module.exports = {
  beginRecording,
  captureScenario,
  chooseValue,
  clickCss,
  closeWorld,
  cssElement,
  deselectAllMonitorKeys,
  ensureBrowser,
  finishRecording,
  mockInitialHealth,
  narrate,
  openWeb,
  rememberResourceCount,
  replaceCss,
  selectOnlyCheckboxByLabel,
  setBlockedUrls,
  setNetworkOffline,
  setViewport,
  sleep,
  timeoutMs,
  waitCssCount,
  waitCssText,
  waitJavaScript,
  waitPath,
  waitResourceCountIncrease,
};
