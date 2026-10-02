module.exports=async context=>{
  const {page,click,shot,close,assert}=context;
  await click('.sb-reader-actions [data-action=style]');
  await page.waitForFunction(()=>!document.querySelector('#compare-styles').disabled);
  const response=page.waitForResponse(r=>r.url().endsWith('/style-comparisons')&&r.status()===200);
  await click('#compare-styles');const comparisons=await(await response).json();
  await page.waitForSelector('.sb-channel');
  assert.equal(await page.$$eval('.sb-channel',e=>e.length),5);
  const channels=['surface','grammar','rhythm','narrative','lexical'];
  const minima=channels.map(channel=>{
    const distances=comparisons.comparisons.map(c=>c.scores.find(s=>s.channel===channel).primary_distance);
    return distances.filter(d=>d===Math.min(...distances)).length;
  });
  assert.deepEqual(await page.$$eval('.sb-channel',es=>es.map(e=>e.querySelectorAll('.closest').length)),minima);
  await shot('desktop-style');await click('[data-action=rawStyles]');
  const values=await page.$eval('#style-score-rows',e=>e.textContent);
  for(const style of comparisons.comparisons)
    for(const score of style.scores)assert.ok(values.includes(Number(score.primary_distance).toFixed(6)));
  await click('[data-action=quality]');
  const quality=page.waitForResponse(r=>r.url().endsWith('/quality-reports')&&r.status()===200);
  await click('#analyze-quality');context.quality=await(await quality).json();
  await page.waitForSelector('.sb-metric');assert.equal(await page.$$eval('.sb-metric',e=>e.length),5);
  await shot('desktop-quality');
  if(context.quality.quality_report.windows.length){
    await page.select('#quality-window','0');assert.equal(await page.$$eval('.sb-metric',e=>e.length),5);
    await page.select('#quality-window','all');
  }
  await click('.sb-evidence summary');
  const evidence=context.quality.quality_report.metrics.find(m=>m.evidence.length).evidence[0];
  await click('.sb-evidence [data-action=evidence]');await page.waitForSelector('.sb-prose mark');
  assert.equal(await page.$$eval('.sb-prose mark',e=>e.map(x=>x.textContent).join('\n')),evidence.text);
  await shot('source-highlight');
  await click('.sb-reader-actions [data-action=style]');await click('#tools-dialog [data-action=details]');
  assert.match(await page.$eval('.sb-details',e=>e.textContent),new RegExp(context.selected.revision.revision_id));
  await close();
};
