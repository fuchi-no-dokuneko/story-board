(() => {
  const choices = document.getElementById('style-choices');
  const rows = document.getElementById('style-score-rows');
  const status = document.getElementById('style-status');
  const button = document.getElementById('compare-styles');
  let request = 0;
  async function load() {
    const current = ++request;
    choices.replaceChildren(); rows.replaceChildren();
    status.textContent = 'Loading styles / 讀取風格…';
    try {
      const result = await fetchJson('/v1/style/');
      if (current !== request) return;
      for (const style of result.styles) {
        const label = document.createElement('label');
        const input = document.createElement('input');
        input.type = 'checkbox'; input.value = style.id;
        input.disabled = style.status !== 'ready'; input.checked = !input.disabled;
        label.append(input, document.createTextNode(`${style.name} — ${style.description}`));
        if (style.error) label.append(document.createTextNode(` (${style.error})`));
        choices.append(label);
      }
      status.textContent = result.error || (result.styles.length ? '' : 'No reference styles / 尚無參考風格');
    } catch (error) { if (current === request) status.textContent = error.message; }
  }
  window.StyleComparison = { select: load };
  document.getElementById('reload-styles').addEventListener('click', load);
  button.addEventListener('click', async () => {
    const selected = window.StoryBlockFeatures.selected;
    const ids = [...choices.querySelectorAll('input:checked')].map(input => input.value);
    if (!selected || !ids.length) { status.textContent = 'Select styles / 請選取風格'; return; }
    button.disabled = true; rows.replaceChildren();
    status.textContent = 'Calculating / 計算中…';
    try {
      const revision = selected.revision;
      const result = await fetchJson(`/v1/novels/${revision.novel_id}/style-comparisons`, {
        method: 'POST', headers: window.StoryBlockFeatures.headers(revision),
        body: JSON.stringify({ revision_id: revision.revision_id, style_ids: ids }),
      });
      if (selected !== window.StoryBlockFeatures.selected) return;
      window.StyleScoreTable.render(rows, result.comparisons);
      status.textContent = 'Comparison complete / 比較完成';
    } catch (error) {
      if (selected === window.StoryBlockFeatures.selected) status.textContent = error.message;
    } finally { button.disabled = false; }
  });
})();
