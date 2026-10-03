function handleClick(e){
  const b=e.target.closest('button,a');if(!b||b.disabled)return;
  const d=b.dataset;
  if(d.open)return openNovel(d.open,{read:true});
  if(d.book)return openNovel(d.book,{read:d.then==='read'});
  if(d.retryBook)return openNovel(d.retryBook,{read:true});
  if(d.library!==undefined)return library();
  if(d.reloadCatalog!==undefined)return loadCatalog();
  if(d.filter){S.filter=d.filter;S.page=0;return refreshLibrary();}
  if(d.page){S.page=Math.max(0,S.page+Number(d.page));return refreshLibrary();}
  if(d.langBtn)return switchLanguage(d.langBtn);
  if(d.tabgo)return mobileGo(d.tabgo);
  if(d.go)return mobileGo(d.go);
  if(d.back!==undefined)return mobileGo(S.stack.at(-2)||'lib');
  if(d.close!==undefined)return closeLayers();
  if(d.chapter!==undefined)return jumpChapter(Number(d.chapter));
  if(d.jump?.startsWith('chapter-'))return jumpChapter(Number(d.jump.slice(8))-1);
  if(d.sheet)return mSheet(d.sheet);
  if(d.metric!==undefined&&d.evidence!==undefined)return showEvidence(Number(d.metric),Number(d.evidence));
  if(d.metric!==undefined)return mSheet('metric',Number(d.metric));
  if(d.dl)return exportNovel(d.dl);
  if(d.copy)return copyValue(d.copy);
  if(d.action==='retryImage')return loadImage(b.closest('figure'));
  if(d.ins){S.ins=d.ins;return renderPanel();}
  if(d.tab){S.analysisTab=d.tab;return renderPanel();}
  if(d.win){S.win=d.win;return renderPanel();}
  if(d.pick){if(S.styleBusy)return;S.picks.has(d.pick)?S.picks.delete(d.pick):S.picks.add(d.pick);S.comparison=null;return renderPanel();}
  if(d.untok!==undefined||d.unname!==undefined){S.names.splice(Number(d.untok??d.unname),1);return renderPanel();}
  if(d.thm||d.font||d.fs){
    captureReading();if(d.thm)S.theme=d.thm;if(d.font)S.font=d.font;if(d.fs)S.fs=Number(d.fs);
    const sheet=!!$('.sheet');prefs();renderApp(false);
    if(S.view==='reading')restoreReading(remembered().find(r=>r.id===S.book));
    if(sheet)mSheet('aa');return;
  }
  return clickId(b.id,d);
}
