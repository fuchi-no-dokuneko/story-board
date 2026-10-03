const SB={
  get novels(){return S.catalog.map(mapBook);},
  get styles(){return S.styles;},
  get service(){return {version:S.online?t('online'):t('offline')};},
  get channels(){return designChannels.map(c=>({...c,
    metric:S.comparison?.comparisons[0]?.scores.find(s=>s.channel===c.id)?.primary_metric||c.metric}));},
  get metricsInfo(){return Object.fromEntries(Object.entries(metricLabels).map(([id,label])=>
    [id,{...label,hint:words[id+'Hint']?.[['en','zh','zhs'].indexOf(S.lang)]||label.hint}]));},
  get comparison(){return {revision_id:S.comparison?.revision_id,rows:Object.fromEntries(
    (S.comparison?.comparisons||[]).map(c=>[c.style_id||c.id,Object.fromEntries(c.scores.map(s=>
      [s.channel,s.primary_distance==null?null:Number(s.primary_distance)]))]))};},
  get quality(){
    const report=S.quality?.quality_report;
    if(!report)return {metrics:[],windows:[],statistics:{},calibration:{}};
    return {...report,windows:report.windows.map(w=>({...w,
      values:Object.fromEntries(w.metrics.map(m=>[m.metric,m.value]))}))};
  },
};
function currentQuality(){
  const report=S.quality?.quality_report;
  return S.win==='all'?report:report?.windows[Number(S.win)];
}
function findBlock(id){
  for(const [chapterIndex,c] of (S.selected?.revision.chapters||[]).entries())
    for(const scene of c.scenes){const block=scene.blocks.find(b=>b.id===id);if(block)return {block,scene,chapterIndex};}
}
function catalogMatches(){
  const q=S.query.trim().toLocaleLowerCase();
  return SB.novels.filter(n=>(S.filter==='all'||(S.filter==='imported'?!n.registered:n.language===S.filter))&&
    [n.title,n.novel_id,...n.characters].some(v=>v.toLocaleLowerCase().includes(q)))
    .sort((a,b)=>S.sort==='title'?a.title.localeCompare(b.title,locale()):
      S.sort==='length'?b.blocks-a.blocks:b.updated_at.localeCompare(a.updated_at));
}
