const fs=require('node:fs'),path=require('node:path');
const {launch}=require('./browser.cjs');
(async()=>{
  const output=path.resolve(process.env.UAT_OUTPUT||'.local/frontend-v2');
  fs.mkdirSync(path.join(output,'downloads'),{recursive:true});
  const runtime=await launch(output);
  try{
    const page=await runtime.browser.newPage();
    const images=await page.evaluate(()=>{
      const canvas=document.createElement('canvas');canvas.width=800;canvas.height=600;
      const ctx=canvas.getContext('2d');
      ctx.fillStyle='#304e62';ctx.fillRect(0,0,800,600);
      ctx.fillStyle='#edba7c';ctx.fillRect(100,90,600,340);
      ctx.fillStyle='#304e62';ctx.font='32px serif';ctx.fillText('The last train',270,275);
      ctx.fillStyle='#fff';ctx.font='22px sans-serif';ctx.fillText('StoryBlock image fixture',270,520);
      return ['image/jpeg','image/png'].map(type=>canvas.toDataURL(type).split(',')[1]);
    });
    for(const [i,format] of ['jpg','png'].entries())
      fs.writeFileSync(path.join(output,'fixture.'+format),Buffer.from(images[i],'base64'));
  }finally{await runtime.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
