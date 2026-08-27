import { request as httpsRequest } from "node:https";

import { parseJsonStrict, stableStringify } from "./json.mjs";
import { ApiResponseError, NetworkError } from "./problem.mjs";

export const DEFAULT_BASE_URL = "https://127.0.0.1:8443";
export const DEFAULT_TIMEOUT_MS = 15_000;
export const DEFAULT_USER_AGENT = "storyblock-author/1.0.0";
const MAX_RESPONSE_BYTES = 128 * 1024 * 1024;

function normalizedHeaders(headers) {
  return Object.fromEntries(Object.entries(headers ?? {}).map(([name, value]) => [
    name.toLowerCase(),
    Array.isArray(value) ? value.join(", ") : value === undefined ? "" : String(value),
  ]));
}

export function connectionPolicy(baseUrl) {
  let url;
  try {
    url = new URL(baseUrl);
  } catch (error) {
    throw new TypeError(`Invalid STORYBLOCK_BASE_URL: ${baseUrl}`, { cause: error });
  }
  if (url.protocol !== "https:") throw new TypeError("StoryBlock base URL must use HTTPS");
  if (url.username !== "" || url.password !== "") throw new TypeError("StoryBlock base URL must not contain credentials");
  if (url.hostname.includes(":")) throw new TypeError("StoryBlock author supports IPv4 endpoints only");
  return Object.freeze({ origin: url.origin, rejectUnauthorized: false, family: 4 });
}

function defaultTransport({ url, method, headers, body, timeoutMs }) {
  return new Promise((resolve, reject) => {
    const request = httpsRequest(url, {
      method,
      headers,
      family: 4,
      rejectUnauthorized: false,
    }, (response) => {
      const chunks = [];
      let total = 0;
      response.on("data", (chunk) => {
        total += chunk.length;
        if (total > MAX_RESPONSE_BYTES) {
          request.destroy(new Error(`response exceeds ${MAX_RESPONSE_BYTES} bytes`));
          return;
        }
        chunks.push(chunk);
      });
      response.on("end", () => resolve({
        status: response.statusCode ?? 0,
        headers: normalizedHeaders(response.headers),
        body: Buffer.concat(chunks),
      }));
    });
    request.setTimeout(timeoutMs, () => request.destroy(new Error(`request timed out after ${timeoutMs}ms`)));
    request.on("error", reject);
    if (body !== undefined) request.write(body);
    request.end();
  });
}

function responseData(body, headers, responseType) {
  if (responseType === "buffer") return body;
  if (body.length === 0) return null;
  const contentType = headers["content-type"] ?? "";
  const shouldParse = responseType === "json" || contentType.includes("json") || contentType.includes("problem+");
  if (!shouldParse) return body.toString("utf8");
  return parseJsonStrict(body.toString("utf8"), "StoryBlock response");
}

export class StoryBlockClient {
  constructor({
    baseUrl = process.env.STORYBLOCK_BASE_URL || DEFAULT_BASE_URL,
    accessKey = process.env.STORYBLOCK_ACCESS_KEY,
    timeoutMs = process.env.STORYBLOCK_TIMEOUT_MS || DEFAULT_TIMEOUT_MS,
    userAgent = process.env.STORYBLOCK_USER_AGENT || DEFAULT_USER_AGENT,
    transport = defaultTransport,
  } = {}) {
    this.policy = connectionPolicy(baseUrl);
    this.accessKey = accessKey;
    this.timeoutMs = Number(timeoutMs);
    this.userAgent = userAgent;
    this.transport = transport;
    if (!Number.isSafeInteger(this.timeoutMs) || this.timeoutMs < 1 || this.timeoutMs > 300_000) {
      throw new TypeError("STORYBLOCK_TIMEOUT_MS must be an integer from 1 to 300000");
    }
    if (typeof this.userAgent !== "string" || this.userAgent.trim() === "" || /[\r\n]/u.test(this.userAgent)) {
      throw new TypeError("STORYBLOCK_USER_AGENT must be non-empty header text");
    }
    if (this.accessKey !== undefined && (typeof this.accessKey !== "string" || this.accessKey.trim() !== this.accessKey || /\s/u.test(this.accessKey))) {
      throw new TypeError("STORYBLOCK_ACCESS_KEY must not contain whitespace");
    }
  }

  async request({ method = "GET", pathname, headers = {}, body, responseType = "auto" }) {
    const url = new URL(pathname, `${this.policy.origin}/`);
    const payload = body === undefined ? undefined : Buffer.from(stableStringify(body), "utf8");
    const requestHeaders = {
      Accept: "application/json, application/problem+json",
      "User-Agent": this.userAgent,
      ...headers,
    };
    if (this.accessKey !== undefined) requestHeaders.Authorization = `Bearer ${this.accessKey}`;
    if (payload !== undefined) {
      requestHeaders["Content-Type"] ??= "application/json";
      requestHeaders["Content-Length"] = String(payload.length);
    }
    let response;
    try {
      response = await this.transport({ url, method, headers: requestHeaders, body: payload, timeoutMs: this.timeoutMs });
    } catch (error) {
      if (error instanceof ApiResponseError) throw error;
      throw new NetworkError(`Unable to reach StoryBlock at ${url.origin}: ${error.message}`, error);
    }
    const normalized = normalizedHeaders(response.headers);
    const rawBody = Buffer.isBuffer(response.body) ? response.body : Buffer.from(response.body ?? "");
    if (response.status < 200 || response.status >= 300) {
      throw new ApiResponseError(response.status, normalized, rawBody);
    }
    return {
      status: response.status,
      headers: normalized,
      data: responseData(rawBody, normalized, responseType),
      rawBody,
    };
  }
}
