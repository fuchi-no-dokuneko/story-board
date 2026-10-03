function updateReadingProgress(){
  if(!S.selected||S.restoring||S.view!=='reading')return;
  const container=readingContainer();if(!container)return;
  const h=Math.max(0,container.scrollHeight-container.clientHeight);
  if(S.mobile){
    const progress=(S.chapter+(h?container.scrollTop/h:0))/Math.max(1,S.selected.revision.chapters.length);
    const slider=$('.slider input');if(slider)slider.value=String(Math.round(progress*100));
    if($('#pct'))$('#pct').textContent=Math.round(progress*100)+'%';
  }else{
    const sections=$$('.chapter-section');let index=0;
    sections.forEach((section,i)=>{if(section.getBoundingClientRect().top<140)index=i;});
    S.chapter=index;
    $('#progress').style.width=(h?container.scrollTop/h*100:0)+'%';
    $('#rbar-chapter').textContent=`${sections[index]?.dataset.title||''} · ${index+1}/${S.novel.chapters}`;
    $$('.chapter-link').forEach(b=>b.setAttribute('aria-current',String(b.dataset.jump==='chapter-'+(index+1))));
  }
}
function trackReading(target){
  S.scrollCleanup?.();let frame=0,timer=0;
  const run=()=>{if(frame)return;frame=requestAnimationFrame(()=>{frame=0;updateReadingProgress();});
    clearTimeout(timer);timer=setTimeout(captureReading,150);};
  target.addEventListener('scroll',run,{passive:true});
  S.scrollCleanup=()=>{target.removeEventListener('scroll',run);cancelAnimationFrame(frame);clearTimeout(timer);};
  run();
}
function dTrackProgress(){trackReading(window);}
function trackMobileProgress(){trackReading($('#rscroll'));}
function jumpChapter(index){
  if(!S.selected)return;captureReading();
  S.chapter=Math.max(0,Math.min(index,S.selected.revision.chapters.length-1));
  if(S.mobile){closeLayers();S.stack=['lib','book','read'];S.mobileTab='read';S.view='reading';
    releaseImages();mRender();$('#rscroll').scrollTop=0;}
  else{$('#chapter-'+(S.chapter+1))?.scrollIntoView({behavior:'instant'});if(innerWidth<1280)dCloseAll();}
  rememberCurrent({anchor:null,top:0});captureReading();
}
function seekReading(percent){
  const count=S.selected?.revision.chapters.length||0;if(!count)return;
  const position=Math.min(.999999,Math.max(0,percent/100))*count;
  jumpChapter(Math.floor(position));
  const el=$('#rscroll');el.scrollTop=(position-Math.floor(position))*(el.scrollHeight-el.clientHeight);
  updateReadingProgress();captureReading();
}
