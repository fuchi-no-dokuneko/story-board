function designStyles(root=document){
  const nodes=[...(root.matches?.('[data-css]')?[root]:[]),...root.querySelectorAll('[data-css]')];
  for(const node of nodes){node.style.cssText=node.dataset.css;node.removeAttribute('data-css');}
}
function toast(message,busy=false){
  $('.toast')?.remove();
  const el=document.createElement('div');el.className='toast';el.setAttribute('role','status');
  if(busy){const spin=document.createElement('span');spin.className='spin';el.append(spin);}
  el.append(document.createTextNode(message));(S.mobile?$('#phone'):document.body).append(el);
  if(!busy)setTimeout(()=>el.remove(),4500);
}
const dToast=toast,mToast=toast;
function closeLayers(){
  S.consoleRequest=(S.consoleRequest||0)+1;
  if(S.mobile)mCloseSheet();else dCloseAll();
}
function renderPanel(){
  if(S.mobile){if(S.stack.at(-1)==='ins')mRender();}
  else if(S.drawer)dRenderDrawer();
}
function resumeBook(){
  const recent=remembered().find(r=>S.catalog.some(n=>n.novel_id===r.id));
  return H.novel(recent?.id)||SB.novels[0]||null;
}
function decorateAnalysis(){
  for(const [id,busy,key] of [['compare-styles',S.styleBusy||S.stylesLoading,'calculating'],['analyze-quality',S.qualityBusy,'analyzing']]){
    const button=$('#'+id);if(button&&busy){button.disabled=true;button.textContent=t(key);}
  }
  $$('[data-style],[data-pick]').forEach(e=>{if(S.styleBusy||S.stylesLoading)e.disabled=true;});
  const host=S.mobile?$('.screen .scroll'):$('#dbody');if(!host)return;
  for(const [id,value] of [['style-status',S.styleError],['quality-status',S.qualityError]]){
    let node=$('#'+id);if(!node){node=document.createElement('p');node.id=id;node.setAttribute('role','status');host.append(node);}
    node.textContent=value||'';node.hidden=!value;
    if(value&&id==='style-status'&&S.mobile){const retry=document.createElement('button');
      retry.className='btn sm';retry.textContent=t('retry');retry.id='reload-styles';node.append(retry);}
  }
}
function qualityNoticeText(){
  const c=S.quality?.quality_report.calibration;
  if(!c)return t('qExplain');
  return !c.humanSources&&!c.aiSources?t('noCorpus'):
    `${t('humanSources')}: ${num(c.humanSources)} · AI: ${num(c.aiSources)}`;
}
function qualityNotice(){return `<div class="note">${esc(qualityNoticeText())}</div>`;}
