module.exports=async c=>{
  const {page,base,longId,click,check,testid}=c;
  await page.setViewport({width:1440,height:900});await page.goto(base+'/?ui=v2');
  await page.waitForSelector('.novel-item');await page.type('#catalog-search','第7號月台');
  check('01 F5 desktop upright digits',await page.$$eval('.novel-item .tcy',es=>es.length)===1);
  check('02 F5 desktop singular',!/1 chapters/.test(await page.$eval('body',e=>e.innerText)));
  await page.goto(base+'/?ui=v2#'+longId);await page.waitForSelector('.story-block');
  check('03 F2 desktop empty scene facts hidden',!(await page.$('.scene-meta'))&&
    !/·\s*·/.test(await page.$eval('#toc',e=>e.innerText)));
  await click('#insightsBtn');await click('[data-tab=quality]');c.longQuality=await c.analyze();
  c.assert.equal(c.longQuality.quality_report.windows.length,714,'Real analysis windows');
  check('04 F1 desktop bounded controls',await page.$$eval('#quality-results [data-win],#quality-results [data-step],#quality-results [data-jump-win]',es=>es.length)<=6);
  check('05 F1 desktop 714 chart',await page.$eval(testid('window-chart'),e=>e.getAttribute('aria-valuemax'))==='714');
  await page.focus('#win-chart');for(let i=0;i<3;i++)await page.keyboard.press('ArrowRight');
  check('06 F1 desktop keyboard',/Window 3 of 714/.test(await c.label()));
  await page.keyboard.press('End');check('07 F1 desktop End',/Window 714 of 714/.test(await c.label()));
  await click(testid('window-jump-max'));
  const metric=await page.$eval('#win-metric',e=>e.value);
  const vals=c.longQuality.quality_report.windows.map(w=>w.metrics.find(m=>m.metric===metric).value);
  const peak=vals.lastIndexOf(Math.max(...vals))+1,label=await c.label();
  check('08 F1 desktop maximum',label.includes(`Window ${peak} of`));
  const w=c.longQuality.quality_report.windows[peak-1];
  check('09 F1 desktop readable locations',/Ch\. .*¶/.test(label)&&!label.includes(`${w.start}–${w.end}`));
  await click(testid('window-scope-clear'));check('10 F1 clear scope',!(await page.$(testid('window-scope'))));
  await page.focus('#win-chart');await page.keyboard.press('Home');await page.keyboard.press('ArrowRight');
  await click(testid('window-show'));await page.waitForSelector('.win-band');
  check('11 F1 desktop full window band',await page.$$eval('.story-block.win-band',es=>es.length)>3);
  const range=c.longQuality.quality_report.windows[1];
  c.assert.deepEqual(await page.$$eval('.win-band[data-text-block]',es=>es.map(e=>e.dataset.textBlock)),
    c.longQuality.block_spans.filter(s=>s.start<range.end&&s.end>range.start).map(s=>s.block_id));
  check('Extra F1 desktop first band visible',await page.$eval('.win-band.first',e=>{const r=e.getBoundingClientRect();return r.top>=58&&r.top<innerHeight;}));
  await require('./fix-chart.cjs')(c);await click(testid('window-show'));await c.shot('fix-desktop-window');
};
