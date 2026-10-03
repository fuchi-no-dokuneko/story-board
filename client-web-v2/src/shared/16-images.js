function releaseImages() {
  S.imageViews.forEach(view=>{view.abort.abort();if(view.url)URL.revokeObjectURL(view.url);});
  S.imageViews.clear();
}
function loadImages() {
  S.imagePromise=Promise.allSettled([...document.querySelectorAll('figure[data-block-id]')].filter(e=>!e.closest('[hidden]')).map(figure=>loadImage(figure)));
  return S.imagePromise;
}
async function loadImage(figure) {
  const descriptor = findBlock(figure.dataset.blockId)?.block.extensions?.['storyblock.image'];
  if (!descriptor) return;
  const image=figure.querySelector('img');
  image.width=descriptor.width_px;image.height=descriptor.height_px;image.alt=descriptor.alt_text||'';
  const status=figure.querySelector('[role=status]')||document.createElement('p');
  const retry=figure.querySelector('button')||document.createElement('button');
  status.setAttribute('role','status');retry.type='button';retry.className='btn sm';
  retry.dataset.action='retryImage';retry.textContent=t('retry');figure.append(status,retry);
  if (figure.view) {figure.view.abort.abort();if(figure.view.url)URL.revokeObjectURL(figure.view.url);S.imageViews.delete(figure.view);}
  const view=figure.view={abort:new AbortController(),url:null};S.imageViews.add(view);
  status.textContent=t('loadingImage');status.hidden=false;retry.hidden=true;image.hidden=true;
  try {
    if (!/^art_[0-9a-f-]+$/.test(descriptor.artifact_id) || !['image/png','image/jpeg'].includes(descriptor.media_type))
      throw new Error(t('imageUnavailable'));
    const response=await request(`/v1/artifacts/${descriptor.artifact_id}`,{
      signal:view.abort.signal,headers:{Accept:descriptor.media_type},redirect:'error'});
    const blob=await response.blob();if(view.abort.signal.aborted)return;
    if(blob.type.split(';')[0]!==descriptor.media_type)throw new Error(t('imageUnavailable'));
    image.src=view.url=URL.createObjectURL(blob);await image.decode();
    if(view.abort.signal.aborted)return;
    image.hidden=false;status.hidden=true;
  } catch(error) {
    if(view.abort.signal.aborted)return;
    if(view.url)URL.revokeObjectURL(view.url);view.url=null;
    status.textContent=`${t('imageUnavailable')}: ${error.message}`;retry.hidden=false;
  }
}
