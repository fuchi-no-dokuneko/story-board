function windowClick(button){
  const d=button.dataset;
  if(d.step)return selectWindow((S.win==='all'?-1:Number(S.win))+Number(d.step));
  if(d.jumpWin!==undefined)return selectWindow(d.jumpWin);
  if(d.mwin!==undefined)return selectMobileWindow(d.mwin);
  if(d.mstep)return selectMobileWindow((S.mwin??-1)+Number(d.mstep));
  if(d.mlist){S.mlist=d.mlist==='hide'?0:Math.min(SB.quality.windows.length,S.mlist+50);return refreshMetricSheet();}
  if(d.mshow||button.id==='show-window')return showWindowInText();
}
document.addEventListener('pointerdown',e=>{
  const chart=e.target.closest('#win-chart');if(!chart)return;
  e.preventDefault();
  const select=x=>{const c=$('#win-chart');if(!c)return;
    const index=FX.indexAt(c,x,SB.quality.windows.length);
    if(S.mobile)selectMobileWindow(index);else selectWindow(index);
    $('#win-chart')?.focus({preventScroll:true});};
  select(e.clientX);
  const move=ev=>select(ev.clientX),up=()=>{
    removeEventListener('pointermove',move);removeEventListener('pointerup',up);removeEventListener('pointercancel',up);
  };
  addEventListener('pointermove',move);addEventListener('pointerup',up);addEventListener('pointercancel',up);
});
document.addEventListener('keydown',e=>{
  if(e.target.id!=='win-chart')return;
  const count=SB.quality.windows.length,cur=S.mobile?(S.mwin??-1):(S.win==='all'?-1:Number(S.win));
  const step={ArrowRight:1,ArrowLeft:-1,PageUp:10,PageDown:-10}[e.key];
  const next=step?cur+step:e.key==='Home'?0:e.key==='End'?count-1:null;if(next===null)return;
  e.preventDefault();if(S.mobile)selectMobileWindow(next);else selectWindow(next);
});
document.addEventListener('change',e=>{
  if(e.target.id==='win-metric'){S.winMetric=e.target.value;renderPanel();$('#win-metric')?.focus({preventScroll:true});}
});
