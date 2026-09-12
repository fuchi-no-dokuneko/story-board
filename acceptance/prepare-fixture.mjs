import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { createTypedId } from '../plugin/scripts/lib/ids.mjs';
import { countHanCodePoints } from '../plugin/scripts/lib/validation.mjs';
const directory = new URL('../.local/refactor-evidence/gui/', import.meta.url);
await mkdir(directory, { recursive: true });
const chapters = await Promise.all([1, 2].map(async (number) => ({
  title: number === 1 ? '第一章：候車室' : '第二章：一碗熱湯',
  text: (await readFile(new URL(`fixtures/last-train-${number}.txt`, import.meta.url), 'utf8')).trim(),
})));
const source = {
  novel_id: createTypedId('nov'), created_at: new Date().toISOString(),
  title: '末班車之前', language: 'zh-Hant', main_characters: ['林雨', '周遠'],
  expected_han_characters: countHanCodePoints(chapters.map(chapter => chapter.text).join('')),
  chapters,
};
await writeFile(new URL('manuscript.json', directory), JSON.stringify(source));
console.log(JSON.stringify({ novel_id: source.novel_id, han_count: source.expected_han_characters }));
