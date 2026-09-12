const { CdpSession } = require('./cdp-session.cjs');
const { CdpElement } = require('./cdp-element.cjs');
class CdpDriver {
  static async create(options) {
    const driver = new CdpDriver(); driver.cdp = new CdpSession(options);
    const { targetId } = await driver.cdp.send('Target.createTarget', { url: 'about:blank' });
    const attached = await driver.cdp.send('Target.attachToTarget', { targetId, flatten: true });
    driver.sessionId = attached.sessionId;
    for (const domain of ['Page', 'Runtime', 'Network']) await driver.sendDevToolsCommand(`${domain}.enable`);
    return driver;
  }
  sendDevToolsCommand(method, params = {}) { return this.cdp.send(method, params, this.sessionId); }
  async evaluate(expression) {
    const result = await this.sendDevToolsCommand('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text);
    return result.result.value;
  }
  executeScript(source, ...args) {
    const values = args.map(arg => arg instanceof CdpElement ? arg.expression() : JSON.stringify(arg));
    return this.evaluate(`(function(){${source}}).apply(null,[${values.join(',')}])`);
  }
  async get(url) {
    const target = new URL(url);
    if (target.protocol !== 'https:' || target.hostname.includes(':')) throw new Error('Browser application URL requires IPv4 HTTPS');
    await this.sendDevToolsCommand('Page.navigate', { url });
    await this.wait(() => this.evaluate("document.readyState === 'complete' && location.href !== 'about:blank'"), 45000);
  }
  getTitle() { return this.evaluate('document.title'); }
  getCurrentUrl() { return this.evaluate('location.href'); }
  async findElements(selector) {
    const count = await this.evaluate(`document.querySelectorAll(${JSON.stringify(selector)}).length`);
    return Array.from({ length: count }, (_, index) => new CdpElement(this, selector, index));
  }
  async wait(predicate, timeout = 45000, message = 'Browser condition timed out') {
    const end = Date.now() + timeout;
    while (Date.now() < end) {
      const result = await predicate(this); if (result) return result;
      await new Promise(resolve => setTimeout(resolve, 50));
    }
    throw new Error(message);
  }
  manage() { return require('./cdp-controls.cjs').controls(this); }
  setNetworkConditions(params) { return this.sendDevToolsCommand('Network.emulateNetworkConditions', { offline: params.offline, latency: 0, downloadThroughput: -1, uploadThroughput: -1 }); }
  deleteNetworkConditions() { return this.setNetworkConditions({ offline: false }); }
  async takeScreenshot() { return (await this.sendDevToolsCommand('Page.captureScreenshot', { format: 'png' })).data; }
  quit() { return this.cdp.close(); }
}
module.exports = { CdpDriver };
