const test=require('node:test'),assert=require('node:assert/strict'),source=require('./source.cjs');
test('window labels and bands map UTF-16 spans across chapter and newline boundaries',()=>{
  const {S,windowLocations,evidenceInWindow}=source(['00-state.js','shared/05-window-locations.js'],['S','windowLocations','evidenceInWindow']);
  S.selected={revision:{chapters:[{scenes:[{blocks:[{id:'a'},{id:'b'}]}]},{scenes:[{blocks:[{id:'c'}]}]}]}};
  // a='月🦊台' has four UTF-16 units; b='夜色'; c='燈火'.
  S.quality={block_spans:[{block_id:'a',start:0,end:4},{block_id:'b',start:5,end:7},{block_id:'c',start:8,end:10}]};
  const windows=[{start:1,end:9},{start:5,end:8},{start:8,end:10}].map(w=>({...w,metrics:[{metric:'test',value:null}]}));
  const rows=windowLocations({windows});
  assert.deepEqual(Array.from(rows,w=>[w.from,w.to,w.chFrom,w.pFrom,w.chTo,w.pTo]),[[0,2,1,1,2,1],[1,1,1,2,1,2],[2,2,2,1,2,1]]);
  assert.equal(rows[0].values.test,null);assert.equal(evidenceInWindow({start:0,end:5},rows[1]),false);
  assert.equal(evidenceInWindow({start:6,end:9},rows[1]),true);
  assert.strictEqual(windowLocations({windows}),rows);
  S.quality={...S.quality};assert.notStrictEqual(windowLocations({windows}),rows);
});
