// V3A-00 Revision 1 supplemental probe. Read-only: no app, test or catalog edits.
// Requires: current jar on 8080 (live catalog), same jar on 18081 with the
// date-shifted scratch copy of app/data (Events fixture only), Vite on 5173.
// Vite's own proxy forwards /api to 8080; only the fixture scenario and the
// synthetic fault/latency scenarios intercept requests, in browser memory.
import {chromium} from '../../../.claude/skills/run-firststep-app/node_modules/playwright/index.mjs';
import fs from 'node:fs';
const dir=new URL('./',import.meta.url).pathname;
const base='http://127.0.0.1:5173/app-next';
const browser=await chromium.launch();
const out={findHelp:{},routes:[],history:{},events:{},faults:[],loading:[]};
const errors=[];
const fresh=async width=>{const p=await browser.newPage({viewport:{width,height:900}});p.on('pageerror',e=>errors.push({url:p.url(),message:e.message}));
  await p.route('**/images/**',async r=>{const u=new URL(r.request().url());if(u.port!=='5173')return r.continue();r.fulfill({response:await r.fetch({url:'http://127.0.0.1:8080'+u.pathname})});});return p;};
const snap=p=>p.evaluate(()=>({url:location.pathname+location.search,h1:[...document.querySelectorAll('h1')].map(e=>e.textContent),main:document.querySelector('main')?.innerText.slice(0,600),overflow:document.documentElement.scrollWidth>innerWidth,status:[...document.querySelectorAll('[role=status]')].map(e=>e.textContent),alerts:[...document.querySelectorAll('[role=alert]')].map(e=>e.textContent)}));
const settle=p=>p.waitForTimeout(500);
const shot=(p,name,w)=>p.screenshot({path:dir+`supp-${name}-${w}.png`,fullPage:true});
const nav=async(p,label,act)=>{const from=p.url();try{await act();}catch(e){return {label,from,failed:e.message.split('\n')[0],main:await p.locator('main').innerText().catch(()=>null)};}await settle(p);const to=p.url();await p.goBack();await settle(p);const back=p.url();await p.goForward();await settle(p);return {label,from,to,back,forward:p.url(),backRestored:back===from,forwardRestored:p.url()===to};};

// ---- 1. Find Help (P08) at both widths
for(const w of [1280,375]){
  const p=await fresh(w);const r={};
  await p.goto(base+'/');await settle(p);
  r.homeConnect=await nav(p,'Home Connect card → Find Help',()=>p.locator('main a[href$="/find-help"]').first().click({timeout:5000}));
  await p.goto(base+'/find-help');await p.locator('#org-search').waitFor();await settle(p);
  r.direct=await snap(p);
  r.gateways=await p.locator('#gateways-title + ul li').evaluateAll(ls=>ls.map(l=>({text:l.innerText,href:l.querySelector('a')?.getAttribute('href')??null,disabled:!!l.querySelector('[aria-disabled=true]')})));
  r.openToEveryone=await p.locator('[aria-labelledby=open-title] li').count();
  r.directoryCount=await p.locator('[aria-labelledby=directory-title] > ul > li').count();
  r.searchLabelled=await p.getByLabel(/search/i).count();
  await shot(p,'find-help',w);
  await p.fill('#org-search','red cross');r.searchRedCross={status:await p.locator('[aria-labelledby=directory-title] [role=status]').innerText(),names:await p.locator('[aria-labelledby=directory-title] > ul > li > a').allInnerTexts()};
  await p.fill('#org-search','zzzz-no-match');r.searchNoMatch={status:await p.locator('[aria-labelledby=directory-title] [role=status]').innerText(),entries:await p.locator('[aria-labelledby=directory-title] > ul > li').count(),sectionText:await p.locator('[aria-labelledby=directory-title]').innerText()};
  await shot(p,'find-help-nomatch',w);
  await p.fill('#org-search','red cross');
  r.orgLink=await nav(p,'Directory → Organization',()=>p.locator('[aria-labelledby=directory-title] > ul > li > a').first().click({timeout:5000}));
  await p.goBack();await settle(p);
  r.searchAfterBack={url:p.url(),value:await p.locator('#org-search').inputValue(),status:await p.locator('[aria-labelledby=directory-title] [role=status]').innerText()};
  r.gatewayLink=await nav(p,'Gateway → Category',()=>p.locator('#gateways-title + ul a').first().click({timeout:5000}));
  await p.goBack();await settle(p);
  r.categoryRendered=(await (async()=>{await p.locator('#gateways-title + ul a').first().click();await settle(p);const s=await snap(p);await p.goBack();await settle(p);return s;})());
  out.findHelp[w]=r;await p.close();
}
// Find Help synthetic states (375): loading, empty payload, 503
{const p=await fresh(375);
 await p.route('**/api/find-help',async r=>{await new Promise(x=>setTimeout(x,2500));try{r.fulfill({response:await r.fetch()});}catch{}});
 await p.goto(base+'/find-help');await p.waitForTimeout(400);out.findHelp.loading=await snap(p);await p.unrouteAll({behavior:'ignoreErrors'});
 await p.route('**/api/find-help',r=>r.fulfill({status:200,contentType:'application/json',body:JSON.stringify({success:true,data:{gateways:[],openToEveryone:[],directory:[]}})}));
 await p.goto(base+'/find-help');await settle(p);out.findHelp.empty=await snap(p);await shot(p,'find-help-empty',375);await p.unroute('**/api/find-help');
 await p.route('**/api/find-help',r=>r.fulfill({status:503,contentType:'application/json',body:JSON.stringify({success:false,errorCode:'UNAVAILABLE',errorMessage:'Audit unavailable fixture',data:null})}));
 await p.goto(base+'/find-help');await settle(p);out.findHelp.error=await snap(p);await p.close();}

// ---- 2. /discover/:facet and catch-all
for(const w of [1280,375]){const p=await fresh(w);
  for(const route of ['/discover/seniors','/discover/not-a-facet','/no-such-route','/originals/OR-999','/organization/not-an-org','/community-notices/not-a-view']){
    await p.goto(base+route);await settle(p);const s=await snap(p);s.route=route;s.width=w;s.mainLinks=await p.locator('main a').evaluateAll(as=>as.map(a=>a.getAttribute('href')));out.routes.push(s);
    await shot(p,'route'+route.replaceAll('/','_'),w);}
  await p.close();}

// ---- 3. History: Article, Updates, Organization (1280). Successful Article and
// Organization pages carry no internal link in <main> (their back link exists
// only in the not-found state), so exits use the Primary nav.
{const p=await fresh(1280);const N='nav[aria-label=Primary] ';
 await p.goto(base+'/');await settle(p);
 out.history.homeToArticle=await nav(p,'Home Original → Article',()=>p.locator('main a[href*="/originals/"]').first().click({timeout:5000}));
 out.history.articleMainInternalLinks=await p.locator('main a[href^="/app-next"]').count();
 out.history.articleToUpdates=await nav(p,'Article → Primary nav Updates',()=>p.locator(N+'a[href*="/updates"]').click({timeout:5000}));
 await p.goto(base+'/');await settle(p);
 out.history.homeToUpdates=await nav(p,'Home → Primary nav Updates',()=>p.locator(N+'a[href*="/updates"]').click({timeout:5000}));
 out.history.updatesLinks=await p.locator('main a').evaluateAll(as=>as.map(a=>({text:a.textContent.trim().slice(0,60),href:a.getAttribute('href'),target:a.getAttribute('target')})));
 out.history.updatesToHousing=await nav(p,'Updates → Primary nav Housing',()=>p.locator(N+'a[href*="/category/housing"]').click({timeout:5000}));
 await p.goto(base+'/category/housing?communityId=wilmington-de');await settle(p);
 out.history.categoryToOrg=await nav(p,'Category Organizations → Organization',()=>p.locator('main a[href*="/organization/"]').first().click({timeout:5000}));
 out.history.orgLinks=await p.locator('main a').evaluateAll(as=>as.map(a=>({text:a.textContent.trim().slice(0,60),href:a.getAttribute('href'),target:a.getAttribute('target')})));
 out.history.orgToUpdates=await nav(p,'Organization → Primary nav Updates',()=>p.locator(N+'a[href*="/updates"]').click({timeout:5000}));
 out.history.orgQueryOnNav=await p.locator(N+'a[href*="/updates"]').getAttribute('href');
 await p.close();}

// ---- 4. Events nonempty — controlled fixture backend 18081 (date-shifted scratch copy)
for(const w of [1280,375]){const p=await fresh(w);
  await p.route('**/api/community-notices**',async r=>{const u=new URL(r.request().url());r.fulfill({response:await r.fetch({url:'http://127.0.0.1:18081'+u.pathname+u.search})});});
  await p.goto(base+'/community-notices/events');await settle(p);const s=await snap(p);
  s.cards=await p.locator('main li').evaluateAll(ls=>ls.map(l=>l.innerText.replace(/\s+/g,' ').slice(0,160)));
  s.images=await p.locator('main img').evaluateAll(is=>is.map(i=>({loaded:i.complete&&i.naturalWidth>0,alt:i.alt})));
  await shot(p,'events-fixture',w);
  s.toFlyers=await nav(p,'Events → Flyers view',()=>p.locator('main a[href*="/community-notices/flyers"], nav a[href*="/community-notices/flyers"]').first().click({timeout:5000}));
  await p.goto(base+'/community-notices');await settle(p);s.overview=await snap(p);await shot(p,'events-fixture-overview',w);
  out.events[w]=s;await p.close();}

// ---- 5. Faults: Article 503 vs genuine 404; loading announcements for Notices/Updates/Find Help
{const p=await fresh(1280);
 await p.goto(base+'/originals/OR-999');await settle(p);out.faults.push({case:'Article genuine 404 (OR-999)',...await snap(p)});
 await p.route('**/api/originals/**',r=>r.fulfill({status:503,contentType:'application/json',body:JSON.stringify({success:false,errorCode:'UNAVAILABLE',errorMessage:'Audit unavailable fixture',data:null})}));
 await p.goto(base+'/originals/OR-003');await settle(p);out.faults.push({case:'Article synthetic 503 (OR-003)',...await snap(p)});await p.unroute('**/api/originals/**');
 for(const [route,api] of [['/community-notices','**/api/community-notices**'],['/updates','**/api/updates/**'],['/category/housing','**/api/category/**'],['/','**/api/home**']]){
   await p.route(api,async r=>{await new Promise(x=>setTimeout(x,2500));try{r.fulfill({response:await r.fetch()});}catch{}});
   await p.goto(base+route);await p.waitForTimeout(400);
   const l=await snap(p);l.route=route;l.loadingNodes=await p.locator('main p').evaluateAll(ps=>ps.map(e=>({text:e.textContent,role:e.getAttribute('role'),live:e.closest('[aria-live]')?.getAttribute('aria-live')??null})).slice(0,4));
   out.loading.push(l);await p.unrouteAll({behavior:'ignoreErrors'});}
 await p.close();}

out.errors=errors;
fs.writeFileSync(dir+'supplement-results.json',JSON.stringify(out,null,2));
await browser.close();
