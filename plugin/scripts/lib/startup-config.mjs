import { readFileSync } from 'node:fs';
const file = new URL('../../config/config.yaml', import.meta.url);
const supported = new Set(['base_url', 'timeout_ms', 'user_agent']);
export function startupConfig() {
  const config = {};
  for (const raw of readFileSync(file, 'utf8').split(/\r?\n/u)) {
    const line = raw.trim();
    if (!line || line.startsWith('#')) continue;
    const match = /^([a-z_]+):\s*(.+)$/u.exec(line);
    if (!match || !supported.has(match[1])) throw new Error('Unsupported client startup setting');
    config[match[1]] = match[2].startsWith('"') ? JSON.parse(match[2]) : match[2];
  }
  return config;
}
