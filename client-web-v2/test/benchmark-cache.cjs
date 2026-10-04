const https=require('https'),fs=require('fs'),path=require('path'),assert=require('assert/strict');
const base=process.env.UAT_BASE_URL||'https://127.0.0.1:9443';
const output=path.resolve(process.env.UAT_OUTPUT||'.local/frontend-v2');
const id=JSON.parse(fs.readFileSync(path.join(output,'windows-fixture.json'))).novel_id;
function call(route,body,hash){return new Promise((resolve,reject)=>{
  const start=performance.now(),encoded=body&&JSON.stringify(body);
  const request=https.request(base+route,{rejectUnauthorized:false,family:4,method:body?'POST':'GET',
    headers:body?{'Content-Type':'application/json','Content-Length':Buffer.byteLength(encoded),
      'If-Match':'"'+hash+'"','Idempotency-Key':crypto.randomUUID()}:{}},response=>{
    let raw='';response.on('data',x=>raw+=x);response.on('end',()=>{
      try{assert.equal(response.statusCode,200,raw.slice(0,500));
        resolve({ms:performance.now()-start,body:JSON.parse(raw),bytes:Buffer.byteLength(raw)});
      }catch(error){reject(error);}
    });
  });request.on('error',reject);request.end(encoded);
});}
(async()=>{
  const results=[];
  async function measure(name,route,body,hash){
    const first=await call(route,body,hash),warm=[];
    for(let i=0;i<5;i++){const value=await call(route,body,hash);assert.deepEqual(value.body,first.body);warm.push(value.ms);}
    results.push({name,firstMs:+first.ms.toFixed(1),repeatMedianMs:+warm.sort((a,b)=>a-b)[2].toFixed(1),bytes:first.bytes,identical:true});
    return first.body;
  }
  const catalog=await measure('catalog','/v1/admin/novels');
  const detail=await measure('detail','/v1/admin/novels/'+id),r=detail.revision;
  const quality=await measure('quality','/v1/novels/'+id+'/quality-reports',{revision_id:r.revision_id,proper_names:[]},r.content_hash);
  const styles=(await call('/v1/style/')).body.styles.filter(s=>s.status==='ready').map(s=>s.id);
  await measure('style','/v1/novels/'+id+'/style-comparisons',{revision_id:r.revision_id,style_ids:styles},r.content_hash);
  const concurrent=await Promise.all(Array.from({length:12},()=>call('/v1/admin/novels/'+id)));
  concurrent.forEach(x=>assert.deepEqual(x.body,detail));
  fs.writeFileSync(path.join(output,'cache-benchmark.json'),JSON.stringify({novels:catalog.total,
    windows:quality.quality_report.windows.length,concurrentReaders:12,results},null,2));
  console.log(JSON.stringify(results));
})().catch(error=>{console.error(error);process.exitCode=1;});
