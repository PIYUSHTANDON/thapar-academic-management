import {chromium} from '../frontend/node_modules/playwright/index.mjs';
import {writeFileSync,readFileSync} from 'node:fs';
const browser=await chromium.launch({channel:'chromium'});
try{
 const page=await browser.newPage({viewport:{width:1920,height:1080},timezoneId:'Asia/Kolkata'});const errors=[],failed=[];
 page.on('pageerror',e=>errors.push(e.message));page.on('console',m=>{if(m.type()==='error')errors.push(m.text());});page.on('response',r=>{if(r.status()>=400)failed.push({path:new URL(r.url()).pathname,status:r.status()});});
 await page.goto('http://127.0.0.1:5173/');await page.getByRole('link',{name:/Faculty Portal/}).waitFor();await page.waitForLoadState('networkidle');await page.screenshot({path:'qa/screenshots/main-localhost-role.png',fullPage:true,animations:'disabled'});await page.getByRole('link',{name:/Faculty Portal/}).click();await page.getByRole('button',{name:'Sign In',exact:true}).waitFor();await page.waitForLoadState('networkidle');await page.screenshot({path:'qa/screenshots/main-localhost-login.png',fullPage:true,animations:'disabled'});
 const health=await page.request.get('http://127.0.0.1:8080/api/v1/health');const proxy=await page.request.get('http://127.0.0.1:5173/api/v1/health');if(health.status()!==200||proxy.status()!==200||errors.length||failed.length)throw Error('Main-site browser/startup check failed');
 const result={url:'http://127.0.0.1:5173/',backendHealth:health.status(),viteProxyHealth:proxy.status(),browser:browser.version(),playwright:JSON.parse(readFileSync('frontend/node_modules/@playwright/test/package.json','utf8')).version,errors,failed,passed:true};writeFileSync('qa/results/local-startup.json',JSON.stringify(result,null,2));console.log(JSON.stringify(result,null,2));
}finally{await browser.close();}
