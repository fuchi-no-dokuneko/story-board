#!/usr/bin/env node

import { existsSync, mkdirSync, rmSync, writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

const acceptanceDirectory = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(acceptanceDirectory, "..");
const suites = {
  uat: "uat.feature",
  "demo-en": "demo-en.feature",
  "demo-yue": "demo-yue.feature",
};

function usage(message) {
  if (message) process.stderr.write(`${message}\n\n`);
  process.stderr.write(
    "Usage: node acceptance/run.mjs <uat|demo-en|demo-yue> " +
    "[--dry-run] [--headless] [--sonar] [--base-url URL] [--tags EXPRESSION]\n",
  );
  process.exit(2);
}

const args = process.argv.slice(2);
const suite = args.shift();
if (!suites[suite]) usage("Choose one configured suite.");

let dryRun = false;
let headless = false;
let submitSonar = false;
let baseUrl = "";
let tags = "";
while (args.length) {
  const option = args.shift();
  if (option === "--dry-run") dryRun = true;
  else if (option === "--headless") headless = true;
  else if (option === "--sonar") submitSonar = true;
  else if (option === "--base-url") baseUrl = args.shift() || usage("--base-url requires a URL.");
  else if (option === "--tags") tags = args.shift() || usage("--tags requires an expression.");
  else usage(`Unknown option: ${option}`);
}
if (submitSonar && suite !== "uat") usage("Only UAT results belong in SonarQube.");
if (submitSonar && dryRun) usage("A dry run is not a product result.");

const cucumberBinary = path.join(
  acceptanceDirectory,
  "node_modules",
  "@cucumber",
  "cucumber",
  "bin",
  "cucumber.js",
);
if (!existsSync(cucumberBinary)) {
  process.stderr.write("Acceptance dependencies are missing. Run: sh acceptance/bootstrap.sh\n");
  process.exit(2);
}

const reportDirectory = path.join(root, "build", "reports", "acceptance", suite);
const rawReport = path.join(reportDirectory, "cucumber.json");
if (!dryRun) {
  rmSync(reportDirectory, { recursive: true, force: true });
  mkdirSync(reportDirectory, { recursive: true });
}
const startedAt = new Date().toISOString();
const childEnvironment = {
  ...process.env,
  NODE_OPTIONS: `${process.env.NODE_OPTIONS || ""} --dns-result-order=ipv4first`.trim(),
  ACCEPTANCE_SUITE: suite,
  ACCEPTANCE_REPORT_DIR: reportDirectory,
  ...(headless ? { ACCEPTANCE_HEADLESS: "1" } : {}),
  ...(baseUrl ? { UAT_BASE_URL: baseUrl } : {}),
};

const cucumberArgs = [
  cucumberBinary,
  path.join("acceptance", "features", suites[suite]),
  "--require", path.join("acceptance", "features", "support", "*.cjs"),
  "--require", path.join("acceptance", "features", "step_definitions", "*.cjs"),
  "--format", "progress",
];
if (dryRun) cucumberArgs.push("--dry-run");
else cucumberArgs.push("--format", `json:${rawReport}`);
if (tags) cucumberArgs.push("--tags", tags);

const cucumber = spawnSync(process.execPath, cucumberArgs, {
  cwd: root,
  env: childEnvironment,
  stdio: "inherit",
});
const cucumberStatus = cucumber.status ?? 1;
if (dryRun) process.exit(cucumberStatus);

if (!existsSync(rawReport)) writeFileSync(rawReport, "[]\n");
const report = spawnSync(process.execPath, [
  path.join(acceptanceDirectory, "report.mjs"),
  "--input", rawReport,
  "--output", reportDirectory,
  "--suite", suite,
  "--started-at", startedAt,
], { cwd: root, env: childEnvironment, stdio: "inherit" });

let sonarStatus = 0;
if (submitSonar) {
  const token = process.env.SONAR_TOKEN;
  if (!token) {
    process.stderr.write("SONAR_TOKEN is required for --sonar.\n");
    sonarStatus = 2;
  } else {
    const scanner = process.env.SONAR_SCANNER_BIN || "sonar-scanner";
    const reportPath = path.relative(root, path.join(reportDirectory, "sonar-test-execution.xml"));
    const scan = spawnSync(scanner, [`-Dsonar.testExecutionReportPaths=${reportPath}`], {
      cwd: root,
      env: {
        ...process.env,
        SONAR_SCANNER_OPTS:
          `${process.env.SONAR_SCANNER_OPTS || ""} ` +
          "-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false",
      },
      stdio: "inherit",
    });
    sonarStatus = scan.error ? 2 : (scan.status ?? 1);
  }
}

process.exit(cucumberStatus || (report.status ?? 1) || sonarStatus);
