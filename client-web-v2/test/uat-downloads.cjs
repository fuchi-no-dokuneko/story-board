const fs=require('node:fs'),path=require('node:path');
module.exports=async({page,output,selected,id,quality,click,close,assert})=>{
  const directory=path.join(output,'downloads');
  const waitFile=async name=>{
    const file=path.join(directory,name),deadline=Date.now()+20000;
    while(Date.now()<deadline){
      if(fs.existsSync(file)&&!fs.existsSync(file+'.part')&&fs.statSync(file).size>0)return fs.readFileSync(file);
      await new Promise(resolve=>setTimeout(resolve,100));
    }
    throw Error('Download missing: '+name);
  };
  for(const file of fs.readdirSync(directory))fs.unlinkSync(path.join(directory,file));
  await click('#exportBtn');await click('#download-txt');
  const text=(await waitFile(selected.novel.title+'.txt')).toString('utf8');
  for(const chapter of selected.revision.chapters)
    for(const scene of chapter.scenes)for(const block of scene.blocks)assert.ok(text.includes(block.text));
  await click('#exportBtn');await click('#download-pdf');const pdf=await waitFile(selected.novel.title+'.pdf');
  assert.equal(pdf.subarray(0,5).toString(),'%PDF-');assert.ok(pdf.length>1000);await close();
  await click('#exportBtn');
  await click('#download-quality');const report=JSON.parse((await waitFile(id+'-quality.json')).toString());
  assert.deepEqual(report,quality);await close();
};
