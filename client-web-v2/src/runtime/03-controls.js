function clickId(id,data){
  if(data.path)return sendConsole(data.path);
  if(id==='compare-styles')return compareStyles();
  if(id==='analyze-quality')return analyzeQuality();
  if(id==='reload-styles')return loadStyles();
  if(id==='rawToggle'){S.raw=!S.raw;return renderPanel();}
  if(id==='saveq')return saveQuality();
  if(id==='operator-clear')return applyToken('');
  if(id==='cx')return closeLayers();
  if(id==='send')return sendConsole();
  if(id==='addname'){
    const value=prompt(t('addName'));if(value?.trim()){S.names.push(value.trim());renderPanel();}
  }
}
document.addEventListener('click',e=>{
  try{handleClick(e);}finally{designStyles();}
  if(e.target.matches('.modal,.scrim'))closeLayers();
});
document.addEventListener('input',e=>{
  if(e.target.id==='catalog-search'){S.query=e.target.value;S.page=0;refreshLibrary();}
  if(e.target.matches('.slider input'))seekReading(Number(e.target.value));
});
document.addEventListener('change',e=>{
  const target=e.target;
  if(target.id==='sort'){S.sort=target.value;S.page=0;refreshLibrary();}
  if(target.dataset.style&&!S.styleBusy){target.checked?S.picks.add(target.dataset.style):S.picks.delete(target.dataset.style);
    S.comparison=null;renderPanel();}
});
document.addEventListener('submit',e=>{
  if(e.target.id!=='operator-form')return;e.preventDefault();
  const value=$('#operator-token').value.trim();if(value)applyToken(value);else toast(t('enterToken'));
});
document.addEventListener('keydown',e=>{
  if(e.key==='Escape'){closeLayers();return;}
  if(e.target.id==='quality-names'&&['Enter',',','，'].includes(e.key)){
    e.preventDefault();addPendingName();renderPanel();$('#quality-names')?.focus();
  }
  if(e.key==='/'&&!['INPUT','SELECT','TEXTAREA'].includes(e.target.tagName)&&S.view==='library'){
    e.preventDefault();$('#catalog-search')?.focus();
  }
  if(e.key==='Tab'){
    const layer=$('.sheet,.modal');if(!layer)return;
    const focusable=$$('button:not(:disabled),input,a[href],select',layer);
    const first=focusable[0],last=focusable.at(-1);
    if(e.shiftKey&&document.activeElement===first){e.preventDefault();last?.focus();}
    else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first?.focus();}
  }
});
