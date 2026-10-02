function settingsPanel() {
  return `<p>${t('settingsHint')}</p>${btn('appearance',t('appearance'))}
    <section class="sb-settings-section"><h3>${t('connection')}</h3><p>${t('connectionHint')}</p>
    <details><summary>${t('optionalCredentials')}</summary><form id="operator-form" autocomplete="off">
    <label for="operator-token">${t('ownerToken')}</label><input id="operator-token" type="password" autocomplete="off" spellcheck="false">
    <div class="sb-actions"><button type="submit" id="operator-connect">${t('useToken')}</button>${btn('clearToken',t('clear'),`id="operator-clear" ${S.token?'':'disabled'}`)}</div>
    <p id="operator-status" role="status">${t(S.token?'tokenActive':'automaticAccess')}</p></form></details></section>
    <section class="sb-settings-section"><h3>${t('advanced')}</h3>${btn('console',t('console'),'id="console-tab"')}
    <p><a href="/">${t('originalUI')} ↗</a></p></section>`;
}
async function applyToken(value) {
  try{window.StoryBlockAuth.authorizationHeaders(value);}catch(error){report(error,'operator-status');return;}
  S.token=value;S.auth++;S.catalogRequest++;S.novelRequest++;S.styleRequest++;S.qualityRequest++;
  S.catalogAbort?.abort();S.novelAbort?.abort();releaseImages();
  S.selected=null;S.catalog=[];S.styles=[];S.comparison=null;S.quality=null;
  S.styleBusy=S.qualityBusy=S.stylesLoading=false;S.styleError=S.qualityError='';
  showView('library');history.replaceState(null,'','?ui=v2');closePanel();
  await loadCatalog();health();
}
function consolePanel() {
  return `<p>${t('consoleHint')}</p><div class="quick-requests sb-actions">
    ${[['health','/actuator/health'],['openapi','/v1/openapi.yaml'],['novels','/v1/admin/novels']].map(([key,path])=>
      btn('quick',t(key),`data-path="${path}" data-method="GET"`)).join('')}</div>
    <label for="path">${t('requestPath')}</label><div class="sb-console-request"><select id="method" aria-label="HTTP method"><option>GET</option></select>
    <input id="path" value="/actuator/health" spellcheck="false">${btn('send',t('send'),'id="send"')}</div>
    <p id="response-status" role="status"></p><pre id="response">${t('ready')}</pre>`;
}
async function sendConsole(path) {
  const target=path||$('path').value,auth=S.auth,ticket=S.consoleRequest=(S.consoleRequest||0)+1;
  if(path)$('path').value=path;
  $('response').textContent=t('loading');
  try{
    const response=await request(target,{headers:{Accept:'*/*'}}),body=await response.text();
    if(S.panel!=='console'||auth!==S.auth||ticket!==S.consoleRequest)return;
    $('response-status').textContent=`${response.status} ${response.statusText}`;
    try{$('response').textContent=JSON.stringify(JSON.parse(body),null,2);}catch{$('response').textContent=body;}
  }catch(error){if(S.panel==='console'&&auth===S.auth&&ticket===S.consoleRequest){$('response-status').textContent=String(error.status||'');$('response').textContent=error.message;}}
}
