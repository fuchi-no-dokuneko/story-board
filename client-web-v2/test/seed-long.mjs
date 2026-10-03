import {writeFileSync,mkdirSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import {resolve} from 'node:path';
import {createTypedId} from '../../plugin/scripts/lib/ids.mjs';
import {countHanCodePoints} from '../../plugin/scripts/lib/validation.mjs';
const output=resolve(process.env.UAT_OUTPUT||'.local/frontend-v2');mkdirSync(output,{recursive:true});
const paragraphs=Array.from({length:133},(_,i)=>`${i+1}：月台上的時鐘慢了三分鐘。林雨讀著沒有寄出的信，望向窗外。🦊 The last train waited.`).join('\n\n');
const chapters=Array.from({length:30},(_,i)=>({title:`第${i+1}章：河岸的燈火`,text:paragraphs}));
const novel={novel_id:createTypedId('nov'),created_at:new Date().toISOString(),
 title:'末班電車不載影子｜完整風格校準稿（非最終版）｜長篇排版驗收',language:'yue-Hant',
 main_characters:['林雨','周遠','車站管理員','售票員','河岸居民','夜班列車長'],
 chapters,expected_han_characters:countHanCodePoints(chapters.map(c=>c.text).join('\n'))};
const file=resolve(output,'long-fixture.json');writeFileSync(file,JSON.stringify(novel));
execFileSync(process.execPath,['plugin/scripts/storyblock-author.mjs','register','--source',file,
 '--timeout-ms','180000','--base-url',process.env.UAT_BASE_URL||'https://127.0.0.1:9443','--json'],{stdio:['ignore','ignore','inherit']});
console.log('Seeded 30 chapters / 已建立三十章長篇資料');
