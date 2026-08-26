const { After, Before, setDefaultTimeout } = require("@cucumber/cucumber");
const runtime = require("../../runtime.cjs");

setDefaultTimeout(runtime.timeoutMs());

Before(function () {
  this.driver = null;
  this.demoRecording = false;
  this.memory = new Map();
});

After(async function ({ pickle }) {
  await runtime.captureScenario(this, pickle.name);
  await runtime.closeWorld(this);
});
