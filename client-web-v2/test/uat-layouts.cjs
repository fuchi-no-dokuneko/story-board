module.exports=async({page,base,id,click,shot,close,assert})=>{
  for(const [width,height,scale] of [[320,640,1],[390,844,2],[768,1024,1],[844,390,1],[1024,600,1],[1920,1080,1]]){
    await page.setViewport({width,height,deviceScaleFactor:scale});
    await page.goto(base+'/?ui=v2');await page.waitForSelector('.novel-item');
    assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Library width '+width);
    await shot(`library-${width}`);
    await page.goto(`${base}/?ui=v2#${id}`);await page.waitForSelector('#reader-title');
    await click('.sb-reader-actions [data-action=appearance]');
    await click('[data-key=size][data-value="28"]');await click('[data-key=spacing][data-value=loose]');
    await click('[data-key=theme][data-value=night]');await close();
    assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Reader width '+width);
    assert.equal(await page.$eval('.story-block',e=>getComputedStyle(e).fontSize),'28px');
    await shot(`reader-large-${width}`);
    await click('.sb-reader-actions [data-action=style]');await click('[data-action=quality]');
    assert.ok(await page.$eval('#tools-dialog',e=>e.scrollWidth<=e.clientWidth+1),'Panel width '+width);
    await shot(`panel-${width}`);await close();
    await click('.sb-reader-actions [data-action=appearance]');
    await click('[data-key=size][data-value="20"]');await click('[data-key=spacing][data-value=normal]');
    await click('[data-key=theme][data-value=paper]');await close();
  }
};
