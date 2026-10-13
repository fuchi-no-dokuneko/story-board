import {mkdirSync,writeFileSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
import {resolve} from 'node:path';
import {createTypedId} from '../../plugin/scripts/lib/ids.mjs';
import {countHanCodePoints} from '../../plugin/scripts/lib/validation.mjs';
const base=process.env.UAT_BASE_URL,output=resolve(process.env.UAT_OUTPUT);
mkdirSync(output,{recursive:true});
const cli=(...args)=>JSON.parse(execFileSync(process.execPath,
  ['plugin/scripts/storyblock-author.mjs',...args,'--base-url',base,'--json'],{encoding:'utf8'}));
const save=(name,value)=>{const p=resolve(output,name+'.json');writeFileSync(p,JSON.stringify(value));return p;};
const text=Array.from({length:16},(_,i)=>`第${i+1}段，雨聲掩蓋了她的回答，茶杯停在桌沿，她仍然注視著窗邊的燈。`).join('\n\n');
const novel={novel_id:createTypedId('nov'),title:'Hidden meaning UAT',created_at:new Date().toISOString(),
  language:'zh-Hant',main_characters:['Lin'],expected_han_characters:countHanCodePoints(text),
  chapters:[{title:'Teacup',text}]};
cli('register','--source',save('manuscript',novel));
const head=cli('read','--novel-id',novel.novel_id);
const revision=head.revision,scene=revision.chapters[0].scenes[0];
const slice=cli('blocks','--novel-id',novel.novel_id,'--revision-id',revision.revision_id,
  '--scene-id',scene.id,'--start','0','--end','2');
if(slice.body.blocks.length!==2)throw Error('Two insertion neighbors must be read');
const sentinel='HIDDEN_SENTINEL_foreshadowing';
const note=sentinel+'\nLin entered. Mary left. Payoff: chapter 9 / scene 2.\n<not-an-element> '+ '長'.repeat(300);
const request={operation:{operation_id:createTypedId('op'),idempotency_key:'hidden-uat',novel_id:novel.novel_id,
  base_revision_id:revision.revision_id,expected_head_hash:revision.content_hash,type:'insert_blocks',
  payload:{insertion_point:{scene_id:scene.id,anchor_block_id:scene.blocks[0].id,position:'after'},
    blocks:[{id:createTypedId('blk'),text:note,meta:{},hidden:true}]}},
  candidate_revision_id:createTypedId('rev'),candidate_created_at:new Date().toISOString()};
const file=save('edit',request);
const preview=cli('preview-edit','--novel-id',novel.novel_id,'--file',file).body;
if(!preview.committable||JSON.stringify(preview.render_packet).includes(sentinel))throw Error('Hidden preview failed');
cli('commit','--novel-id',novel.novel_id,'--file',file);
save('hidden-fixture',{id:novel.novel_id,sentinel,noteId:request.operation.payload.blocks[0].id});
console.log('Hidden note registered, read, previewed and committed / 隱藏註記已完成正常編輯流程');
