function selectWindow(index){
  S.win=index==='all'?'all':String(Math.max(0,Math.min(SB.quality.windows.length-1,Number(index))));
  const focus=document.activeElement?.id==='win-chart';renderPanel();
  if(focus)$('#win-chart')?.focus({preventScroll:true});
}
function refreshMetricSheet(){
  const body=$('.sheet .sb');if(!body)return;
  const scroll=body.scrollTop,focus=document.activeElement?.id==='win-chart';
  body.innerHTML=mMetricSheetBody(S.marg);designStyles(body);body.scrollTop=scroll;
  if(focus)$('#win-chart')?.focus({preventScroll:true});
}
function selectMobileWindow(index){
  S.mwin=index==='all'?null:Math.max(0,Math.min(SB.quality.windows.length-1,Number(index)));
  refreshMetricSheet();
}
async function showWindowInText(){
  const w=SB.quality.windows[S.mobile?S.mwin:Number(S.win)];if(!w)return;
  const selected=S.selected,report=S.quality;
  if(S.mobile){S.immersive=false;jumpChapter(w.chFrom-1);}else if(innerWidth<1100)dCloseAll();
  await S.imagePromise;
  if(S.selected!==selected||S.quality!==report)return;
  $$('.win-band').forEach(el=>el.classList.remove('win-band','first'));
  const nodes=new Map($$('[data-text-block]').map(el=>[el.dataset.textBlock,el]));
  const spans=report.block_spans;let first;
  for(let i=w.from;i<=w.to;i++){
    const node=nodes.get(spans[i]?.block_id);if(!node)continue;
    node.classList.add('win-band');if(!first)first=node;
  }
  first?.classList.add('first');first?.scrollIntoView({block:'start',behavior:'instant'});captureReading();
}
