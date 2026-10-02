const fs=require('node:fs'),path=require('node:path'),os=require('node:os');
const local=path.resolve(__dirname,'../.local-tool-app/node_modules/puppeteer-core');
const puppeteer=require(process.env.PUPPETEER_MODULE||(fs.existsSync(local)?local:
  path.join(os.homedir(),'UAT-firefox/automation/node_modules/puppeteer-core')));
exports.launch=async output=>{
  const profile=fs.mkdtempSync(path.join(output,'firefox-'));
  const browser=await puppeteer.launch({browser:'firefox',headless:true,acceptInsecureCerts:true,
    executablePath:process.env.FIREFOX_BINARY||path.join(os.homedir(),'UAT-firefox/firefox/firefox'),
    userDataDir:profile,env:{...process.env,TMPDIR:path.resolve('.local/tmp'),MOZ_DISABLE_CONTENT_SANDBOX:'1'},
    extraPrefsFirefox:{'network.dns.disableIPv6':true,'browser.download.folderList':2,
      'browser.download.dir':path.join(output,'downloads'),'browser.download.useDownloadDir':true,
      'browser.helperApps.neverAsk.saveToDisk':'application/pdf,text/plain,application/json,application/octet-stream',
      'pdfjs.disabled':true,'browser.download.always_ask_before_handling_new_types':false}});
  return {browser,close:async()=>{await browser.close();fs.rmSync(profile,{recursive:true,force:true});}};
};
exports.read=uri=>new Promise((resolve,reject)=>{
  require('node:https').get(uri,{rejectUnauthorized:false,family:4},response=>{
    let body='';response.on('data',part=>body+=part);response.on('end',()=>{
      try{if(response.statusCode!==200)throw Error(body);resolve(JSON.parse(body));}catch(e){reject(e);}
    });
  }).on('error',reject);
});
