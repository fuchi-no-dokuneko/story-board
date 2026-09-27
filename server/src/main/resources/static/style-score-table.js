window.StyleScoreTable = {
  render(body, comparisons) {
    const channels = ['surface', 'grammar', 'rhythm', 'narrative', 'lexical'];
    const distanceOf = score => score?.primary_distance == null ? NaN : Number(score.primary_distance);
    const minima = new Map(channels.map(channel => [channel, Infinity]));
    for (const comparison of comparisons) {
      for (const score of comparison.scores) {
        const distance = distanceOf(score);
        if (Number.isFinite(distance) && distance < minima.get(score.channel)) {
          minima.set(score.channel, distance);
        }
      }
    }
    for (const comparison of comparisons) {
      const row = document.createElement('tr');
      const name = document.createElement('th');
      name.scope = 'row'; name.textContent = comparison.name; row.append(name);
      for (const channel of channels) {
        const score = comparison.scores.find(value => value.channel === channel);
        const distance = distanceOf(score);
        const cell = document.createElement('td');
        cell.textContent = Number.isFinite(distance) ? distance.toFixed(6) : '—';
        if (Number.isFinite(distance) && distance === minima.get(channel)) {
          const emphasis = document.createElement('strong');
          emphasis.textContent = cell.textContent;
          cell.replaceChildren(emphasis);
        }
        cell.title = score?.primary_metric || '';
        row.append(cell);
      }
      body.append(row);
    }
  },
};
