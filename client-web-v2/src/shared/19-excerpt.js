function evidenceExcerpt(e){
  const span=S.quality?.block_spans[e.block],block=span&&findBlock(span.block_id)?.block;
  if(!block)return H.esc(e.text);
  const start=Math.max(0,e.start-span.start),end=Math.min(block.text.length,e.end-span.start);
  return H.esc(block.text.slice(0,start))+'<mark>'+H.esc(block.text.slice(start,end))+
    '</mark>'+H.esc(block.text.slice(end));
}
