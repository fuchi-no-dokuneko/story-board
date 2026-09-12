import { readdirSync, readFileSync, writeFileSync, renameSync, chmodSync } from "node:fs";
import { resolve, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(fileURLToPath(new URL("../", import.meta.url)));
function assemble(directory) {
  for (const entry of readdirSync(directory, { withFileTypes: true })) {
    if (!entry.isDirectory() || ["node_modules", ".local-tool-app", ".git"].includes(entry.name)) continue;
    const path = join(directory, entry.name);
    if (!entry.name.endsWith(".parts")) { assemble(path); continue; }
    const target = path.slice(0, -6);
    const data = Buffer.concat(readdirSync(path).filter(name => name.endsWith(".part"))
      .sort().map(name => readFileSync(join(path, name))));
    const pending = `${target}.pending`;
    writeFileSync(pending, data);
    renameSync(pending, target);
    if (data.subarray(0, 2).toString() === "#!") chmodSync(target, 0o755);
  }
}
assemble(root);
