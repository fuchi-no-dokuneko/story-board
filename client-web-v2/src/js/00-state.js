const words = {};
const $ = (selector, root = document) => root === document
  ? document.getElementById(selector) || document.querySelector(selector)
  : root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const esc = value => String(value ?? '').replace(/[&<>"']/g,
  c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const readLocal = (key, fallback) => {
  try { return JSON.parse(localStorage.getItem('sb2.' + key)) ?? fallback; }
  catch { return fallback; }
};
const saveLocal = (key, value) => {
  try { localStorage.setItem('sb2.' + key, JSON.stringify(value)); } catch {}
};
const saved = readLocal('readerPreferences', {});
const S = {
  lang: ['en','zh','zhs'].includes(saved.lang) ? saved.lang : 'en',
  mobile: typeof matchMedia === 'function' && matchMedia('(max-width:760px)').matches,
  prefs: {size:18,lh:'normal',font:'serif',theme:'paper',width:'narrow',...saved.desktop},
  fs: saved.fs || 19, font:saved.font || 'serif', theme:saved.theme || 'auto',
  token:'',auth:0,catalog:[],page:0,query:'',filter:'all',sort:'updated',
  selected:null,chapter:0,view:'library',panel:'',inspected:null,
  catalogRequest:0,novelRequest:0,styleRequest:0,qualityRequest:0,
  catalogAbort:null,novelAbort:null,imageViews:new Set(),details:new Map(),
  styles:[],picks:new Set(),comparison:null,quality:null,qualityWindow:'all',
  names:[],raw:false,online:null,loading:false,blocks:false,
  stack:['lib'],mobileTab:'lib',analysisTab:'style',ins:'style',drawer:false,
  immersive:false,catalogError:'',styleError:'',qualityError:'',scrollCleanup:null,
};
const locale = () => ({en:'en',zh:'zh-Hant',zhs:'zh-Hans'}[S.lang]);
const t = key => (S.mobile ? mobileWords : desktopWords)[key]?.[S.lang]
  || words[key]?.[['en','zh','zhs'].indexOf(S.lang)] || key;
const num = value => new Intl.NumberFormat(locale()).format(Number(value) || 0);
function prefs() {
  document.documentElement.lang=locale();
  document.documentElement.dataset.theme=S.mobile?S.theme:'light';
  saveLocal('readerPreferences',{lang:S.lang,desktop:S.prefs,fs:S.fs,font:S.font,theme:S.theme});
}
