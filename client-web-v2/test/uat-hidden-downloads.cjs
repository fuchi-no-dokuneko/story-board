const fs=require('node:fs'),path=require('node:path');
module.exports=async({page,base,output,fixture,selected,visible,assert,click,close,shot})=>{
  const file=path.join(output,'downloads',selected.novel.title+'.txt');
  async function download(selector){
    fs.rmSync(file,{force:true});await click(selector);
    await page.waitForFunction(()=>document.body.innerText.includes('ready')||document.body.innerText.includes('準備'));
    const deadline=Date.now()+20000;
    while(!fs.existsSync(file)||fs.existsSync(file+'.part')){
      if(Date.now()>deadline)throw Error('TXT download missing');await new Promise(r=>setTimeout(r,100));
    }
    const text=fs.readFileSync(file,'utf8');assert.ok(!text.includes(fixture.sentinel));
    for(const block of visible)assert.ok(text.includes(block.text));
  }
  await page.setViewport({width:1440,height:900});await page.goto(`${base}/?ui=v2#${fixture.id}`);
  await page.waitForSelector('.story-hidden');
  assert.equal(await page.$eval('#bookinfo',e=>[...e.querySelectorAll('dt')].find(e=>e.textContent==='Blocks').nextElementSibling.textContent),String(visible.length));
  await click('#exportBtn');await download('#download-txt');await close();
  for(const [language,label] of [['en','Hidden meaning'],['zh','隱藏含義'],['zhs','隐藏含义']]){
    await click('#back');await click(`[data-lang-btn=${language}]`);await page.goto(`${base}/?ui=v2#${fixture.id}`);
    await page.waitForSelector('.story-hidden');assert.equal(await page.$eval('.story-hidden header',e=>e.textContent),label);
  }
  await click('#insightsBtn');await click('[data-tab=quality]');await click('#analyze-quality');
  await page.waitForSelector('.metric');await close();
  for(const [width,height] of [[1280,900],[390,844]]){
    await page.setViewport({width,height});await page.goto(`${base}/#${fixture.id}`);
    await page.waitForSelector('.story-hidden');
    assert.equal(await page.$$eval('.story-block',es=>es.length),visible.length);
    assert.equal(await page.$eval('#stat-blocks',e=>e.textContent),String(visible.length));
    assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
    assert.ok(await page.$eval('.story-hidden',e=>!e.hasAttribute('data-text-block')&&!e.hasAttribute('data-block-id')));
    await page.$eval('.story-hidden',e=>e.scrollIntoView());await shot('hidden-legacy-'+width);
  }
  await download('#download-txt');
};
