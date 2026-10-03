module.exports=async context=>{
  const {page,click,shot,close,assert}=context;
  await click('#insightsBtn');
  await page.waitForFunction(()=>!document.querySelector('#compare-styles').disabled);
  const response=page.waitForResponse(r=>r.url().endsWith('/style-comparisons')&&r.status()===200);
  await click('#compare-styles');const comparisons=await(await response).json();
  await page.waitForSelector('.chan');
  assert.equal(await page.$$eval('.chan',e=>e.length),5);
  const channels=['surface','grammar','rhythm','narrative','lexical'];
  const minima=channels.map(channel=>{
    const distances=comparisons.comparisons.map(c=>c.scores.find(s=>s.channel===channel).primary_distance);
    return distances.filter(d=>d===Math.min(...distances)).length;
  });
  assert.deepEqual(await page.$$eval('.chan',es=>es.map(e=>e.querySelectorAll('.bar.min').length)),minima);
  await shot('desktop-style');await click('#rawToggle');
  const values=await page.$eval('#style-score-rows',e=>e.textContent);
  for(const style of comparisons.comparisons)
    for(const score of style.scores)assert.ok(values.includes(Number(score.primary_distance).toFixed(6)));
  await click('[data-tab=quality]');
  const quality=page.waitForResponse(r=>r.url().endsWith('/quality-reports')&&r.status()===200);
  await click('#analyze-quality');context.quality=await(await quality).json();
  await page.waitForSelector('.metric');assert.equal(await page.$$eval('.metric',e=>e.length),5);
  await shot('desktop-quality');
  if(context.quality.quality_report.windows.length){
    await click('[data-win="0"]');assert.equal(await page.$$eval('.metric',e=>e.length),5);
    await click('[data-win=all]');
  }
  await click('.ev summary');
  const evidence=context.quality.quality_report.metrics.find(m=>m.evidence.length).evidence[0];
  await click('.ev [data-evidence]');await page.waitForSelector('#reader-content mark');
  assert.equal(await page.$$eval('#reader-content mark',e=>e.map(x=>x.textContent).join('\n')),evidence.text);
  await shot('source-highlight');
  await click('[data-tab=about]');
  assert.match(await page.$eval('.kv',e=>e.textContent),new RegExp(context.selected.revision.revision_id));
  await close();
};
