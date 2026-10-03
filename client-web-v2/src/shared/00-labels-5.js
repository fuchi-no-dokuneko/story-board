const metricLabels = {
    repeated_phrase_coverage: { en: 'Repeated phrase coverage', zh: '重複片語覆蓋率', zhs: '重复片语覆盖率', hint: 'Share of words inside phrases (4–8 words) that repeat.' },
    opening_collision:        { en: 'Opening collision',        zh: '句首碰撞率',     zhs: '句首碰撞率',     hint: 'Sentences that share their first 3 words with another.' },
    skeleton_reuse:           { en: 'Skeleton reuse',           zh: '句式骨架重用率', zhs: '句式骨架重用率', hint: 'Sentences whose function-word skeleton repeats.' },
    rhythm_predictability:    { en: 'Rhythm predictability',    zh: '節奏排列可預測度', zhs: '节奏排列可预测度', hint: 'How predictable the sentence-length sequence is.' },
    formulaic_bias:           { en: 'Formulaic bias',           zh: '跨來源套語偏向值', zhs: '跨来源套语偏向值', hint: 'Lean toward phrases over-used by AI reference sources.' },
  };
