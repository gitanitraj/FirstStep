import {chromium} from '../../../.claude/skills/run-firststep-app/node_modules/playwright/index.mjs';
import fs from 'node:fs';
const dir=new URL('./',import.meta.url).pathname;
const browser=await chromium.launch();const page=await browser.newPage({viewport:{width:375,height:900}});
await page.route('**/api/**',async r=>{const u=new URL(r.request().url());if(!u.pathname.startsWith('/api/'))return r.continue();const response=await r.fetch({url:'http://127.0.0.1:18080'+u.pathname+u.search});await r.fulfill({response});});
const base='http://127.0.0.1:5173/app-next';const out={};
await page.goto(base+'/category/housing?communityId=wilmington-de');await page.getByRole('link',{name:'All Housing Help',exact:true}).click();await page.getByRole('heading',{name:'All Housing Help',exact:true}).waitFor();
out.help={url:page.url(),before:await page.locator('main li').count()};await page.getByRole('button',{name:/32 more/i}).click();out.help.after=await page.locator('main li').count();await page.locator('main summary').click();const narrow=page.locator('main details a').first();out.help.topicLink=await narrow.getAttribute('href');await narrow.click();await page.waitForTimeout(400);out.topic={url:page.url(),text:await page.locator('main').innerText()};await page.goBack();await page.waitForTimeout(300);out.help.back=page.url();
await page.goto(base+'/category/housing?communityId=wilmington-de');await page.getByRole('button',{name:'First Rate News',exact:true}).click();out.editorial=await page.locator('#active-collection').innerText();
for(const path of ['/api/originals/OR-003','/api/category/housing/help?communityId=bear-de','/api/category/housing/not-a-topic','/api/organization/not-an-org','/api/updates/government?communityId=bear-de']){let r=await page.request.get('http://127.0.0.1:18080'+path);out[path]={status:r.status(),body:await r.json()};}
out.states=[];
fs.writeFileSync(dir+'journeys.json',JSON.stringify(out,null,2));
for(const path of ['/','/category/housing','/category/housing/help','/community-notices','/updates','/originals/OR-003']){
 await page.route('**/api/**',r=>new URL(r.request().url()).pathname.startsWith('/api/')?r.fulfill({status:503,contentType:'application/json',body:JSON.stringify({success:false,errorCode:'UNAVAILABLE',errorMessage:'Audit unavailable fixture',data:null})}):r.continue());
 await page.goto(base+path);await page.waitForTimeout(400);out.states.push({path,text:await page.locator('main').innerText(),alerts:await page.locator('[role=alert]').count()});
 await page.unroute('**/api/**');
}
fs.writeFileSync(dir+'journeys.json',JSON.stringify(out,null,2));await browser.close();
