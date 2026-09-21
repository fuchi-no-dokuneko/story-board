(() => {
  const byId = id => document.getElementById(id);
  const button = byId('analyze-quality'), save = byId('download-quality');
  const status = byId('quality-status'), range = byId('quality-window');
  let result = null, request = 0;
  window.QualityReport = { select() {
    request++; result = null; save.disabled = true; button.disabled = false;
    status.textContent = ''; byId('quality-score-rows').replaceChildren();
    byId('quality-evidence').replaceChildren(); byId('quality-statistics').textContent = '';
    range.replaceChildren(new Option('Whole text / 全文','all'));
  } };
  button.addEventListener('click', async () => {
    const selected = window.StoryBlockFeatures.selected;
    if (!selected) return;
    const current = ++request;
    button.disabled = true; save.disabled = true;
    status.textContent = 'Analyzing / 分析中…';
    try {
      const revision = selected.revision;
      const names = [...new Set(byId('quality-names').value.split(/[,，\n]/u).map(s => s.trim()).filter(Boolean))];
      const response = await fetchJson(`/v1/novels/${revision.novel_id}/quality-reports`, {
        method: 'POST', headers: window.StoryBlockFeatures.headers(revision),
        body: JSON.stringify({ revision_id: revision.revision_id, proper_names: names }),
      });
      if (current !== request) return;
      result = response;
      const report = result.quality_report;
      range.replaceChildren(new Option('Whole text / 全文','all'));
      report.windows.forEach((w,i) => range.add(new Option(`${i+1}: ${w.start}–${w.end}`,String(i))));
      window.QualityView.render(report);
      status.textContent = `Complete / 完成 · Reference sources / 參考來源: human / 真人 ${report.calibration.humanSources}, AI ${report.calibration.aiSources}`;
      save.disabled = false;
    } catch (error) { if (current === request) status.textContent = error.message; }
    finally { if (current === request) button.disabled = false; }
  });
  range.addEventListener('change', () => {
    if (result) window.QualityView.render(range.value === 'all' ? result.quality_report : result.quality_report.windows[Number(range.value)]);
  });
  save.addEventListener('click', () => {
    if (!result) return;
    const url = URL.createObjectURL(new Blob([JSON.stringify(result,null,2)], { type: 'application/json' }));
    const anchor = document.createElement('a'); anchor.href = url;
    anchor.download = `${result.novel_id}-quality.json`; anchor.click();
    setTimeout(() => URL.revokeObjectURL(url),10000);
  });
})();
