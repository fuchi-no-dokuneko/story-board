const test=require('node:test'),assert=require('node:assert/strict'),source=require('./source.cjs');
test('reopening reuses catalog-matched detail and refreshes a changed head',async()=>{
  let calls=0,revision='rev_first';
  const {S,openNovel,library}=source(['00-state.js','shared/12-selection.js'],['S','openNovel','library'],{
    json:async()=>{calls++;return {novel:{head_revision_id:revision},revision:{revision_id:revision,chapters:[]}};},
    captureReading(){},closeLayers(){},releaseImages(){},renderApp(){},remembered:()=>[],restoreReading(){},rememberCurrent(){},
  });
  S.catalog=[{novel_id:'nov_abc12',head_revision_id:revision}];
  await openNovel('nov_abc12');library();await openNovel('nov_abc12');assert.equal(calls,1);
  revision='rev_second';S.catalog[0].head_revision_id=revision;
  library();await openNovel('nov_abc12');assert.equal(calls,2);assert.equal(S.selected.revision.revision_id,revision);
  library();S.catalog=[];await openNovel('nov_abc12');assert.equal(calls,3,'Unknown catalog head is not reusable');
});
