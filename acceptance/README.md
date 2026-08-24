# StoryBlock GUI acceptance

Three local Cucumber pipelines cover the read-only novel library:

- `run-uat.sh` is the daily pass/fail checklist for a human or AI browser agent.
- `run-demo-en.sh` records the English product introduction.
- `run-demo-yue.sh` records the Traditional Chinese Cantonese introduction.

## Prerequisites and execution

The complete UAT requires the trusted-LAN StoryBlock API, its independently
registered E2E novel, Chromium, and ChromeDriver. A dry run validates all
Gherkin and step bindings without opening a browser.

```bash
sh acceptance/bootstrap.sh
acceptance/run-uat.sh --dry-run

export STORYBLOCK_UAT_NOVEL_ID='nov_<uuidv7>'
export STORYBLOCK_UAT_NOVEL_TITLE='registered title'
export STORYBLOCK_UAT_CHARACTER='one main character'
export STORYBLOCK_UAT_TEXT_MARKER='persisted story excerpt'
acceptance/run-uat.sh --headless --base-url https://127.0.0.1:8443/
```

Do not point UAT at irreplaceable data. The suite never mutates a novel; the E2E
authoring agent registers its fixture beforehand through the tracked skill.

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
| Online and degraded states | Header-state scenarios | Product opening | 產品開場 |
| Persisted 10,000-Han story and hashes | Exact reader checks | Main flow | 主要流程 |
| Five characters and aggregate counts | Exact reader checks | Main flow | 主要流程 |
| Catalog search and chapter navigation | Search and navigation | Reader flow | 閱讀流程 |
| GET-only health and OpenAPI diagnostics | Diagnostics | Contract flow | 合約流程 |
| Browser network failure and recovery | Offline then online | Not recorded | 不錄影 |
| Responsive narrow reader | Mobile viewport | Not recorded | 不錄影 |
