/*
 * Annotated reference: docs/v2-completion-plan/evidence/im12/browser-check.mjs
 * WHAT: Reproduces the read-only packaged/browser or deterministic audit evidence; environment paths and cleanup are explicit.
 * WHY: IM-12 fixes demonstrated identity/public-boundary gaps while keeping catalog data, selectors and internal ranking unchanged.
 * INTERACTIONS: Consumes captured test evidence or temporary copied catalogs; local browser/HTTP processes are terminated after verification.
 * ALTERNATIVES: A generalized relationship graph, blanket field blacklist, inferred provider/host or universal default Community would exceed or violate the approved contract.
 */
// BEGIN MIRRORED SOURCE
import { chromium } from '/Users/composedmedia/.npm/_npx/e41f203b7505f1fb/node_modules/playwright/index.mjs';
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const out=new URL('./',import.meta.url).pathname;
const browser=await chromium.launch({headless:true});const page=await browser.newPage();
const errors=[];page.on('pageerror',e=>errors.push(e.message));const checks=[];
try {
 await page.goto('http://127.0.0.1:4182/app-next/category/housing?communityId=wilmington-de');
 await page.getByRole('heading',{name:'Featured topics',exact:true}).waitFor();
 const response=await page.request.get('http://127.0.0.1:4182/api/category/housing?communityId=wilmington-de');
 assert.equal(response.status(),200);const api=(await response.json()).data;
 const featured=page.getByRole('region',{name:'Featured topics',exact:true});
 assert.deepEqual(await featured.getByRole('heading',{level:3}).allTextContents(),['Transitional Housing','Emergency Shelter','Homeownership','Rental Assistance']);
 const positions=api.destination.featuredTopics.flatMap(t=>t.items);const cards=featured.getByRole('listitem');
 assert.equal(await cards.count(),12);
 for(let i=0;i<positions.length;i++) {
  assert.equal(await cards.nth(i).getByText(positions[i].item.title,{exact:true}).count(),1);
  assert.equal(await cards.nth(i).getByRole('link',{name:positions[i].item.title,exact:true}).getAttribute('href'),positions[i].href);
 }
 assert.deepEqual(positions.map(p=>p.item.id),['HA-002','HA-003','HA-005','HA-016','HA-001','HA-006','HA-011','HA-013','HA-021','HA-008','HA-023','HA-026']);
 assert(!/TIMELY|DURABLE|VISUAL|FALLBACK/.test(await featured.innerText()));
 for(const width of [375,1280]) {
  await page.setViewportSize({width,height:1000});
  const metrics=await page.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth}));
  assert(metrics.scroll<=width);checks.push(metrics);
  await page.screenshot({path:out+`housing-${width}.png`,fullPage:true});
 }
 const topic=featured.getByRole('link',{name:'Transitional Housing',exact:true});await topic.focus();await topic.press('Enter');
 await page.getByRole('heading',{name:'Transitional Housing',level:1,exact:true}).waitFor();
 assert(page.url().includes('/transitional-housing?communityId=wilmington-de'));
 await page.reload();await page.getByRole('heading',{name:'Transitional Housing',level:1,exact:true}).waitFor();
 await page.goBack();await page.getByRole('heading',{name:'Featured topics',exact:true}).waitFor();
 assert(page.url().includes('communityId=wilmington-de'));
 // J3 provider destination keeps the Category's requested owner through click, refresh and back.
 await page.goto('http://127.0.0.1:4182/app-next/category/housing?communityId=wilmington-de');
 const provider=page.locator('a[href*="/organization/"]').first();await provider.waitFor();
 const providerHref=await provider.getAttribute('href');assert(providerHref.includes('communityId=wilmington-de'));
 await provider.focus();await provider.press('Enter');await page.getByRole('heading',{level:1}).waitFor();
 assert(page.url().includes('/organization/')&&page.url().includes('communityId=wilmington-de'));
 const providerApi=(await (await page.request.get('http://127.0.0.1:4182/api'+new URL(page.url()).pathname.replace('/app-next','')+'?communityId=wilmington-de')).json()).data;
 await page.getByRole('heading',{name:providerApi.name,level:1,exact:true}).waitFor();
 for(const r of providerApi.resources)assert.equal(await page.locator('main li').getByText(r.title,{exact:true}).count(),1);
 await page.reload();await page.getByRole('heading',{name:providerApi.name,level:1,exact:true}).waitFor();
 await page.goBack();await page.getByRole('heading',{name:'Featured topics',exact:true}).waitFor();
 await page.goto('http://127.0.0.1:4182/app-next/category/food?communityId=bear-de');
 await page.getByRole('heading',{name:'Food',level:1,exact:true}).waitFor();
 const help=page.locator('a[href*="/category/food/help"]').first();await help.waitFor();
 assert((await help.getAttribute('href')).includes('communityId=bear-de'));await help.click();
 await page.getByText('Alpha Worship Center - Pantry of Hope',{exact:true}).waitFor();
 assert.equal(await page.getByText('Red Lion United Methodist Church',{exact:true}).count(),1);
 assert(page.url().includes('communityId=bear-de'));
 await page.screenshot({path:out+'bear-food.png',fullPage:true});
 assert.deepEqual(errors,[]);
 await fs.writeFile(out+'browser-result.json',JSON.stringify({status:'PASS',browser:await browser.version(),checks,
  ids:positions.map(p=>p.item.id),roles:positions.map(p=>p.role),hrefs:positions.map(p=>p.href),keyboardTopicNavigation:true,
  refreshAndBack:true,communityPreserved:true,providerHref,providerId:providerApi.id,providerResourceIds:providerApi.resources.map(r=>r.id),bearFoodIds:["SD-032","SD-042"],noSemanticRoleBadges:true,errors,
  limitation:'Focused built-app integration; not exhaustive WCAG, physical devices or external source reachability.'},null,2));
} finally {await browser.close();}
