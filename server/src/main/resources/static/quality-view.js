window.QualityView = {
  names: {
    repeated_phrase_coverage: '重複片語覆蓋率 / Repeated phrase coverage',
    opening_collision: '句首碰撞率 / Opening collision',
    skeleton_reuse: '句式骨架重用率 / Skeleton reuse',
    rhythm_predictability: '節奏排列可預測度 / Rhythm predictability',
    formulaic_bias: '跨來源套語偏向值 / Formulaic bias',
  },
  number(value) { return value == null ? '—' : Number(value).toFixed(6); },
  render(report) {
    const rows = document.getElementById('quality-score-rows');
    const evidence = document.getElementById('quality-evidence');
    rows.replaceChildren(); evidence.replaceChildren();
    for (const metric of report.metrics) {
      const name = this.names[metric.metric] || metric.metric;
      const row = document.createElement('tr');
      const state = { OK: 'OK / 完成', INSUFFICIENT_DATA: 'Insufficient data / 樣本不足',
        UNCALIBRATED: 'No reference corpus / 尚無參考語料' }[metric.status] || metric.status;
      for (const value of [name, this.number(metric.value), metric.validSamples,
        metric.referencePercentile == null ? '—' : `${metric.referencePercentile.toFixed(1)}%`, state]) {
        const cell = document.createElement('td'); cell.textContent = value; row.append(cell);
      }
      rows.append(row);
      if (metric.evidence.length) {
        const details = document.createElement('details');
        const summary = document.createElement('summary'); summary.textContent = name;
        details.append(summary);
        for (const item of metric.evidence) {
          const line = document.createElement('p');
          line.textContent = `[${item.start}–${item.end}] ${item.text} (${item.reason})`;
          details.append(line);
        }
        evidence.append(details);
      }
    }
    const stats = report.statistics;
    document.getElementById('quality-statistics').textContent =
      `${report.words} words / 詞 · ${report.sentences} sentences / 句 · JSD ${this.number(stats.wordFrequencyJsd)} · MATTR ${this.number(stats.mattr)}`;
  },
};
