const test=require('node:test'),{execFileSync}=require('node:child_process');
test('repository assembly includes each renderer once and produces valid JavaScript',()=>{
  execFileSync('python3',['scripts/assemble-sources.py']);
  execFileSync(process.execPath,['--check','server/src/main/resources/static/app.js']);
});
