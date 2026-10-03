function remembered() {
  const list=readLocal('recent',[]);
  return Array.isArray(list)?list.filter(x=>x&&typeof x.id==='string'):[];
}
function mapBook(n) {
  if(!n)return null;
  const stored=remembered().find(x=>x.id===n.novel_id);
  const detail=S.details.get(n.novel_id);
  const blocks=detail?.revision.chapters.flatMap(c=>c.scenes.flatMap(s=>s.blocks))||[];
  const palette=[18,160,230,280,95,200];
  const hue=palette[[...n.novel_id].reduce((v,c)=>v+c.charCodeAt(0),0)%palette.length];
  return {...n,han:n.han_character_count,chapters:n.chapter_count,scenes:n.scene_count,
    blocks:n.block_count,characters:n.main_characters||[],registered:n.agent_write_registered,
    head_hash:n.head_hash,han_hash:n.han_text_sha256,parent_revision_id:detail?.revision.parent_revision_id,
    hue,images:blocks.filter(b=>b.extensions?.['storyblock.image']).length,
    progress:stored?.progress||0,chapterTitle:stored?.chapterTitle||'',};
}
function valueLabel(value) {
  if(value==null)return '';
  if(typeof value!=='object')return String(value);
  if(value.mode==='inherited')return t('inherited');
  if(value.mode==='unknown')return t('unknown');
  if(value.mode==='not_applicable')return t('notApplicable');
  return 'value' in value?valueLabel(value.value):JSON.stringify(value);
}
function mapChapters(id) {
  return (S.details.get(id)?.revision.chapters||[]).map(c=>({...c,scenes:c.scenes.map(s=>{
    const seed=s.initial_meta||{};
    return {...s,transition:s.transition_mode,meta:{time:valueLabel(seed.time),
      location:valueLabel(seed.location),weather:valueLabel(seed.weather),present:seed.present_character_ids||[]},
      blocks:s.blocks.map(b=>({...b,image:!!b.extensions?.['storyblock.image'],
        alt:b.extensions?.['storyblock.image']?.alt_text||''}))};
  })}));
}
Object.defineProperties(S,{
  novel:{get:()=>mapBook(S.selected?.novel)},book:{get:()=>S.selected?.novel.novel_id},
  q:{get:()=>S.query,set:v=>{S.query=v;}},
  compared:{get:()=>!!S.comparison,set:v=>{if(!v)S.comparison=null;}},
  analyzed:{get:()=>!!S.quality,set:v=>{if(!v)S.quality=null;}},
  win:{get:()=>S.qualityWindow,set:v=>{S.qualityWindow=v;}},
});
