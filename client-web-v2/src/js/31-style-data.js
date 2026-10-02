async function loadStyles() {
  const ticket=++S.styleRequest;S.stylesLoading=true;S.styleError='';S.comparison=null;
  if(S.panel==='style')renderPanel();
  try {
    const result=await json('/v1/style/');
    if(ticket!==S.styleRequest)return;
    S.styles=result.styles||[];S.picks=new Set(S.styles.filter(s=>s.status==='ready').map(s=>s.id));
    S.styleError=result.error||'';
  }catch(error){if(ticket===S.styleRequest)S.styleError=error.message;}
  finally{if(ticket===S.styleRequest){S.stylesLoading=false;if(S.panel==='style')renderPanel();}}
}
async function compareStyles() {
  const selected=S.selected, ids=[...S.picks];if(!selected||!ids.length||S.styleBusy)return;
  const ticket=++S.styleRequest;S.styleBusy=true;S.styleError='';S.comparison=null;renderPanel();
  try {
    const r=selected.revision;
    const result=await json(`/v1/novels/${r.novel_id}/style-comparisons`,revisionPost(r,{style_ids:ids}));
    if(ticket!==S.styleRequest||selected!==S.selected)return;
    S.comparison=result;
  }catch(error){if(ticket===S.styleRequest)S.styleError=error.message;}
  finally{if(ticket===S.styleRequest){S.styleBusy=false;if(S.panel==='style')renderPanel();}}
}
function stylePanel() {
  return `<p class="sb-muted">${t('styleHint')} <a href="/style-math.html" target="_blank" rel="noopener">${t('formulas')} ↗</a></p>
    <div id="style-choices">${S.styles.map(style=>`<label class="sb-style-choice"><input type="checkbox" data-style="${esc(style.id)}"
      ${S.picks.has(style.id)?'checked':''} ${style.status!=='ready'||S.styleBusy?'disabled':''}>
      <span><b>${esc(style.name)}</b><small>${esc(style.description||'')}</small>
      <small>${esc(style.error||t(style.status))}</small></span></label>`).join('')}</div>
    ${!S.styles.length?`<p>${t(S.stylesLoading?'loading':'noStyles')}</p>`:''}
    <div class="sb-actions">${btn('compare',t(S.styleBusy?'calculating':'compare'),`id="compare-styles" class="sb-accent" ${!S.picks.size||S.styleBusy||S.stylesLoading?'disabled':''}`)}
    ${btn('reloadStyles',t('refresh'),`id="reload-styles" ${S.styleBusy||S.stylesLoading?'disabled':''}`)}</div>
    <p id="style-status" role="status">${esc(S.styleError||'')}</p>
    ${S.comparison?styleResults():''}`;
}
