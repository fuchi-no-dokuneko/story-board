const test=require('node:test'),assert=require('node:assert/strict');
const source=require('./source.cjs');
const auth=require('../../server/src/main/resources/static/auth.js');
test('analysis uses the selected revision and discards a result after switching books',async()=>{
  let finish,request;
  const {S,analyzeQuality}=source(['00-state.js','10-api.js','33-quality-data.js'],['S','analyzeQuality'],{
    window:{StoryBlockAuth:auth},renderPanel(){},
    fetch:(url,options)=>{request={url,options};return new Promise(resolve=>finish=resolve);},
  });
  S.selected={revision:{novel_id:'nov_abc12',revision_id:'rev_def34',content_hash:'sha256:abc'}};
  S.names='林雨, 周遠，林雨';const pending=analyzeQuality();
  assert.equal(request.options.method,'POST');assert.equal(request.options.headers['If-Match'],'"sha256:abc"');
  assert.deepEqual(JSON.parse(request.options.body),{revision_id:'rev_def34',proper_names:['林雨','周遠']});
  S.selected={revision:{novel_id:'nov_new56'}};
  finish({ok:true,json:async()=>({revision_id:'rev_def34',quality_report:{}})});await pending;
  assert.equal(S.quality,null);assert.equal(S.qualityBusy,false);
});
test('style channels rank independently and preserve full precision in the table',()=>{
  const {S,channelRows,styleResults,styleTable}=source(['00-state.js','32-style-results.js'],
    ['S','channelRows','styleResults','styleTable']);
  const channels=['surface','grammar','rhythm','narrative','lexical'];
  S.comparison={revision_id:'rev_abc12',comparisons:[
    {name:'抒情',scores:channels.map(channel=>({channel,primary_distance:channel==='surface'?0.123456:0.9}))},
    {name:'懸疑',scores:channels.map(channel=>({channel,primary_distance:channel==='surface'?0.123457:0.1}))},
  ]};
  assert.equal(channelRows('surface')[0].name,'抒情');assert.equal(channelRows('rhythm')[0].name,'懸疑');
  assert.equal((styleResults().match(/class="sb-channel"/g)||[]).length,5);
  assert.equal((styleResults().match(/sb-distance closest/g)||[]).length,5);
  assert.match(styleTable(),/<strong>0.123456<\/strong>/);assert.match(styleTable(),/<td>0.123457<\/td>/);
});
