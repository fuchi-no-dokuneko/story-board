const test=require('node:test'),assert=require('node:assert/strict');
const source=require('./source.cjs');
const auth=require('../../server/src/main/resources/static/auth.js');
test('quality includes the pending name and discards results after changing books',async()=>{
  let finish,request;
  const {S,analyzeQuality,elements}=source(['00-state.js','10-api.js','shared/13-analysis.js'],['S','analyzeQuality'],{
    window:{StoryBlockAuth:auth},renderPanel(){},
    fetch:(url,options)=>{request={url,options};return new Promise(resolve=>finish=resolve);},
  });
  S.selected={revision:{novel_id:'nov_abc12',revision_id:'rev_def34',content_hash:'sha256:abc'}};
  S.names=['林雨'];elements.set('quality-names',{value:'周遠，林雨'});const pending=analyzeQuality();
  assert.equal(request.options.method,'POST');assert.equal(request.options.headers['If-Match'],'"sha256:abc"');
  assert.deepEqual(JSON.parse(request.options.body),{revision_id:'rev_def34',proper_names:['林雨','周遠']});
  S.selected={revision:{novel_id:'nov_new56'}};
  finish({ok:true,json:async()=>({revision_id:'rev_def34',quality_report:{}})});await pending;
  assert.equal(S.quality,null);assert.equal(S.qualityBusy,false);
});
test('channel minima are independent and missing distances never win',()=>{
  const {S,H}=source(['00-state.js','shared/02-presenters.js','shared/03-analysis-model.js'],['S','H'],{
    designChannels:[{id:'surface'},{id:'rhythm'}],mapChapters(){},metricLabels:{},
  });
  S.comparison={comparisons:[
    {style_id:'a',scores:[{channel:'surface',primary_distance:.123456},{channel:'rhythm',primary_distance:null}]},
    {style_id:'b',scores:[{channel:'surface',primary_distance:.123457},{channel:'rhythm',primary_distance:.1}]},
  ]};S.picks=new Set(['a','b']);
  assert.equal(H.columnStats().surface.closest,'a');assert.equal(H.columnStats().rhythm.closest,'b');
  assert.equal(H.d6(.123456),'0.123456');assert.equal(H.d6(null),'—');
});
