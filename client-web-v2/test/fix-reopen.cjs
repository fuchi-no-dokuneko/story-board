module.exports=async c=>{
  const {page,base,id,click,check}=c;let requests=0;
  const record=r=>{if(r.method()==='GET'&&new URL(r.url()).pathname==='/v1/admin/novels/'+id)requests++;};
  await page.setViewport({width:1440,height:900});await page.goto(base+'/?ui=v2');
  await page.waitForSelector('.novel-item');page.on('request',record);
  try{
    await page.type('#catalog-search',c.selected.novel.title);
    await click(`[data-open="${id}"]`);await page.waitForSelector('.story-block');
    await click('#back');await click(`#hero [data-open="${id}"]`);await page.waitForSelector('.story-block');
    check('Extra F6 re-open downloads novel once',requests===1,requests);
  }finally{page.off('request',record);}
};
