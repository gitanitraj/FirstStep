import { chromium } from '/Users/composedmedia/.npm/_npx/e41f203b7505f1fb/node_modules/playwright/index.mjs';
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const out=new URL('./',import.meta.url).pathname;
const browser=await chromium.launch({headless:true});
const page=await browser.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
const checks=[];
try {
 await page.goto('http://127.0.0.1:4180/app-next/community-notices/events?communityId=wilmington-de');
 await page.getByRole('heading',{name:'Community Notices',exact:true}).waitFor();
 const response=await page.request.get('http://127.0.0.1:4180/api/community-notices/events?communityId=wilmington-de');
 const events=(await response.json()).data;
 await page.waitForFunction(()=>!document.body.innerText.includes('Loading'));
 for(const item of events.items) assert.equal(await page.getByText(item.title,{exact:true}).count(),1);
 const nav=page.getByRole('navigation',{name:'Community Notices views',exact:true});
 for(const link of await nav.getByRole('link').all()) assert((await link.getAttribute('href')).includes('communityId=wilmington-de'));
 const flyerLink=nav.getByRole('link',{name:/Flyers/});await flyerLink.focus();await flyerLink.press('Enter');
 await page.getByRole('img',{name:'United Neighbors 2026',exact:true}).waitFor();
 assert(page.url().includes('/community-notices/flyers?communityId=wilmington-de'));
 const image=page.getByRole('img',{name:'United Neighbors 2026',exact:true});
 assert.equal(await image.getAttribute('src'),'/images/seasonal/United%20Neighbors%202026.jpg');
 await image.scrollIntoViewIfNeeded();
 await page.waitForFunction(()=>[...document.images].filter(i=>i.alt==='United Neighbors 2026').every(i=>i.complete&&i.naturalWidth>0));
 for(const width of [375,1280]) {
  await page.setViewportSize({width,height:1000});
  const metrics=await page.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth}));
  assert(metrics.scroll<=width);checks.push(metrics);
  await page.screenshot({path:out+`flyers-${width}.png`,fullPage:true});
 }
 const contrast=page.getByRole('button',{name:/contrast/i});await contrast.click();assert.equal(await contrast.getAttribute('aria-pressed'),'true');
 await page.getByRole('button',{name:'Switch language',exact:true}).click();
 await page.getByRole('link',{name:/Folletos/}).waitFor();
 assert(page.url().includes('communityId=wilmington-de'));
 await page.screenshot({path:out+'flyers-spanish-contrast.png',fullPage:true});
 assert.deepEqual(errors,[]);
 await fs.writeFile(out+'browser-result.json',JSON.stringify({status:'PASS',browser:await browser.version(),checks,eventIds:events.items.map(i=>i.id),
  flyerMedia:await image.getAttribute('src'),keyboardViewNavigation:true,communityPreserved:true,spanish:true,highContrast:true,errors,
  limitation:'Focused local built-app smoke; not exhaustive WCAG or physical-device verification.'},null,2));
} finally {await browser.close();}
