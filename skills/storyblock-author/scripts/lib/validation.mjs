import { getSchema, loadSchemas } from "./dtos.mjs";
import { stableStringify } from "./json.mjs";
import { EXIT_CODES } from "./problem.mjs";
import { isCanonicalInstant } from "./time.mjs";

const SENTENCE_TERMINATOR = String.raw`(?:[。！？!?]+|(?<!\d)\.{1,3}(?!\d)|…{1,2})`;
const CLOSING_MARKS = String.raw`[」』”’"'）)】》〉〕］}]*`;
const REGISTRATION_SENTENCE = /\s*(.*?(?:[。！？!?]+|…{1,2})[」』”’"'）)】》〉〕］}]*?)/gsuy;

export class PayloadValidationError extends Error {
  constructor(dto, issues) {
    super(`${dto} validation failed with ${issues.length} issue${issues.length === 1 ? "" : "s"}`);
    this.name = "PayloadValidationError";
    this.dto = dto;
    this.issues = issues;
    this.exitCode = EXIT_CODES.VALIDATION;
  }
}

function resolvePointer(root, reference) {
  if (!reference.startsWith("#/")) throw new TypeError(`not a local reference: ${reference}`);
  return reference.slice(2).split("/").reduce((value, token) => {
    const key = token.replaceAll("~1", "/").replaceAll("~0", "~");
    if (value === null || typeof value !== "object" || !(key in value)) {
      throw new TypeError(`unresolved schema reference ${reference}`);
    }
    return value[key];
  }, root);
}

function resolveReference(reference, root, schemas) {
  if (reference.startsWith("#/")) return { schema: resolvePointer(root, reference), root };
  const name = reference.replace(/\.schema\.json$/u, "");
  const schema = schemas.get(name);
  if (schema === undefined) throw new TypeError(`unresolved schema reference ${reference}`);
  return { schema, root: schema };
}

function typeMatches(value, type) {
  switch (type) {
    case "null": return value === null;
    case "array": return Array.isArray(value);
    case "object": return value !== null && typeof value === "object" && !Array.isArray(value);
    case "integer": return Number.isSafeInteger(value);
    case "number": return typeof value === "number" && Number.isFinite(value);
    case "string": return typeof value === "string";
    case "boolean": return typeof value === "boolean";
    default: return true;
  }
}

function codePointLength(text) {
  return Array.from(text).length;
}

function graphemeLength(text) {
  const segmenter = new Intl.Segmenter("und", { granularity: "grapheme" });
  return [...segmenter.segment(text.normalize("NFC"))]
    .filter(({ segment }) => !["\n", "\r", "\r\n"].includes(segment)).length;
}

function sentenceShape(text) {
  const normalized = text.normalize("NFC");
  const sentenceEnd = new RegExp(`${SENTENCE_TERMINATOR}${CLOSING_MARKS}`, "gu");
  const completeEnd = new RegExp(`${SENTENCE_TERMINATOR}${CLOSING_MARKS}\\s*$`, "u");
  return {
    count: [...normalized.matchAll(sentenceEnd)].length,
    complete: normalized.trim() !== "" && completeEnd.test(normalized),
  };
}

function registrationSentences(text) {
  const normalized = text.normalize("NFC").trim();
  const matcher = new RegExp(REGISTRATION_SENTENCE.source, REGISTRATION_SENTENCE.flags);
  const sentences = [];
  let consumed = 0;
  for (let match = matcher.exec(normalized); match !== null; match = matcher.exec(normalized)) {
    const sentence = match[1].trim();
    if (sentence !== "") sentences.push(sentence);
    consumed = matcher.lastIndex;
  }
  const trailing = normalized.slice(consumed).trim();
  if (trailing !== "") sentences.push(`${trailing}。`);
  return sentences;
}

function deepEqual(left, right) {
  return stableStringify(left) === stableStringify(right);
}

function knownProperties(schema, root, schemas, seen = new Set()) {
  if (schema === true || schema === false || schema === null || typeof schema !== "object") return new Set();
  if (schema.$ref) {
    const key = `${schema.$ref}|${root.$id ?? "local"}`;
    if (seen.has(key)) return new Set();
    seen.add(key);
    const resolved = resolveReference(schema.$ref, root, schemas);
    return knownProperties(resolved.schema, resolved.root, schemas, seen);
  }
  const result = new Set(Object.keys(schema.properties ?? {}));
  for (const member of schema.allOf ?? []) {
    for (const property of knownProperties(member, root, schemas, seen)) result.add(property);
  }
  return result;
}

function validateNode(value, schema, context) {
  if (schema === true || schema === undefined) return [];
  if (schema === false) return [{ path: context.path, message: "value is prohibited by the schema" }];
  if (schema.$ref) {
    const resolved = resolveReference(schema.$ref, context.root, context.schemas);
    return validateNode(value, resolved.schema, { ...context, root: resolved.root });
  }

  const issues = [];
  const issue = (message, path = context.path) => issues.push({ path, message });

  if (schema.allOf) {
    for (const member of schema.allOf) issues.push(...validateNode(value, member, context));
  }
  if (schema.oneOf) {
    const results = schema.oneOf.map((member) => validateNode(value, member, context));
    const matches = results.filter((result) => result.length === 0).length;
    if (matches !== 1) {
      issue(`must match exactly one of ${schema.oneOf.length} alternatives (matched ${matches})`);
      if (matches === 0) issues.push(...results.sort((a, b) => a.length - b.length)[0].slice(0, 3));
    }
  }
  if (schema.anyOf) {
    const results = schema.anyOf.map((member) => validateNode(value, member, context));
    if (!results.some((result) => result.length === 0)) {
      issue(`must match at least one of ${schema.anyOf.length} alternatives`);
      issues.push(...results.sort((a, b) => a.length - b.length)[0].slice(0, 3));
    }
  }
  if (schema.not && validateNode(value, schema.not, context).length === 0) {
    issue("must not match the prohibited schema");
  }

  if (Object.hasOwn(schema, "const") && !deepEqual(value, schema.const)) {
    issue(`must equal ${JSON.stringify(schema.const)}`);
  }
  if (schema.enum && !schema.enum.some((entry) => deepEqual(entry, value))) {
    issue(`must be one of ${schema.enum.map((entry) => JSON.stringify(entry)).join(", ")}`);
  }

  const types = schema.type === undefined ? [] : Array.isArray(schema.type) ? schema.type : [schema.type];
  if (types.length > 0 && !types.some((type) => typeMatches(value, type))) {
    issue(`must have type ${types.join(" or ")}`);
    return issues;
  }

  if (typeof value === "string") {
    const length = codePointLength(value);
    if (schema.minLength !== undefined && length < schema.minLength) issue(`must contain at least ${schema.minLength} Unicode code points`);
    if (schema.maxLength !== undefined && length > schema.maxLength) issue(`must contain at most ${schema.maxLength} Unicode code points`);
    if (schema.pattern !== undefined) {
      let pattern;
      try {
        pattern = new RegExp(schema.pattern, "u");
      } catch (error) {
        throw new TypeError(`invalid schema pattern ${schema.pattern}`, { cause: error });
      }
      if (!pattern.test(value)) issue(`must match ${schema.pattern}`);
    }
    if (schema.format === "date-time" && !isCanonicalInstant(value)) issue("must be canonical UTC Instant text");
    if (schema.format === "uri") {
      try {
        const url = new URL(value);
        if (url.protocol === "") throw new Error("missing scheme");
      } catch {
        issue("must be an absolute URI");
      }
    }
    if (schema.format === "uri-reference" && /[\u0000-\u0020]/u.test(value)) issue("must be a URI reference");
    if (schema["x-storyblock-max-graphemes"] !== undefined
      && graphemeLength(value) > schema["x-storyblock-max-graphemes"]) {
      issue(`must contain at most ${schema["x-storyblock-max-graphemes"]} grapheme clusters`);
    }
    if (schema["x-storyblock-sentence-count"] !== undefined) {
      const shape = sentenceShape(value);
      const configured = schema["x-storyblock-sentence-count"];
      const allowed = Array.isArray(configured) ? configured : [configured];
      if (!allowed.includes(shape.count)) issue(`must contain ${allowed.join(" or ")} complete sentences (found ${shape.count})`);
      if (!shape.complete) issue("must end at a recognized complete sentence boundary");
    }
  }

  if (typeof value === "number" && Number.isFinite(value)) {
    if (schema.minimum !== undefined && value < schema.minimum) issue(`must be at least ${schema.minimum}`);
    if (schema.maximum !== undefined && value > schema.maximum) issue(`must be at most ${schema.maximum}`);
    if (schema.exclusiveMinimum !== undefined && value <= schema.exclusiveMinimum) issue(`must be greater than ${schema.exclusiveMinimum}`);
    if (schema.exclusiveMaximum !== undefined && value >= schema.exclusiveMaximum) issue(`must be less than ${schema.exclusiveMaximum}`);
  }

  if (Array.isArray(value)) {
    if (schema.minItems !== undefined && value.length < schema.minItems) issue(`must contain at least ${schema.minItems} items`);
    if (schema.maxItems !== undefined && value.length > schema.maxItems) issue(`must contain at most ${schema.maxItems} items`);
    if (schema.uniqueItems) {
      const encoded = value.map((entry) => stableStringify(entry));
      if (new Set(encoded).size !== encoded.length) issue("must contain unique items");
    }
    const prefixItems = schema.prefixItems ?? [];
    for (let index = 0; index < prefixItems.length && index < value.length; index += 1) {
      issues.push(...validateNode(value[index], prefixItems[index], { ...context, path: `${context.path}/${index}` }));
    }
    if (schema.items && schema.items !== true) {
      for (let index = prefixItems.length; index < value.length; index += 1) {
        issues.push(...validateNode(value[index], schema.items, { ...context, path: `${context.path}/${index}` }));
      }
    }
    if (schema.contains && !value.some((entry, index) => validateNode(entry, schema.contains, { ...context, path: `${context.path}/${index}` }).length === 0)) {
      issue("must contain an item matching the contains schema");
    }
  }

  if (value !== null && typeof value === "object" && !Array.isArray(value)) {
    for (const required of schema.required ?? []) {
      if (!Object.hasOwn(value, required)) issue(`is missing required property ${required}`, `${context.path}/${required}`);
    }
    const properties = schema.properties ?? {};
    for (const [name, member] of Object.entries(properties)) {
      if (Object.hasOwn(value, name)) {
        issues.push(...validateNode(value[name], member, { ...context, path: `${context.path}/${name}` }));
      }
    }
    if (schema.propertyNames) {
      for (const name of Object.keys(value)) {
        issues.push(...validateNode(name, schema.propertyNames, { ...context, path: `${context.path}/${name}` }));
      }
    }
    const known = knownProperties(schema, context.root, context.schemas);
    const additional = Object.keys(value).filter((name) => !known.has(name));
    if (schema.additionalProperties === false || schema.unevaluatedProperties === false) {
      for (const name of additional) issue(`contains unknown property ${name}`, `${context.path}/${name}`);
    } else if (schema.additionalProperties && typeof schema.additionalProperties === "object") {
      for (const name of additional) {
        issues.push(...validateNode(value[name], schema.additionalProperties, { ...context, path: `${context.path}/${name}` }));
      }
    }
    if (schema.maxProperties !== undefined && Object.keys(value).length > schema.maxProperties) issue(`must contain at most ${schema.maxProperties} properties`);
    if (schema.minProperties !== undefined && Object.keys(value).length < schema.minProperties) issue(`must contain at least ${schema.minProperties} properties`);
  }

  return issues;
}

export function hanSequence(text) {
  if (typeof text !== "string") return "";
  return (text.normalize("NFC").match(/\p{Script=Han}/gu) ?? []).join("");
}

export function countHanCodePoints(text) {
  return Array.from(hanSequence(text)).length;
}

function registrationIssues(value) {
  if (value === null || typeof value !== "object" || Array.isArray(value)) return [];
  const issues = [];
  const nonBlank = ["title", "language"];
  for (const field of nonBlank) {
    if (typeof value[field] === "string" && value[field].trim() === "") issues.push({ path: `#/${field}`, message: "must not be blank" });
  }
  if (Array.isArray(value.main_characters)) {
    value.main_characters.forEach((name, index) => {
      if (typeof name === "string" && name.trim() === "") issues.push({ path: `#/main_characters/${index}`, message: "must not be blank" });
    });
  }
  if (Array.isArray(value.chapters)) {
    value.chapters.forEach((chapter, index) => {
      if (chapter && typeof chapter.title === "string" && chapter.title.trim() === "") issues.push({ path: `#/chapters/${index}/title`, message: "must not be blank" });
      if (chapter && typeof chapter.text === "string" && chapter.text.trim() === "") issues.push({ path: `#/chapters/${index}/text`, message: "must not be blank" });
      if (chapter && typeof chapter.text === "string") {
        for (const sentence of registrationSentences(chapter.text)) {
          if (graphemeLength(sentence) > 100) {
            issues.push({ path: `#/chapters/${index}/text`, message: "contains a sentence longer than 100 grapheme clusters" });
            break;
          }
        }
      }
    });
    const complete = value.chapters.map((chapter) => typeof chapter?.text === "string" ? chapter.text : "").join("");
    const actual = countHanCodePoints(complete);
    if (Number.isSafeInteger(value.expected_han_characters) && actual !== value.expected_han_characters) {
      issues.push({ path: "#/expected_han_characters", message: `declares ${value.expected_han_characters}, but chapter text contains ${actual} Han code points` });
    }
  }
  return issues;
}

function operationIssues(value) {
  if (value === null || typeof value !== "object" || Array.isArray(value)) return [];
  const range = value.payload?.range ?? value.payload?.block;
  if (!range || !Array.isArray(range.expected_blocks) || range.expected_blocks.length === 0) return [];
  const issues = [];
  const first = range.expected_blocks[0]?.block_id;
  const last = range.expected_blocks.at(-1)?.block_id;
  if (first !== range.first_block_id) issues.push({ path: "#/payload/range/first_block_id", message: "must match the first expected_blocks block_id" });
  if (last !== range.last_block_id) issues.push({ path: "#/payload/range/last_block_id", message: "must match the last expected_blocks block_id" });
  return issues;
}

export async function validateDto(name, value, { throwOnError = true } = {}) {
  const schema = await getSchema(name);
  const schemas = await loadSchemas();
  const issues = validateNode(value, schema, { root: schema, schemas, path: "#" });
  if (name === "AgentNovelRegistrationRequest") issues.push(...registrationIssues(value));
  if (name === "OperationEnvelope" || name.endsWith("Operation")) issues.push(...operationIssues(value));
  if (issues.length > 0 && throwOnError) throw new PayloadValidationError(name, issues);
  return { valid: issues.length === 0, dto: name, issues };
}

export async function validateInline(value, schema) {
  const schemas = await loadSchemas();
  const issues = validateNode(value, schema, { root: schema, schemas, path: "#" });
  return { valid: issues.length === 0, issues };
}
