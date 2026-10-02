const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {launch,read}=require('./browser.cjs');
(async()=>{
  const base=process.env.UAT_BASE_URL||'https://127.0.0.1:9443';
  const output=path.resolve(process.env.UAT_OUTPUT||'.local/frontend-v2');
  fs.mkdirSync(path.join(output,'downloads'),{recursive:true});
  const id=process.env.UAT_NOVEL_ID||JSON.parse(fs.readFileSync(path.join(output,'fixture/manuscript.json'))).novel_id;
  const selected=await read(`${base}/v1/admin/novels/${id}`),runtime=await launch(output);
  const results=[],errors=[],requests=[];let page;
  try{
    page=await runtime.browser.newPage();page.setDefaultTimeout(20000);
    page.on('pageerror',e=>errors.push(e.message));
    page.on('console',m=>{if(/Content-Security-Policy|Refused to (load|execute)/i.test(m.text()))errors.push(m.text());});
    page.on('request',r=>requests.push({method:r.method(),path:new URL(r.url()).pathname}));
    const context={page,base,output,id,selected,assert,read,
      click:selector=>page.click(selector),
      shot:name=>page.screenshot({path:path.join(output,name+'.png')}),
      close:()=>page.click('#tools-dialog [data-action=close]'),
    };
    for(const name of ['library','reading','analysis','downloads','layouts','legacy','faults']){
      await require(`./uat-${name}.cjs`)(context);results.push(name);console.log(`PASS ${name}`);
    }
    assert.deepEqual(errors,[],'No uncaught browser errors');
    const writes=requests.filter(r=>r.method!=='GET'&&r.method!=='HEAD');
    assert.ok(writes.every(r=>r.method==='POST'&&/\/(style-comparisons|quality-reports|pdf-renders)$/.test(r.path)));
    const after=await read(`${base}/v1/admin/novels/${id}`);
    assert.equal(after.novel.head_revision_id,selected.novel.head_revision_id);
    fs.writeFileSync(path.join(output,'browser-results.json'),JSON.stringify({browser:await runtime.browser.version(),
      base,results,uncaughtErrors:errors,requests:requests.length,analysisAndExportRequests:writes.length,
      unchangedRevision:after.novel.head_revision_id},null,2));
  }catch(error){
    if(page){await page.screenshot({path:path.join(output,'failure.png')});
      console.error(await page.$eval('body',e=>e.innerText.slice(-1400)));}
    throw error;
  }finally{await runtime.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
