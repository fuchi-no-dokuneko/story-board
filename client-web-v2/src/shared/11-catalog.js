async function loadCatalog(){
  const ticket=++S.catalogRequest;
  S.catalogAbort?.abort();const abort=S.catalogAbort=new AbortController();
  S.loading=true;S.catalogError='';refreshLibrary();
  const timer=setTimeout(()=>abort.abort(),45000);
  try{
    const items=[];let page=0,total=1;
    do{
      const result=await json(`/v1/admin/novels?page=${page}&size=100`,{signal:abort.signal});
      if(ticket!==S.catalogRequest)return;
      items.push(...result.items);total=result.total_pages;page++;
      S.catalog=[...new Map(items.map(item=>[item.novel_id,item])).values()];
      refreshLibrary();
    }while(page<total);
  }catch(error){
    if(ticket===S.catalogRequest)S.catalogError=error.name==='AbortError'?t('catalogTimeout'):error.message;
  }finally{
    clearTimeout(timer);
    if(ticket===S.catalogRequest){S.loading=false;refreshLibrary();}
  }
}
function refreshLibrary(){
  if(S.mobile){
    if(S.stack.at(-1)==='lib'){
      const input=$('#catalog-search'),focused=input===document.activeElement;
      const scroll=$('.screen .scroll')?.scrollTop||0;
      mRender();if($('.screen .scroll'))$('.screen .scroll').scrollTop=scroll;
      if(focused)$('#catalog-search')?.focus({preventScroll:true});
    }
  }else if($('#library')){dRenderLibrary();H.apply();designStyles();updateHealth();}
  const host=$('#catalog-loading');
  if(host&&S.catalogError){
    const retry=document.createElement('button');retry.type='button';retry.className='btn sm';
    retry.textContent=t('retry');retry.dataset.reloadCatalog='true';host.append(' ',retry);
  }
}
