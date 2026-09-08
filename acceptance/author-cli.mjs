import { execFileSync } from 'node:child_process';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
export { createTypedId as id } from '../plugin/scripts/lib/ids.mjs';
export const output = resolve('.local/refactor-evidence/image-workflow');
mkdirSync(output, { recursive: true });
export const json = path => JSON.parse(readFileSync(path, 'utf8'));
export function save(name, value) {
  const path = `${output}/${name}`;
  writeFileSync(path, JSON.stringify(value, null, 2), { mode: 0o600 });
  return path;
}
export function cli(...args) {
  return JSON.parse(execFileSync(process.execPath,
    ['plugin/scripts/storyblock-author.mjs', ...args, '--json'],
    { encoding: 'utf8', timeout: 60000, maxBuffer: 32 * 1024 * 1024 }));
}
