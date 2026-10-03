function renderApp(capture=true){
  const recent=capture?(captureReading(),remembered().find(r=>r.id===S.book)):null;
  S.scrollCleanup?.();S.scrollCleanup=null;releaseImages();
  document.documentElement.dataset.ui='v2';
  document.documentElement.dataset.layout=S.mobile?'mobile':'desktop';
  document.body.className='sb2';prefs();
  document.title=S.selected?`${S.selected.novel.title} · StoryBlock`:'StoryBlock';
  if(S.mobile){
    document.body.innerHTML='<main class="stage"><div class="phone" id="phone"><div id="app"></div><div id="overlay"></div></div></main>';
    if(!S.selected&&['book','read','ins'].includes(S.stack.at(-1)))renderLoading($('#app'));
    else mRender();
  }else{
    document.body.innerHTML=desktopShell;H.apply();bindDesktop();dRenderLibrary();
    $('#catalog-search').value=S.query;$('#sort').value=S.sort;
    if(S.view!=='library'){
      $('#library').hidden=true;$('#topbar').hidden=true;$('#reader').hidden=false;
      if(S.selected){dRenderReader();if(S.drawer){$('#drawer').classList.add('open');document.body.classList.add('drawer-open');}}else renderLoading($('#reader-content'));
    }
  }
  designStyles();updateHealth();if(capture&&recent)restoreReading(recent);
}
function renderLoading(target){
  target.innerHTML=`<div class="${S.mobile?'screen':'page'}"><div class="${S.mobile?'scroll':''}" role="status">
    <p>${esc(S.readError||t('loading'))}</p>${S.readError?`<button class="btn sm" data-retry-book="${esc(S.retryId)}">${t('retry')}</button>`:''}
    <button class="btn sm" data-library>${t('library')}</button></div></div>`;
}
function bindDesktop(){
  $('#home').onclick=library;$('#back').onclick=library;
  $('#tocBtn').onclick=()=>{const open=!$('#toc').classList.contains('open');dCloseAll('toc');$('#toc').classList.toggle('open',open);if(open)dScrim(()=>dCloseAll());};
  $('#tocClose').onclick=()=>dCloseAll();
  $('#aaBtn').onclick=e=>{e.stopPropagation();dOpenAa($('#aaBtn'));designStyles();};
  $('#exportBtn').onclick=e=>{e.stopPropagation();dOpenExport($('#exportBtn'));designStyles();};
  $('#connBtn').onclick=e=>{e.stopPropagation();dOpenConn($('#connBtn'));designStyles();};
  $('#insightsBtn').onclick=()=>dOpenDrawer();$('#drawerClose').onclick=()=>dCloseAll();
}
