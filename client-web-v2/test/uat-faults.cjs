module.exports=async({page,base,id,click,close,assert})=>{
  let bookFails=true,imageFails=true;
  const intercept=request=>{
    const pathname=new URL(request.url()).pathname;
    if(bookFails&&pathname===`/v1/admin/novels/${id}`)
      return request.respond({status:503,contentType:'application/json',body:'{"detail":"Temporary read failure"}'});
    if(imageFails&&pathname.startsWith('/v1/artifacts/')){
      imageFails=false;return request.respond({status:503,contentType:'application/json',body:'{"detail":"Temporary image failure"}'});
    }
    return request.continue();
  };
  await page.setRequestInterception(true);page.on('request',intercept);
  try{
    await page.goto(`${base}/?ui=v2#${id}`);
    await page.waitForSelector('#reader-view [data-action=open]');bookFails=false;
    await click('#reader-view [data-action=open]');await page.waitForSelector('#reader-title');
    await page.waitForSelector('.story-image [data-action=retryImage]:not([hidden])');
    await click('.story-image [data-action=retryImage]:not([hidden])');
    await page.waitForFunction(()=>[...document.querySelectorAll('.story-image img')].every(e=>!e.hidden));
    await click('.sb-top [data-action=settings]');await click('#console-tab');
    await click('.quick-requests [data-path="/actuator/health"]');
    await page.waitForFunction(()=>document.querySelector('#response').textContent.includes('UP'));
    await click('.quick-requests [data-path="/v1/openapi.yaml"]');
    await page.waitForFunction(()=>document.querySelector('#response').textContent.includes('openapi:'));
    await close();await click('.sb-top [data-action=settings]');await click('.sb-settings-section details summary');
    await page.type('#operator-token','reader-test-token');await click('#operator-connect');
    await page.waitForFunction(()=>!document.querySelector('#tools-dialog').open);
    assert.doesNotMatch(await page.evaluate(()=>JSON.stringify({...localStorage})),/reader-test-token/);
    await page.reload();await page.waitForSelector('.novel-item');
    assert.equal(await page.$$eval('textarea,[contenteditable=true]',e=>e.length),0);
  }finally{page.off('request',intercept);await page.setRequestInterception(false);}
};
