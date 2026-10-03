const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const directory=path.resolve(__dirname,'../src');
module.exports=function source(files,names,extras={}) {
  const elements=new Map(),storage=new Map();
  const element=id=>{
    if(!elements.has(id))elements.set(id,{hidden:true,textContent:'',value:'',innerHTML:'',dataset:{},isConnected:true,focus(){}});
    return elements.get(id);
  };
  const context={Intl,URL,URLSearchParams,AbortController,crypto,console,setTimeout,clearTimeout,performance,
    localStorage:{getItem:k=>storage.get(k)||null,setItem:(k,v)=>storage.set(k,v)},
    document:{getElementById:id=>elements.get(id),querySelector:s=>element(s.replace(/^#/,'')),
      querySelectorAll:()=>[],documentElement:{dataset:{}},body:{dataset:{}}},
    location:{origin:'https://127.0.0.1:9443'},history:{pushState(){},replaceState(){}},
    window:{scrollTo(){}},desktopWords:{},mobileWords:{},...extras};
  const code=files.map(f=>fs.readFileSync(path.join(directory,f.includes('/')?f:'js/'+f),'utf8')).join('\n');
  vm.runInNewContext(code+`\nglobalThis.subject={${names.join(',')}};`,context);
  return {...context.subject,context,elements,storage};
};
