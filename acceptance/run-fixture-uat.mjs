import { readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
const source = JSON.parse(readFileSync('.local/refactor-evidence/gui/manuscript.json', 'utf8'));
const child = spawnSync('./acceptance/run-uat.sh',
  ['--headless', '--base-url', process.env.STORYBLOCK_BASE_URL || 'https://127.0.0.1:18443/'], {
    stdio: 'inherit', env: {
      ...process.env, STORYBLOCK_UAT_NOVEL_ID: source.novel_id,
      STORYBLOCK_UAT_NOVEL_TITLE: source.title,
      STORYBLOCK_UAT_CHARACTER: source.main_characters[0],
      STORYBLOCK_UAT_TEXT_MARKER: source.chapters[0].text.slice(0, 10),
      STORYBLOCK_UAT_SOURCE: '.local/refactor-evidence/gui/manuscript.json',
    },
  });
if (child.error) throw child.error;
process.exit(child.status ?? 1);
