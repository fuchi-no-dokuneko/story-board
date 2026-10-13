const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {launch,read}=require('./browser.cjs');
(async()=>{
  const base=process.env.UAT_BASE_URL,output=path.resolve(process.env.UAT_OUTPUT);
  const fixture=JSON.parse(fs.readFileSync(path.join(output,'hidden-fixture.json')));
  const selected=await read(`${base}/v1/admin/novels/${fixture.id}`);
  const raw=selected.revision.chapters.flatMap(c=>c.scenes.flatMap(s=>s.blocks));
  const visible=raw.filter(b=>b.extensions?.['storyblock.hidden']!==true);
  assert.equal(raw.length,visible.length+1);assert.equal(selected.novel.block_count,visible.length);
  const runtime=await launch(output),errors=[];let page;
  try{
    page=await runtime.browser.newPage();page.setDefaultTimeout(20000);
    page.on('pageerror',e=>errors.push(e.message));
    const context={page,base,output,fixture,selected,visible,assert,read,
      shot:name=>page.screenshot({path:path.join(output,name+'.png')}),
      click:selector=>page.click(selector),close:()=>page.keyboard.press('Escape')};
    await require('./uat-hidden-layouts.cjs')(context);
    await require('./uat-hidden-downloads.cjs')(context);
    assert.deepEqual(errors,[]);
    fs.writeFileSync(path.join(output,'hidden-browser-results.json'),JSON.stringify({
      browser:await runtime.browser.version(),viewports:8,visibleBlocks:visible.length,
      rawBlocks:raw.length,note:fixture.noteId,checks:['position','escape','translations','styles',
        'large-font','progress','counts','quality','v2-txt','legacy-txt'],uncaughtErrors:errors},null,2));
    console.log('PASS hidden browser layouts and exports / 隱藏區塊介面及匯出驗收通過');
  }catch(error){if(page)await page.screenshot({path:path.join(output,'failure.png')});throw error;}
  finally{await runtime.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
