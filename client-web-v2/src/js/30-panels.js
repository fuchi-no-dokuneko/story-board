function closePanel() {
  const dialog=$('tools-dialog');
  if(dialog?.open)dialog.close();S.panel='';
  S.consoleRequest=(S.consoleRequest||0)+1;
}
function openPanel(panel) {
  if (['style','quality','details','export','contents'].includes(panel) && !S.selected) {
    toast(t('chooseNovel'));return;
  }
  S.panel=panel;renderPanel();
  if(!$('tools-dialog').open)$('tools-dialog').showModal();
  if(panel==='style'&&!S.styles.length)loadStyles();
}
function renderPanel() {
  if(!S.panel)return;
  const panel=S.panel, insights=['style','quality','details'].includes(panel);
  const dialog=$('tools-dialog'), active=document.activeElement;
  const focus=dialog.contains(active)?{id:active.id,data:JSON.stringify(active.dataset)}:null;
  const scroll=dialog.dataset.panel===panel?dialog.scrollTop:0;
  const content={style:stylePanel,quality:qualityPanel,details:detailPanel,
    export:exportPanel,appearance:appearancePanel,settings:settingsPanel,
    console:consolePanel,contents:()=>`<nav class="sb-contents">${contents()}</nav>`}[panel];
  $('tools-dialog').innerHTML=`<header class="sb-panel-head"><h2 id="panel-title">${t(insights?'insights':panel)}</h2>
    ${btn('close','×',`aria-label="${t('close')}"`)}</header>
    ${insights?`<nav class="sb-panel-tabs" aria-label="${t('insights')}">${['style','quality','details']
      .map(k=>btn(k,t(k),`aria-pressed="${panel===k}"`)).join('')}</nav>`:''}
    <div class="sb-panel-body">${content?content():''}</div>`;
  dialog.dataset.panel=panel;
  if(focus)[...dialog.querySelectorAll('button,input,select')].find(el=>
    focus.id?el.id===focus.id:JSON.stringify(el.dataset)===focus.data)?.focus({preventScroll:true});
  dialog.scrollTop=scroll;
}
function appearancePanel() {
  const choices=(key,items)=>`<div class="sb-setting"><span>${t(key)}</span><div class="sb-segment">${items.map(value=>
    btn('preference',typeof value==='number'?value:t(value),`data-key="${key}" data-value="${value}" aria-pressed="${S[key]===value}"`)).join('')}</div></div>`;
  return `<p class="sb-muted">${t('appearanceHint')}</p>`+choices('size',[18,20,22,24,28])+
    choices('spacing',['normal','loose'])+choices('font',['serif','sans'])+choices('theme',['paper','white','night'])+
    `<div class="sb-setting"><label for="block-mode">${t('blockDetails')}</label><input id="block-mode" type="checkbox" ${S.blocks?'checked':''}></div>`;
}
