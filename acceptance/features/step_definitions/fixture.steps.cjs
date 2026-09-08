const { When } = require('@cucumber/cucumber');
const runtime = require('../../runtime.cjs');

When('I select the registered acceptance novel', async function () {
  const id = process.env.STORYBLOCK_UAT_NOVEL_ID;
  if (!/^nov_[a-f0-9-]+$/.test(id || '')) throw new Error('Missing acceptance novel ID');
  await runtime.clickCss(this, `.novel-item[data-novel-id="${id}"]`);
  await runtime.waitCssText(this, '#reader-id', id);
});
