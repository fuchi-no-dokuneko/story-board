const test=require('node:test'),assert=require('node:assert/strict');
const source=require('./source.cjs');
test('a console response cannot overwrite the next visit',async()=>{
  let finish;
  const {sendConsole,elements}=source(['00-state.js','shared/19-connection.js'],['sendConsole'],{
    request:()=>new Promise(resolve=>finish=resolve),
  });
  const pending=sendConsole('/v1/openapi.yaml');
  elements.get('response').isConnected=false;
  elements.set('response',{textContent:'Ready',isConnected:true});
  finish({status:200,statusText:'OK',text:async()=>'openapi: 3.1.0'});await pending;
  assert.equal(elements.get('response').textContent,'Ready');
});
