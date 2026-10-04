module.exports=async c=>{
  const {page,click,check,testid}=c;
  check('Extra F1 phone list starts collapsed',await page.$$eval('.sheet .wrow',es=>es.length)===0);
  await c.shot('fix-mobile-chart');
  await click(testid('window-list-more'));const first=await page.$$eval('.sheet .wrow',es=>es.length);
  await click(testid('window-list-more'));const second=await page.$$eval('.sheet .wrow',es=>es.length);
  check('25 F1 phone list pages by 50',first===50&&second===100,{first,second});
  await click('[data-mlist=hide]');check('Extra F1 phone hide list',await page.$$eval('.sheet .wrow',es=>es.length)===0);
  await click(testid('window-show'));await page.waitForSelector('.win-band');
  const visible=await page.$eval('.win-band',e=>{const r=e.getBoundingClientRect();return r.top>=56&&r.top<innerHeight;});
  check('26 F1 phone first band visible',visible);await c.shot('fix-mobile-window');
  await click('[data-tabgo=ins]');await click('[data-metric="0"]');
  await page.focus('#win-chart');await page.keyboard.press('End');await click(testid('window-show'));
  await page.waitForSelector('.chapter-section:not([hidden]) .win-band');
  check('Extra F1 phone opens selected chapter',await page.$eval('.chapter-section:not([hidden])',e=>e.id)==='chapter-30');
};
