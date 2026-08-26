#!/usr/bin/env node

import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { request as httpsRequest } from "node:https";
import { pathToFileURL } from "node:url";

export const DEFAULT_BASE_URL = "https://127.0.0.1:8443";
export const E2E_PROFILE = "minecraft-10k";

const REQUEST_TIMEOUT_MS = 15_000;
const DEFAULT_TOKEN_FILE = ".local/storyblock/secrets/owner-token";
const HAN_CODE_POINTS = /\p{Script=Han}/gu;
const UUID_V7 = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-7[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$/;
const UTC_INSTANT = /^((?:\d{4}|\+\d{5,10}|-\d{4,10}))-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{3}|\d{6}|\d{9}))?Z$/;
const MANUSCRIPT_KEYS = [
  "novel_id",
  "created_at",
  "title",
  "language",
  "main_characters",
  "zombie_count",
  "tnt_cannon_count",
  "expected_han_characters",
  "chapters",
];
const CHAPTER_KEYS = ["title", "text"];

export class ContractError extends Error {
  constructor(message) {
    super(message);
    this.name = "ContractError";
  }
}

export class HttpError extends Error {
  constructor(statusCode, responseBody) {
    super(`StoryBlock returned HTTP ${statusCode}`);
    this.name = "HttpError";
    this.statusCode = statusCode;
    this.responseBody = responseBody;
  }
}

function requireObject(value, location) {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    throw new ContractError(`${location} must be a JSON object`);
  }
}

function requireExactKeys(value, expected, location) {
  const actual = Object.keys(value).sort();
  const wanted = [...expected].sort();
  if (actual.length !== wanted.length || actual.some((key, index) => key !== wanted[index])) {
    throw new ContractError(`${location} must contain exactly: ${wanted.join(", ")}`);
  }
}

function requireNonEmptyString(value, location) {
  if (typeof value !== "string" || value.trim() === "") {
    throw new ContractError(`${location} must be a non-empty string`);
  }
}

function requireNonNegativeInteger(value, location) {
  if (!Number.isSafeInteger(value) || value < 0) {
    throw new ContractError(`${location} must be a non-negative safe integer`);
  }
}

export function validateNovelId(value, location = "novel_id") {
  requireNonEmptyString(value, location);
  if (!value.startsWith("nov_") || !UUID_V7.test(value.slice(4))) {
    throw new ContractError(`${location} must be nov_<RFC 9562 UUIDv7>`);
  }
  return value;
}

function parseCanonicalYear(text) {
  if (/^\d{4}$/.test(text)) {
    return Number(text);
  }
  if (/^\+[1-9]\d{4,9}$/.test(text)) {
    const year = Number(text.slice(1));
    return year > 9_999 && year <= 1_000_000_000 ? year : null;
  }
  if (/^-(?:\d{4}|[1-9]\d{4,9})$/.test(text) && text !== "-0000") {
    const year = -Number(text.slice(1));
    return year >= -1_000_000_000 ? year : null;
  }
  return null;
}

function daysInMonth(year, month) {
  if (month === 2) {
    const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
    return leap ? 29 : 28;
  }
  return [4, 6, 9, 11].includes(month) ? 30 : 31;
}

export function validateCreatedAt(value, location = "created_at") {
  requireNonEmptyString(value, location);
  const match = UTC_INSTANT.exec(value);
  if (match === null) {
    throw new ContractError(`${location} must be canonical UTC Instant text`);
  }
  const [, yearText, monthText, dayText, hourText, minuteText, secondText, fraction] = match;
  const year = parseCanonicalYear(yearText);
  const month = Number(monthText);
  const day = Number(dayText);
  const hour = Number(hourText);
  const minute = Number(minuteText);
  const second = Number(secondText);
  const validDateTime = year !== null
    && month >= 1 && month <= 12
    && day >= 1 && day <= daysInMonth(year, month)
    && hour <= 23
    && minute <= 59
    && second <= 59;
  const canonicalFraction = fraction === undefined
    || (!/^0+$/.test(fraction) && (fraction.length === 3 || !fraction.endsWith("000")));
  if (!validDateTime || !canonicalFraction) {
    throw new ContractError(`${location} must be canonical UTC Instant text`);
  }
  return value;
}

export function hanSequence(text) {
  if (typeof text !== "string") {
    throw new ContractError("Han analysis input must be a string");
  }
  return (text.match(HAN_CODE_POINTS) ?? []).join("");
}

export function countHanCodePoints(text) {
  return Array.from(hanSequence(text)).length;
}

export function sha256(value) {
  return createHash("sha256").update(value, "utf8").digest("hex");
}

export function stableStringify(value) {
  if (value === null || typeof value !== "object") {
    const encoded = JSON.stringify(value);
    if (encoded === undefined) {
      throw new ContractError("Cannot encode an undefined JSON value");
    }
    return encoded;
  }
  if (Array.isArray(value)) {
    return `[${value.map(stableStringify).join(",")}]`;
  }
  return `{${Object.keys(value).sort().map((key) => `${JSON.stringify(key)}:${stableStringify(value[key])}`).join(",")}}`;
}

export function deriveIdempotencyKey(manuscript) {
  return `storyblock-${sha256(stableStringify(manuscript))}`;
}

function normalizeCharacterName(value, index) {
  requireNonEmptyString(value, `main_characters[${index}]`);
  return value.trim().normalize("NFC");
}

function validateProfile(manuscript, hanCount, profile) {
  if (profile === undefined) {
    return;
  }
  if (profile !== E2E_PROFILE) {
    throw new ContractError(`Unknown profile: ${profile}`);
  }
  if (hanCount !== 10_000 || manuscript.expected_han_characters !== 10_000) {
    throw new ContractError(`${E2E_PROFILE} requires exactly 10000 Han code points`);
  }
  if (manuscript.zombie_count !== 1_000) {
    throw new ContractError(`${E2E_PROFILE} requires zombie_count=1000`);
  }
  if (manuscript.tnt_cannon_count !== 1_000) {
    throw new ContractError(`${E2E_PROFILE} requires tnt_cannon_count=1000`);
  }
}

export function validateManuscript(manuscript, { profile } = {}) {
  requireObject(manuscript, "manuscript");
  requireExactKeys(manuscript, MANUSCRIPT_KEYS, "manuscript");
  validateNovelId(manuscript.novel_id);
  validateCreatedAt(manuscript.created_at);
  requireNonEmptyString(manuscript.title, "title");
  requireNonEmptyString(manuscript.language, "language");

  if (!Array.isArray(manuscript.main_characters) || manuscript.main_characters.length !== 5) {
    throw new ContractError("main_characters must contain exactly five names");
  }
  const normalizedNames = manuscript.main_characters.map(normalizeCharacterName);
  if (new Set(normalizedNames).size !== 5) {
    throw new ContractError("main_characters must contain exactly five unique names");
  }

  requireNonNegativeInteger(manuscript.zombie_count, "zombie_count");
  requireNonNegativeInteger(manuscript.tnt_cannon_count, "tnt_cannon_count");
  requireNonNegativeInteger(manuscript.expected_han_characters, "expected_han_characters");

  if (!Array.isArray(manuscript.chapters) || manuscript.chapters.length === 0) {
    throw new ContractError("chapters must contain at least one chapter");
  }
  manuscript.chapters.forEach((chapter, index) => {
    requireObject(chapter, `chapters[${index}]`);
    requireExactKeys(chapter, CHAPTER_KEYS, `chapters[${index}]`);
    requireNonEmptyString(chapter.title, `chapters[${index}].title`);
    if (typeof chapter.text !== "string") {
      throw new ContractError(`chapters[${index}].text must be a string`);
    }
  });

  const text = manuscript.chapters.map((chapter) => chapter.text).join("");
  const sequence = hanSequence(text);
  const count = Array.from(sequence).length;
  if (count !== manuscript.expected_han_characters) {
    throw new ContractError(`expected_han_characters=${manuscript.expected_han_characters}, calculated=${count}`);
  }
  validateProfile(manuscript, count, profile);

  return Object.freeze({
    hanSequence: sequence,
    hanCount: count,
    hanSha256: sha256(sequence),
  });
}

export function connectionPolicy(baseUrl = DEFAULT_BASE_URL) {
  let url;
  try {
    url = new URL(baseUrl);
  } catch {
    throw new ContractError(`Invalid base URL: ${baseUrl}`);
  }
  if (url.protocol !== "https:") {
    throw new ContractError("StoryBlock base URL must use HTTPS");
  }
  if (url.username !== "" || url.password !== "") {
    throw new ContractError("StoryBlock base URL must not contain credentials");
  }
  if (url.hostname.includes(":")) {
    throw new ContractError("StoryBlock helper supports IPv4 only");
  }
  return Object.freeze({
    origin: url.origin,
    rejectUnauthorized: false,
    family: 4,
  });
}

function endpointUrl(baseUrl, pathname, searchParams) {
  const policy = connectionPolicy(baseUrl);
  const url = new URL(pathname, `${policy.origin}/`);
  if (searchParams !== undefined) {
    url.search = searchParams.toString();
  }
  return { policy, url };
}

export async function requestJson({
  baseUrl = DEFAULT_BASE_URL,
  method = "GET",
  pathname,
  searchParams,
  headers = {},
  body,
  token,
}) {
  const { policy, url } = endpointUrl(baseUrl, pathname, searchParams);
  const payload = body === undefined ? undefined : stableStringify(body);
  const requestHeaders = { Accept: "application/json", ...headers };
  if (token !== undefined) {
    requireNonEmptyString(token, "bearer token");
    if (token.trim() !== token || /\s/.test(token)) {
      throw new ContractError("bearer token must not contain whitespace");
    }
    requestHeaders.Authorization = `Bearer ${token}`;
  }
  if (payload !== undefined) {
    requestHeaders["Content-Length"] = Buffer.byteLength(payload, "utf8");
  }

  return new Promise((resolve, reject) => {
    const request = httpsRequest(url, {
      method,
      headers: requestHeaders,
      family: policy.family,
      rejectUnauthorized: policy.rejectUnauthorized,
    }, (response) => {
      response.setEncoding("utf8");
      let responseBody = "";
      response.on("data", (chunk) => {
        responseBody += chunk;
      });
      response.on("end", () => {
        const statusCode = response.statusCode ?? 0;
        if (statusCode < 200 || statusCode >= 300) {
          reject(new HttpError(statusCode, responseBody));
          return;
        }
        if (responseBody.trim() === "") {
          resolve(null);
          return;
        }
        try {
          resolve(JSON.parse(responseBody));
        } catch {
          reject(new ContractError(`StoryBlock returned non-JSON content for ${pathname}`));
        }
      });
    });
    request.setTimeout(REQUEST_TIMEOUT_MS, () => {
      request.destroy(new Error(`StoryBlock request timed out after ${REQUEST_TIMEOUT_MS}ms`));
    });
    request.on("error", reject);
    if (payload !== undefined) {
      request.write(payload);
    }
    request.end();
  });
}

export function health({ baseUrl = DEFAULT_BASE_URL, token, request = requestJson } = {}) {
  return request({ baseUrl, pathname: "/actuator/health", token });
}

export function listNovels({
  baseUrl = DEFAULT_BASE_URL,
  page = 0,
  size = 50,
  query = "",
  token,
  request = requestJson,
} = {}) {
  requireNonNegativeInteger(page, "page");
  if (!Number.isSafeInteger(size) || size < 1) {
    throw new ContractError("size must be a positive safe integer");
  }
  if (typeof query !== "string") {
    throw new ContractError("query must be a string");
  }
  const searchParams = new URLSearchParams({ page: String(page), size: String(size), q: query });
  return request({ baseUrl, pathname: "/v1/admin/novels", searchParams, token });
}

export function readNovel({
  baseUrl = DEFAULT_BASE_URL,
  novelId,
  token,
  request = requestJson,
} = {}) {
  validateNovelId(novelId, "novelId");
  return request({
    baseUrl,
    pathname: `/v1/admin/novels/${encodeURIComponent(novelId)}`,
    token,
  });
}

export async function registerNovel({
  baseUrl = DEFAULT_BASE_URL,
  manuscript,
  idempotencyKey,
  profile,
  token,
  request = requestJson,
}) {
  const payloadText = stableStringify(manuscript);
  const payload = JSON.parse(payloadText);
  const analysis = validateManuscript(payload, { profile });
  const payloadSha256 = sha256(payloadText);
  const key = idempotencyKey ?? `storyblock-${payloadSha256}`;
  requireNonEmptyString(key, "idempotencyKey");
  const response = await request({
    baseUrl,
    method: "POST",
    pathname: "/v1/agent/novels",
    headers: {
      "Content-Type": "application/json",
      "If-Match": "*",
      "Idempotency-Key": key,
    },
    body: payload,
    token,
  });
  return {
    command: "register",
    idempotency_key: key,
    payload_sha256: payloadSha256,
    source: {
      novel_id: payload.novel_id,
      created_at: payload.created_at,
      han_character_count: analysis.hanCount,
      han_sha256: analysis.hanSha256,
    },
    response,
  };
}

function sameStringArray(left, right) {
  return Array.isArray(left)
    && Array.isArray(right)
    && left.length === right.length
    && left.every((value, index) => value === right[index]);
}

function persistedText(revision) {
  if (!Array.isArray(revision.chapters)) {
    throw new ContractError("detail.revision.chapters must be an array");
  }
  const blocks = [];
  revision.chapters.forEach((chapter, chapterIndex) => {
    requireObject(chapter, `detail.revision.chapters[${chapterIndex}]`);
    if (!Array.isArray(chapter.scenes)) {
      throw new ContractError(`detail.revision.chapters[${chapterIndex}].scenes must be an array`);
    }
    chapter.scenes.forEach((scene, sceneIndex) => {
      requireObject(scene, `detail.revision.chapters[${chapterIndex}].scenes[${sceneIndex}]`);
      if (!Array.isArray(scene.blocks)) {
        throw new ContractError(`detail.revision.chapters[${chapterIndex}].scenes[${sceneIndex}].blocks must be an array`);
      }
      scene.blocks.forEach((block, blockIndex) => {
        requireObject(block, `detail.revision.chapters[${chapterIndex}].scenes[${sceneIndex}].blocks[${blockIndex}]`);
        if (typeof block.text !== "string") {
          throw new ContractError(`detail.revision.chapters[${chapterIndex}].scenes[${sceneIndex}].blocks[${blockIndex}].text must be a string`);
        }
        blocks.push(block.text);
      });
    });
  });
  return blocks.join("");
}

export function verifyDetail(manuscript, detail, { novelId, profile } = {}) {
  const source = validateManuscript(manuscript, { profile });
  const targetNovelId = novelId ?? manuscript.novel_id;
  validateNovelId(targetNovelId, "novelId");
  if (targetNovelId !== manuscript.novel_id) {
    throw new ContractError("novelId must equal manuscript.novel_id");
  }
  requireObject(detail, "detail response");
  requireNonEmptyString(detail.schema_version, "detail.schema_version");
  requireObject(detail.novel, "detail.novel");
  requireObject(detail.revision, "detail.revision");
  const persistedSequence = hanSequence(persistedText(detail.revision));
  const persisted = {
    hanSequence: persistedSequence,
    hanCount: Array.from(persistedSequence).length,
    hanSha256: sha256(persistedSequence),
  };
  const checks = {
    novel_id: detail.novel.novel_id === manuscript.novel_id
      && detail.revision.novel_id === manuscript.novel_id,
    created_at: detail.revision.created_at === manuscript.created_at,
    han_sequence: persisted.hanSequence === source.hanSequence,
    han_count: persisted.hanCount === source.hanCount
      && detail.novel.han_character_count === source.hanCount,
    han_sha256: persisted.hanSha256 === source.hanSha256
      && detail.novel.han_text_sha256 === `sha256:${source.hanSha256}`,
    main_characters: sameStringArray(detail.novel.main_characters, manuscript.main_characters),
    zombie_count: detail.novel.zombie_count === manuscript.zombie_count,
    tnt_cannon_count: detail.novel.tnt_cannon_count === manuscript.tnt_cannon_count,
  };
  const failedChecks = Object.entries(checks)
    .filter(([, passed]) => !passed)
    .map(([name]) => name);

  return {
    ok: failedChecks.length === 0,
    novel_id: targetNovelId,
    schema_version: detail.schema_version,
    source: {
      novel_id: manuscript.novel_id,
      created_at: manuscript.created_at,
      han_character_count: source.hanCount,
      han_sha256: source.hanSha256,
      zombie_count: manuscript.zombie_count,
      tnt_cannon_count: manuscript.tnt_cannon_count,
    },
    persisted: {
      novel_id: detail.novel.novel_id,
      created_at: detail.revision.created_at,
      han_character_count: persisted.hanCount,
      han_sha256: persisted.hanSha256,
      zombie_count: detail.novel.zombie_count,
      tnt_cannon_count: detail.novel.tnt_cannon_count,
    },
    checks,
    failed_checks: failedChecks,
  };
}

export async function verifyNovel({
  baseUrl = DEFAULT_BASE_URL,
  novelId,
  manuscript,
  profile,
  token,
  request = requestJson,
}) {
  validateManuscript(manuscript, { profile });
  const targetNovelId = novelId ?? manuscript.novel_id;
  if (targetNovelId !== manuscript.novel_id) {
    throw new ContractError("novelId must equal manuscript.novel_id");
  }
  const detail = await readNovel({ baseUrl, novelId: targetNovelId, token, request });
  return verifyDetail(manuscript, detail, { novelId: targetNovelId, profile });
}

async function readManuscript(path) {
  requireNonEmptyString(path, "source path");
  let content;
  try {
    content = await readFile(path, "utf8");
  } catch (error) {
    throw new ContractError(`Cannot read source manuscript: ${error.message}`);
  }
  try {
    return JSON.parse(content);
  } catch (error) {
    throw new ContractError(`Source manuscript is not valid JSON: ${error.message}`);
  }
}

async function readToken(path, { optional = false } = {}) {
  requireNonEmptyString(path, "token file");
  let content;
  try {
    content = await readFile(path, "utf8");
  } catch (error) {
    if (optional && error.code === "ENOENT") {
      return undefined;
    }
    throw new ContractError(`Cannot read token file: ${error.message}`);
  }
  const token = content.trim();
  requireNonEmptyString(token, "bearer token");
  if (/\s/.test(token)) {
    throw new ContractError("bearer token must not contain whitespace");
  }
  return token;
}

function parseOptions(args) {
  const options = {};
  for (let index = 0; index < args.length; index += 1) {
    const token = args[index];
    if (!token.startsWith("--")) {
      throw new ContractError(`Unexpected argument: ${token}`);
    }
    const name = token.slice(2);
    const value = args[index + 1];
    if (value === undefined || value.startsWith("--")) {
      throw new ContractError(`Missing value for --${name}`);
    }
    if (Object.hasOwn(options, name)) {
      throw new ContractError(`Duplicate option: --${name}`);
    }
    options[name] = value;
    index += 1;
  }
  return options;
}

function rejectUnknownOptions(options, allowed) {
  const unknown = Object.keys(options).filter((name) => !allowed.includes(name));
  if (unknown.length > 0) {
    throw new ContractError(`Unknown option(s): ${unknown.map((name) => `--${name}`).join(", ")}`);
  }
}

function integerOption(value, fallback, name) {
  if (value === undefined) {
    return fallback;
  }
  if (!/^\d+$/.test(value)) {
    throw new ContractError(`--${name} must be an integer`);
  }
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed)) {
    throw new ContractError(`--${name} is outside the safe integer range`);
  }
  return parsed;
}

function requireOption(options, name) {
  const value = options[name];
  if (value === undefined) {
    throw new ContractError(`Missing required option: --${name}`);
  }
  return value;
}

function usage() {
  return `Usage:
  storyblock-author.mjs health [--base-url URL] [--token-file FILE]
  storyblock-author.mjs list [--page N] [--size N] [--query TEXT] [--base-url URL] [--token-file FILE]
  storyblock-author.mjs register --source FILE [--idempotency-key KEY] [--profile minecraft-10k] [--base-url URL] [--token-file FILE]
  storyblock-author.mjs read --novel-id ID [--base-url URL] [--token-file FILE]
  storyblock-author.mjs verify --source FILE [--novel-id ID] [--profile minecraft-10k] [--base-url URL] [--token-file FILE]`;
}

async function runCli(argv) {
  const [command, ...args] = argv;
  if (command === undefined || command === "help" || command === "--help") {
    process.stdout.write(`${usage()}\n`);
    return 0;
  }
  const options = parseOptions(args);
  const baseUrl = options["base-url"] ?? process.env.STORYBLOCK_BASE_URL ?? DEFAULT_BASE_URL;
  const explicitTokenFile = options["token-file"] ?? process.env.STORYBLOCK_TOKEN_FILE;
  const token = explicitTokenFile === undefined
    ? await readToken(DEFAULT_TOKEN_FILE, { optional: true })
    : await readToken(explicitTokenFile);
  let result;

  switch (command) {
    case "health":
      rejectUnknownOptions(options, ["base-url", "token-file"]);
      result = await health({ baseUrl, token });
      break;
    case "list":
      rejectUnknownOptions(options, ["base-url", "page", "size", "query", "token-file"]);
      result = await listNovels({
        baseUrl,
        page: integerOption(options.page, 0, "page"),
        size: integerOption(options.size, 50, "size"),
        query: options.query ?? "",
        token,
      });
      break;
    case "register": {
      rejectUnknownOptions(options, [
        "base-url", "source", "idempotency-key", "profile", "token-file",
      ]);
      const manuscript = await readManuscript(requireOption(options, "source"));
      result = await registerNovel({
        baseUrl,
        manuscript,
        idempotencyKey: options["idempotency-key"],
        profile: options.profile,
        token,
      });
      break;
    }
    case "read":
      rejectUnknownOptions(options, ["base-url", "novel-id", "token-file"]);
      result = await readNovel({
        baseUrl,
        novelId: requireOption(options, "novel-id"),
        token,
      });
      break;
    case "verify": {
      rejectUnknownOptions(options, [
        "base-url", "novel-id", "source", "profile", "token-file",
      ]);
      const manuscript = await readManuscript(requireOption(options, "source"));
      result = await verifyNovel({
        baseUrl,
        novelId: options["novel-id"],
        manuscript,
        profile: options.profile,
        token,
      });
      break;
    }
    default:
      throw new ContractError(`Unknown command: ${command}\n${usage()}`);
  }

  process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  return result?.ok === false ? 1 : 0;
}

async function main() {
  try {
    process.exitCode = await runCli(process.argv.slice(2));
  } catch (error) {
    if (error instanceof HttpError) {
      process.stderr.write(`${error.message}: ${error.responseBody}\n`);
    } else {
      process.stderr.write(`${error.name ?? "Error"}: ${error.message}\n`);
    }
    process.exitCode = 1;
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  await main();
}
