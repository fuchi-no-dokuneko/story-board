# StoryBlock GUI acceptance

Three local Cucumber pipelines cover the browser console:

- `run-uat.sh` is the daily pass/fail checklist for a human or AI browser agent.
- `run-demo-en.sh` records the English product introduction.
- `run-demo-yue.sh` records the Traditional Chinese Cantonese introduction.

## Prerequisites and execution

The complete UAT requires the StoryBlock API, Chromium, ChromeDriver, and the
owner token used by the running API. A dry run validates all Gherkin and step
bindings without opening a browser or requiring credentials.

```bash
sh acceptance/bootstrap.sh
acceptance/run-uat.sh --dry-run

export STORYBLOCK_UAT_OWNER_TOKEN='same-local-owner-token-used-by-the-api'
acceptance/run-uat.sh --headless --base-url http://127.0.0.1:8080/
```

Do not point UAT at irreplaceable data. The current suite does not create a
novel: authenticated POST and DELETE checks stop at the API's required mutation
preconditions because the console intentionally exposes no custom-header input.

Every real run writes `checklist.json`, `sonar-test-execution.xml`, `summary.md`,
raw Cucumber JSON, and one screenshot per scenario under
`build/reports/acceptance/<suite>/`. Dry runs are binding checks, not product
passes. Use `--sonar` only for a completed UAT run; demo reports are recording
evidence and must not count as acceptance coverage.

## Recording hooks

Set executable wrapper paths in `DEMO_TTS_COMMAND`,
`DEMO_RECORD_START_COMMAND`, and `DEMO_RECORD_STOP_COMMAND`. The TTS wrapper
receives `DEMO_TTS_LANGUAGE`, `DEMO_TTS_TEXT`, and `DEMO_TTS_MIN_SECONDS`.
Without wrappers, narration is printed and still waits for its declared timing.

## Feature coverage matrix

| Visible feature or transition | Daily UAT | English demo | Cantonese demo |
| --- | --- | --- | --- |
| Initial online, degraded, and unavailable states | Header-state scenarios | Product opening | 產品開場 |
| Health shortcut and formatted JSON | Health request | Main flow | 主要流程 |
| OpenAPI shortcut and plain YAML | API contract | Main flow | 主要流程 |
| GET, POST, DELETE request controls | Protected and DELETE requests | POST example | POST 示例 |
| Optional masked bearer token | Missing, invalid, and owner token | Security guidance | 安全指引 |
| Typed HTTP error rendering | 401 and 428 responses | 401 explanation | 401 解說 |
| Browser network failure and recovery | Offline then online | Not recorded | 不錄影 |
| Reload clears token and edits | Reload scenario | Not recorded | 不錄影 |
