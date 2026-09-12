const { Then } = require('@cucumber/cucumber');
const { readFileSync } = require('node:fs');
const assert = require('node:assert/strict');
const runtime = require('../../runtime.cjs');

Then('the reader matches the registered manuscript', async function () {
  const source = JSON.parse(readFileSync(process.env.STORYBLOCK_UAT_SOURCE || '.local/refactor-evidence/gui/manuscript.json', 'utf8'));
  const driver = await runtime.ensureBrowser(this);
  const actual = await driver.evaluate(`({
    stats: [...document.querySelectorAll('.novel-stats dt')].map(node => node.textContent),
    han: document.getElementById('stat-han').textContent,
    characters: [...document.querySelectorAll('#character-list li')].map(node => node.textContent),
    text: [...document.querySelectorAll('.story-block')].map(node => node.textContent).join(''),
    chapters: document.querySelectorAll('#chapter-nav .chapter-link').length,
  })`);
  assert.deepEqual(actual.stats, ['Han characters', 'Chapters', 'Scenes', 'Blocks']);
  assert.equal(Number(actual.han.replaceAll(',', '')), source.expected_han_characters);
  assert.deepEqual(actual.characters, source.main_characters.length ? source.main_characters : ['Not recorded']);
  const compact = text => text.normalize('NFC').replace(/\s+/gu, '');
  assert.equal(compact(actual.text), compact(source.chapters.map(chapter => chapter.text).join('')));
  assert.equal(actual.chapters, source.chapters.length);
});
