module.exports=async({page,base,id,click,close,assert})=>{
  let bookFails=true,imageFails=true,pdfFails=false;
  const intercept=request=>{
    const pathname=new URL(request.url()).pathname;
    if(bookFails&&pathname===`/v1/admin/novels/${id}`)
      return request.respond({status:503,contentType:'application/json',body:'{"detail":"Temporary read failure"}'});
    if(imageFails&&pathname.startsWith('/v1/artifacts/')){
      imageFails=false;return request.respond({status:503,contentType:'application/json',body:'{"detail":"Temporary image failure"}'});
    }
    if(pdfFails&&pathname.endsWith('/pdf-renders'))return request.respond({status:503,contentType:'application/json',body:'{"detail":"PDF unavailable"}'});
    return request.continue();
  };
  await page.setViewport({width:1440,height:1000});await page.setRequestInterception(true);page.on('request',intercept);
  try{
    await page.goto(`${base}/?ui=v2#${id}`);
    await page.waitForSelector('[data-retry-book]');bookFails=false;
    await click('[data-retry-book]');await page.waitForSelector('#reader-title');
    await page.waitForSelector('.story-image [data-action=retryImage]:not([hidden])');
    await click('.story-image [data-action=retryImage]:not([hidden])');
    await page.waitForFunction(()=>[...document.querySelectorAll('.story-image img')].every(e=>!e.hidden));
    assert.equal(await page.$$eval('[data-action=retryImage]',e=>e.length),2);
    await click('#back');await click('#connBtn');await click('#console-tab');
    await click('.quick-requests [data-path="/actuator/health"]');
    await page.waitForFunction(()=>document.querySelector('#response').textContent.includes('UP'));
    await click('.quick-requests [data-path="/v1/openapi.yaml"]');
    await page.waitForFunction(()=>document.querySelector('#response').textContent.includes('openapi:'));
    await close();await click('#connBtn');await click('.pop details summary');
    await page.type('#operator-token','reader-test-token');await click('#operator-connect');
    await page.waitForSelector('.novel-item');
    assert.doesNotMatch(await page.evaluate(()=>JSON.stringify({...localStorage})),/reader-test-token/);
    await page.setViewport({width:390,height:844});await page.goto(`${base}/?ui=v2#${id}`);
    await page.waitForSelector('.rtop');pdfFails=true;await click('[data-sheet=export]');await click('#download-pdf');
    await page.waitForFunction(()=>document.querySelector('.toast')?.textContent.includes('PDF unavailable'));
    assert.equal(await page.$('.sheet'),null);
  }finally{page.off('request',intercept);await page.setRequestInterception(false);}
};
