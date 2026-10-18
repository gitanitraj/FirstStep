import { chromium } from '/Users/composedmedia/.npm/_npx/e41f203b7505f1fb/node_modules/playwright/index.mjs';
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const out=(process.env.IM13_OUTPUT_DIR??new URL('./',import.meta.url).pathname).replace(/\/?$/, '/');
const origin=process.env.IM13_ORIGIN??'http://127.0.0.1:8113';
const browser=await chromium.launch({headless:true});const context=await browser.newContext({viewport:{width:1280,height:1000}});const page=await context.newPage();
const checks=[],errors=[],assets=[];page.on('pageerror',e=>errors.push(e.message));
page.on('response',r=>{if(/\.(woff2?|png|jpe?g|css|js)(\?|$)/i.test(r.url()))assets.push({url:r.url(),status:r.status()});});
async function test(id,fn){try{checks.push({id,status:'PASS',facts:await fn()});}catch(e){checks.push({id,status:'FAIL',error:e.message});await page.screenshot({path:out+'failure-'+id+'.png',fullPage:true});}}
async function open(path){await page.goto(origin+'/app-next'+path);await page.locator('main h1,main [role="alert"]').first().waitFor();}
async function keyboard(locator,key='Enter'){await locator.focus();await locator.press(key);}
async function api(path){const r=await page.request.get(origin+path);assert.equal(r.status(),200);return (await r.json()).data;}
async function semantics(label){
 const facts=await page.evaluate(()=>({main:document.querySelectorAll('main').length,h1:document.querySelectorAll('main h1').length,
  unnamedLinks:[...document.querySelectorAll('a')].filter(a=>a.getClientRects().length&&!((a.getAttribute('aria-label')||a.textContent||'').trim())).map(a=>a.outerHTML),
  missingAlt:[...document.images].filter(i=>!i.hasAttribute('alt')).map(i=>i.src),
  duplicates:[...document.querySelectorAll('[id]')].map(e=>e.id).filter((id,i,ids)=>ids.indexOf(id)!==i),
  overflow:document.documentElement.scrollWidth>innerWidth}));
 assert.equal(facts.main,1,label);assert.equal(facts.h1,1,label);assert.deepEqual(facts.unnamedLinks,[],label);assert.deepEqual(facts.missingAlt,[],label);assert.deepEqual(facts.duplicates,[],label);assert(!facts.overflow,label);return facts;
}
const q='?communityId=wilmington-de';
try{
 await test('T02-J1-help',async()=>{
  await open('/category/housing'+q);assert.equal(await page.locator('main h1').evaluate(e=>document.activeElement===e),true);
  const help=page.getByRole('region',{name:'Get help',exact:true});assert.equal(await help.getByRole('listitem').count(),4);
  await keyboard(page.locator('main a[href*="/category/housing/help"]').first());
  await page.getByRole('heading',{name:'All Housing Help',level:1,exact:true}).waitFor();
  const data=await api('/api/category/housing/help'+q);assert.equal(data.items.length,36);assert.equal(data.metadata.totalCount,36);
  await keyboard(page.getByRole('button',{name:'32 more',exact:true}));assert.equal(await page.getByRole('region',{name:'Get help',exact:true}).getByRole('listitem').count(),36);
  const all=await semantics('All Help');await open('/category/housing/eviction-prevention'+q);
  await page.getByRole('status').filter({hasText:'No current help'}).waitFor();assert.equal((await api('/api/category/housing/eviction-prevention'+q)).items.length,0);
  return {preview:4,fullHelp:36,zeroHelpTopic:true,semantics:all};
 });
 await test('T03-J2-topics',async()=>{
  await open('/category/housing'+q);const data=await api('/api/category/housing'+q);const topics=data.destination.featuredTopics;
  assert.deepEqual(topics.map(t=>t.slug),['transitional-housing','emergency-shelter','homeownership','rental-assistance']);
  const ids=topics.flatMap(t=>t.items.map(p=>p.item.id));assert.deepEqual(ids,['HA-002','HA-003','HA-005','HA-016','HA-001','HA-006','HA-011','HA-013','HA-021','HA-008','HA-023','HA-026']);
  for(const t of topics)assert.deepEqual(t.items.map(p=>p.role),['DURABLE','FALLBACK','FALLBACK']);
  const region=page.getByRole('region',{name:'Featured topics',exact:true});assert.equal(await region.getByRole('listitem').count(),12);assert(!/DURABLE|TIMELY|VISUAL|FALLBACK/.test(await region.innerText()));
  await keyboard(region.getByRole('link',{name:'Transitional Housing',exact:true}));await page.getByRole('heading',{name:'Transitional Housing',level:1,exact:true}).waitFor();
  assert(await page.locator('main h1').evaluate(e=>document.activeElement===e));await page.reload();await page.getByRole('heading',{name:'Transitional Housing',level:1,exact:true}).waitFor();
  await page.goBack();await page.getByRole('heading',{name:'Featured topics',exact:true}).waitFor();assert(page.url().includes(q));return {ids,roles:['DURABLE','FALLBACK','FALLBACK'],keyboardRefreshBack:true};
 });
 await test('T04-J3-provider',async()=>{
  await open('/category/housing'+q);const offering=await api('/api/resources/HA-001'+q);assert.equal(offering.organization_id,'ORG-001');
  assert.equal(await page.getByRole('region',{name:'Featured topics',exact:true}).getByRole('link',{name:'American Red Cross',exact:true}).count(),1);
  const provider=page.getByRole('region',{name:'Organizations',exact:true}).getByRole('link',{name:'American Red Cross',exact:true});
  assert((await provider.getAttribute('href')).endsWith('/organization/american-red-cross'+q));await keyboard(provider);
  await page.getByRole('heading',{name:'American Red Cross',level:1,exact:true}).waitFor();assert(await page.locator('main h1').evaluate(e=>document.activeElement===e));
  const data=await api('/api/organization/american-red-cross'+q);assert.deepEqual(data.resources.map(r=>r.id),['HA-001']);
  assert.equal(await page.locator('main li').getByRole('link',{name:'American Red Cross',exact:true}).count(),1);
  await page.goBack();await page.getByRole('heading',{name:'Featured topics',exact:true}).waitFor();assert(page.url().includes(q));return {offering:'HA-001',provider:'ORG-001',path:'Existing offering inventory → Category Organizations association → provider offering card → browser back',inCardProviderLinkClaimed:false};
 });
 await test('T05-J4-collections',async()=>{
  await open('/category/housing'+q);const data=await api('/api/category/housing'+q);const map=Object.fromEntries(data.collections.map(c=>[c.key,c]));
  assert.deepEqual(map['policy-info'].items.map(i=>i.id),['NP-004','NP-005']);assert.deepEqual(map.flyers.items.map(i=>i.id),['FL-002']);assert(!map.law);
  const law=page.getByRole('button',{name:'Law',exact:true});await keyboard(law,'Space');assert.equal(await law.getAttribute('aria-pressed'),'true');assert(await law.evaluate(e=>document.activeElement===e));
  await page.getByRole('status').filter({hasText:'No current items in this collection.'}).waitFor();
  await keyboard(page.getByRole('button',{name:'Flyers',exact:true}));const flyers=page.getByRole('region',{name:'Flyers',exact:true});assert.equal(await flyers.getByRole('listitem').count(),1);
  const image=flyers.getByRole('img');await image.waitFor();await image.evaluate(i=>i.decode());assert(await image.evaluate(i=>i.naturalWidth>0));
  const media=await image.getAttribute('src');assert(media.startsWith('/images/seasonal/'));const mediaResponse=await page.request.get(origin+media);assert.equal(mediaResponse.status(),200);assert(mediaResponse.headers()['content-type'].startsWith('image/'));
  await keyboard(page.getByRole('button',{name:'First Rate News',exact:true}));const news=page.getByRole('region',{name:'First Rate News',exact:true});assert.equal(await news.getByRole('listitem').count(),2);
  await keyboard(news.getByRole('button',{name:'2 more',exact:true}));assert.equal(await news.getByRole('listitem').count(),4);
  for(const i of map['first-rate-news'].items.filter(i=>i.contentType==='EXPERT'))assert.equal(await news.getByRole('link',{name:i.title,exact:true}).count(),0);
  const original=map['first-rate-news'].items.find(i=>i.id==='OR-003');const link=news.getByRole('link',{name:original.title,exact:true});assert((await link.getAttribute('href')).includes('/originals/OR-003'+q));await keyboard(link);
  await page.getByRole('heading',{name:original.title,level:1,exact:true}).waitFor();assert(await page.locator('main h1').evaluate(e=>document.activeElement===e));await semantics('Article');
  await open('/originals/OR-003?communityId=bear-de');await page.getByRole('alert').waitFor();assert.equal(await page.getByRole('heading',{name:original.title,exact:true}).count(),0);
  return {policy:['NP-004','NP-005'],flyers:['FL-002'],firstRateNews:map['first-rate-news'].items.map(i=>i.id),law:'empty local cache',media,scopedArticleNotFound:true};
 });
 await test('T10-bear-empty-invalid',async()=>{
  await open('/category/food?communityId=bear-de');const data=await api('/api/category/food?communityId=bear-de');assert.equal(data.destination.communityId,'bear-de');
  await keyboard(page.locator('main a[href*="/category/food/help"]').first());await page.getByRole('heading',{name:'All Food Help',exact:true}).waitFor();
  assert.deepEqual((await api('/api/category/food/help?communityId=bear-de')).items.map(i=>i.id),['SD-032','SD-042']);
  const missingProvider=await api('/api/resources/SD-032?communityId=bear-de');assert.equal(missingProvider.organization_id,null);
  await open('/category/housing?communityId=unknown');await page.getByRole('status').filter({hasText:'No current help'}).waitFor();
  assert.equal((await api('/api/category/housing/help?communityId=unknown')).items.length,0);
  await open('/category/not-a-category?communityId=bear-de');await page.getByRole('alert').waitFor();await semantics('not found');return {bearIds:['SD-032','SD-042'],noProviderInvented:true,unknownEmpty:true,invalidNotFound:true};
 });
 await test('T13-responsive',async()=>{
  const measurements=[];
  for(const width of [375,1280,1440]){await page.setViewportSize({width,height:1000});await open('/category/housing'+q);
   const org=await page.getByRole('region',{name:'Organizations',exact:true}).boundingBox();const rail=await page.getByRole('complementary',{name:'More context',exact:true}).boundingBox();
   const help=await page.getByRole('region',{name:'Get help',exact:true}).boundingBox();const dimensions=await semantics('Housing '+width);
   if(width===375)assert(rail.y>=org.y+org.height);else {assert(rail.x>help.x);assert(help.width>rail.width);}
   await page.screenshot({path:out+`housing-${width}.png`,fullPage:true});await page.screenshot({path:out+`housing-${width}-top.png`});measurements.push({width,help,org,rail,...dimensions});
  }return measurements;
 });
 await test('T14-loading-and-network',async()=>{
  let release;const gate=new Promise(r=>{release=r;});
  await page.route('**/api/category/housing?*',async route=>{await gate;await route.continue();});
  await page.goto(origin+'/app-next/category/housing'+q);await page.getByRole('status').filter({hasText:'Loading'}).waitFor();release();
  await page.getByRole('heading',{name:'Featured topics',exact:true}).waitFor();await page.unroute('**/api/category/housing?*');
  await page.route('**/api/category/housing?*',route=>route.abort('connectionfailed'));
  await page.reload();const alert=page.getByRole('alert');await alert.waitFor();assert((await alert.innerText()).includes('We could not load help right now. Please try again.'));assert(!(await alert.innerText()).includes('ERR_'));
  await page.unroute('**/api/category/housing?*');await open('/category/housing/not-a-topic'+q);await semantics('Topic error');return {delayedRealResponse:true,networkAbortAnnounced:true,topicErrorH1:true,syntheticFaultsOnly:true};
 });
 await test('T15-spanish-high-contrast-fonts',async()=>{
  await page.setViewportSize({width:375,height:1000});await open('/category/housing'+q);
  await keyboard(page.getByRole('button',{name:'Switch language',exact:true}));assert.equal(await page.locator('html').getAttribute('lang'),'es');
  await page.getByRole('heading',{name:'Temas destacados',exact:true}).waitFor();await page.getByRole('heading',{name:'Qué debes saber',exact:true}).waitFor();await page.getByRole('complementary',{name:'Más contexto',exact:true}).waitFor();
  await keyboard(page.getByRole('button',{name:'Alternar alto contraste',exact:true}));await page.waitForTimeout(400);
  assert(await page.locator('body').evaluate(e=>e.classList.contains('high-contrast')));await semantics('Spanish high contrast');
  const pressed=await page.getByRole('button',{name:'Alternar alto contraste',exact:true}).getAttribute('aria-pressed');assert.equal(pressed,'true');
  await keyboard(page.getByRole('button',{name:'Leyes',exact:true}),'Space');const law=page.getByRole('button',{name:'Leyes',exact:true});
  const outline=await law.evaluate(e=>{const s=getComputedStyle(e);return {style:s.outlineStyle,width:s.outlineWidth,color:s.outlineColor,focusVisible:e.matches(':focus-visible')};});assert(outline.focusVisible&&outline.style==='solid');assert.equal(outline.color,'rgb(255, 255, 0)');
  await page.screenshot({path:out+'spanish-high-contrast-375.png',fullPage:true});await page.screenshot({path:out+'spanish-high-contrast-top.png'});
  await page.evaluate(()=>document.fonts.ready);const fonts=await page.evaluate(()=>({body:document.fonts.check('17px "Open Sans"'),heading:document.fonts.check('700 26px Montserrat'),faces:[...document.fonts].filter(f=>f.status==='loaded').map(f=>({family:f.family,weight:f.weight,status:f.status}))}));assert(fonts.body&&fonts.heading);assert(fonts.faces.some(f=>f.family.includes('Open Sans')));assert(fonts.faces.some(f=>f.family.includes('Montserrat')));
  await page.reload();await page.getByRole('heading',{name:'Temas destacados',exact:true}).waitFor();assert(await page.locator('body').evaluate(e=>e.classList.contains('high-contrast')));
  const cdp=await context.newCDPSession(page);await fs.writeFile(out+'final-spanish-ax-tree.json',JSON.stringify(await cdp.send('Accessibility.getFullAXTree'),null,2));
  await keyboard(page.getByRole('button',{name:'Cambiar idioma',exact:true}));await keyboard(page.getByRole('button',{name:'Toggle high contrast',exact:true}));
  return {documentLang:'es',diacritics:['Qué','Más'],outline,fonts,persistence:true,tool:'Chromium AX tree + DOM checks; axe/pa11y unavailable'};
 });
 await test('T13-synthetic-law-layout',async()=>{
  const real=await api('/api/category/housing'+q);real.collections.push({key:'law',labelKey:'collection.law',layer:'INFORMATION',count:1,items:[{contentType:'LAW',id:'SYNTHETIC-LAYOUT',title:'Synthetic legal context with a deliberately long title to verify wrapping without squeezing the primary reading column',summary:'Layout-only synthetic public DTO; not an admitted live law or new catalog record.',organization:null,location:null,cost:null,urgency:null,date:null,url:'https://example.org/law',imageUrl:null}]});
  await page.route('**/api/category/housing?*',route=>route.fulfill({json:{success:true,data:real,errorCode:null,errorMessage:null}}));
  const facts=[];for(const width of [375,1280]){await page.setViewportSize({width,height:1000});await open('/category/housing'+q);await keyboard(page.getByRole('button',{name:'Law',exact:true}));const law=page.getByRole('region',{name:'Law',exact:true});const box=await law.boundingBox();assert(box.width>250);await semantics('Synthetic law '+width);await page.screenshot({path:out+`synthetic-law-${width}.png`,fullPage:true});facts.push({width,columnWidth:box.width});}
  await page.unroute('**/api/category/housing?*');return {synthetic:true,notLiveLawCoverage:true,measurements:facts};
 });
 const result={status:checks.every(c=>c.status==='PASS')?'PASS':'FAIL',browser:await browser.version(),origin,checks,errors,assets,limitations:['Not a full WCAG audit','No physical device or assistive screen reader execution','External provider reachability/factual accuracy not certified','Synthetic loading/network and Law layout cases explicitly labeled']};
 assert.deepEqual(errors,[]);await fs.writeFile(out+'browser-result.json',JSON.stringify(result,null,2));console.log(JSON.stringify(checks,null,2));assert.equal(result.status,'PASS');
}finally{await browser.close();}
