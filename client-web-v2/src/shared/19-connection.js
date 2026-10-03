function updateHealth(){
  const target=$('#status-text');if(!target)return;
  const value=t(S.online==null?'checking':S.online?'online':'offline');
  if(target.tagName==='BUTTON'){
    [...target.childNodes].filter(n=>n.nodeType===3).forEach(n=>n.remove());target.append(value);
  }else target.textContent=value;
  target.dataset.state=S.online?'up':'down';
  if($('#status-dot'))$('#status-dot').dataset.state=target.dataset.state;
}
async function applyToken(value){
  try{window.StoryBlockAuth.authorizationHeaders(value);}catch(error){toast(error.message);return;}
  S.token=value;S.auth++;S.catalogRequest++;S.novelRequest++;S.styleRequest++;S.qualityRequest++;
  S.catalogAbort?.abort();S.novelAbort?.abort();releaseImages();
  S.selected=null;S.catalog=[];S.details.clear();S.styles=[];S.picks.clear();S.comparison=S.quality=null;
  S.styleBusy=S.qualityBusy=S.stylesLoading=false;S.styleError=S.qualityError='';
  S.view='library';S.stack=['lib'];S.mobileTab='lib';history.replaceState(null,'','?ui=v2');
  renderApp(false);loadCatalog();health();
}
async function sendConsole(path){
  const target=path||$('#path').value,auth=S.auth,ticket=S.consoleRequest=(S.consoleRequest||0)+1;
  if(path)$('#path').value=path;
  const responseNode=$('#response'),status=$('#response-status'),start=performance.now();
  responseNode.textContent=t('loading');status.textContent='';
  try{
    const response=await request(target,{headers:{Accept:'*/*'}}),body=await response.text();
    if(!responseNode.isConnected||auth!==S.auth||ticket!==S.consoleRequest)return;
    status.textContent=`${response.status} ${response.statusText} · ${Math.round(performance.now()-start)} ms`;
    try{responseNode.textContent=JSON.stringify(JSON.parse(body),null,2);}catch{responseNode.textContent=body;}
  }catch(error){if(responseNode.isConnected&&auth===S.auth&&ticket===S.consoleRequest){status.textContent=String(error.status||'');responseNode.textContent=error.message;}}
}
