import assert from "node:assert/strict";
import { test } from "node:test";

import { getSchema, listDtos, loadSchemas } from "../../scripts/lib/dtos.mjs";
import { countHanCodePoints, validateDto } from "../../scripts/lib/validation.mjs";

test("all DTO schemas parse and every reference resolves", async () => {
  const dtos = await listDtos();
  const schemas = await loadSchemas();
  assert.equal(dtos.length, 135);
  assert.equal(schemas.size, 135);

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

test("operation range guards bind first and last expected block ids", async () => {
  const value = structuredClone((await getSchema("DeleteBlockRangeOperation")).examples[0]);
  value.payload.range.first_block_id = "blk_018f0f5e-7b4a-7c00-8000-000000000099";
  const result = await validateDto("DeleteBlockRangeOperation", value, { throwOnError: false });
  assert.equal(result.valid, false);
  assert.ok(result.issues.some(({ message }) => message.includes("first expected_blocks")));
});
