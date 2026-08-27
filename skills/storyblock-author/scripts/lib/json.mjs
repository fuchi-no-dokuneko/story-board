import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";

export class StrictJsonError extends Error {
  constructor(message, source = "JSON") {
    super(`${source}: ${message}`);
    this.name = "StrictJsonError";
  }
}

class Parser {
  constructor(text, source) {
    this.text = text;
    this.source = source;
    this.index = 0;
  }

  error(message) {
    throw new StrictJsonError(`${message} at character ${this.index + 1}`, this.source);
  }

  skipWhitespace() {
    while (this.index < this.text.length && /[\u0009\u000a\u000d\u0020]/u.test(this.text[this.index])) {
      this.index += 1;
    }
  }

  parse() {
    this.skipWhitespace();
    const value = this.value();
    this.skipWhitespace();
    if (this.index !== this.text.length) {
      this.error("unexpected trailing content");
    }
    return value;
  }

  value() {
    const character = this.text[this.index];
    if (character === "{") return this.object();
    if (character === "[") return this.array();
    if (character === '"') return this.string();
    if (character === "t") return this.literal("true", true);
    if (character === "f") return this.literal("false", false);
    if (character === "n") return this.literal("null", null);
    if (character === "-" || /[0-9]/u.test(character ?? "")) return this.number();
    this.error("expected a JSON value");
  }

  literal(token, value) {
    if (!this.text.startsWith(token, this.index)) {
      this.error(`expected ${token}`);
    }
    this.index += token.length;
    return value;
  }

  string() {
    const start = this.index;
    this.index += 1;
    while (this.index < this.text.length) {
      const character = this.text[this.index];
      if (character === '"') {
        this.index += 1;
        try {
          return JSON.parse(this.text.slice(start, this.index));
        } catch {
          this.error("invalid string escape");
        }
      }
      if (character === "\\") {
        this.index += 1;
        const escaped = this.text[this.index];
        if (escaped === "u") {
          const digits = this.text.slice(this.index + 1, this.index + 5);
          if (!/^[0-9a-fA-F]{4}$/u.test(digits)) {
            this.error("invalid Unicode escape");
          }
          this.index += 5;
          continue;
        }
        if (!/["\\/bfnrt]/u.test(escaped ?? "")) {
          this.error("invalid string escape");
        }
        this.index += 1;
        continue;
      }
      if (character.codePointAt(0) < 0x20) {
        this.error("unescaped control character in string");
      }
      this.index += 1;
    }
    this.error("unterminated string");
  }

  number() {
    const match = /^-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?/u.exec(this.text.slice(this.index));
    if (match === null) {
      this.error("invalid number");
    }
    this.index += match[0].length;
    const value = Number(match[0]);
    if (!Number.isFinite(value)) {
      this.error("number is outside the finite JavaScript range");
    }
    if (!match[0].includes(".") && !/[eE]/u.test(match[0]) && !Number.isSafeInteger(value)) {
      this.error("integer is outside the safe JavaScript range");
    }
    return value;
  }

  array() {
    const result = [];
    this.index += 1;
    this.skipWhitespace();
    if (this.text[this.index] === "]") {
      this.index += 1;
      return result;
    }
    while (true) {
      this.skipWhitespace();
      result.push(this.value());
      this.skipWhitespace();
      const delimiter = this.text[this.index];
      this.index += 1;
      if (delimiter === "]") return result;
      if (delimiter !== ",") this.error("expected ',' or ']' in array");
    }
  }

  object() {
    const result = {};
    const keys = new Set();
    this.index += 1;
    this.skipWhitespace();
    if (this.text[this.index] === "}") {
      this.index += 1;
      return result;
    }
    while (true) {
      this.skipWhitespace();
      if (this.text[this.index] !== '"') this.error("expected a string property name");
      const key = this.string();
      if (keys.has(key)) this.error(`duplicate property ${JSON.stringify(key)}`);
      keys.add(key);
      this.skipWhitespace();
      if (this.text[this.index] !== ":") this.error("expected ':' after property name");
      this.index += 1;
      this.skipWhitespace();
      result[key] = this.value();
      this.skipWhitespace();
      const delimiter = this.text[this.index];
      this.index += 1;
      if (delimiter === "}") return result;
      if (delimiter !== ",") this.error("expected ',' or '}' in object");
    }
  }
}

export function parseJsonStrict(text, source = "JSON") {
  if (typeof text !== "string") {
    throw new StrictJsonError("input must be text", source);
  }
  return new Parser(text, source).parse();
}

export async function readJsonFile(path) {
  const text = await readFile(path, "utf8");
  return parseJsonStrict(text, path);
}

export function stableStringify(value) {
  if (value === null || typeof value !== "object") {
    if (typeof value === "number" && !Number.isFinite(value)) {
      throw new StrictJsonError("cannot encode a non-finite number");
    }
    const encoded = JSON.stringify(value);
    if (encoded === undefined) throw new StrictJsonError("cannot encode undefined");
    return encoded;
  }
  if (Array.isArray(value)) {
    return `[${value.map((entry) => stableStringify(entry)).join(",")}]`;
  }
  const keys = Object.keys(value).sort();
  return `{${keys.map((key) => `${JSON.stringify(key)}:${stableStringify(value[key])}`).join(",")}}`;
}

export function sha256(value) {
  const bytes = Buffer.isBuffer(value) ? value : Buffer.from(value, "utf8");
  return createHash("sha256").update(bytes).digest("hex");
}

export function canonicalHash(value) {
  return `sha256:${sha256(stableStringify(value))}`;
}

export function deriveIdempotencyKey(namespace, value) {
  return `storyblock-${namespace}-${sha256(stableStringify(value))}`.slice(0, 200);
}
