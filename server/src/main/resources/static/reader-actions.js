(() => {
  const status = document.getElementById('reader-action-status');
  const features = window.StoryBlockFeatures = {
    selected: null,
    select(payload) {
      this.selected = payload;
      status.textContent = '';
      window.StyleComparison?.select();
    },
    headers(revision) {
      return { ...authorizationHeaders(state.operatorToken),
        'Content-Type': 'application/json', 'If-Match': `"${revision.content_hash}"`,
        'Idempotency-Key': crypto.randomUUID() };
    },
  };
  async function download(format) {
    const selected = features.selected;
    if (!selected) return;
    const revision = selected.revision;
    const button = document.getElementById(`download-${format}`);
    button.disabled = true;
    status.textContent = 'Preparing download / 正在準備下載…';
    try {
      let blob;
      if (format === 'txt') {
        const text = revision.chapters.map(chapter => [chapter.title || '',
          ...chapter.scenes.map(scene => scene.blocks.map(block => block.text).join('\n')),
        ].join('\n\n')).join('\n\n');
        blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
      } else {
        const response = await fetch(`/v1/novels/${revision.novel_id}/pdf-renders`, {
          method: 'POST', headers: features.headers(revision),
          body: JSON.stringify({ revision_id: revision.revision_id }),
        });
        if (!response.ok) {
          const error = await response.json();
          throw new Error(error.detail || `PDF error: ${response.status}`);
        }
        blob = await response.blob();
      }
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `${(selected.novel.title || revision.novel_id).replace(/[\\/:*?"<>|]/g, '_')}.${format}`;
      anchor.click();
      setTimeout(() => URL.revokeObjectURL(url), 10000);
      status.textContent = 'Download ready / 下載已準備';
    } catch (error) { status.textContent = error.message; }
    finally { button.disabled = false; }
  }
  for (const format of ['pdf', 'txt']) {
    document.getElementById(`download-${format}`).addEventListener('click', () => download(format));
  }
})();
