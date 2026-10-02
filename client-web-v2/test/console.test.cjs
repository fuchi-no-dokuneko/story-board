const test=require('node:test'),assert=require('node:assert/strict');
const source=require('./source.cjs');
test('reopening the console does not show a response from its previous visit',async()=>{
  let finish;
  const {S,sendConsole,closePanel,elements}=source(['00-state.js','30-panels.js','37-settings.js'],
    ['S','sendConsole','closePanel'],{request:()=>new Promise(resolve=>finish=resolve)});
  S.panel='console';const pending=sendConsole('/v1/openapi.yaml');
  closePanel();S.panel='console';elements.get('response').textContent='Ready';
  finish({status:200,statusText:'OK',text:async()=>'openapi: 3.1.0'});await pending;
  assert.equal(elements.get('response').textContent,'Ready');
});
