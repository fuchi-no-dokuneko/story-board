module.exports=async({page,base,id,click,shot,close,assert})=>{
  for(const [width,height,scale] of [[320,640,1],[390,844,2],[768,1024,1],[844,390,1],[1024,600,1],[1920,1080,1]]){
    const mobile=width<=760;await page.setViewport({width,height,deviceScaleFactor:scale});
    await page.goto(base+'/?ui=v2');await page.waitForSelector('.novel-item');
    assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Library width '+width);
    await shot(`library-${width}`);
    await page.goto(`${base}/?ui=v2#${id}`);await page.waitForSelector('.story-block');
    if(mobile){await click('.rtop [data-sheet=aa]');await click('[data-fs="22"]');await click('[data-thm=dark]');}
    else{await click('#aaBtn');await page.$eval('#fs',e=>{e.value=24;e.dispatchEvent(new Event('input',{bubbles:true}));});
      await click('[data-pref=lh][data-val=loose]');await click('[data-pref=theme][data-val=night]');}
    await close();
    assert.equal(await page.$eval('.story-block',e=>getComputedStyle(e).color),mobile?'rgb(233, 236, 244)':'rgb(217, 214, 207)');
    assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Reader width '+width);
    assert.equal(await page.$eval('.story-block',e=>getComputedStyle(e).fontSize),mobile?'22px':'24px');
    assert.equal(await page.$eval(mobile?'.rtop':'.rbar',e=>e.getBoundingClientRect().height),mobile?56:58);
    await shot(`reader-large-${width}`);
    await click(mobile?'[data-tabgo=ins]':'#insightsBtn');await click(mobile?'[data-ins=quality]':'[data-tab=quality]');
    assert.ok(await page.$eval(mobile?'.screen':'.drawer',e=>e.scrollWidth<=e.clientWidth+1),'Panel width '+width);
    await shot(`panel-${width}`);await close();
    await page.evaluate(()=>localStorage.removeItem('sb2.readerPreferences'));
  }
};
