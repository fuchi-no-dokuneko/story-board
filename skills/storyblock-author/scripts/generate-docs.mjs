#!/usr/bin/env node

import { readdir, readFile, writeFile } from "node:fs/promises";

const skillRoot = new URL("../", import.meta.url);
const references = new URL("references/", skillRoot);

function escapeTable(value) {
  return String(value ?? "").replaceAll("|", "\\|").replaceAll("\n", " ");
}

function code(value) {
  return value === undefined || value === null ? "none" : `\`${value}\``;
}

function parameterText(parameters) {
  if (parameters.length === 0) return "none";
  return parameters.map((entry) => {
    const required = entry.required ? "required" : "optional";
    const constraints = Object.entries(entry.schema ?? {})
      .map(([name, value]) => `${name}=${JSON.stringify(value)}`).join(", ");
    return `\`${entry.name}\` (${required}${constraints === "" ? "" : `; ${constraints}`})`;
  }).join("; ");
}

function schemaFields(schema) {
  const properties = Object.keys(schema.properties ?? {});
  if (properties.length === 0) {
    if (schema.oneOf) return `one of: ${schema.oneOf.map((entry) => code(entry.$ref ?? entry.title ?? "inline schema")).join(", ")}`;
    if (schema.allOf) return `composition of: ${schema.allOf.map((entry) => code(entry.$ref ?? entry.title ?? "inline schema")).join(", ")}`;
    return "No named top-level fields.";
  }
  const required = new Set(schema.required ?? []);
  return properties.map((name) => `${code(name)} (${required.has(name) ? "required" : "optional"})`).join(", ");
}

function schemaConstraints(schema) {
  const keys = ["type", "const", "enum", "minLength", "maxLength", "minimum", "maximum", "minItems", "maxItems", "uniqueItems", "additionalProperties", "unevaluatedProperties"];
  const values = keys.filter((key) => Object.hasOwn(schema, key)).map((key) => `${key}=${JSON.stringify(schema[key])}`);
  if (schema.oneOf) values.push(`oneOf=${schema.oneOf.length}`);
  if (schema.allOf) values.push(`allOf=${schema.allOf.length}`);
  if (schema["x-server-constraints"]) values.push(...schema["x-server-constraints"]);
  return values.length === 0 ? "See the linked schema for nested constraints." : values.join("; ");
}

async function generateEndpoints() {
  const manifest = JSON.parse(await readFile(new URL("endpoints.json", references), "utf8"));
  const lines = [
    "# StoryBlock endpoint catalog",
    "",
    `This catalog contains ${manifest.endpoints.length} code-verified programmatic routes. Static browser assets are excluded. Paths are full deployed paths, including \`/v1\`.`,
    "",
    "The repository OpenAPI was treated only as an extraction aid. Controller code corrects three known differences: the OpenAPI endpoint itself is added, the monitor status variable is \`runId\`, and rewrite-proposal response DTOs follow \`RewriteProposalController\` rather than the published \`JobAccepted\`/proposal schemas. Actuator routes come from runtime configuration and security tests.",
    "",
    "Every `/v1` mutation requires `If-Match` and `Idempotency-Key` in the filter, even when the controller does not consume both values. Every protected `/v1` request is rate-limited per authenticated identity.",
    "",
  ];
  for (const endpoint of manifest.endpoints) {
    lines.push(`## ${endpoint.id}`, "");
    lines.push(`- Method and path: \`${endpoint.method} ${endpoint.path}\``);
    lines.push(`- Purpose: ${endpoint.summary}`);
    lines.push(`- Auth: ${endpoint.auth}; scopes: ${endpoint.scopes.length ? endpoint.scopes.map(code).join(", ") : "none"}; roles: ${endpoint.roles.length ? endpoint.roles.map(code).join(", ") : "none"}.`);
    lines.push(`- Path parameters: ${parameterText(endpoint.pathParameters)}.`);
    lines.push(`- Query parameters: ${parameterText(endpoint.queryParameters)}.`);
    lines.push(`- Request headers: ${parameterText(endpoint.requestHeaders)}.`);
    lines.push(`- Request DTO: ${endpoint.requestBody ? `${code(endpoint.requestBody.dto)} (${endpoint.requestBody.contentType}, ${endpoint.requestBody.required ? "required" : "optional"})` : "none"}.`);
    lines.push(`- Responses: ${endpoint.responses.map((response) => `${response.status} ${response.dto ? code(response.dto) : "no body"}${response.contentType ? ` (${response.contentType})` : ""}`).join("; ")}.`);
    lines.push(`- Errors: ${endpoint.errors.map((error) => `${error.status} ${code(error.dto)} (${error.contentType})`).join("; ")}.`);
    if (endpoint.authorizationNotes) lines.push(`- Authorization note: ${endpoint.authorizationNotes}`);
    lines.push(`- Confidence: \`${endpoint.confidence}\`.`);
    if (endpoint.openQuestion) lines.push(`- OPEN QUESTION: ${endpoint.openQuestion}`);
    lines.push(`- Sources: ${endpoint.source.map(code).join(", ")}.`, "");
  }
  await writeFile(new URL("endpoints.md", references), `${lines.join("\n")}\n`);
}

async function generateDtos() {
  const directory = new URL("dtos/", references);
  const files = (await readdir(directory)).filter((name) => name.endsWith(".schema.json")).sort();
  const lines = [
    "# StoryBlock DTO catalog",
    "",
    `This index covers ${files.length} standalone JSON Schema draft 2020-12 files. Each schema embeds a machine-valid example and source provenance.`,
    "",
  ];
  for (const file of files) {
    const schema = JSON.parse(await readFile(new URL(file, directory), "utf8"));
    const name = file.replace(/\.schema\.json$/u, "");
    lines.push(`## ${name}`, "");
    lines.push(`- Purpose: ${schema.description}`);
    lines.push(`- JSON Schema: [${file}](dtos/${file}).`);
    lines.push(`- Fields: ${schemaFields(schema)}`);
    lines.push(`- Constraints: ${schemaConstraints(schema)}`);
    lines.push(`- Example: \`examples[0]\` is embedded in the linked schema.`);
    lines.push(`- Sources: ${(schema["x-source"] ?? []).map(code).join(", ")}.`);
    lines.push(`- Confidence: \`${schema["x-confidence"] ?? "open-question"}\`.`);
    if (schema["x-open-question"]) lines.push(`- OPEN QUESTION: ${schema["x-open-question"]}`);
    lines.push("");
  }
  await writeFile(new URL("dtos.md", references), `${lines.join("\n")}\n`);
}

await generateEndpoints();
await generateDtos();
