function shell() {
  document.body.className = 'sb2'; prefs();
  document.body.innerHTML = `
    <a class="sb-skip" href="#main-content">${t('skip')}</a>
    <header class="sb-top">
      ${btn('library', '<span class="sb-seal" aria-hidden="true">書</span><span><b>StoryBlock</b><small>'+t('library')+'</small></span>', 'class="sb-brand" id="library-tab"')}
      <div class="sb-search" role="search"><label class="sr-only" for="catalog-search">${t('search')}</label>
        <input id="catalog-search" type="search" placeholder="${t('search')}" autocomplete="off"><kbd>/</kbd></div>
      <div class="sb-top-actions"><div class="sb-language" aria-label="${t('language')}">
        ${['en','zh-Hant','zh-Hans'].map((lang,i)=>btn('language',['EN','繁','简'][i],`data-lang="${lang}" aria-pressed="${S.lang===lang}"`)).join('')}</div>
        <span class="sb-status" id="service-pill"><i aria-hidden="true"></i><span id="status-text">${t('checking')}</span></span>
        ${btn('settings','⚙',`aria-label="${t('settings')}" title="${t('settings')}"`)}</div>
    </header>
    <main id="main-content" tabindex="-1">
      <p id="app-message" class="sb-message" role="status" hidden></p>
      <button id="connection-hint" data-action="settings" hidden></button>
      <section id="library-view" aria-label="${t('library')}"></section>
      <section id="reader-view" hidden aria-label="${t('reading')}"></section>
    </main>
    <nav class="sb-bottom" aria-label="${t('navigation')}">
      ${btn('library',icon('▤',t('library')))}${btn('reading',icon('▥',t('reading')))}
      ${btn('style',icon('▥',t('insights')))}${btn('settings',icon('⚙',t('settings')))}
    </nav>
    <dialog id="tools-dialog" class="sb-dialog" aria-labelledby="panel-title"></dialog>
    <output id="toast" class="sb-toast" role="status" hidden></output>`;
  $('catalog-search').value = S.query;
  renderLibrary();
}
function toast(message) {
  $('toast').textContent = message; $('toast').hidden = false;
  clearTimeout(toast.timer); toast.timer = setTimeout(()=>{$('toast').hidden=true;},4500);
}
function showView(view) {
  S.view = view;
  $('library-view').hidden = view !== 'library';
  $('reader-view').hidden = view !== 'reading';
  document.body.dataset.view = view;
  document.querySelectorAll('.sb-bottom button').forEach(b=>b.setAttribute('aria-current',b.dataset.action===view?'page':'false'));
}
