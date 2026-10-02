document.addEventListener('input',event=>{
  if(event.target.id==='catalog-search'){
    S.query=event.target.value;S.page=0;
    if(S.view!=='library')library();else renderLibrary();
  }
  if(event.target.id==='quality-names')S.names=event.target.value;
});
document.addEventListener('change',event=>{
  const target=event.target;
  if(target.id==='catalog-sort'){S.sort=target.value;S.page=0;renderLibrary();}
  if(target.dataset.style){target.checked?S.picks.add(target.dataset.style):S.picks.delete(target.dataset.style);S.comparison=null;renderPanel();}
  if(target.id==='quality-window'){S.qualityWindow=target.value;renderPanel();}
  if(target.id==='block-mode'){S.blocks=target.checked;if(S.selected)renderReader();}
});
document.addEventListener('submit',event=>{
  if(event.target.id==='operator-form'){
    event.preventDefault();const value=$('operator-token').value.trim();
    if(value)applyToken(value);else report(new Error(t('enterToken')),'operator-status');
  }
});
document.addEventListener('keydown',event=>{
  if(event.key==='Escape'){closePanel();return;}
  if(event.key==='/'&&!['INPUT','SELECT','TEXTAREA'].includes(event.target.tagName)&&!$('tools-dialog').open){
    event.preventDefault();$('catalog-search').focus();
  }
});
function route() {
  let id='';try{id=decodeURIComponent(location.hash.slice(1));}catch{}
  if(/^nov_[A-Za-z0-9]{5}$/.test(id))openNovel(id,{fromHistory:true,focus:false});
  else{S.novelRequest++;S.novelAbort?.abort();closePanel();releaseImages();showView('library');renderLibrary();}
}
window.addEventListener('popstate',route);
window.addEventListener('pagehide',releaseImages);
window.addEventListener('pageshow',event=>{if(event.persisted&&S.selected&&S.view==='reading')renderReader();});
shell();showView('library');health();loadCatalog();route();
