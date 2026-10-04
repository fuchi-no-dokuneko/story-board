const fs=require('fs'),path=require('path');
module.exports=async c=>{
  c.fixChecks=[];
  c.check=(name,condition,detail)=>{
    c.fixChecks.push({name,pass:!!condition,detail});c.assert.ok(condition,name+' '+(detail??''));
  };
  c.longId=JSON.parse(fs.readFileSync(path.join(c.output,'windows-fixture.json'))).novel_id;
  c.testid=id=>'[data-testid="'+id+'"]';
  c.label=()=>c.page.$eval(c.testid('window-label'),e=>e.innerText);
  c.analyze=async()=>{
    const response=c.page.waitForResponse(r=>r.url().endsWith('/quality-reports')&&r.status()===200,{timeout:120000});
    await c.click('#analyze-quality');const report=await(await response).json();
    await c.page.waitForSelector('.metric,.mrow');return report;
  };
  try{
    for(const name of ['desktop','short','theme','mobile','reopen'])
      await require('./fix-'+name+'.cjs')(c);
  }finally{
    fs.writeFileSync(path.join(c.output,'zip-acceptance.json'),JSON.stringify(c.fixChecks,null,2));
  }
};
