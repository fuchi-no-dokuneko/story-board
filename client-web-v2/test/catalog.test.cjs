const test=require('node:test'),assert=require('node:assert/strict');
const source=require('./source.cjs');
const files=['00-state.js','12-catalog-data.js'];
test('search, language and imported filters work together across a catalog',()=>{
  const {S,catalogMatches}=source(files,['S','catalogMatches']);
  S.catalog=[{novel_id:'nov_abc12',title:'Last Train',language:'en',main_characters:['Lin'],
    agent_write_registered:false,block_count:12,updated_at:'2026-10-01'},
    {novel_id:'nov_def34',title:'末班車',language:'zh-Hant',main_characters:['林雨'],
      agent_write_registered:true,block_count:30,updated_at:'2026-10-02'}];
  S.query='LAST';assert.equal(catalogMatches()[0].title,'Last Train');
  S.query='lin';assert.equal(catalogMatches()[0].novel_id,'nov_abc12');
  S.filter='zh-Hant';assert.equal(catalogMatches().length,0);
  S.query='';S.filter='imported';assert.equal(catalogMatches()[0].language,'en');
  S.filter='all';S.sort='length';assert.equal(catalogMatches()[0].block_count,30);
});
test('a refreshed catalog cannot be overwritten by an earlier response',async()=>{
  const pending=[];
  const {S,loadCatalog}=source(files,['S','loadCatalog'],{
    json:()=>new Promise(resolve=>pending.push(resolve)),renderLibrary(){},report(){},
  });
  const first=loadCatalog(),oldSignal=S.catalogAbort.signal,second=loadCatalog();
  assert.ok(oldSignal.aborted);
  pending[1]({items:[{novel_id:'nov_new12'}],total_pages:1});await second;
  pending[0]({items:[{novel_id:'nov_old34'}],total_pages:1});await first;
  assert.equal(S.catalog[0].novel_id,'nov_new12');assert.equal(S.loading,false);
});
test('paging collects real metadata once per ID before filtering',async()=>{
  const paths=[];
  const {S,loadCatalog}=source(files,['S','loadCatalog'],{renderLibrary(){},report(){},
    json:async path=>{paths.push(path);return {items:[{novel_id:'nov_abc12'},
      {novel_id:paths.length===1?'nov_def34':'nov_ghi56'}],total_pages:2};}});
  await loadCatalog();assert.equal(paths.length,2);assert.match(paths[1],/page=1&size=100/);
  assert.equal(S.catalog.length,3);
});
