const channels=['surface','grammar','rhythm','narrative','lexical'];
function channelRows(channel) {
  return (S.comparison?.comparisons||[]).map(c=>{
    const score=c.scores.find(s=>s.channel===channel);
    return {name:c.name,value:score?.primary_distance==null?null:Number(score.primary_distance),metric:score?.primary_metric||''};
  }).sort((a,b)=>(a.value??Infinity)-(b.value??Infinity));
}
function decimal(value,digits=4) {return value==null||!Number.isFinite(Number(value))?'—':Number(value).toFixed(digits);}
function styleResults() {
  const result=S.comparison;
  const heading=`<p class="sb-note">${t('calculatedFor')} <code>${esc(result.revision_id)}</code></p>
    ${btn('rawStyles',t(S.raw?'showBars':'showTable'))}`;
  if(S.raw) return heading+styleTable();
  return heading+`<div id="style-score-rows">`+channels.map(channel=>{
    const rows=channelRows(channel), values=rows.map(r=>r.value).filter(v=>v!=null&&Number.isFinite(v));
    const min=Math.min(...values),max=Math.max(1e-12,...values);
    return `<section class="sb-channel"><h3>${t(channel)}</h3><p class="sb-muted">${esc(rows[0]?.metric||'')}</p>
      ${rows.map(row=>`<div class="sb-distance ${row.value===min?'closest':''}"><div><b>${esc(row.name)}</b>
        ${row.value===min?`<span class="sb-badge">${t('closest')}</span>`:''}<span title="${decimal(row.value,6)}">${decimal(row.value)}</span></div>
        <meter min="0" max="${max}" value="${row.value??0}" aria-label="${esc(row.name)} ${t(channel)}: ${decimal(row.value,6)}"></meter></div>`).join('')}
      ${channel==='narrative'?`<small>${t('textOnlyReference')}</small>`:''}</section>`;
  }).join('')+'</div>';
}
function styleTable() {
  return `<div class="sb-table-scroll"><table><thead><tr><th>${t('style')}</th>${channels.map(c=>`<th>${t(c)}</th>`).join('')}</tr></thead>
    <tbody id="style-score-rows">${S.comparison.comparisons.map(c=>`<tr><th scope="row">${esc(c.name)}</th>${channels.map(channel=>{
      const distance=c.scores.find(s=>s.channel===channel)?.primary_distance;
      const min=Math.min(...channelRows(channel).filter(r=>r.value!=null).map(r=>r.value));
      return `<td>${distance!=null&&Number(distance)===min?'<strong>':''}${decimal(distance,6)}${distance!=null&&Number(distance)===min?'</strong>':''}</td>`;
    }).join('')}</tr>`).join('')}</tbody></table></div>`;
}
