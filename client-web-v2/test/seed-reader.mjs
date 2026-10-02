import {execFileSync} from 'node:child_process';
import {readFileSync} from 'node:fs';
import {resolve} from 'node:path';
const output=resolve(process.env.UAT_OUTPUT||'.local/frontend-v2');
const base=process.env.UAT_BASE_URL||'https://127.0.0.1:9443';
execFileSync(process.execPath,['acceptance/prepare-reader-images.mjs',base,
  resolve(output,'fixture.jpg'),resolve(output,'fixture.png'),resolve(output,'fixture')]);
const {novel_id}=JSON.parse(readFileSync(resolve(output,'fixture/manuscript.json'),'utf8'));
execFileSync(process.execPath,['plugin/scripts/storyblock-author.mjs','commit','--novel-id',novel_id,
  '--file',resolve(output,'fixture/edit.json'),'--base-url',base,'--json']);
console.log('Reader fixture ready / 閱讀測試資料已建立');
