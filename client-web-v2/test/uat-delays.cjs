module.exports=async({page,base,id,assert})=>{
  await page.setViewport({width:1440,height:1000});
  for(const query of ['', '?ui=v2']){
    let held;
    const intercept=request=>{if(new URL(request.url()).pathname==='/v1/admin/novels')held=request;else request.continue();};
    await page.setRequestInterception(true);page.on('request',intercept);
    try{
      await page.goto(base+'/'+query+'#'+id,{waitUntil:'domcontentloaded'});
      await page.waitForSelector('.story-block',{timeout:5000});assert.ok(held,'Catalog still pending');
      assert.ok(await page.$eval('#catalog-loading',e=>!e.hidden));
      await held.respond({status:503,contentType:'application/json',body:'{"detail":"Catalog unavailable"}'});held=null;
      await page.waitForFunction(()=>document.querySelector('#catalog-loading')?.textContent.includes('Catalog unavailable')||document.querySelector('#catalog-empty')?.textContent.includes('Catalog unavailable'));
      assert.ok(await page.$eval('#reader-content',e=>!e.hidden),'Reader survives catalog failure');
    }finally{if(held)await held.continue();page.off('request',intercept);await page.setRequestInterception(false);}
  }
};
