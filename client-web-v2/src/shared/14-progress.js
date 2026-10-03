function rememberCurrent(extra={}){
  if(!S.selected)return;
  const id=S.book,old=remembered().find(r=>r.id===id)||{};
  saveLocal('recent',[{...old,...extra,id,chapter:S.chapter,
    chapterTitle:S.selected.revision.chapters[S.chapter]?.title||''},
    ...remembered().filter(r=>r.id!==id)].slice(0,50));
}
function readingContainer(){return S.mobile?$('#rscroll'):document.scrollingElement;}
function captureReading(){
  if(!S.selected||S.restoring||S.view!=='reading')return;
  const container=readingContainer();if(!container)return;
  const boundary=S.mobile?(S.immersive?0:$('.rtop').getBoundingClientRect().height):58;
  const blocks=$$('[data-text-block]').filter(e=>!e.closest('[hidden]'));
  let lo=0,hi=blocks.length;
  while(lo<hi){const mid=(lo+hi)>>1;if(blocks[mid].getBoundingClientRect().bottom<boundary)lo=mid+1;else hi=mid;}
  const target=blocks[lo],height=Math.max(0,container.scrollHeight-container.clientHeight);
  const fraction=height?container.scrollTop/height:0;
  const progress=S.mobile?(S.chapter+fraction)/Math.max(1,S.selected.revision.chapters.length):fraction;
  rememberCurrent({anchor:target?.dataset.textBlock,offset:target?target.getBoundingClientRect().top-boundary:0,
    top:container.scrollTop,progress:Math.min(1,Math.max(0,progress))});
}
async function restoreReading(recent){
  if(S.view!=='reading'||!S.selected)return;
  const selected=S.selected;S.restoring=true;
  await S.imagePromise;await new Promise(resolve=>requestAnimationFrame(resolve));
  if(selected!==S.selected){S.restoring=false;return;}
  const container=readingContainer();if(!container){S.restoring=false;return;}
  const target=$$('[data-text-block]').find(e=>e.dataset.textBlock===recent?.anchor&&!e.closest('[hidden]'));
  const boundary=S.mobile?$('.rtop').getBoundingClientRect().height:58;
  if(target&&recent?.top>0)container.scrollTop+=target.getBoundingClientRect().top-boundary-(recent.offset||0);
  else if(S.mobile)container.scrollTop=recent?.top||0;
  else if(recent?.chapter>0)$('#chapter-'+(recent.chapter+1))?.scrollIntoView();
  else container.scrollTop=0;
  S.restoring=false;updateReadingProgress();
}
