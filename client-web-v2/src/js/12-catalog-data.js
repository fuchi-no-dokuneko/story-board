async function loadCatalog() {
  const ticket = ++S.catalogRequest;
  S.catalogAbort?.abort(); S.catalogAbort = new AbortController();
  S.loading = true; renderLibrary(); $('app-message').hidden = true;
  try {
    const items = [], signal = S.catalogAbort.signal;
    let page = 0, total = 1;
    do {
      const result = await json(`/v1/admin/novels?page=${page}&size=100`, {signal});
      if (ticket !== S.catalogRequest) return;
      items.push(...result.items); total = result.total_pages; page++;
    } while (page < total);
    S.catalog = [...new Map(items.map(item=>[item.novel_id,item])).values()];
    $('connection-hint').hidden = true;
  } catch (error) { if (ticket === S.catalogRequest) report(error); }
  finally { if (ticket === S.catalogRequest) { S.loading = false; renderLibrary(); } }
}
function catalogMatches() {
  const q = S.query.trim().toLocaleLowerCase();
  return S.catalog.filter(n=>
    (S.filter==='all' || (S.filter==='imported' ? !n.agent_write_registered : n.language===S.filter)) &&
    [n.title,n.novel_id,...(n.main_characters||[])].some(v=>String(v||'').toLocaleLowerCase().includes(q)))
    .sort((a,b)=>S.sort==='title'?a.title.localeCompare(b.title,S.lang):
      S.sort==='length'?b.block_count-a.block_count:String(b.updated_at).localeCompare(a.updated_at));
}
function cover(novel, small = false) {
  const shade = [...novel.novel_id].reduce((a,c)=>a+c.charCodeAt(0),0)%6;
  return `<span class="sb-cover shade-${shade} ${small?'small':''}" aria-hidden="true">
    <span class="sb-cover-title" ${/^(zh|yue)/.test(novel.language)?'data-vertical="true"':''}>${esc(novel.title)}</span>
    <span class="sb-cover-language">${esc(novel.language)}</span></span>`;
}
function remembered() {
  const recent = readLocal('recent', []);
  return Array.isArray(recent) ? recent.filter(x=>x&&typeof x.id==='string').slice(0,50) : [];
}
