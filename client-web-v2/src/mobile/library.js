function refreshMobileLibrary(){
  if(!$('#novel-list'))return mRender();
  const template=document.createElement('template');template.innerHTML=mLibScreen();
  for(const selector of ['#novel-list','#catalog-total','#catalog-loading','.segs']){
    const current=$(selector),next=template.content.querySelector(selector);
    if(current&&next)current.replaceChildren(...next.childNodes);
  }
  const current=$('.cont'),next=template.content.querySelector('.cont');
  if(current){if(next)current.replaceWith(next);else current.remove();}
  else if(next)$('.segs')?.before(next);
  designStyles();
}
