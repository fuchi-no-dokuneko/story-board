import { readFile } from "node:fs/promises";

import { parseJsonStrict } from "./json.mjs";
import { UnknownResourceError, UsageError } from "./problem.mjs";

const MANIFEST_URL = new URL("../../references/endpoints.json", import.meta.url);
let manifestCache;

export async function loadEndpointManifest() {
  if (manifestCache !== undefined) return manifestCache;
  manifestCache = parseJsonStrict(await readFile(MANIFEST_URL, "utf8"), "references/endpoints.json");
  return manifestCache;
}

export async function listEndpoints() {
  return (await loadEndpointManifest()).endpoints;
}

export async function getEndpoint(id) {
  const endpoint = (await listEndpoints()).find((entry) => entry.id === id);
  if (endpoint === undefined) throw new UnknownResourceError(`Unknown endpoint: ${id}`);
  return endpoint;
}

function requireParameterObject(params, field) {
  const value = params[field] ?? {};
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    throw new UsageError(`params.${field} must be an object`);
  }
  return value;
}

export function materializeEndpoint(endpoint, params = {}) {
  if (params === null || typeof params !== "object" || Array.isArray(params)) {
    throw new UsageError("--params must contain a JSON object");
  }
  const pathValues = requireParameterObject(params, "path");
  const queryValues = requireParameterObject(params, "query");
  const headerValues = requireParameterObject(params, "headers");
  let pathname = endpoint.path;
  for (const parameter of endpoint.pathParameters) {
    const value = pathValues[parameter.name];
    if (value === undefined || value === null || String(value) === "") {
      throw new UsageError(`Missing path parameter: ${parameter.name}`);
    }
    pathname = pathname.replace(`{${parameter.name}}`, encodeURIComponent(String(value)));
  }
  if (/\{[^}]+\}/u.test(pathname)) throw new UsageError(`Unresolved endpoint path: ${pathname}`);

  const allowedPath = new Set(endpoint.pathParameters.map((entry) => entry.name));
  const allowedQuery = new Set(endpoint.queryParameters.map((entry) => entry.name));
  for (const name of Object.keys(pathValues)) if (!allowedPath.has(name)) throw new UsageError(`Unknown path parameter: ${name}`);
  for (const name of Object.keys(queryValues)) if (!allowedQuery.has(name)) throw new UsageError(`Unknown query parameter: ${name}`);

  const search = new URLSearchParams();
  for (const parameter of endpoint.queryParameters) {
    const value = queryValues[parameter.name];
    if (value === undefined || value === null) {
      if (parameter.required) throw new UsageError(`Missing query parameter: ${parameter.name}`);
      continue;
    }
    const values = Array.isArray(value) ? value : [value];
    for (const entry of values) search.append(parameter.name, String(entry));
  }
  const query = search.toString();
  return {
    method: endpoint.method,
    pathname: query === "" ? pathname : `${pathname}?${query}`,
    headers: Object.fromEntries(Object.entries(headerValues).map(([name, value]) => [name, String(value)])),
  };
}

export function resetEndpointCacheForTests() {
  manifestCache = undefined;
}
