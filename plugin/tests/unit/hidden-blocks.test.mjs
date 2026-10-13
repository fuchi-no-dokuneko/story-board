import assert from 'node:assert/strict';
import {test} from 'node:test';
import {validateDto} from '../../scripts/lib/validation.mjs';
import {getSchema} from '../../scripts/lib/dtos.mjs';

const note={id:'blk_Hid01',text:'The teacup hides a motive.\n'+ 'Payoff in chapter 9. '.repeat(20),meta:{},hidden:true};
const validate=value=>validateDto('BlockDraft',value,{throwOnError:false});

test('hidden drafts allow long explanations while retaining visible text rules',async()=>{
  assert.equal((await validate(note)).valid,true);
  assert.equal((await validate({...note,text:'象'.repeat(2000)})).valid,true);
  assert.equal((await validate({...note,text:'象'.repeat(2001)})).valid,false);
  assert.equal((await validate({...note,text:' \n '})).valid,false);
  const {hidden,...visible}=note;
  assert.equal((await validate(visible)).valid,false);
  assert.equal((await validate({...visible,text:'她微笑了。'})).valid,true);
  assert.equal((await validate({...visible,text:'一。二。三。'})).valid,false);
});

test('hidden drafts reject state metadata, images and reserved extension spelling',async()=>{
  assert.equal((await validate({...note,meta:{provenance:{author:'Lin'}}})).valid,true);
  for(const field of ['time','location','weather','speech','actions','presence_events','pov','narrative_mode'])
    assert.equal((await validate({...note,meta:{[field]:[]}})).valid,false,field);
  assert.equal((await validate({...note,hidden:false})).valid,false);
  assert.equal((await validate({...note,image:{}})).valid,false);
  const {hidden,...visible}=note;
  assert.equal((await validate({...visible,extensions:{'storyblock.hidden':true}})).valid,false);
});

test('canonical notes preserve normalized text without applying visible sentence rules',async()=>{
  const canonical=structuredClone((await getSchema('CanonicalRevision')).examples[0]);
  canonical.chapters=[{id:'ch_Hid01',order_key:'80000000000000000000000000000000',scenes:[{
    id:'scn_Hid01',chapter_id:'ch_Hid01',order_key:'80000000000000000000000000000000',transition_mode:'opening',
    blocks:[{id:'blk_Hid01',block_version_id:'blv_Hid01',order_key:'80000000000000000000000000000000',text:'Motive',meta:{}}]}]}];
  const block=canonical.chapters[0].scenes[0].blocks[0];
  block.extensions={'storyblock.hidden':true};block.meta={};block.text='Motive\n'+ '象'.repeat(200);
  assert.equal((await validateDto('CanonicalRevision',canonical,{throwOnError:false})).valid,true);
  block.text=' Cafe\u0301 ';
  assert.equal((await validateDto('CanonicalRevision',canonical,{throwOnError:false})).valid,false);
});
