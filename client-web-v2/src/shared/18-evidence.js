function evidenceOverlaps(spans,start,end){
  return spans.filter(s=>s.start<end&&s.end>start).map(s=>({id:s.block_id,
    start:Math.max(0,start-s.start),end:Math.min(s.end-s.start,end-s.start)}));
}
function showEvidence(metricIndex,evidenceIndex){
  const report=S.mobile?S.quality?.quality_report:currentQuality();
  const evidence=report?.metrics[metricIndex]?.evidence[evidenceIndex];
  if(!evidence||S.quality.revision_id!==S.selected?.revision.revision_id)return;
  const overlaps=evidenceOverlaps(S.quality.block_spans,evidence.start,evidence.end);
  const first=overlaps.map(x=>findBlock(x.id)).find(Boolean);
  if(!first){toast(t('evidenceUnavailable'));return;}
  if(S.mobile)jumpChapter(first.chapterIndex);else if(innerWidth<1100)dCloseAll();
  $$('.story-block mark,figcaption mark').forEach(m=>m.replaceWith(m.textContent));
  for(const hit of overlaps){
    const target=$$('[data-text-block]').find(e=>e.dataset.textBlock===hit.id);if(!target)continue;
    const value=target.textContent,mark=document.createElement('mark');mark.textContent=value.slice(hit.start,hit.end);
    target.replaceChildren(document.createTextNode(value.slice(0,hit.start)),mark,document.createTextNode(value.slice(hit.end)));
  }
  const mark=$('#reader-content mark');mark?.scrollIntoView({block:'center',behavior:'instant'});
  if(mark){mark.tabIndex=-1;mark.focus({preventScroll:true});}captureReading();
}
