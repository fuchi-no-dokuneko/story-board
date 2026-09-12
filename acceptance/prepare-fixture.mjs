import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { createTypedId } from '../plugin/scripts/lib/ids.mjs';
import { countHanCodePoints } from '../plugin/scripts/lib/validation.mjs';
const directory = new URL('../.local/refactor-evidence/gui/', import.meta.url);
await mkdir(directory, { recursive: true });
const text = await readFile(new URL('../plugin/examples/demo-novel-rendered.txt', import.meta.url), 'utf8');
const lines = text.trim().split('\n');
lines.push(...Array(8).fill('春風吹過安靜的村莊了。'));
const split = Math.floor(lines.length / 2);
const source = {
  novel_id: createTypedId('nov'), created_at: new Date().toISOString().replace('.000Z', 'Z'),
  title: 'StoryBlock GUI 驗收稿', language: 'zh-Hant',
  main_characters: ['洛恩', '艾莉婭', '塞拉斯', '伊瑟琳', '阿斯塔羅'],
  zombie_count: 1000, tnt_cannon_count: 1000,
  expected_han_characters: countHanCodePoints(lines.join('\n')),
  chapters: [
    { title: '第一章', text: lines.slice(0, split).join('\n') },
    { title: '第二章', text: lines.slice(split).join('\n') },
  ],
};
await writeFile(new URL('manuscript.json', directory), JSON.stringify(source));
console.log(JSON.stringify({ novel_id: source.novel_id, han_count: source.expected_han_characters }));
