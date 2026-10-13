module.exports=async({page,base,fixture,visible,assert,click,close,shot})=>{
  for(const [width,height,scale] of [[320,640,1],[390,844,2],[768,1024,1],[844,390,1],[1024,600,1],[1920,1080,1]]){
    await page.setViewport({width,height,deviceScaleFactor:scale});
    await page.goto(`${base}/?ui=v2#${fixture.id}`);await page.waitForSelector('.story-hidden');
    assert.deepEqual(await page.$$eval('[data-text-block]',es=>es.map(e=>e.textContent)),visible.map(b=>b.text));
    const note=await page.$eval('.story-hidden',e=>({role:e.getAttribute('role'),id:e.dataset.hiddenBlockId,
      text:e.textContent,tag:e.tagName,anchor:e.hasAttribute('data-text-block')||e.hasAttribute('data-block-id'),
      previous:e.previousElementSibling?.dataset.textBlock,next:e.nextElementSibling?.dataset.textBlock,
      white:getComputedStyle(e).whiteSpace,background:getComputedStyle(e).backgroundColor,
      rule:getComputedStyle(e).borderLeftWidth,font:parseFloat(getComputedStyle(e).fontSize),
      prose:parseFloat(getComputedStyle(document.querySelector('[data-text-block]')).fontSize)}));
    assert.equal(note.tag,'ASIDE');assert.equal(note.role,'note');assert.equal(note.id,fixture.noteId);
    assert.equal(note.anchor,false);assert.equal(note.previous,visible[0].id);assert.equal(note.next,visible[1].id);
    assert.ok(note.text.includes(fixture.sentinel));assert.equal(await page.$('not-an-element'),null);
    assert.equal(note.white,'pre-wrap');assert.notEqual(note.background,'rgba(0, 0, 0, 0)');
    assert.equal(note.rule,'3px');assert.ok(note.font<note.prose);
    if(width<=760){await click('.rtop [data-sheet=aa]');await click('[data-fs="22"]');}
    else{await click('#aaBtn');await page.$eval('#fs',e=>{e.value=24;e.dispatchEvent(new Event('input',{bubbles:true}));});}
    await close();
    assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'No horizontal overflow '+width);
    await page.$eval('.story-hidden',e=>e.scrollIntoView());
    assert.ok(await page.$eval('.story-hidden header',e=>e.getBoundingClientRect().top>58),'Note header clears reading toolbar');
    await page.waitForFunction(()=>JSON.parse(localStorage.getItem('sb2.recent')||'[]')[0]?.anchor);
    const anchor=await page.evaluate(()=>JSON.parse(localStorage.getItem('sb2.recent'))[0].anchor);
    assert.ok(visible.some(b=>b.id===anchor));await shot('hidden-v2-'+width);
  }
};
