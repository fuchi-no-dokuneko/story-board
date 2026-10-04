const test=require('node:test'),assert=require('node:assert/strict'),source=require('./source.cjs');
function helpers(lang='en'){
  const H={lang,num:String,pick:e=>e[lang],esc:s=>String(s).replaceAll('&','&amp;').replaceAll("'",'&#39;').replaceAll('<','&lt;')};
  return source(['shared/04-fixes.js'],['FX'],{H}).FX;
}
test('readable metric labels, plurals, Chinese measure words and vertical digits',()=>{
  const FX=helpers();assert.equal(FX.count(1,'chapter'),'1 chapter');assert.equal(FX.count(30,'chapter'),'30 chapters');
  assert.equal(helpers('zh').count(2,'scene'),'2 個場景');assert.equal(helpers('zhs').count(2,'novel'),'2 本小说');
  assert.equal(FX.tcy("第7號 O'Brien 128"),'第<span class="tcy">7</span>號 O&#39;Brien 128');
  assert.equal(FX.metricLabel('robust_l1_input'),'L1 distance');assert.equal(FX.metricLabel('unknown'),'unknown');
  assert.equal(helpers('zhs').metricLabel('wasserstein_distance'),'Wasserstein 距离');
  assert.equal(FX.sceneFacts({time:'',location:'',weather:'',present:[]}).length,0);
  assert.equal(FX.sceneFacts({time:'夜',location:'月台',weather:'雨',present:['林雨']}).length,4);
});
test('uncalibrated chart values remain missing and bounds stay within the report',()=>{
  const FX=helpers();assert.equal(FX.stats([null,null]),null);
  const stats=FX.stats([null,.3,.1,.9]);assert.equal(stats.minAt,2);assert.equal(stats.maxAt,3);
  assert.equal(stats.median,.3);const el={getBoundingClientRect:()=>({left:100,width:200})};
  assert.equal(FX.indexAt(el,0,714),0);assert.equal(FX.indexAt(el,300,714),713);
});
