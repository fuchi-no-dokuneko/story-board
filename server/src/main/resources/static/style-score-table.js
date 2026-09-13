window.StyleScoreTable = {
  render(body, comparisons) {
    for (const comparison of comparisons) {
      const row = document.createElement('tr');
      const name = document.createElement('th');
      name.scope = 'row'; name.textContent = comparison.name; row.append(name);
      for (const channel of ['surface', 'grammar', 'rhythm', 'narrative', 'lexical']) {
        const score = comparison.scores.find(value => value.channel === channel);
        const cell = document.createElement('td');
        cell.textContent = score ? Number(score.primary_distance).toFixed(6) : '—';
        cell.title = score?.primary_metric || '';
        row.append(cell);
      }
      body.append(row);
    }
  },
};
