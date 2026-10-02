function renderReader() {
  if (!S.selected) return;
  releaseImages();
  const {novel:n,revision:r} = S.selected, chapter = r.chapters[S.chapter];
  document.title = `${n.title} · StoryBlock`;
  $('reader-view').innerHTML = `<div class="sb-reader-bar">
    ${btn('library','‹ '+t('library'))}<div class="sb-reader-caption"><b>${esc(n.title)}</b><small>${num(chapter?S.chapter+1:0)} / ${num(r.chapters.length)}</small></div>
    <div class="sb-reader-actions">${btn('contents','☷',`aria-label="${t('contents')}"`)}${btn('appearance','Aa',`aria-label="${t('appearance')}"`)}
    ${btn('export',icon('↓',t('export')))}${btn('style',icon('▥',t('insights')))}</div></div>
    <div class="sb-reader-layout"><aside class="sb-outline" aria-label="${t('contents')}"><h2>${t('contents')}</h2><nav id="chapter-nav">${contents()}</nav>
      <dl class="sb-mini-stats"><dt>${t('chapters')}</dt><dd>${num(n.chapter_count)}</dd><dt>${t('scenes')}</dt><dd>${num(n.scene_count)}</dd>
      <dt>${t('blocks')}</dt><dd>${num(n.block_count)}</dd></dl>${btn('details',t('revision'))}</aside>
      <article id="reader-content" class="sb-prose" lang="${esc(n.language)}">
        <header class="sb-book-heading"><p class="sb-eyebrow">${esc(n.language)} · ${t('readOnly')}</p><h1 id="reader-title" tabindex="-1">${esc(n.title)}</h1>
        <ul class="sb-cast" id="character-list">${(n.main_characters||[]).map(c=>`<li>${esc(c)}</li>`).join('')}</ul></header>
        <div id="chapter-content">${chapter?chapterContent(chapter):`<p>${t('emptyRevision')}</p>`}</div>
        <nav class="sb-chapter-end" aria-label="${t('chapters')}">${btn('chapter',t('previousChapter'),`data-chapter="${S.chapter-1}" ${S.chapter===0?'disabled':''}`)}
        <span>${num(chapter?S.chapter+1:0)} / ${num(r.chapters.length)}</span>${btn('chapter',t('nextChapter'),`data-chapter="${S.chapter+1}" ${S.chapter+1>=r.chapters.length?'disabled':''}`)}</nav>
      </article></div>`;
  document.body.dataset.blocks = String(S.blocks);
  loadImages(); showView('reading');
}
function contents() {
  return S.selected.revision.chapters.map((c,i)=>btn('chapter',esc(c.title||`${t('chapter')} ${i+1}`),
    `class="chapter-link" data-chapter="${i}" aria-current="${i===S.chapter?'true':'false'}"`)).join('');
}
