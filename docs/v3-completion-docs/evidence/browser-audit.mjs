import { chromium } from '../../../.claude/skills/run-firststep-app/node_modules/playwright/index.mjs';
import fs from 'node:fs';
const dir = new URL('./', import.meta.url).pathname;
const browser = await chromium.launch({headless:true});
const page = await browser.newPage();
const errors=[]; page.on('pageerror', e=>errors.push(e.message));
const base='http://127.0.0.1:5173/app-next';
await page.route(/\/api\/|\/images\/seasonal\//, async route=>{
 const u=new URL(route.request().url());
 if(!u.pathname.startsWith('/api/')&&!u.pathname.startsWith('/images/seasonal/')) return route.continue();
 const response=await route.fetch({url:'http://127.0.0.1:18080'+u.pathname+u.search});
 await route.fulfill({response});
});
const api={};
for(const path of ['/api/home','/api/category/housing','/api/category/housing/help','/api/updates/government','/api/community-notices/flyers']){
 const r=await page.request.get('http://127.0.0.1:18080'+path);api[path]={status:r.status(),body:await r.json()};
}
fs.writeFileSync(dir+'api.json',JSON.stringify(api,null,2));
const housing=api['/api/category/housing'].body.data;
const topic=housing.destination.featuredTopics[0].slug;
const org=housing.organizations[0].slug;
const routes=['/','/category/housing','/category/housing/'+topic,'/category/housing/help','/community-notices','/community-notices/events','/community-notices/meetings','/community-notices/announcements','/community-notices/flyers','/updates','/originals/OR-003','/organization/'+org,'/category/invalid/help','/about','/community','/discover'];
const results=[];
for(const width of [1280,375]){
 await page.setViewportSize({width,height:900});
 for(const [i,route] of routes.entries()){
  await page.goto(base+route);await page.locator('main').waitFor();await page.waitForTimeout(650);
  const before=page.url();const snapshot=await page.evaluate(()=>({text:document.body.innerText,overflow:document.documentElement.scrollWidth>innerWidth,headings:[...document.querySelectorAll('h1,h2,h3')].map(e=>e.textContent),links:[...document.querySelectorAll('main a')].map(e=>({text:e.textContent,href:e.getAttribute('href')})),images:[...document.querySelectorAll('main img')].map(e=>({src:e.src,loaded:e.complete&&e.naturalWidth>0,fit:getComputedStyle(e).objectFit})),alerts:[...document.querySelectorAll('[role=alert],[role=status]')].map(e=>e.textContent)}));
  await page.screenshot({path:dir+`page-${width}-${i}.png`,fullPage:true});
  const internal=page.locator('main a[href^="/app-next/"]').first();
  let history=null;if(await internal.count()){await internal.click();await page.waitForTimeout(250);const target=page.url();await page.goBack();await page.waitForTimeout(200);const back=page.url();await page.goForward();await page.waitForTimeout(200);history={target,back,forward:page.url(),restored:back===before};}
  results.push({width,route,snapshot,history});
 }
}
await page.goto(base+'/category/housing/help?communityId=wilmington-de');await page.waitForTimeout(500);
const expansion={before:await page.locator('main li').count()};
const more=page.getByRole('button',{name:/see more/i});if(await more.count()){await more.click();expansion.after=await page.locator('main li').count();}
await page.getByRole('button',{name:/contrast/i}).click();expansion.contrast=await page.locator('body').getAttribute('class');await page.keyboard.press('Tab');expansion.focus=await page.evaluate(()=>({tag:document.activeElement.tagName,outline:getComputedStyle(document.activeElement).outline}));
fs.writeFileSync(dir+'browser-results.json',JSON.stringify({results,expansion,errors},null,2));
await browser.close();
