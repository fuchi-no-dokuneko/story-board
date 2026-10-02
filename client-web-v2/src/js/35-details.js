function detailPanel() {
  const {novel:n,revision:r}=S.selected;
  const fields=[['novelId',n.novel_id],['revision',r.revision_id],['parent',r.parent_revision_id||'—'],
    ['updated',r.created_at||n.updated_at],['contentHash',r.content_hash],['hanHash',n.han_text_sha256]];
  const block=S.inspected&&findBlock(S.inspected)?.block;
  return `<dl class="sb-details">${fields.map(([key,value])=>`<div><dt>${t(key)}</dt><dd><code>${esc(value)}</code>
    ${btn('copy',t('copy'),`data-copy="${esc(value)}"`)}</dd></div>`).join('')}</dl>
    <p>${num(n.han_character_count)} ${t('han')} · ${num(n.scene_count)} ${t('scenes')} · ${num(n.block_count)} ${t('blocks')}</p>
    <p id="reader-registration">${t(n.agent_write_registered?'registered':'imported')}</p>
    ${btn('blockMode',t(S.blocks?'proseView':'blockDetails'))}
    ${block?`<section class="sb-block-info"><h3>${t('recordedMetadata')}</h3><code>${esc(block.id)}</code>
      <blockquote>${esc(block.text)}</blockquote><pre>${esc(JSON.stringify(block.meta||{},null,2))}</pre></section>`:`<p class="sb-muted">${t('inspectHint')}</p>`}`;
}
async function copyValue(value) {
  try{await navigator.clipboard.writeText(value);toast(t('copied'));}
  catch{toast(t('copyFailed'));}
}
