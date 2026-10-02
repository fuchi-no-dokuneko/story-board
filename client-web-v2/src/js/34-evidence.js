function metricCard(metric,index) {
  return `<section class="sb-metric"><header><h3>${t(metric.metric)}</h3><b>${decimal(metric.value)}</b></header>
    <p>${t(metric.metric+'Hint')}</p><div class="sb-metric-meta"><span class="sb-badge">${t(metric.status)}</span>
    <span>${num(metric.validSamples)} ${t('samples')}</span><span>${t('percentile')}: ${metric.referencePercentile==null?'—':decimal(metric.referencePercentile,1)+'%'}</span></div>
    ${metric.evidence.length?`<details class="sb-evidence"><summary>${t('evidence')} (${num(metric.evidence.length)})</summary>
    ${metric.evidence.map((e,i)=>`<blockquote><p>${esc(e.text)}</p><small>${esc(e.reason)}</small>
      ${btn('evidence',t('showInText'),`data-metric="${index}" data-evidence="${i}"`)}</blockquote>`).join('')}</details>`:''}</section>`;
}
function evidenceOverlaps(spans,start,end) {
  return spans.filter(s=>s.start<end&&s.end>start).map(s=>({id:s.block_id,
    start:Math.max(0,start-s.start),end:Math.min(s.end-s.start,end-s.start)}));
}
function showEvidence(metricIndex,evidenceIndex) {
  const evidence=currentQuality()?.metrics[metricIndex]?.evidence[evidenceIndex];
  if(!evidence||!S.quality||S.quality.revision_id!==S.selected?.revision.revision_id)return;
  const overlaps=evidenceOverlaps(S.quality.block_spans,evidence.start,evidence.end);
  const first=overlaps.map(o=>findBlock(o.id)).find(Boolean);
  if(!first){toast(t('evidenceUnavailable'));return;}
  jumpChapter(first.chapterIndex,false);
  for(const hit of overlaps){
    const target=[...document.querySelectorAll('[data-text-block]')].find(p=>p.dataset.textBlock===hit.id);
    if(!target)continue;
    const value=target.textContent, mark=document.createElement('mark');
    mark.textContent=value.slice(hit.start,hit.end);
    target.replaceChildren(document.createTextNode(value.slice(0,hit.start)),mark,document.createTextNode(value.slice(hit.end)));
  }
  const target=document.querySelector('.sb-prose mark');
  target?.scrollIntoView({block:'center',behavior:'instant'});
  if(target){target.tabIndex=-1;target.focus({preventScroll:true});}
}
