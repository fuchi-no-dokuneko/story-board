module.exports=async({page,base,click,shot,assert})=>{
  await page.setViewport({width:1440,height:1000});await page.goto(base+'/?ui=v2');
  await page.waitForFunction(()=>document.querySelectorAll('.novel-item').length===12);
  assert.equal(await page.$eval('#page-label',e=>e.textContent),'1 / 2');
  await shot('desktop-library');await click('#page-next');
  assert.equal(await page.$$eval('.novel-item',e=>e.length),2);
  await click('#page-previous');await page.type('#catalog-search','LAST TRAIN');
  assert.equal(await page.$$eval('.novel-item',e=>e.length),1);
  assert.match(await page.$eval('.novel-item',e=>e.textContent),/Last Train Home/);
  await page.$eval('#catalog-search',e=>{e.value='';e.dispatchEvent(new Event('input',{bubbles:true}));});
  await click('[data-filter=en]');assert.equal(await page.$$eval('.novel-item',e=>e.length),2);
  await click('[data-filter=all]');await page.type('#catalog-search','no matching story');
  assert.ok(await page.$eval('#catalog-empty',e=>!e.hidden));
  await page.$eval('#catalog-search',e=>{e.value='';e.dispatchEvent(new Event('input',{bubbles:true}));});
  await page.select('#catalog-sort','title');
  assert.equal(await page.$$eval('textarea,[contenteditable=true]',e=>e.length),0);
};
