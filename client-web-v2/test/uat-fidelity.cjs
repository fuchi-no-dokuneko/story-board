const fs=require('fs'),path=require('path'),{execFileSync}=require('child_process');
module.exports=async({page,base,id,output,click,assert})=>{
  const refs=path.join(output,'references');fs.mkdirSync(refs,{recursive:true});
  await page.evaluate(()=>localStorage.removeItem('sb2.readerPreferences'));
  await page.reload();
  const checks=[];
  for(const [mode,file,width,height] of [['desktop','concept-a-reader.html',1440,1000],['mobile','concept-d-pocket.html',390,844]]){
    const html=execFileSync('unzip',['-p','storyblock-ui-concepts.zip',file],{encoding:'utf8'});
    const css=html.match(/<style>([\s\S]*?)<\/style>/)[1];
    const folder=`client-web-v2/src/design/${mode}.css.parts`;
    assert.equal(fs.readdirSync(folder).sort().map(f=>fs.readFileSync(path.join(folder,f),'utf8')).join(''),css);
    fs.writeFileSync(path.join(refs,file),html);
    const ref=await page.browser().newPage();
    try{
      await ref.setViewport({width,height});await ref.goto('file://'+path.join(refs,file));
      if(mode==='desktop')await ref.click('[data-open]');else await ref.click('[data-then=read]');
      await ref.waitForSelector(mode==='desktop'?'.story-block':'.rtop');
      await page.setViewport({width,height});await page.goto(base+'/?ui=v2#'+id);await page.waitForSelector(mode==='desktop'?'.rbar':'.rtop');
      const targets=mode==='desktop'?['.rbar','.page','.story-block','.scene-meta']:['.rtop','.rbot','.rtext','.rtext p:not(.meta)'];
      for(const selector of targets){
        const measure=e=>{const s=getComputedStyle(e);return Object.fromEntries(['height','maxWidth','paddingLeft','paddingRight','fontFamily','fontSize','lineHeight','textIndent','textAlign','color','backgroundColor'].filter(k=>k!=='height'||e.matches('.rbar,.rtop,.rbot')).map(k=>[k,s[k]]));};
        const expected=await ref.$eval(selector,measure),actual=await page.$eval(selector,measure);
        assert.deepEqual(actual,expected,mode+' '+selector);checks.push({mode,selector,actual});
      }
      if(mode==='desktop'){
        await ref.click('#insightsBtn');await click('#insightsBtn');
        assert.equal(await page.$eval('.drawer',e=>e.getBoundingClientRect().width),await ref.$eval('.drawer',e=>e.getBoundingClientRect().width));
      }
    }finally{await ref.close();}
  }
  fs.writeFileSync(path.join(output,'fidelity.json'),JSON.stringify(checks,null,2));
};
