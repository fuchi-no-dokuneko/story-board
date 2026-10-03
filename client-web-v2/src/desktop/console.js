function dOpenConsole(){
  dCloseAll();
  const paths=['/actuator/health','/v1/openapi.yaml','/v1/admin/novels','/v1/style/'];
  $('#layer').innerHTML=`<div class="modal" role="dialog" aria-modal="true" aria-labelledby="ct"><div class="box" id="console-view">
    <header><div><h3 id="ct">API console</h3><p>${t('consoleSub')}</p></div><button class="ib" type="button" id="cx" aria-label="${t('close')}">✕</button></header>
    <div class="cline"><span id="method">GET</span><input id="path" value="/actuator/health" spellcheck="false" aria-label="Path"><button class="btn primary" type="button" id="send">${t('send')}</button></div>
    <div class="quick-requests">${paths.map(p=>`<button class="chip" type="button" data-method="GET" data-path="${p}">${p}</button>`).join('')}</div>
    <div class="rstat" id="response-status"></div><pre class="resp" id="response">${t('consoleReady')}</pre></div></div>`;
}
