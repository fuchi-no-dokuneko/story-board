module.exports=async({page,base,id,selected,click,shot,assert})=>{
  await page.setViewport({width:1440,height:1000});await page.goto(`${base}/#${id}`);
  await page.waitForFunction(title=>document.querySelector('#reader-title')?.textContent===title,{},selected.novel.title);
  assert.equal(await page.$$eval('#chapter-nav .chapter-link',e=>e.length),2);
  assert.equal(await page.$$eval('.story-image img',e=>e.length),2);
  await page.waitForFunction(()=>!document.querySelector('#compare-styles').disabled);
  await click('#compare-styles');await page.waitForFunction(()=>document.querySelectorAll('#style-score-rows tr').length===2);
  await click('#analyze-quality');await page.waitForFunction(()=>!document.querySelector('#download-quality').disabled);
  await click('#console-tab');await click('.quick-requests [data-path="/actuator/health"]');
  await page.waitForFunction(()=>document.querySelector('#response').textContent.includes('UP'));
  await click('#library-tab');await shot('legacy-reader');
  assert.equal(await page.$$eval('textarea,[contenteditable=true]',e=>e.length),0);
  await page.setViewport({width:390,height:844});
  assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  await shot('legacy-mobile');
};
