async function analyzeQuality() {
  const selected=S.selected;if(!selected||S.qualityBusy)return;
  const ticket=++S.qualityRequest;S.qualityBusy=true;S.qualityError='';S.quality=null;renderPanel();
  try {
    const revision=selected.revision;
    const names=[...new Set(S.names.split(/[,，\n]/u).map(s=>s.trim()).filter(Boolean))];
    const result=await json(`/v1/novels/${revision.novel_id}/quality-reports`,revisionPost(revision,{proper_names:names}));
    if(ticket!==S.qualityRequest||selected!==S.selected)return;
    S.quality=result;S.qualityWindow='all';
  }catch(error){if(ticket===S.qualityRequest)S.qualityError=error.message;}
  finally{if(ticket===S.qualityRequest){S.qualityBusy=false;if(S.panel==='quality')renderPanel();}}
}
function qualityPanel() {
  return `<p class="sb-muted">${t('qualityHint')}</p><label for="quality-names">${t('properNames')}</label>
    <input id="quality-names" value="${esc(S.names)}" placeholder="${t('namesPlaceholder')}">
    <p class="sb-muted">${t('namesHint')}</p><div class="sb-actions">
    ${btn('analyze',t(S.qualityBusy?'analyzing':'analyze'),`id="analyze-quality" class="sb-accent" ${S.qualityBusy?'disabled':''}`)}
    ${btn('saveQuality',t('saveReport'),`id="download-quality" ${!S.quality?'disabled':''}`)}</div>
    <p id="quality-status" role="status">${esc(S.qualityError||'')}</p>${S.quality?qualityResults():''}`;
}
function currentQuality() {
  const report=S.quality?.quality_report;
  return S.qualityWindow==='all'?report:report?.windows[Number(S.qualityWindow)];
}
function qualityResults() {
  const full=S.quality.quality_report, report=currentQuality();
  const sources=full.calibration;
  return `<p class="sb-note">${sources.humanSources?`${t('humanSources')}: ${num(sources.humanSources)} · AI: ${num(sources.aiSources)}`:t('noCorpus')}</p>
    <label for="quality-window">${t('textRange')}</label><select id="quality-window"><option value="all">${t('wholeText')}</option>
    ${full.windows.map((w,i)=>`<option value="${i}" ${S.qualityWindow===String(i)?'selected':''}>${t('window')} ${i+1} · ${w.start}–${w.end}</option>`).join('')}</select>
    <div id="quality-score-rows">${report.metrics.map((m,i)=>metricCard(m,i)).join('')}</div>
    <p id="quality-statistics" class="sb-muted">${num(report.words)} ${t('words')} · ${num(report.sentences)} ${t('sentences')} · MATTR ${decimal(report.statistics.mattr)} · JSD ${decimal(report.statistics.wordFrequencyJsd)}</p>`;
}
