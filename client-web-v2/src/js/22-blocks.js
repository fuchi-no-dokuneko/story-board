function chapterContent(chapter) {
  return `<section class="chapter-section"><h2 id="chapter-heading" tabindex="-1">${esc(chapter.title||t('untitled'))}</h2>
    ${chapter.scenes.map(scene=>`<section class="sb-scene" data-scene-id="${esc(scene.id)}">
      ${scene.title&&scene.title!==chapter.title?`<h3>${esc(scene.title)}</h3>`:''}
      <p class="sb-scene-meta">${sceneSummary(scene)}</p>
      ${scene.blocks.map(block=>blockContent(block)).join('')}</section>`).join('')}</section>`;
}
function valueLabel(value) {
  if (value == null) return '';
  if (typeof value!=='object') return String(value);
  if (value.mode==='inherited') return t('inherited');
  if (value.mode==='unknown') return t('unknown');
  if (value.mode==='not_applicable') return t('notApplicable');
  if ('value' in value) return valueLabel(value.value);
  return JSON.stringify(value);
}
function sceneSummary(scene) {
  const seed = scene.initial_meta || {};
  return [scene.transition_mode ? t(scene.transition_mode) : '',seed.time,seed.location,seed.weather]
    .filter(Boolean).map(value=>esc(valueLabel(value))).join(' · ');
}
function blockContent(block) {
  const image = block.extensions?.['storyblock.image'];
  const text = `<span class="sb-block-text" data-text-block="${esc(block.id)}">${esc(block.text)}</span>`;
  const tools = btn('inspect',t('inspect'),`class="sb-inspect" data-block="${esc(block.id)}" aria-label="${t('inspect')} ${esc(block.id)}"`);
  if (!image) return `<div class="sb-block" data-block-id="${esc(block.id)}"><p class="story-block">${text}</p>${tools}</div>`;
  return `<figure class="story-image sb-block" data-block-id="${esc(block.id)}">
    <img alt="${esc(image.alt_text)}" width="${Number(image.width_px)}" height="${Number(image.height_px)}" hidden>
    <p class="sb-image-status" role="status">${t('loadingImage')}</p>
    ${btn('retryImage',t('retry'),`data-block="${esc(block.id)}" hidden`)}
    <figcaption class="story-block">${text}</figcaption>${tools}</figure>`;
}
function findBlock(id) {
  for (const [chapterIndex, chapter] of (S.selected?.revision.chapters||[]).entries())
    for (const scene of chapter.scenes) {
      const block=scene.blocks.find(b=>b.id===id);
      if(block) return {block,scene,chapterIndex};
    }
}
