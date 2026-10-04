module.exports=async c=>{
  const {page,check,testid,longQuality}=c;
  const expected=longQuality.quality_report.metrics.filter(m=>
    longQuality.quality_report.windows.some(w=>w.metrics.find(x=>x.metric===m.metric).value!=null)).map(m=>m.metric);
  c.assert.deepEqual(await page.$$eval('#win-metric option',es=>es.map(e=>e.value)),expected);
  await page.focus('#win-chart');await page.keyboard.press('Home');await page.keyboard.press('PageUp');
  check('Extra F1 PageUp +10',/Window 11 of 714/.test(await c.label()));
  await page.keyboard.press('PageDown');check('Extra F1 PageDown -10',/Window 1 of 714/.test(await c.label()));
  const chart=await page.$('#win-chart');await chart.scrollIntoView();const b=await chart.boundingBox();
  await page.mouse.move(b.x+b.width*.1,b.y+b.height/2);await page.mouse.down();
  await page.mouse.move(b.x+b.width*.7,b.y+b.height/2,{steps:5});await page.mouse.up();
  const index=Number((await c.label()).match(/Window (\d+)/)[1]);
  check('Extra F1 drag selects window',Math.abs(index-500)<=2,index);
  const selected=longQuality.quality_report.windows[index-1];
  c.assert.deepEqual(await page.$$eval('.metric .val',es=>es.map(e=>e.textContent)),
    selected.metrics.map(m=>m.value==null?'—':m.value.toFixed(4)));
  check('Extra F1 five values and whole deltas',await page.$$eval('.metric .whole',es=>es.length)===5);
  const metric=await page.$eval('#win-metric',e=>e.value);
  const values=longQuality.quality_report.windows.map(w=>w.metrics.find(m=>m.metric===metric).value);
  const minimum=values.indexOf(Math.min(...values))+1;
  await c.click(testid('window-jump-min'));check('Extra F1 minimum',(await c.label()).includes(`Window ${minimum} of 714`));
};
