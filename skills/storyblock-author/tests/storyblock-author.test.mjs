import assert from "node:assert/strict";
import { test } from "node:test";

import {
  ContractError,
  E2E_PROFILE,
  connectionPolicy,
  countHanCodePoints,
  deriveIdempotencyKey,
  hanSequence,
  health,
  listNovels,
  readNovel,
  registerNovel,
  stableStringify,
  validateCreatedAt,
  validateManuscript,
  validateNovelId,
  verifyDetail,
} from "../scripts/storyblock-author.mjs";

const NOVEL_ID = "nov_018f0f5e-7b4a-7c00-8000-000000000001";
const OTHER_NOVEL_ID = "nov_018f0f5e-7b4a-7c00-8000-000000000002";
const CREATED_AT = "2026-08-24T12:00:00Z";

function manuscript(overrides = {}) {
  return {
    novel_id: NOVEL_ID,
    created_at: CREATED_AT,
    title: "測試稿",
    language: "zh-Hant",
    main_characters: ["甲", "乙", "丙", "丁", "戊"],
    zombie_count: 2,
    tnt_cannon_count: 3,
    expected_han_characters: 3,
    chapters: [{ title: "第一章", text: "甲，𠀀 A乙!" }],
    ...overrides,
  };
}

test("Han analysis counts Unicode code points and excludes non-Han content", () => {
  const text = "甲，𠀀 A乙🙂";
  assert.equal(hanSequence(text), "甲𠀀乙");
  assert.equal(countHanCodePoints(text), 3);
  assert.equal(Array.from(hanSequence(text)).length, 3);
  assert.equal(validateManuscript(manuscript()).hanCount, 3);
});

test("registration metadata uses a typed RFC 9562 UUIDv7 and canonical UTC Instant", () => {
  assert.equal(validateNovelId(NOVEL_ID), NOVEL_ID);
  assert.throws(
    () => validateNovelId("nov_018f0f5e-7b4a-6c00-8000-000000000001"),
    /RFC 9562 UUIDv7/,
  );
  assert.throws(
    () => validateNovelId("nov_018f0f5e-7b4a-7c00-7000-000000000001"),
    /RFC 9562 UUIDv7/,
  );
  assert.throws(() => validateNovelId(`book_${NOVEL_ID.slice(4)}`), /nov_</);

  for (const instant of [
    CREATED_AT,
    "2026-08-24T12:00:00.123Z",
    "2026-08-24T12:00:00.123456Z",
    "2026-08-24T12:00:00.123456789Z",
  ]) {
    assert.equal(validateCreatedAt(instant), instant);
  }
  for (const instant of [
    "2026-08-24T12:00:00+00:00",
    "2026-08-24T12:00:00.1Z",
    "2026-08-24T12:00:00.000Z",
    "2026-08-24T12:00:00.123000Z",
    "2026-02-29T12:00:00Z",
  ]) {
    assert.throws(() => validateCreatedAt(instant), /canonical UTC Instant/);
  }
});

test("manuscript requires exactly the nine registration fields", () => {
  const missingId = manuscript();
  delete missingId.novel_id;
  assert.throws(() => validateManuscript(missingId), /must contain exactly/);
  assert.throws(
    () => validateManuscript({ ...manuscript(), generated_at: CREATED_AT }),
    /must contain exactly/,
  );
});

test("manuscript validation requires five unique normalized names", () => {
  const duplicate = manuscript({ main_characters: ["甲", "乙", "丙", "丁", " 甲 "] });
  assert.throws(
    () => validateManuscript(duplicate),
    (error) => error instanceof ContractError && /five unique names/.test(error.message),
  );
});

test("manuscript validation rejects a declared Han count mismatch", () => {
  assert.throws(
    () => validateManuscript(manuscript({ expected_han_characters: 4 })),
    /expected_han_characters=4, calculated=3/,
  );
});

test("minecraft E2E profile enforces exact requested aggregate values", () => {
  const valid = manuscript({
    zombie_count: 1_000,
    tnt_cannon_count: 1_000,
    expected_han_characters: 10_000,
    chapters: [{ title: "長篇", text: "漢".repeat(10_000) }],
  });
  assert.equal(validateManuscript(valid, { profile: E2E_PROFILE }).hanCount, 10_000);
  assert.throws(
    () => validateManuscript({ ...valid, zombie_count: 999 }, { profile: E2E_PROFILE }),
    /zombie_count=1000/,
  );
  assert.throws(
    () => validateManuscript({ ...valid, tnt_cannon_count: 999 }, { profile: E2E_PROFILE }),
    /tnt_cannon_count=1000/,
  );
});

test("registration retries reuse the exact complete payload and deterministic key", async () => {
  const source = manuscript();
  const calls = [];
  const request = async (input) => {
    calls.push(input);
    return { novel_id: NOVEL_ID };
  };

  const first = await registerNovel({ manuscript: source, token: "test-owner-token", request });
  const reordered = Object.fromEntries(Object.entries(structuredClone(source)).reverse());
  const second = await registerNovel({ manuscript: reordered, request });

  assert.equal(calls[0].method, "POST");
  assert.equal(calls[0].pathname, "/v1/agent/novels");
  assert.equal(calls[0].headers["If-Match"], "*");
  assert.equal(calls[0].headers["Content-Type"], "application/json");
  assert.equal(calls[0].headers["Idempotency-Key"], deriveIdempotencyKey(source));
  assert.equal(calls[0].token, "test-owner-token");
  assert.equal(first.idempotency_key, second.idempotency_key);
  assert.equal(first.payload_sha256, second.payload_sha256);
  assert.equal(stableStringify(calls[0].body), stableStringify(calls[1].body));
  assert.equal(calls[0].body.novel_id, NOVEL_ID);
  assert.equal(calls[0].body.created_at, CREATED_AT);
  assert.notEqual(
    deriveIdempotencyKey(source),
    deriveIdempotencyKey({ ...source, novel_id: OTHER_NOVEL_ID }),
  );
  assert.notEqual(
    deriveIdempotencyKey(source),
    deriveIdempotencyKey({ ...source, created_at: "2026-08-24T12:00:01Z" }),
  );
});

test("list uses the contracted pagination and query names", async () => {
  let call;
  await listNovels({
    page: 0,
    size: 50,
    query: "村莊",
    request: async (input) => {
      call = input;
      return {};
    },
  });
  assert.equal(call.pathname, "/v1/admin/novels");
  assert.equal(call.searchParams.toString(), "page=0&size=50&q=%E6%9D%91%E8%8E%8A");
});

test("health and read use their exact contracted endpoints", async () => {
  const calls = [];
  const request = async (input) => {
    calls.push(input);
    return {};
  };
  await health({ request });
  await readNovel({ novelId: NOVEL_ID, request });
  assert.equal(calls[0].pathname, "/actuator/health");
  assert.equal(calls[1].pathname, `/v1/admin/novels/${NOVEL_ID}`);
  assert.throws(() => readNovel({ novelId: "nov_invalid", request }), /RFC 9562 UUIDv7/);
});

test("verification compares Han sequence, digest, and aggregate metadata", () => {
  const source = manuscript();
  const matchingDetail = {
    schema_version: "1.0.0",
    novel: {
      novel_id: source.novel_id,
      main_characters: [...source.main_characters],
      zombie_count: source.zombie_count,
      tnt_cannon_count: source.tnt_cannon_count,
      han_character_count: source.expected_han_characters,
      han_text_sha256: `sha256:${validateManuscript(source).hanSha256}`,
    },
    revision: {
      novel_id: source.novel_id,
      created_at: source.created_at,
      chapters: [{
        scenes: [{
          blocks: [{ text: "甲!" }, { text: "𠀀\n乙" }],
        }],
      }],
    },
  };
  const matching = verifyDetail(source, matchingDetail);
  assert.equal(matching.ok, true);
  assert.deepEqual(matching.failed_checks, []);
  assert.equal(matching.source.han_sha256, matching.persisted.han_sha256);

  const mismatching = verifyDetail(source, {
    ...matchingDetail,
    novel: {
      ...matchingDetail.novel,
      novel_id: OTHER_NOVEL_ID,
      zombie_count: 999,
    },
    revision: {
      ...matchingDetail.revision,
      created_at: "2026-08-24T12:00:01Z",
      chapters: [{ scenes: [{ blocks: [{ text: "甲𠀀" }] }] }],
    },
  });
  assert.equal(mismatching.ok, false);
  assert.deepEqual(mismatching.failed_checks, [
    "novel_id",
    "created_at",
    "han_sequence",
    "han_count",
    "han_sha256",
    "zombie_count",
  ]);
  assert.throws(
    () => verifyDetail(source, matchingDetail, { novelId: OTHER_NOVEL_ID }),
    /must equal manuscript.novel_id/,
  );
});

test("self-signed TLS is limited to local/private IPv4 endpoints", () => {
  assert.equal(connectionPolicy("https://127.0.0.1:8443").rejectUnauthorized, false);
  assert.equal(connectionPolicy("https://10.1.2.3:8443").rejectUnauthorized, false);
  assert.equal(connectionPolicy("https://172.31.1.2:8443").rejectUnauthorized, false);
  assert.equal(connectionPolicy("https://192.168.1.2:8443").rejectUnauthorized, false);
  assert.equal(connectionPolicy("https://100.64.1.2:8443").rejectUnauthorized, false);
  assert.equal(connectionPolicy("https://localhost:8443").rejectUnauthorized, false);
  assert.throws(
    () => connectionPolicy("https://storyblock.example"),
    /only localhost or private IPv4/,
  );
  assert.throws(() => connectionPolicy("http://127.0.0.1:8080"), /must use HTTPS/);
  assert.throws(() => connectionPolicy("https://[::1]:8443"), /IPv4 only/);
});
