function windowLocations(report){
  if(S.windowSource===S.quality&&S.windowNovel===S.selected)return S.windowRows;
  const spans=S.quality?.block_spans||[],positions=new Map();
  S.selected?.revision.chapters.forEach((c,ci)=>{
    let paragraph=0;
    for(const scene of c.scenes)for(const block of scene.blocks)
      positions.set(block.id,{chapter:ci+1,paragraph:++paragraph});
  });
  function at(offset){
    let lo=0,hi=spans.length;
    while(lo<hi){const mid=(lo+hi)>>1;if(spans[mid].start<=offset)lo=mid+1;else hi=mid;}
    return Math.max(0,lo-1);
  }
  S.windowRows=report.windows.map((w,i)=>{
    let from=at(w.start),to=at(Math.max(w.start,w.end-1));
    if(spans[from]?.end<=w.start&&from<spans.length-1)from++;
    const a=positions.get(spans[from]?.block_id),b=positions.get(spans[to]?.block_id);
    return {...w,i,from,to,chFrom:a?.chapter||1,pFrom:a?.paragraph||1,
      chTo:b?.chapter||1,pTo:b?.paragraph||1,
      values:Object.fromEntries(w.metrics.map(m=>[m.metric,m.value]))};
  });
  S.windowSource=S.quality;S.windowNovel=S.selected;return S.windowRows;
}
function evidenceInWindow(e,w){return !w||(e.start<w.end&&e.end>w.start);}
