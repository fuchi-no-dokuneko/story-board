const H={
  get lang(){return S.lang;},num,esc,
  pick:entry=>entry?.[S.lang]||entry?.en||'',
  langName:code=>H.pick(langNames[code])||code,
  transition:mode=>H.pick(transitionNames[mode])||mode||'',
  status:status=>H.pick(statusNames[status])||t(status),
  metric:id=>H.pick(metricLabels[id])||id,
  channel:c=>c[S.lang]||c.en,
  d6:v=>decimal(v,6),d3:v=>decimal(v,3),
  date:iso=>iso?new Date(iso).toLocaleDateString(locale(),{year:'numeric',month:'short',day:'numeric'}):'—',
  ago(iso){
    const days=Math.round((Date.now()-new Date(iso))/864e5);
    return days<30?new Intl.RelativeTimeFormat(locale(),{numeric:'auto'}).format(-Math.max(0,days),'day'):H.date(iso);
  },
  novel:id=>mapBook(S.details.get(id)?.novel||S.catalog.find(n=>n.novel_id===id)),
  body:mapChapters,
  blocksOf:id=>mapChapters(id).flatMap(c=>c.scenes.flatMap(s=>s.blocks)),
  styleName:id=>S.styles.find(s=>s.id===id)?.name||id,
  apply(dict,root=document){
    $$('[data-i18n]',root).forEach(el=>{el.textContent=t(el.dataset.i18n);});
    $$('[data-i18n-ph]',root).forEach(el=>{el.placeholder=t(el.dataset.i18nPh);});
    $$('[data-i18n-aria]',root).forEach(el=>el.setAttribute('aria-label',t(el.dataset.i18nAria)));
    $$('[data-lang-btn]',root).forEach(el=>el.setAttribute('aria-pressed',String(el.dataset.langBtn===S.lang)));
  },
  columnStats(){
    return Object.fromEntries(SB.channels.map(c=>{
      const entries=Object.entries(SB.comparison.rows).filter(([id])=>S.picks.has(id));
      const values=entries.map(([,r])=>r[c.id]).filter(Number.isFinite);
      const min=Math.min(...values),max=Math.max(1e-12,...values);
      return [c.id,{min,max,closest:entries.find(([,r])=>r[c.id]===min)?.[0]}];
    }));
  },
  ev:(id,metric)=>(metric.evidence||[]).map((e,i)=>({...e,index:i,
    block:H.blocksOf(id).findIndex(b=>b.id===S.quality?.block_spans.find(s=>s.start<e.end&&s.end>e.start)?.block_id)})),
};
function decimal(value,digits=4){return value==null||!Number.isFinite(Number(value))?'—':Number(value).toFixed(digits);}
