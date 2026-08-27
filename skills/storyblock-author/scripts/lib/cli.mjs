import { writeFile } from "node:fs/promises";

import { StoryBlockClient } from "./api-client.mjs";
import { getSchema, listDtos } from "./dtos.mjs";
import { getEndpoint, listEndpoints, loadEndpointManifest, materializeEndpoint } from "./endpoints.mjs";
import { createTypedId, ID_PREFIXES, requireTypedId } from "./ids.mjs";
import { deriveIdempotencyKey, readJsonFile, sha256, stableStringify } from "./json.mjs";
import {
  ApiResponseError,
  EXIT_CODES,
  NetworkError,
  UsageError,
  problemReport,
} from "./problem.mjs";
import { PayloadValidationError, countHanCodePoints, hanSequence, validateDto, validateInline } from "./validation.mjs";

const BOOLEAN_OPTIONS = new Set(["force", "help", "json"]);
const GLOBAL_OPTIONS = new Set(["access-key", "base-url", "help", "json", "timeout-ms", "user-agent"]);

const HELP = `Usage: storyblock-author <command> [options]

Discovery (offline):
  endpoints [--json]
  describe <endpoint-id> [--json]
  dtos [--json]
  validate --dto <DtoName> --file <payload.json> [--json]
  call <endpoint-id> [--params <params.json>] [--body <body.json>] [--json]
  id --prefix <nov|rev|op|blk|...> [--json]

Authoring and reads:
  health [--json]
  register --source <manuscript.json> [--idempotency-key <key>] [--json]
  verify --source <manuscript.json> [--novel-id <id>] [--json]
  list [--page 0] [--size 25] [--query <text>] [--json]
  read --novel-id <id> [--json]
  preview-edit --novel-id <id> --file <request.json> [--json]
  commit --novel-id <id> --file <request.json> [--json]
  render --novel-id <id> --file <request.json> [--json]
  export --novel-id <id> [--revision-id <id>] [--format canonical-revision|canonical-package] [--json]
  job --job-id <id> [--json]
  artifact --artifact-id <id> [--output <file>] [--force] [--json]

Connection options may appear after the command:
  --base-url <https-url>       STORYBLOCK_BASE_URL (default https://127.0.0.1:8443)
  --access-key <secret>        STORYBLOCK_ACCESS_KEY (optional in trusted-LAN mode)
  --timeout-ms <milliseconds>  STORYBLOCK_TIMEOUT_MS
  --user-agent <value>         STORYBLOCK_USER_AGENT

Generic call params JSON:
  {"path":{"novelId":"..."},"query":{"limit":50},"headers":{"If-Match":"\"sha256:...\"","Idempotency-Key":"..."}}
`;

export function parseCliArgs(argv) {
  if (!Array.isArray(argv)) throw new UsageError("CLI arguments must be an array");
  const command = argv[0];
  if (command === undefined || command === "-h" || command === "--help") {
    return { command: "help", options: new Map(), positionals: [] };
  }
  if (command.startsWith("-")) throw new UsageError("The command must be the first argument");
  const options = new Map();
  const positionals = [];
  for (let index = 1; index < argv.length; index += 1) {
    const token = argv[index];
    if (token === "-h") {
      options.set("help", true);
      continue;
    }
    if (!token.startsWith("--")) {
      positionals.push(token);
      continue;
    }
    const equals = token.indexOf("=");
    const name = token.slice(2, equals < 0 ? undefined : equals);
    if (!/^[a-z][a-z0-9-]*$/u.test(name)) throw new UsageError(`Invalid option: ${token}`);
    if (options.has(name)) throw new UsageError(`Option may appear only once: --${name}`);
    if (BOOLEAN_OPTIONS.has(name)) {
      if (equals >= 0) throw new UsageError(`--${name} does not accept a value`);
      options.set(name, true);
      continue;
    }
    const value = equals >= 0 ? token.slice(equals + 1) : argv[++index];
    if (value === undefined || (equals < 0 && value.startsWith("--"))) throw new UsageError(`--${name} requires a value`);
    options.set(name, value);
  }
  return { command, options, positionals };
}

function assertOptions(parsed, commandOptions = []) {
  const allowed = new Set([...GLOBAL_OPTIONS, ...commandOptions]);
  for (const name of parsed.options.keys()) {
    if (!allowed.has(name)) throw new UsageError(`Unknown option for ${parsed.command}: --${name}`);
  }
}

function requiredOption(parsed, name) {
  const value = parsed.options.get(name);
  if (typeof value !== "string" || value === "") throw new UsageError(`${parsed.command} requires --${name}`);
  return value;
}

function integerOption(parsed, name, fallback, minimum, maximum) {
  const raw = parsed.options.get(name);
  if (raw === undefined) return fallback;
  if (!/^-?\d+$/u.test(raw)) throw new UsageError(`--${name} must be an integer`);
  const value = Number(raw);
  if (!Number.isSafeInteger(value) || value < minimum || value > maximum) {
    throw new UsageError(`--${name} must be between ${minimum} and ${maximum}`);
  }
  return value;
}

function expectPositionals(parsed, count, label = "argument") {
  if (parsed.positionals.length !== count) {
    throw new UsageError(`${parsed.command} requires ${count} positional ${label}${count === 1 ? "" : "s"}`);
  }
}

function clientFor(parsed, overrides = {}) {
  return new StoryBlockClient({
    baseUrl: parsed.options.get("base-url") ?? process.env.STORYBLOCK_BASE_URL,
    accessKey: parsed.options.get("access-key") ?? process.env.STORYBLOCK_ACCESS_KEY,
    timeoutMs: parsed.options.get("timeout-ms") ?? process.env.STORYBLOCK_TIMEOUT_MS,
    userAgent: parsed.options.get("user-agent") ?? process.env.STORYBLOCK_USER_AGENT,
    ...overrides,
  });
}

function quoted(hash) {
  return `"${hash}"`;
}

function responseReport(response) {
  return { status: response.status, headers: response.headers, body: response.data };
}

async function fetchNovel(client, novelId) {
  requireTypedId(novelId, "nov", "novel_id");
  const headResponse = await client.request({ pathname: `/v1/novels/${encodeURIComponent(novelId)}` });
  await validateDto("NovelHead", headResponse.data);
  const revisionId = headResponse.data.head_revision_id;
  const revisionResponse = await client.request({
    pathname: `/v1/novels/${encodeURIComponent(novelId)}/revisions/${encodeURIComponent(revisionId)}`,
  });
  await validateDto("CanonicalRevision", revisionResponse.data);
  return {
    head: headResponse.data,
    head_headers: headResponse.headers,
    revision: revisionResponse.data,
    revision_headers: revisionResponse.headers,
  };
}

async function fetchRevision(client, novelId, revisionId) {
  requireTypedId(novelId, "nov", "novel_id");
  requireTypedId(revisionId, "rev", "revision_id");
  const response = await client.request({
    pathname: `/v1/novels/${encodeURIComponent(novelId)}/revisions/${encodeURIComponent(revisionId)}`,
  });
  await validateDto("CanonicalRevision", response.data);
  return response;
}

function manuscriptAnalysis(manuscript) {
  const text = manuscript.chapters.map((chapter) => chapter.text).join("");
  const sequence = hanSequence(text);
  return {
    sequence,
    count: countHanCodePoints(text),
    hash: `sha256:${sha256(sequence)}`,
  };
}

function persistedText(revision) {
  return revision.chapters.flatMap((chapter) => chapter.scenes)
    .flatMap((scene) => scene.blocks)
    .map((block) => block.text)
    .join("");
}

function verificationReport(source, persisted) {
  const expected = manuscriptAnalysis(source);
  const actualSequence = hanSequence(persistedText(persisted.revision));
  const actual = {
    sequence: actualSequence,
    count: Array.from(actualSequence).length,
    hash: `sha256:${sha256(actualSequence)}`,
  };
  const extensions = persisted.revision.extensions ?? {};
  const checks = {
    novel_id: persisted.revision.novel_id === source.novel_id,
    created_at: persisted.revision.created_at === source.created_at,
    han_sequence: actual.sequence === expected.sequence,
    han_count: actual.count === expected.count && extensions["han-character-count"] === expected.count,
    han_sha256: actual.hash === expected.hash && extensions["han-text-sha256"] === expected.hash,
    title: extensions.title === source.title,
    language: extensions.language === source.language,
    main_characters: stableStringify(extensions["main-characters"]) === stableStringify(source.main_characters),
    zombie_count: extensions["zombie-count"] === source.zombie_count,
    tnt_cannon_count: extensions["tnt-cannon-count"] === source.tnt_cannon_count,
    agent_registration: extensions["agent-write-registered"] === true,
  };
  return {
    command: "verify",
    ok: Object.values(checks).every(Boolean),
    novel_id: source.novel_id,
    head_revision_id: persisted.head.head_revision_id,
    checks,
    failed_checks: Object.entries(checks).filter(([, value]) => !value).map(([name]) => name),
    source: { han_character_count: expected.count, han_text_sha256: expected.hash },
    persisted: { han_character_count: actual.count, han_text_sha256: actual.hash },
  };
}

class VerificationError extends Error {
  constructor(report) {
    super(`Persistence verification failed: ${report.failed_checks.join(", ")}`);
    this.name = "VerificationError";
    this.exitCode = EXIT_CODES.VALIDATION;
    this.report = report;
  }
}

function caseInsensitiveHeader(headers, name) {
  const found = Object.entries(headers).find(([candidate]) => candidate.toLowerCase() === name.toLowerCase());
  return found?.[1];
}

async function validateCallParameters(endpoint, params) {
  for (const [groupName, descriptors] of [["path", endpoint.pathParameters], ["query", endpoint.queryParameters]]) {
    const group = params[groupName] ?? {};
    for (const descriptor of descriptors) {
      if (group[descriptor.name] === undefined) continue;
      const values = Array.isArray(group[descriptor.name]) && descriptor.schema.type !== "array"
        ? group[descriptor.name]
        : [group[descriptor.name]];
      for (const value of values) {
        const result = await validateInline(value, descriptor.schema);
        if (!result.valid) throw new PayloadValidationError(`${endpoint.id}:${descriptor.name}`, result.issues);
      }
    }
  }
}

async function execute(parsed, context) {
  if (parsed.command === "help" || parsed.options.get("help") === true) return { value: HELP, kind: "text" };

  switch (parsed.command) {
    case "endpoints": {
      assertOptions(parsed);
      expectPositionals(parsed, 0);
      const endpoints = await listEndpoints();
      if (parsed.options.get("json")) return { value: await loadEndpointManifest() };
      return { value: endpoints.map((entry) => `${entry.id}\t${entry.method}\t${entry.path}\t${entry.summary}`).join("\n"), kind: "text" };
    }
    case "describe": {
      assertOptions(parsed);
      expectPositionals(parsed, 1, "endpoint id");
      return { value: await getEndpoint(parsed.positionals[0]) };
    }
    case "dtos": {
      assertOptions(parsed);
      expectPositionals(parsed, 0);
      const dtos = await listDtos();
      if (parsed.options.get("json")) return { value: { count: dtos.length, dtos } };
      return { value: dtos.map((entry) => `${entry.name}\t${entry.confidence}\t${entry.description}`).join("\n"), kind: "text" };
    }
    case "validate": {
      assertOptions(parsed, ["dto", "file"]);
      expectPositionals(parsed, 0);
      const dto = requiredOption(parsed, "dto");
      await getSchema(dto);
      const file = requiredOption(parsed, "file");
      const value = await readJsonFile(file);
      const result = await validateDto(dto, value);
      return { value: { ...result, file } };
    }
    case "id": {
      assertOptions(parsed, ["prefix"]);
      expectPositionals(parsed, 0);
      const prefix = requiredOption(parsed, "prefix");
      if (!ID_PREFIXES.includes(prefix)) throw new UsageError(`--prefix must be one of: ${ID_PREFIXES.join(", ")}`);
      return { value: { prefix, id: createTypedId(prefix) } };
    }
    case "call": {
      assertOptions(parsed, ["body", "params"]);
      expectPositionals(parsed, 1, "endpoint id");
      const endpoint = await getEndpoint(parsed.positionals[0]);
      const params = parsed.options.has("params") ? await readJsonFile(parsed.options.get("params")) : {};
      await validateCallParameters(endpoint, params);
      const body = parsed.options.has("body") ? await readJsonFile(parsed.options.get("body")) : undefined;
      if (endpoint.requestBody?.required && body === undefined) throw new UsageError(`${endpoint.id} requires --body`);
      if (endpoint.requestBody && body !== undefined) await validateDto(endpoint.requestBody.dto, body);
      if (!endpoint.requestBody && body !== undefined) throw new UsageError(`${endpoint.id} does not accept a JSON request body`);
      const call = materializeEndpoint(endpoint, params);
      for (const header of endpoint.requestHeaders.filter((entry) => entry.required)) {
        if (caseInsensitiveHeader(call.headers, header.name) === undefined) throw new UsageError(`${endpoint.id} requires params.headers.${header.name}`);
      }
      if (["novels.edit-previews.create", "novels.commits.create"].includes(endpoint.id)) {
        const key = caseInsensitiveHeader(call.headers, "Idempotency-Key");
        const etag = caseInsensitiveHeader(call.headers, "If-Match");
        if (body.operation.idempotency_key !== key) throw new PayloadValidationError(endpoint.requestBody.dto, [{ path: "#/operation/idempotency_key", message: "must equal the Idempotency-Key header" }]);
        if (quoted(body.operation.expected_head_hash) !== etag) throw new PayloadValidationError(endpoint.requestBody.dto, [{ path: "#/operation/expected_head_hash", message: "must equal the unquoted If-Match header value" }]);
      }
      const response = await clientFor(parsed, context.clientOverrides).request({
        ...call,
        body,
        responseType: endpoint.id === "artifacts.download" ? "buffer" : "auto",
      });
      return { value: responseReport(response) };
    }
    case "health": {
      assertOptions(parsed);
      expectPositionals(parsed, 0);
      const response = await clientFor(parsed, context.clientOverrides).request({ pathname: "/actuator/health" });
      return { value: responseReport(response) };
    }
    case "list": {
      assertOptions(parsed, ["page", "query", "size"]);
      expectPositionals(parsed, 0);
      const page = integerOption(parsed, "page", 0, 0, Number.MAX_SAFE_INTEGER);
      const size = integerOption(parsed, "size", 25, 1, 100);
      const query = parsed.options.get("query") ?? "";
      const search = new URLSearchParams({ page: String(page), size: String(size), q: query });
      const response = await clientFor(parsed, context.clientOverrides).request({ pathname: `/v1/admin/novels?${search}` });
      return { value: responseReport(response) };
    }
    case "read": {
      assertOptions(parsed, ["novel-id"]);
      expectPositionals(parsed, 0);
      const value = await fetchNovel(clientFor(parsed, context.clientOverrides), requiredOption(parsed, "novel-id"));
      return { value };
    }
    case "register": {
      assertOptions(parsed, ["idempotency-key", "source"]);
      expectPositionals(parsed, 0);
      const sourcePath = requiredOption(parsed, "source");
      const manuscript = await readJsonFile(sourcePath);
      await validateDto("AgentNovelRegistrationRequest", manuscript);
      const analysis = manuscriptAnalysis(manuscript);
      const idempotencyKey = parsed.options.get("idempotency-key") ?? deriveIdempotencyKey("register", manuscript);
      if (idempotencyKey.length < 1 || idempotencyKey.length > 200) throw new UsageError("--idempotency-key must contain 1 to 200 characters");
      const response = await clientFor(parsed, context.clientOverrides).request({
        method: "POST",
        pathname: "/v1/agent/novels",
        headers: { "If-Match": "*", "Idempotency-Key": idempotencyKey },
        body: manuscript,
      });
      return { value: {
        command: "register",
        idempotency_key: idempotencyKey,
        payload_sha256: sha256(stableStringify(manuscript)),
        source: { novel_id: manuscript.novel_id, created_at: manuscript.created_at, han_character_count: analysis.count, han_text_sha256: analysis.hash },
        http: responseReport(response),
      } };
    }
    case "verify": {
      assertOptions(parsed, ["novel-id", "source"]);
      expectPositionals(parsed, 0);
      const manuscript = await readJsonFile(requiredOption(parsed, "source"));
      await validateDto("AgentNovelRegistrationRequest", manuscript);
      const novelId = parsed.options.get("novel-id") ?? manuscript.novel_id;
      if (novelId !== manuscript.novel_id) throw new UsageError("--novel-id must equal source novel_id");
      const persisted = await fetchNovel(clientFor(parsed, context.clientOverrides), novelId);
      const report = verificationReport(manuscript, persisted);
      if (!report.ok) throw new VerificationError(report);
      return { value: report };
    }
    case "preview-edit":
    case "commit": {
      assertOptions(parsed, ["file", "novel-id"]);
      expectPositionals(parsed, 0);
      const novelId = requiredOption(parsed, "novel-id");
      requireTypedId(novelId, "nov", "novel_id");
      const body = await readJsonFile(requiredOption(parsed, "file"));
      const dto = parsed.command === "commit" ? "CommitRequest" : "EditPreviewRequest";
      await validateDto(dto, body);
      if (body.operation.novel_id !== novelId) throw new PayloadValidationError(dto, [{ path: "#/operation/novel_id", message: "must equal --novel-id" }]);
      const pathname = parsed.command === "commit" ? "commits" : "edit-previews";
      const response = await clientFor(parsed, context.clientOverrides).request({
        method: "POST",
        pathname: `/v1/novels/${encodeURIComponent(novelId)}/${pathname}`,
        headers: {
          "If-Match": quoted(body.operation.expected_head_hash),
          "Idempotency-Key": body.operation.idempotency_key,
        },
        body,
      });
      return { value: responseReport(response) };
    }
    case "render": {
      assertOptions(parsed, ["file", "novel-id"]);
      expectPositionals(parsed, 0);
      const novelId = requiredOption(parsed, "novel-id");
      const body = await readJsonFile(requiredOption(parsed, "file"));
      await validateDto("RenderRequest", body);
      const client = clientFor(parsed, context.clientOverrides);
      const revision = await fetchRevision(client, novelId, body.revision_id);
      const hash = revision.data.content_hash;
      const response = await client.request({
        method: "POST", pathname: `/v1/novels/${encodeURIComponent(novelId)}/renders`,
        headers: { "If-Match": quoted(hash), "Idempotency-Key": deriveIdempotencyKey("render", { novelId, body }) }, body,
      });
      return { value: responseReport(response) };
    }
    case "export": {
      assertOptions(parsed, ["format", "idempotency-key", "novel-id", "revision-id"]);
      expectPositionals(parsed, 0);
      const novelId = requiredOption(parsed, "novel-id");
      const client = clientFor(parsed, context.clientOverrides);
      let revisionId = parsed.options.get("revision-id");
      let hash;
      if (revisionId === undefined) {
        const novel = await fetchNovel(client, novelId);
        revisionId = novel.head.head_revision_id;
        hash = novel.head.head_hash;
      } else {
        const revision = await fetchRevision(client, novelId, revisionId);
        hash = revision.data.content_hash;
      }
      const body = { revision_id: revisionId, format: parsed.options.get("format") ?? "canonical-revision" };
      await validateDto("ExportRequest", body);
      const response = await client.request({
        method: "POST", pathname: `/v1/novels/${encodeURIComponent(novelId)}/exports`,
        headers: { "If-Match": quoted(hash), "Idempotency-Key": parsed.options.get("idempotency-key") ?? deriveIdempotencyKey("export", { novelId, body }) }, body,
      });
      return { value: responseReport(response) };
    }
    case "job": {
      assertOptions(parsed, ["job-id"]);
      expectPositionals(parsed, 0);
      const jobId = requiredOption(parsed, "job-id");
      requireTypedId(jobId, "job", "job_id");
      const response = await clientFor(parsed, context.clientOverrides).request({ pathname: `/v1/jobs/${encodeURIComponent(jobId)}` });
      return { value: responseReport(response) };
    }
    case "artifact": {
      assertOptions(parsed, ["artifact-id", "force", "output"]);
      expectPositionals(parsed, 0);
      const artifactId = requiredOption(parsed, "artifact-id");
      requireTypedId(artifactId, "art", "artifact_id");
      const response = await clientFor(parsed, context.clientOverrides).request({ pathname: `/v1/artifacts/${encodeURIComponent(artifactId)}`, responseType: "buffer" });
      const output = parsed.options.get("output");
      if (output !== undefined) {
        try {
          await writeFile(output, response.data, { flag: parsed.options.get("force") ? "w" : "wx", mode: 0o600 });
        } catch (error) {
          if (error.code === "EEXIST") throw new UsageError(`Refusing to overwrite ${output}; pass --force to replace it`);
          throw error;
        }
      }
      return { value: {
        status: response.status,
        headers: response.headers,
        artifact_id: artifactId,
        bytes: response.data.length,
        output: output ?? null,
        content_base64: output === undefined ? response.data.toString("base64") : undefined,
      } };
    }
    default:
      throw new UsageError(`Unknown command: ${parsed.command}`);
  }
}

function writeValue(io, value, { json = false, kind } = {}) {
  if (kind === "text") {
    io.stdout(value.endsWith("\n") ? value : `${value}\n`);
    return;
  }
  io.stdout(`${JSON.stringify(value, null, json ? 0 : 2)}\n`);
}

function errorValue(error) {
  if (error instanceof ApiResponseError) return { error: error.name, ...problemReport(error) };
  if (error instanceof PayloadValidationError) return { error: error.name, message: error.message, dto: error.dto, issues: error.issues };
  if (error instanceof VerificationError) return error.report;
  return { error: error.name ?? "Error", message: error.message };
}

export async function runCli(argv, {
  stdout = (text) => process.stdout.write(text),
  stderr = (text) => process.stderr.write(text),
  clientOverrides = {},
} = {}) {
  let parsed;
  try {
    parsed = parseCliArgs(argv);
    const result = await execute(parsed, { clientOverrides });
    writeValue({ stdout, stderr }, result.value, { json: parsed.options.get("json") === true, kind: result.kind });
    return EXIT_CODES.SUCCESS;
  } catch (error) {
    const json = parsed?.options.get("json") === true;
    const exitCode = Number.isInteger(error.exitCode) ? error.exitCode
      : error instanceof ApiResponseError ? error.exitCode
        : error instanceof NetworkError ? EXIT_CODES.NETWORK
          : EXIT_CODES.FAILURE;
    if (error instanceof VerificationError) {
      writeValue({ stdout, stderr }, error.report, { json });
    } else if (json) {
      stderr(`${JSON.stringify(errorValue(error))}\n`);
    } else if (error instanceof ApiResponseError) {
      const report = problemReport(error);
      stderr(`StoryBlock API error ${report.status} ${report.code ?? ""}\n${report.detail ?? error.message}\nrequest_id: ${report.request_id ?? "unavailable"}\n`);
      if (report.retry_after !== null) stderr(`retry_after: ${report.retry_after}\n`);
    } else if (error instanceof PayloadValidationError) {
      stderr(`${error.message}\n${error.issues.map((entry) => `${entry.path}: ${entry.message}`).join("\n")}\n`);
    } else {
      stderr(`${error.message}\n`);
      if (error instanceof UsageError) stderr("Run storyblock-author --help for usage.\n");
    }
    return exitCode;
  }
}

export { HELP };
