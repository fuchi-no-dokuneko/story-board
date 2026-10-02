async function openNovel(id, options = {}) {
  const ticket = ++S.novelRequest;
  S.novelAbort?.abort(); S.novelAbort = new AbortController();
  S.styleRequest++; S.qualityRequest++; releaseImages();
  closePanel(); showView('reading');
  $('reader-view').innerHTML = `<div id="reader-loading" class="sb-message" role="status">${t('loading')}</div>`;
  $('app-message').hidden = true; S.selected = null;
  try {
    let payload = await json(`/v1/admin/novels/${encodeURIComponent(id)}`,{signal:S.novelAbort.signal});
    if (payload.novel.head_revision_id !== payload.revision.revision_id)
      payload = await json(`/v1/admin/novels/${encodeURIComponent(id)}`,{signal:S.novelAbort.signal});
    if (ticket !== S.novelRequest) return;
    if (payload.novel.head_revision_id !== payload.revision.revision_id) throw new Error(t('revisionChanged'));
    S.selected = payload; S.comparison = null; S.quality = null; S.inspected = null;
    S.styleBusy=S.qualityBusy=S.stylesLoading=false;S.styleError=S.qualityError='';
    S.names = (payload.novel.main_characters||[]).join(', '); S.qualityWindow = 'all';
    const rememberedChapter = remembered().find(r=>r.id===id)?.chapter || 0;
    S.chapter = Math.max(0,Math.min(options.chapter ?? rememberedChapter,payload.revision.chapters.length-1));
    renderReader(); remember();
    if (!options.fromHistory) history.pushState(null,'',`?ui=v2#${encodeURIComponent(id)}`);
    if (options.focus !== false) $('reader-title')?.focus();
  } catch (error) {
    if (ticket !== S.novelRequest || error.name==='AbortError') return;
    $('reader-view').innerHTML = `<div class="sb-message"><h1>${t('unableRead')}</h1><p>${esc(error.message)}</p>
      ${btn('open',t('retry'),`data-id="${esc(id)}"`)} ${btn('library',t('library'))}</div>`;
    report(error);
  }
}
function remember() {
  if (!S.selected) return;
  const id = S.selected.novel.novel_id;
  saveLocal('recent',[{id,chapter:S.chapter},...remembered().filter(r=>r.id!==id)].slice(0,50));
}
function library() {
  S.novelRequest++; S.novelAbort?.abort(); closePanel(); releaseImages();
  showView('library'); renderLibrary();document.title='StoryBlock';
  history.pushState(null,'','?ui=v2'); window.scrollTo(0,0);
}
function jumpChapter(index, focus = true) {
  if (!S.selected) return;
  S.chapter = Math.max(0,Math.min(index,S.selected.revision.chapters.length-1));
  closePanel(); renderReader(); remember(); window.scrollTo(0,0);
  if (focus) $('chapter-heading')?.focus();
}
