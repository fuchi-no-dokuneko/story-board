function mobileGo(screen){
  captureReading();closeLayers();
  if(['read','book','ins'].includes(screen)&&!S.selected){
    const book=resumeBook();if(book)openNovel(book.novel_id,{read:screen==='read'}).then(()=>{if(S.selected&&screen==='ins')mobileGo('ins');});else toast(t('chooseNovel'));return;
  }
  S.view={lib:'library',book:'book',read:'reading',ins:'insights',set:'settings',console:'console'}[screen];
  S.stack={lib:['lib'],book:['lib','book'],read:['lib','book','read'],ins:['ins'],set:['set'],console:['set','console']}[screen];
  S.mobileTab=['lib','read','ins','set'].includes(screen)?screen:S.mobileTab;
  if(screen==='read')S.immersive=false;
  renderApp(false);if(screen==='read')restoreReading(remembered().find(r=>r.id===S.book));
  if(screen==='ins'&&!S.styles.length&&!S.stylesLoading)loadStyles();
}
function switchLanguage(lang){
  captureReading();S.lang=lang;prefs();renderApp(false);
  if(S.view==='reading')restoreReading(remembered().find(r=>r.id===S.book));
}
function route(){
  let id='';try{id=decodeURIComponent(location.hash.slice(1));}catch{}
  if(/^nov_[A-Za-z0-9]{5}$/.test(id))openNovel(id,{read:true,fromHistory:true});
  else{S.novelRequest++;S.novelAbort?.abort();S.view='library';S.stack=['lib'];S.mobileTab='lib';renderApp(false);}
}
function copyValue(value){
  navigator.clipboard.writeText(value).then(()=>toast(t('copied'))).catch(()=>toast(t('copyFailed')));
}
