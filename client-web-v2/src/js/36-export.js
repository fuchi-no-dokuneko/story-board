const exporting=new Set();
function exportPanel() {
  return `<p>${t('exportHint')}</p><div class="sb-export-options">
    ${['pdf','txt'].map(format=>btn('download',`<b>${format.toUpperCase()}</b><span>${t(format+'Hint')}</span>`,
      `id="download-${format}" data-format="${format}" ${exporting.has(format)?'disabled':''}`)).join('')}</div>
    <p id="reader-action-status" role="status">${exporting.size?t('preparing'):''}</p>`;
}
async function exportNovel(format) {
  const selected=S.selected;if(!selected||exporting.has(format))return;
  exporting.add(format);if(S.panel==='export')renderPanel();
  try {
    const revision=selected.revision;let blob;
    if(format==='txt') {
      const text=revision.chapters.map(chapter=>[chapter.title||'',
        ...chapter.scenes.map(scene=>scene.blocks.map(block=>block.text).join('\n'))].join('\n\n')).join('\n\n');
      blob=new Blob([text],{type:'text/plain;charset=utf-8'});
    }else{
      const options=revisionPost(revision,{});options.headers.Accept='application/pdf';
      const response=await request(`/v1/novels/${revision.novel_id}/pdf-renders`,options);
      blob=await response.blob();
    }
    downloadBlob(blob,`${selected.novel.title||revision.novel_id}.${format}`);
    toast(`${t('downloadReady')} · ${selected.novel.title}`);
  }catch(error){toast(error.message);}
  finally{exporting.delete(format);if(S.panel==='export')renderPanel();}
}
function saveQuality() {
  if(!S.quality)return;
  downloadBlob(new Blob([JSON.stringify(S.quality,null,2)],{type:'application/json'}),`${S.quality.novel_id}-quality.json`);
}
