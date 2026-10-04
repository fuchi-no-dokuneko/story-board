const {launch}=require('./browser.cjs');
module.exports=async c=>{
  const {base,id,check}=c,runtime=await launch(c.output,{colorScheme:'dark'});
  const page=await runtime.browser.newPage(),errors=[];
  page.on('pageerror',e=>errors.push(e.message));const click=s=>page.click(s);
  try{
    await page.setViewport({width:1440,height:900});
    await page.goto(base+'/?ui=v2');await page.waitForSelector('.novel-item');
    c.assert.ok(await page.evaluate(()=>matchMedia('(prefers-color-scheme:dark)').matches));
    check('15 F3 Auto follows dark OS',await page.$eval('body',e=>getComputedStyle(e).backgroundColor)==='rgb(21, 19, 15)');
    await page.goto(base+'/?ui=v2#'+id);await page.waitForSelector('.story-block');
    await click('#aaBtn');await click('[data-pref=theme][data-val=white]');
    check('16 F3 White page dark text',await page.$eval('.story-block',e=>getComputedStyle(e).color)==='rgb(39, 35, 29)');
    await click('[data-appearance=light]');
    check('Extra F3 Light overrides dark OS',await page.$eval('html',e=>e.dataset.theme)==='light');
    await page.reload();await page.waitForSelector('.story-block');
    check('Extra F3 appearance persists',await page.$eval('html',e=>e.dataset.theme)==='light');
    await page.setViewport({width:390,height:844});await page.waitForSelector('.rtop');
    check('Extra F3 appearance shared on mobile',await page.$eval('html',e=>e.dataset.theme)==='light');
    await click('[data-sheet=aa]');await click('[data-thm=auto]');
    check('Extra F3 Auto removes data-theme',await page.$eval('html',e=>!e.hasAttribute('data-theme')));
    c.assert.deepEqual(errors,[]);
  }finally{await runtime.close();}
};
