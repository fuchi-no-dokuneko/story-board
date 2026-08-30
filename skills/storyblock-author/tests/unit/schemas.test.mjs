import assert from "node:assert/strict";
import { test } from "node:test";

import { getSchema, listDtos, loadSchemas } from "../../scripts/lib/dtos.mjs";
import { countHanCodePoints, validateDto } from "../../scripts/lib/validation.mjs";

test("all DTO schemas parse and every reference resolves", async () => {
  const dtos = await listDtos();
  const schemas = await loadSchemas();
  assert.equal(dtos.length, 140);
  assert.equal(schemas.size, 140);

  const visit = (value, owner) => {
    if (Array.isArray(value)) {
      value.forEach((entry) => visit(entry, owner));
      return;
    }
    if (value === null || typeof value !== "object") return;
    if (typeof value.$ref === "string" && !value.$ref.startsWith("#/")) {
      assert.ok(schemas.has(value.$ref.replace(/\.schema\.json$/u, "")), `${owner}: unresolved ${value.$ref}`);
    }
    Object.values(value).forEach((entry) => visit(entry, owner));
  };

  for (const { name } of dtos) {
    const schema = await getSchema(name);
    assert.equal(schema.$schema, "https://json-schema.org/draft/2020-12/schema");
    assert.equal(typeof schema.title, "string");
    assert.ok(schema.title.length > 0);
    assert.ok(schema.description.length > 0);
    assert.ok(schema.examples.length > 0);
    assert.ok(schema["x-source"].length > 0);
    visit(schema, name);
  }
});

test("every embedded DTO example validates locally", async (context) => {
  for (const { name } of await listDtos()) {
    await context.test(name, async () => {
      const schema = await getSchema(name);
      for (const example of schema.examples) {
        const result = await validateDto(name, example, { throwOnError: false });
        assert.deepEqual(result.issues, []);
        assert.equal(result.valid, true);
      }
    });
  }
});

test("registration validation enforces exact Han count and unknown fields", async () => {
  const valid = structuredClone((await getSchema("AgentNovelRegistrationRequest")).examples[0]);
  assert.equal(countHanCodePoints(valid.chapters[0].text), 4);
  assert.equal((await validateDto("AgentNovelRegistrationRequest", valid)).valid, true);

  await assert.rejects(
    validateDto("AgentNovelRegistrationRequest", { ...valid, expected_han_characters: 5 }),
    (error) => error.issues.some(({ message }) => /chapter text contains 4 Han code points/u.test(message)),
  );
  await assert.rejects(
    validateDto("AgentNovelRegistrationRequest", { ...valid, invented: true }),
    /validation failed/u,
  );
});

test("block validation mirrors complete one-or-two-sentence boundaries", async () => {
  const valid = structuredClone((await getSchema("BlockDraft")).examples[0]);
  valid.text = "溫度是 3.14 度。結果正常！";
  assert.equal((await validateDto("BlockDraft", valid)).valid, true);

  const threeSentences = await validateDto("BlockDraft", { ...valid, text: "一。二。三。" }, { throwOnError: false });
  assert.equal(threeSentences.valid, false);
  assert.ok(threeSentences.issues.some(({ message }) => message.includes("found 3")));

  const incomplete = await validateDto("BlockDraft", { ...valid, text: "仲未講完" }, { throwOnError: false });
  assert.equal(incomplete.valid, false);
  assert.ok(incomplete.issues.some(({ message }) => message.includes("complete sentence boundary")));
});

test("registration rejects a sentence that the server cannot fit in a block", async () => {
  const value = structuredClone((await getSchema("AgentNovelRegistrationRequest")).examples[0]);
  value.chapters[0].text = `${"字".repeat(100)}。`;
  value.expected_han_characters = 100;
  const result = await validateDto("AgentNovelRegistrationRequest", value, { throwOnError: false });

  assert.equal(result.valid, false);
  assert.ok(result.issues.some(({ path, message }) => path === "#/chapters/0/text" && message.includes("longer than 100")));
});

test("operation range guards bind first and last expected block ids", async () => {
  const value = structuredClone((await getSchema("DeleteBlockRangeOperation")).examples[0]);
  value.payload.range.first_block_id = "blk_018f0f5e-7b4a-7c00-8000-000000000099";
  const result = await validateDto("DeleteBlockRangeOperation", value, { throwOnError: false });
  assert.equal(result.valid, false);
  assert.ok(result.issues.some(({ message }) => message.includes("first expected_blocks")));
});

test("character image references require five identities and plain-background variants", async () => {
  const value = structuredClone((await getSchema("CharacterImageReferenceConfig")).examples[0]);
  assert.equal((await validateDto("CharacterImageReferenceConfig", value)).valid, true);

  const fourCharacters = structuredClone(value);
  fourCharacters.characters.pop();
  const missingCharacter = await validateDto("CharacterImageReferenceConfig", fourCharacters, { throwOnError: false });
  assert.equal(missingCharacter.valid, false);
  assert.ok(missingCharacter.issues.some(({ message }) => message.includes("at least 5")));

  const oneVariant = structuredClone(value);
  oneVariant.characters[0].variants.pop();
  const missingVariant = await validateDto("CharacterImageReferenceConfig", oneVariant, { throwOnError: false });
  assert.equal(missingVariant.valid, false);
  assert.ok(missingVariant.issues.some(({ message }) => message.includes("at least 2")));

  const nonPlain = structuredClone(value);
  nonPlain.characters[0].variants[0].background = "harbor";
  const background = await validateDto("CharacterImageReferenceConfig", nonPlain, { throwOnError: false });
  assert.equal(background.valid, false);
  assert.ok(background.issues.some(({ path, message }) => path.endsWith("/background") && message.includes("plain")));
});
