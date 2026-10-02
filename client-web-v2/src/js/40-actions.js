const actions={
  library,refresh:loadCatalog,open:b=>openNovel(b.dataset.id),
  reading:()=>{const id=S.selected?.novel.novel_id||remembered()[0]?.id;
    if(id)openNovel(id);else toast(t('chooseNovel'));},
  filter:b=>{S.filter=b.dataset.filter;S.page=0;renderLibrary();},
  previous:()=>{S.page=Math.max(0,S.page-1);renderLibrary();},
  next:()=>{S.page++;renderLibrary();},chapter:b=>jumpChapter(Number(b.dataset.chapter)),
  close:closePanel,compare:compareStyles,reloadStyles:loadStyles,analyze:analyzeQuality,
  rawStyles:()=>{S.raw=!S.raw;renderPanel();},saveQuality,
  evidence:b=>showEvidence(Number(b.dataset.metric),Number(b.dataset.evidence)),
  download:b=>exportNovel(b.dataset.format),copy:b=>copyValue(b.dataset.copy),
  inspect:b=>{S.inspected=b.dataset.block;openPanel('details');},
  blockMode:()=>{S.blocks=!S.blocks;renderReader();renderPanel();},
  retryImage:b=>loadImage(b.closest('figure')),clearToken:()=>applyToken(''),
  send:()=>sendConsole(),quick:b=>sendConsole(b.dataset.path),
  preference:b=>{const key=b.dataset.key;S[key]=key==='size'?Number(b.dataset.value):b.dataset.value;prefs();renderPanel();},
  language:b=>switchLanguage(b.dataset.lang),
};
['style','quality','details','settings','console','appearance','export','contents'].forEach(panel=>actions[panel]=()=>openPanel(panel));
function switchLanguage(lang) {
  const view=S.view,panel=S.panel;closePanel();S.lang=lang;shell();
  if(S.selected&&view==='reading')renderReader();showView(view);health();
  if(panel)openPanel(panel);
}
document.addEventListener('click',event=>{
  const button=event.target.closest('[data-action]');
  if(button&&!button.disabled){const action=actions[button.dataset.action];if(action)action(button);}
  if(event.target===$('tools-dialog')){
    const r=event.target.getBoundingClientRect();
    if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)closePanel();
  }
});
