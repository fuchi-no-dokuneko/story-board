const { rmSync } = require('node:fs');
const { once } = require('node:events');
const { launch } = require('./cdp-launch.cjs');
class CdpSession {
  constructor(options = {}) {
    this.pending = new Map(); this.sequence = 0; this.buffer = ''; this.events = [];
    Object.assign(this, launch.call({}, options));
    this.process.stdio[4].setEncoding('utf8');
    this.process.stdio[4].on('data', chunk => this.receive(chunk));
    this.process.on('error', error => this.fail(error));
    this.process.on('exit', () => this.fail(new Error('Browser exited')));
  }
  fail(error) { for (const item of this.pending.values()) { clearTimeout(item.timer); item.reject(error); } this.pending.clear(); }
  receive(chunk) {
    this.buffer += chunk;
    let end;
    while ((end = this.buffer.indexOf('\0')) >= 0) {
      const message = JSON.parse(this.buffer.slice(0, end)); this.buffer = this.buffer.slice(end + 1);
      const waiter = this.pending.get(message.id);
      if (!waiter) { this.events.push(message); continue; }
      clearTimeout(waiter.timer);
      this.pending.delete(message.id);
      if (message.error) waiter.reject(new Error(message.error.message)); else waiter.resolve(message.result);
    }
  }
  send(method, params = {}, sessionId) {
    return new Promise((resolve, reject) => {
      const id = ++this.sequence;
      const timer = setTimeout(() => { this.pending.delete(id); reject(new Error(`CDP timed out: ${method}`)); }, 30000);
      this.pending.set(id, { resolve, reject, timer });
      this.process.stdio[3].write(JSON.stringify({ id, method, params, sessionId }) + '\0');
    });
  }
  async close() {
    if (this.process.exitCode !== null || this.process.signalCode !== null) {
      rmSync(this.profile, { recursive: true, force: true }); return;
    }
    const exited = once(this.process, 'exit');
    await this.send('Browser.close').catch(() => {});
    if (this.process.exitCode === null) this.process.kill();
    await exited;
    rmSync(this.profile, { recursive: true, force: true });
  }
}
module.exports = { CdpSession };
