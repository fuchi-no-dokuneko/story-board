async function openNovel(id,options={}){
  captureReading();const ticket=++S.novelRequest;
  S.novelAbort?.abort();S.novelAbort=new AbortController();
  S.styleRequest++;S.qualityRequest++;releaseImages();closeLayers();
  S.view=options.read||!S.mobile?'reading':'book';S.readError='';S.selected=null;
  S.stack=S.view==='reading'?['lib','book','read']:['lib','book'];
  S.mobileTab=S.view==='reading'?'read':'lib';renderApp(false);
  try{
    let payload=await json(`/v1/admin/novels/${encodeURIComponent(id)}`,{signal:S.novelAbort.signal});
    if(payload.novel.head_revision_id!==payload.revision.revision_id)
      payload=await json(`/v1/admin/novels/${encodeURIComponent(id)}`,{signal:S.novelAbort.signal});
    if(ticket!==S.novelRequest)return;
    if(payload.novel.head_revision_id!==payload.revision.revision_id)throw new Error(t('revisionChanged'));
    S.selected=payload;S.details.set(id,payload);S.comparison=S.quality=null;
    S.styleBusy=S.qualityBusy=S.stylesLoading=false;S.styleError=S.qualityError='';
    S.names=[...(payload.novel.main_characters||[])];S.win='all';
    const recent=remembered().find(r=>r.id===id);
    S.chapter=Math.max(0,Math.min(recent?.chapter||0,payload.revision.chapters.length-1));
    if(!options.fromHistory)history.pushState(null,'',`?ui=v2#${encodeURIComponent(id)}`);
    renderApp(false);restoreReading(recent);rememberCurrent(recent);
  }catch(error){
    if(ticket!==S.novelRequest||error.name==='AbortError')return;
    S.readError=error.message;S.retryId=id;renderApp(false);
  }
}
function library(){
  captureReading();S.novelRequest++;S.novelAbort?.abort();closeLayers();releaseImages();
  S.scrollCleanup?.();S.scrollCleanup=null;S.view='library';S.stack=['lib'];S.mobileTab='lib';
  history.pushState(null,'','?ui=v2');renderApp(false);window.scrollTo(0,0);
}
const dCloseNovel=library;
