import { parseJsonStrict } from "./json.mjs";

export const EXIT_CODES = Object.freeze({
  SUCCESS: 0,
  FAILURE: 1,
  USAGE: 2,
  VALIDATION: 3,
  AUTH: 4,
  API: 5,
  NETWORK: 6,
  UNKNOWN: 7,
});

export class UsageError extends Error {
  constructor(message) {
    super(message);
    this.name = "UsageError";
    this.exitCode = EXIT_CODES.USAGE;
  }
}

export class UnknownResourceError extends Error {
  constructor(message) {
    super(message);
    this.name = "UnknownResourceError";
    this.exitCode = EXIT_CODES.UNKNOWN;
  }
}

export class NetworkError extends Error {
  constructor(message, cause) {
    super(message, cause === undefined ? undefined : { cause });
    this.name = "NetworkError";
    this.exitCode = EXIT_CODES.NETWORK;
  }
}

export function parseProblemBody(body, status, headers = {}) {
  const text = Buffer.isBuffer(body) ? body.toString("utf8") : String(body ?? "");
  let parsed;
  try {
    parsed = text.trim() === "" ? null : parseJsonStrict(text, "API problem response");
  } catch {
    parsed = null;
  }
  const value = parsed !== null && typeof parsed === "object" && !Array.isArray(parsed)
    ? parsed
    : {
        type: "about:blank",
        title: "Non-problem API error",
        status,
        code: "UNPARSEABLE_ERROR_RESPONSE",
        detail: text.trim() || `StoryBlock returned HTTP ${status}`,
        instance: null,
        request_id: headers["x-request-id"] ?? null,
      };
  return {
    ...value,
    status: Number.isInteger(value.status) ? value.status : status,
    retry_after: headers["retry-after"] ?? null,
  };
}

export class ApiResponseError extends Error {
  constructor(status, headers, body) {
    const problem = parseProblemBody(body, status, headers);
    super(`${problem.code ?? "API_ERROR"}: ${problem.detail ?? problem.title ?? `HTTP ${status}`}`);
    this.name = "ApiResponseError";
    this.status = status;
    this.headers = headers;
    this.problem = problem;
    this.exitCode = status === 401 || status === 403 ? EXIT_CODES.AUTH : EXIT_CODES.API;
  }
}

export function problemReport(error) {
  const problem = error.problem;
  return {
    status: error.status,
    type: problem.type ?? null,
    title: problem.title ?? null,
    code: problem.code ?? null,
    detail: problem.detail ?? null,
    instance: problem.instance ?? null,
    request_id: problem.request_id ?? error.headers?.["x-request-id"] ?? null,
    retry_after: problem.retry_after ?? null,
    validation_issues: problem.violations ?? problem.validation_issues ?? null,
    problem,
  };
}
