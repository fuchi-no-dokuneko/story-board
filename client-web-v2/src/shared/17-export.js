const exporting=new Set();
async function exportNovel(format){
  if(format==='json'){saveQuality();return;}
  const selected=S.selected;if(!selected||exporting.has(format))return;
  exporting.add(format);closeLayers();toast(t('preparing'),true);
  try{
    const r=selected.revision;let blob;
    if(format==='txt'){
      const text=r.chapters.map(c=>[c.title||'',...c.scenes.map(s=>s.blocks.map(b=>b.text).join('\n'))].join('\n\n')).join('\n\n');
      blob=new Blob([text],{type:'text/plain;charset=utf-8'});
    }else{
      const options=revisionPost(r,{});options.headers.Accept='application/pdf';
      blob=await(await request(`/v1/novels/${r.novel_id}/pdf-renders`,options)).blob();
    }
    downloadBlob(blob,`${selected.novel.title||r.novel_id}.${format}`);
    toast(`${t('ready')} · ${selected.novel.title}`);
  }catch(error){toast(error.message);}
  finally{exporting.delete(format);}
}
function saveQuality(){
  if(!S.quality)return;
  downloadBlob(new Blob([JSON.stringify(S.quality,null,2)],{type:'application/json'}),`${S.quality.novel_id}-quality.json`);
  closeLayers();toast(t('ready'));
}
