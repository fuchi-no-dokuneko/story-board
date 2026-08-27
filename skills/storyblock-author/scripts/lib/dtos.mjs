import { readdir, readFile } from "node:fs/promises";

import { parseJsonStrict } from "./json.mjs";
import { UnknownResourceError } from "./problem.mjs";

const DTO_DIRECTORY = new URL("../../references/dtos/", import.meta.url);
let schemaCache;

export async function loadSchemas() {
  if (schemaCache !== undefined) return schemaCache;
  const names = (await readdir(DTO_DIRECTORY))
    .filter((name) => name.endsWith(".schema.json"))
    .sort();
  const entries = await Promise.all(names.map(async (file) => {
    const text = await readFile(new URL(file, DTO_DIRECTORY), "utf8");
    const schema = parseJsonStrict(text, file);
    return [file.replace(/\.schema\.json$/u, ""), schema];
  }));
  schemaCache = new Map(entries);
  return schemaCache;
}

export async function getSchema(name) {
  if (typeof name !== "string" || !/^[A-Za-z][A-Za-z0-9]*$/u.test(name)) {
    throw new UnknownResourceError(`Unknown DTO: ${name}`);
  }
  const schemas = await loadSchemas();
  const schema = schemas.get(name);
  if (schema === undefined) throw new UnknownResourceError(`Unknown DTO: ${name}`);
  return schema;
}

export async function listDtos() {
  const schemas = await loadSchemas();
  return [...schemas.entries()].map(([name, schema]) => ({
    name,
    title: schema.title ?? name,
    description: schema.description ?? "",
    confidence: schema["x-confidence"] ?? "open-question",
    file: `references/dtos/${name}.schema.json`,
    source: schema["x-source"] ?? [],
    open_question: schema["x-open-question"] ?? null,
  }));
}

export function resetSchemaCacheForTests() {
  schemaCache = undefined;
}
