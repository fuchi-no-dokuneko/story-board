import { mkdir, writeFile } from "node:fs/promises";

export async function writePage(url, content) {
  if (Buffer.byteLength(content) > 3000 || content.split("\n").length > 200) {
    throw new Error(`Documentation page exceeds repository limits: ${url.pathname}`);
  }
  await writeFile(url, content);
}

export async function writeIndex(references, kind, names, introduction) {
  await mkdir(new URL(`${kind}/`, references), { recursive: true });
  const pages = [];
  for (let offset = 0; offset < names.length; offset += 15) {
    const page = `${kind}-index-${1 + offset / 15}.md`;
    pages.push(page);
    const links = names.slice(offset, offset + 15).map(name => `- [${name}](${kind}/${name}.md)`).join("\n");
    await writePage(new URL(page, references),
      `# ${kind}: ${1 + offset / 15}\n\nSelect an entry.\n選擇項目。\n选择项目。\n\n${links}\n`);
  }
  await writePage(new URL(`${kind}.md`, references),
    `# ${kind}\n\n${introduction}\n\n${pages.map((page, i) => `- [${i + 1}](${page})`).join("\n")}\n`);
}

export const contractLink = (path) =>
  `Read the complete machine-readable contract: [JSON](${path}).\n` +
  `完整機器可讀契約：[JSON](${path})。\n` +
  `完整机器可读契约：[JSON](${path})。\n`;
