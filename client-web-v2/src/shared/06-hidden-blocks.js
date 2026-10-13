function isHiddenBlock(block){return block.hidden===true||block.extensions?.['storyblock.hidden']===true;}
function visibleTextBlocks(blocks){return blocks.filter(block=>!isHiddenBlock(block));}
function hiddenBlockNote(block){
  return `<aside class="story-hidden" role="note" data-hidden-block-id="${H.esc(block.id)}"><header>${H.esc(t('hiddenMeaning'))}</header><div>${H.esc(block.text)}</div></aside>`;
}
