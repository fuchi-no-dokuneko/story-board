#!/usr/bin/env node

import { pathToFileURL } from "node:url";

import { runCli } from "./lib/cli.mjs";

export * from "./lib/api-client.mjs";
export * from "./lib/cli.mjs";
export * from "./lib/dtos.mjs";
export * from "./lib/endpoints.mjs";
export * from "./lib/ids.mjs";
export * from "./lib/json.mjs";
export * from "./lib/problem.mjs";
export * from "./lib/time.mjs";
export * from "./lib/validation.mjs";

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  process.exitCode = await runCli(process.argv.slice(2));
}
