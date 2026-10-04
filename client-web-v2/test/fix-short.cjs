module.exports=async c=>{
  const {page,base,id,click,check,testid}=c;
  await page.goto(base+'/?ui=v2#'+id);await page.waitForSelector('.story-block');
  await click('#insightsBtn');await click('[data-tab=quality]');c.shortQuality=await c.analyze();
  c.assert.equal(c.shortQuality.quality_report.windows.length,2);
  check('12 F1 desktop short chips',!!(await page.$(testid('window-chips')))&&!(await page.$('#win-chart')));
  await click('[data-win="0"]');
  const w=c.shortQuality.quality_report.windows[0];
  const evidence=await page.$$eval('[data-evidence]',es=>es.map(e=>[+e.dataset.metric,+e.dataset.evidence]));
  check('13 F1 evidence filtered',evidence.length>0&&evidence.every(([m,i])=>{
    const e=c.shortQuality.quality_report.metrics[m].evidence[i];return e.start<w.end&&e.end>w.start;
  }));
  await click('[data-tab=style]');await page.waitForFunction(()=>!document.querySelector('#compare-styles').disabled);
  await click('#compare-styles');await page.waitForSelector('.chan');
  const labels=await page.$eval('#dbody',e=>e.innerText);
  check('14 F4 readable API metric names',!/_distance|robust_l1/.test(labels)&&
    ['Jensen–Shannon distance','Wasserstein distance','L1 distance'].every(x=>labels.includes(x)));
};
