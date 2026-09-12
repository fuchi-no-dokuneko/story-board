const { When } = require('@cucumber/cucumber');
const runtime = require('../../runtime.cjs');

When('I select the registered acceptance novel', async function () {
  const id = process.env.STORYBLOCK_UAT_NOVEL_ID;
  if (!/^nov_[A-Za-z0-9]{5}$/.test(id || '')) throw new Error('Invalid acceptance novel ID');
  await runtime.clickCss(this, `.novel-item[data-novel-id="${id}"]`);
  await runtime.waitCssText(this, '#reader-id', id);
});
