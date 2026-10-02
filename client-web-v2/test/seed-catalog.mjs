import {readFileSync,writeFileSync,existsSync,mkdirSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import {resolve} from 'node:path';
import {createTypedId} from '../../plugin/scripts/lib/ids.mjs';
import {countHanCodePoints} from '../../plugin/scripts/lib/validation.mjs';
const base=process.env.UAT_BASE_URL||'https://127.0.0.1:9443';
const output=resolve(process.env.UAT_OUTPUT||'.local/frontend-v2');
mkdirSync(output,{recursive:true});
const file=resolve(output,'catalog-fixture.json');
const titles=['雨停之前','遠方的信','旧书店的灯','沿海公路','月台上的咖啡',
  '山城慢行','Last Train Home','冬日来信','海邊散步','午後的車站','窗外的雨','霧中的燈塔','The Quiet Library'];
const english='Lin waited at the quiet station. The last train had not arrived.\n\n'+
  'A letter rested beside her coffee. She opened it and began to read.';
const chinese=readFileSync('acceptance/fixtures/last-train-1.txt','utf8');
const novels=existsSync(file)?JSON.parse(readFileSync(file,'utf8')):titles.map((title,i)=>{
  const text=i===6||i===12?english:chinese;
  return {novel_id:createTypedId('nov'),created_at:new Date().toISOString(),title,
    language:i===6||i===12?'en':i===2||i===7?'zh-Hans':'zh-Hant',
    main_characters:i===6||i===12?['Lin']:['林雨','周遠'],expected_han_characters:countHanCodePoints(text),
    chapters:[{title:i===6||i===12?'The station':'第一章',text}]};
});
writeFileSync(file,JSON.stringify(novels,null,2));
for(const novel of novels){
  const source=resolve(output,'catalog-source.json');writeFileSync(source,JSON.stringify(novel));
  execFileSync(process.execPath,['plugin/scripts/storyblock-author.mjs','register','--source',source,
    '--base-url',base,'--json'],{stdio:['ignore','ignore','inherit']});
}
console.log(`Seeded ${novels.length} catalog fixtures / 已建立書庫測試資料`);
