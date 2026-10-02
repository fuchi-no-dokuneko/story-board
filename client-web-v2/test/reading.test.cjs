const test=require('node:test'),assert=require('node:assert/strict');
const source=require('./source.cjs');
test('reading preferences and position persist without credentials or story text',()=>{
  const {S,prefs,remember,storage}=source(['00-state.js','12-catalog-data.js','20-selection.js'],
    ['S','prefs','remember']);
  S.token='private-token';S.lang='zh-Hant';S.size=28;
  S.selected={novel:{novel_id:'nov_abc12'},revision:{chapters:[{text:'private text'}]}};
  S.chapter=2;prefs();remember();
  assert.deepEqual([...storage.keys()].sort(),['sb2.preferences','sb2.recent']);
  assert.equal(JSON.parse(storage.get('sb2.preferences')).size,28);
  assert.deepEqual(JSON.parse(storage.get('sb2.recent')),[{id:'nov_abc12',chapter:2}]);
  assert.doesNotMatch([...storage.values()].join(''),/private/);
});
test('returning to the shelf cancels a pending book without reopening it',async()=>{
  let finish,rendered=0,view;
  const {S,openNovel,library}=source(['00-state.js','20-selection.js'],['S','openNovel','library'],{
    json:()=>new Promise(resolve=>finish=resolve),closePanel(){},releaseImages(){},renderLibrary(){},
    showView:value=>view=value,renderReader:()=>rendered++,
  });
  const opening=openNovel('nov_abc12');library();
  finish({novel:{head_revision_id:'rev_abc12'},revision:{revision_id:'rev_abc12',chapters:[]}});
  await opening;assert.equal(view,'library');assert.equal(S.selected,null);assert.equal(rendered,0);
});
test('evidence offsets preserve emoji and exact fragments across paragraphs',()=>{
  const {evidenceOverlaps}=source(['34-evidence.js'],['evidenceOverlaps']);
  const first='林雨看著🦊，沒有寄出的信。',second='沒有寄出的信，仍在桌上。';
  const spans=[{block_id:'blk_abc12',start:0,end:first.length},
    {block_id:'blk_def34',start:first.length+1,end:first.length+1+second.length}];
  const start=first.indexOf('🦊'),end=first.length+1+7;
  const hits=evidenceOverlaps(spans,start,end);
  assert.equal(first.slice(hits[0].start,hits[0].end),'🦊，沒有寄出的信。');
  assert.equal(second.slice(hits[1].start,hits[1].end),'沒有寄出的信，');
});
