#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import process from "node:process";

function option(name) {
  const index = process.argv.indexOf(name);
  if (index < 0 || !process.argv[index + 1]) throw new Error(`Missing ${name}`);
  return process.argv[index + 1];
}

function escapeXml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&apos;");
}

function localCommit() {
  try {
    return execFileSync("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim();
  } catch {
    return "unknown";
  }
}

const input = option("--input");
const output = option("--output");
const suite = option("--suite");
const startedAt = option("--started-at");
const project = JSON.parse(readFileSync(path.resolve("acceptance/project.json"), "utf8"));
const cucumber = JSON.parse(readFileSync(input, "utf8"));
const checks = [];

for (const feature of cucumber) {
  for (const scenario of feature.elements || []) {
    if (scenario.type !== "scenario") continue;
    const steps = (scenario.steps || []).map((step) => ({
      keyword: String(step.keyword || "").trim(),
      name: step.name,
      passed: step.result?.status === "passed",
      status: step.result?.status || "unknown",
      durationMs: Math.round(Number(step.result?.duration || 0) / 1_000_000),
      error: step.result?.error_message || "",
    }));
    checks.push({
      id: scenario.id,
      feature: feature.name,
      name: scenario.name,
      passed: steps.length > 0 && steps.every((step) => step.passed),
      durationMs: steps.reduce((sum, step) => sum + step.durationMs, 0),
      steps,
    });
  }
}

if (!checks.length) {
  checks.push({
    id: `${suite};runner`,
    feature: suite,
    name: "Cucumber reported at least one scenario",
    passed: false,
    durationMs: 0,
    steps: [{ keyword: "Then", name: "a scenario exists", passed: false, status: "missing", durationMs: 0, error: "No scenarios were reported." }],
  });
}

const checklist = {
  schemaVersion: 1,
  repository: project.repository,
  platform: project.platform,
  suite,
  commit: localCommit(),
  startedAt,
  finishedAt: new Date().toISOString(),
  passed: checks.every((check) => check.passed),
  checks,
};
writeFileSync(path.join(output, "checklist.json"), `${JSON.stringify(checklist, null, 2)}\n`);

const testCases = checks.map((check) => {
  const failures = check.steps.filter((step) => !step.passed);
  const failure = failures.length
    ? `<failure message="UAT checklist failed">${escapeXml(failures.map((step) => `${step.keyword} ${step.name}: ${step.error || step.status}`).join("\n"))}</failure>`
    : "";
  return `<testCase name="${escapeXml(check.name)}" duration="${check.durationMs}">${failure}</testCase>`;
}).join("");
const xml = `<?xml version="1.0" encoding="UTF-8"?>\n<testExecutions version="1"><file path="acceptance/features/${escapeXml(suite)}.feature">${testCases}</file></testExecutions>\n`;
writeFileSync(path.join(output, "sonar-test-execution.xml"), xml);

const passedCount = checks.filter((check) => check.passed).length;
const summary = [
  `# ${project.repository} ${suite} report`,
  "",
  `- Passed: **${checklist.passed}**`,
  `- Scenarios: **${passedCount}/${checks.length} passed**`,
  `- Commit: \`${checklist.commit}\``,
  `- Started: ${checklist.startedAt}`,
  `- Finished: ${checklist.finishedAt}`,
  "",
  "| Scenario | Passed | Duration |",
  "| --- | --- | ---: |",
  ...checks.map((check) => `| ${check.name.replaceAll("|", "\\|")} | ${check.passed} | ${check.durationMs} ms |`),
  "",
].join("\n");
writeFileSync(path.join(output, "summary.md"), summary);
process.stdout.write(`${project.repository} ${suite}: ${passedCount}/${checks.length} scenarios passed\n`);
