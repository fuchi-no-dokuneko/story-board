import { randomBytes } from "node:crypto";

export const ID_PREFIXES = Object.freeze([
  "ana", "art", "aud", "blk", "blv", "ch", "fnd", "job", "key", "mis",
  "mpr", "mrun", "nov", "op", "prp", "rev", "scn", "sle", "spf", "spv",
]);

const UUID_V7 = /^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/u;

export function typedIdPattern(prefix) {
  if (!ID_PREFIXES.includes(prefix)) throw new TypeError(`unsupported StoryBlock ID prefix: ${prefix}`);
  return new RegExp(`^${prefix}_${UUID_V7.source.slice(1, -1)}$`, "u");
}

export function requireTypedId(value, prefix, location = `${prefix}_id`) {
  if (typeof value !== "string" || !typedIdPattern(prefix).test(value)) {
    throw new TypeError(`${location} must be ${prefix}_<RFC 9562 UUIDv7>`);
  }
  return value;
}

export function createTypedId(prefix, timestamp = Date.now()) {
  if (!ID_PREFIXES.includes(prefix)) throw new TypeError(`unsupported StoryBlock ID prefix: ${prefix}`);
  if (!Number.isSafeInteger(timestamp) || timestamp < 0 || timestamp > 0xffffffffffff) {
    throw new TypeError("UUIDv7 timestamp must be an unsigned 48-bit millisecond integer");
  }
  const bytes = randomBytes(16);
  let remaining = BigInt(timestamp);
  for (let index = 5; index >= 0; index -= 1) {
    bytes[index] = Number(remaining & 0xffn);
    remaining >>= 8n;
  }
  bytes[6] = 0x70 | (bytes[6] & 0x0f);
  bytes[8] = 0x80 | (bytes[8] & 0x3f);
  const hex = bytes.toString("hex");
  const uuid = `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
  return `${prefix}_${uuid}`;
}
