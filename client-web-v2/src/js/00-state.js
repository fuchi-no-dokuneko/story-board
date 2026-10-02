const words = {};
const $ = id => document.getElementById(id);
const esc = value => String(value ?? '').replace(/[&<>"']/g,
  c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const readLocal = (key, fallback) => {
  try { return JSON.parse(localStorage.getItem('sb2.' + key)) ?? fallback; }
  catch { return fallback; }
};
const saveLocal = (key, value) => {
  try { localStorage.setItem('sb2.' + key, JSON.stringify(value)); } catch { /* Optional storage. */ }
};
const saved = readLocal('preferences', {});
const S = {
  lang: ['en','zh-Hant','zh-Hans'].includes(saved.lang) ? saved.lang : 'en',
  theme: ['paper','white','night'].includes(saved.theme) ? saved.theme : 'paper',
  size: [18,20,22,24,28].includes(saved.size) ? saved.size : 20,
  spacing: ['normal','loose'].includes(saved.spacing) ? saved.spacing : 'normal',
  font: ['serif','sans'].includes(saved.font) ? saved.font : 'serif',
  token: '', auth: 0, catalog: [], page: 0, query: '', filter: 'all', sort: 'updated',
  selected: null, chapter: 0, view: 'library', panel: '', inspected: null,
  catalogRequest: 0, novelRequest: 0, styleRequest: 0, qualityRequest: 0,
  catalogAbort: null, novelAbort: null, imageViews: new Set(),
  styles: [], picks: new Set(), comparison: null, quality: null, qualityWindow: 'all',
  names: '', raw: false, online: null, loading: false, blocks: false,
};
const t = key => words[key]?.[['en','zh-Hant','zh-Hans'].indexOf(S.lang)] || key;
const num = value => new Intl.NumberFormat(S.lang).format(Number(value) || 0);
const btn = (action, label, extra = '') => `<button type="button" data-action="${action}" ${extra}>${label}</button>`;
const icon = (symbol, label) => `<span aria-hidden="true">${symbol}</span><span>${label}</span>`;
function prefs() {
  document.documentElement.dataset.ui = 'v2';
  document.documentElement.lang = S.lang;
  document.body.dataset.theme = S.theme;
  document.body.dataset.size = S.size;
  document.body.dataset.spacing = S.spacing;
  document.body.dataset.font = S.font;
  saveLocal('preferences', {lang:S.lang,theme:S.theme,size:S.size,spacing:S.spacing,font:S.font});
}
