import assert from 'node:assert/strict';
import { test } from 'node:test';
import { getSchema } from '../../scripts/lib/dtos.mjs';
import { validateDto } from '../../scripts/lib/validation.mjs';

test('ordinary manuscripts allow varying casts and non-Han text without demo metadata', async () => {
  const example = (await getSchema('AgentNovelRegistrationRequest')).examples[0];
  for (const main_characters of [[], ['Mina'], ['Mina', 'Sam', 'Jo', 'Lee', 'Pat', 'Alex']]) {
    const request = { ...example, language: 'en', main_characters, expected_han_characters: 0,
      chapters: [{ title: 'Rain', text: 'Rain taps the roof. Mina opens the door.' }] };
    assert.equal((await validateDto('AgentNovelRegistrationRequest', request)).valid, true);
    await assert.rejects(validateDto('AgentNovelRegistrationRequest', { ...request, zombie_count: 0 }), /validation failed/);
    await assert.rejects(validateDto('AgentNovelRegistrationRequest', { ...request, tnt_cannon_count: 0 }), /validation failed/);
  }
});
