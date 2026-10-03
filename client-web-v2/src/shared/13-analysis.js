async function loadStyles(){
  const ticket=++S.styleRequest;S.stylesLoading=true;S.styleError='';S.comparison=null;renderPanel();
  try{
    const result=await json('/v1/style/');if(ticket!==S.styleRequest)return;
    S.styles=result.styles||[];S.picks=new Set(S.styles.filter(s=>s.status==='ready').map(s=>s.id));
    S.styleError=result.error||'';
  }catch(error){if(ticket===S.styleRequest)S.styleError=error.message;}
  finally{if(ticket===S.styleRequest){S.stylesLoading=false;renderPanel();}}
}
async function compareStyles(){
  const selected=S.selected,ids=[...S.picks];if(!selected||!ids.length||S.styleBusy)return;
  const ticket=++S.styleRequest;S.styleBusy=true;S.styleError='';S.comparison=null;renderPanel();
  try{
    const r=selected.revision;
    const result=await json(`/v1/novels/${r.novel_id}/style-comparisons`,revisionPost(r,{style_ids:ids}));
    if(ticket===S.styleRequest&&selected===S.selected)S.comparison=result;
  }catch(error){if(ticket===S.styleRequest)S.styleError=error.message;}
  finally{if(ticket===S.styleRequest){S.styleBusy=false;renderPanel();}}
}
async function analyzeQuality(){
  const selected=S.selected;if(!selected||S.qualityBusy)return;
  addPendingName();const ticket=++S.qualityRequest;S.qualityBusy=true;S.qualityError='';S.quality=null;renderPanel();
  try{
    const r=selected.revision;
    const result=await json(`/v1/novels/${r.novel_id}/quality-reports`,
      revisionPost(r,{proper_names:[...new Set(S.names)]}));
    if(ticket===S.qualityRequest&&selected===S.selected){S.quality=result;S.win='all';}
  }catch(error){if(ticket===S.qualityRequest)S.qualityError=error.message;}
  finally{if(ticket===S.qualityRequest){S.qualityBusy=false;renderPanel();}}
}
function addPendingName(){
  const input=$('#quality-names');
  if(input?.value.trim()){
    S.names=[...new Set([...S.names,...input.value.split(/[,，\n]/u).map(s=>s.trim()).filter(Boolean)])];input.value='';
  }
}
