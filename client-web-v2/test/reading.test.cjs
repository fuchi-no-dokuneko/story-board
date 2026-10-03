const test=require('node:test'),assert=require('node:assert/strict');
const source=require('./source.cjs');
test('preferences and position persist without credentials or story text',()=>{
  const {S,prefs,rememberCurrent,storage}=source(['00-state.js','shared/01-model.js','shared/14-progress.js'],
    ['S','prefs','rememberCurrent']);
  S.token='private-token';S.lang='zh';S.prefs.size=22;
  S.selected={novel:{novel_id:'nov_abc12'},revision:{chapters:[{title:'chapter',text:'private text'}]}};
  prefs();rememberCurrent({anchor:'blk_abc12',offset:-12,top:1600,progress:.4});
  assert.deepEqual([...storage.keys()].sort(),['sb2.readerPreferences','sb2.recent']);
  assert.equal(JSON.parse(storage.get('sb2.readerPreferences')).desktop.size,22);
  assert.equal(JSON.parse(storage.get('sb2.recent'))[0].top,1600);
  assert.doesNotMatch([...storage.values()].join(''),/private/);
});
test('returning to the library cancels a pending novel without reopening it',async()=>{
  let finish;
  const {S,openNovel,library}=source(['00-state.js','shared/12-selection.js'],['S','openNovel','library'],{
    json:()=>new Promise(resolve=>finish=resolve),closeLayers(){},releaseImages(){},renderApp(){},captureReading(){},
  });
  const opening=openNovel('nov_abc12');library();
  finish({novel:{head_revision_id:'rev_abc12'},revision:{revision_id:'rev_abc12',chapters:[]}});
  await opening;assert.equal(S.view,'library');assert.equal(S.selected,null);
});
test('evidence offsets preserve emoji and exact fragments across paragraphs',()=>{
  const {evidenceOverlaps}=source(['shared/18-evidence.js'],['evidenceOverlaps']);
  const first='林雨看著🦊，沒有寄出的信。',second='沒有寄出的信，仍在桌上。';
  const spans=[{block_id:'blk_abc12',start:0,end:first.length},
    {block_id:'blk_def34',start:first.length+1,end:first.length+1+second.length}];
  const hits=evidenceOverlaps(spans,first.indexOf('🦊'),first.length+1+7);
  assert.equal(first.slice(hits[0].start,hits[0].end),'🦊，沒有寄出的信。');
  assert.equal(second.slice(hits[1].start,hits[1].end),'沒有寄出的信，');
});
