import {writeFileSync,mkdirSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import {resolve} from 'node:path';
import {createTypedId} from '../../plugin/scripts/lib/ids.mjs';
const output=resolve(process.env.UAT_OUTPUT||'.local/frontend-v2');mkdirSync(output,{recursive:true});
// 715 × 128 words; 256-word windows stepping by 128 produce exactly 714.
// All reports come from the real analyzer; no injected metrics or window data.
const sentences=['The quiet train waited beside the empty station.',
  'Rain covered windows while lanterns flickered beyond them.',
  'She placed the letter carefully under her cup.',
  'Several passengers suddenly ran towards the distant bridge.',
  'Across wet rooftops an old bell rang twice.'];
const chapters=Array.from({length:30},(_,i)=>({title:`Chapter ${i+1}`,
  text:Array.from({length:i<25?24:23},(_,p)=>Array.from({length:16},(_,s)=>
    sentences[i%3===0?0:(s+p)%sentences.length]).join(' ')).join('\n\n')}));
const novel={novel_id:createTypedId('nov'),created_at:new Date().toISOString(),
  title:'末班電車不載影子｜714 windows',language:'en',main_characters:[],
  expected_han_characters:0,chapters};
const file=resolve(output,'windows-fixture.json');writeFileSync(file,JSON.stringify(novel));
execFileSync(process.execPath,['plugin/scripts/storyblock-author.mjs','register','--source',file,
  '--timeout-ms','180000','--base-url',process.env.UAT_BASE_URL||'https://127.0.0.1:9443','--json'],
  {stdio:['ignore','ignore','inherit']});
console.log('Seeded 714-window fixture / 已建立 714 視窗測試資料');
