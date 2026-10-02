function renderLibrary() {
  if (!$('library-view')) return;
  const matching = catalogMatches(), pages = Math.ceil(matching.length/12);
  S.page = Math.min(S.page,Math.max(0,pages-1));
  const recent = remembered().find(r=>S.catalog.some(n=>n.novel_id===r.id));
  const resume = recent && S.catalog.find(n=>n.novel_id===recent.id);
  $('library-view').innerHTML = `<div class="sb-library">
    ${resume?`<div class="sb-resume">${cover(resume,true)}<div><p class="sb-eyebrow">${t('continue')}</p><h2>${esc(resume.title)}</h2>
      <p>${t('chapter')} ${num((recent.chapter||0)+1)}</p></div>${btn('open',t('continue')+' →',`class="sb-accent" data-id="${esc(resume.novel_id)}"`)}</div>`:
    `<div class="sb-welcome"><p class="sb-eyebrow">${t('readOnly')}</p><h1>${t('welcome')}</h1><p>${t('welcomeHint')}</p></div>`}
    <div class="sb-library-heading"><div><h2>${t('library')}</h2><p id="catalog-total">${num(matching.length)} ${t('novels')}</p></div>
      <div class="sb-filters">${[['all',t('all')],['zh-Hant','繁中'],['zh-Hans','简中'],['en','English'],['imported',t('imported')]]
        .map(([v,label])=>btn('filter',label,`data-filter="${v}" aria-pressed="${S.filter===v}"`)).join('')}</div>
      <label class="sr-only" for="catalog-sort">${t('sort')}</label><select id="catalog-sort">${[['updated','recentlyUpdated'],['title','byTitle'],['length','byLength']]
        .map(([v,k])=>`<option value="${v}" ${S.sort===v?'selected':''}>${t(k)}</option>`).join('')}</select>
      ${btn('refresh','↻',`id="catalog-refresh" aria-label="${t('refresh')}"`)}</div>
    <p id="catalog-loading" role="status" ${S.loading?'':'hidden'}>${t('loading')}</p>
    <p id="catalog-empty" ${!S.loading&&!matching.length?'':'hidden'}>${t('noNovels')}</p>
    <ol class="sb-books" id="novel-list">${matching.slice(S.page*12,S.page*12+12).map(n=>`<li>${btn('open',cover(n)+
      `<h3>${esc(n.title)}</h3><p>${num(n.chapter_count)} ${t('chapters')} · ${num(n.block_count)} ${t('blocks')}</p><small>${esc(n.novel_id)}</small>`,
      `class="novel-item sb-book" data-id="${esc(n.novel_id)}" data-novel-id="${esc(n.novel_id)}"`)}</li>`).join('')}</ol>
    <nav class="sb-pagination" aria-label="${t('pages')}">${btn('previous',t('previous'),`id="page-previous" ${S.page===0?'disabled':''}`)}
      <span id="page-label">${num(pages?S.page+1:0)} / ${num(pages)}</span>${btn('next',t('next'),`id="page-next" ${S.page+1>=pages?'disabled':''}`)}</nav></div>`;
}
