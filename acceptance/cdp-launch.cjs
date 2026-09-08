const { spawn } = require('node:child_process');
const { mkdirSync, mkdtempSync, existsSync, rmSync } = require('node:fs');
const { join, resolve } = require('node:path');

function launch(options) {
    const root = resolve(__dirname, '..');
    const temporary = join(root, '.local/tmp');
    const profiles = join(__dirname, '.local-tool-app/browser-runs');
    mkdirSync(temporary, { recursive: true });
    mkdirSync(profiles, { recursive: true });
    this.profile = mkdtempSync(join(profiles, 'run-'));
    const binary = process.env.CHROME_BINARY || [
      '/snap/chromium/current/usr/lib/chromium-browser/chrome',
      '/usr/bin/chromium', '/usr/bin/google-chrome',
    ].find(existsSync);
    if (!binary) throw new Error('Chromium is not installed');
    this.process = spawn(binary, [
      '--remote-debugging-pipe', '--no-sandbox', '--no-first-run',
      '--disable-background-networking', '--disable-component-update',
      '--disable-dev-shm-usage', '--disable-ipv6', '--password-store=basic',
      '--ignore-certificate-errors', '--window-size=1440,1000',
      `--user-data-dir=${this.profile}`, ...(options.headless === false ? [] : ['--headless=new']),
      ...(options.arguments || []), 'about:blank',
    ], { stdio: ['ignore', 'ignore', 'ignore', 'pipe', 'pipe'], env: {
      ...process.env, TMPDIR: temporary, XDG_CACHE_HOME: join(temporary, 'browser-cache'),
    } });
    return { process: this.process, profile: this.profile };
}
module.exports = { launch };
