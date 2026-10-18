import { chromium } from '/Users/composedmedia/.npm/_npx/e41f203b7505f1fb/node_modules/playwright/index.mjs';
import fs from 'node:fs/promises';
const out=(process.env.IM13_OUTPUT_DIR??new URL('./',import.meta.url).pathname).replace(/\/?$/, '/');
const origin=process.env.IM13_ORIGIN??'http://127.0.0.1:8113';
const phase=process.argv[2]??'before';
const browser=await chromium.launch({headless:true});const page=await browser.newPage({viewport:{width:1280,height:1000}});
const observations=[];
async function check(id,fn){try{observations.push({id,status:'PASS',facts:await fn()});}catch(e){observations.push({id,status:'FAIL',error:e.message});}}
function require(value,message){if(!value)throw Error(message);}
async function ready(path){await page.goto(origin+'/app-next'+path);await page.locator('main h1, main [role="alert"]').first().waitFor();}
async function contrast(){return page.locator('main').evaluate(main=>{
 const rgb=s=>(s.match(/[\d.]+/g)||[]).map(Number);const lum=c=>c.slice(0,3).map(n=>{n/=255;return n<=.04045?n/12.92:((n+.055)/1.055)**2.4;}).reduce((v,n,i)=>v+n*[.2126,.7152,.0722][i],0);
 const facts=[];
 for(const e of main.querySelectorAll('*')){
  if(!Array.from(e.childNodes).some(n=>n.nodeType===3&&n.textContent.trim()))continue;
  const box=e.getBoundingClientRect(),s=getComputedStyle(e);if(!box.width||!box.height||s.visibility==='hidden')continue;
  let p=e,bg;while(p){const c=rgb(getComputedStyle(p).backgroundColor);if(c.length===3||c[3]===1){bg=c;break;}p=p.parentElement;}
  if(!bg)bg=[255,255,255];const color=rgb(s.color);const a=lum(color),b=lum(bg);const ratio=(Math.max(a,b)+.05)/(Math.min(a,b)+.05);
  const large=parseFloat(s.fontSize)>=24||(parseFloat(s.fontSize)>=18.66&&parseInt(s.fontWeight)>=700);const threshold=large?3:4.5;
  if(ratio<threshold)facts.push({text:e.textContent.trim().slice(0,80),tag:e.tagName,color:s.color,background:bg,ratio,threshold});
 }return facts;
 });}
try{
 await check('article-explicit-community',async()=>{await ready('/originals/OR-003?communityId=bear-de');const alert=page.getByRole('alert');require(await alert.count()===1,'Wrong-community Article renders approved Wilmington body; frontend omitted scope');return {wrongOwnerNotFound:true};});
 await check('article-route-entry-focus',async()=>{await ready('/originals/OR-003?communityId=wilmington-de');const tag=await page.evaluate(()=>document.activeElement.tagName);require(tag==='H1','Article focus is '+tag+' instead of its heading');return {tag};});
 await check('provider-route-entry-focus',async()=>{await ready('/organization/american-red-cross?communityId=wilmington-de');await page.getByRole('heading',{name:'American Red Cross',level:1,exact:true}).waitFor();const tag=await page.evaluate(()=>document.activeElement.tagName);require(tag==='H1','Provider focus is '+tag+' instead of its heading');return {tag};});
 await check('topic-notfound-heading',async()=>{await ready('/category/housing/not-a-topic?communityId=wilmington-de');require(await page.locator('main h1').count()===1,'Topic not-found main has no h1');return {alert:true};});
 for(const mode of ['normal','high-contrast']){
  await ready('/category/housing?communityId=wilmington-de');if(mode==='high-contrast'){await page.getByRole('button',{name:'Toggle high contrast'}).click();await page.waitForTimeout(400);}
  await page.getByRole('button',{name:'Flyers',exact:true}).click();
  const bad=await contrast();observations.push({id:mode+'-housing-flyer-text-contrast',status:bad.length?'FAIL':'PASS',facts:bad});
  const law=page.getByRole('button',{name:'Law',exact:true});await law.focus();await law.press('Tab');await page.keyboard.press('Shift+Tab');
  const focus=await law.evaluate(e=>{const s=getComputedStyle(e);return {color:s.outlineColor,style:s.outlineStyle,width:s.outlineWidth,keyboardVisible:e.matches(':focus-visible')};});
  observations.push({id:mode+'-focus-style',status:focus.style!=='none'?'PASS':'FAIL',facts:focus});
  if(mode==='high-contrast'){await page.locator('main li a').first().hover();await page.waitForTimeout(400);const hover=await contrast();observations.push({id:'high-contrast-card-hover',status:hover.length?'FAIL':'PASS',facts:hover});}
  await page.screenshot({path:out+phase+'-'+mode+'.png',fullPage:true});
  if(mode==='high-contrast'){await page.getByRole('button',{name:'Toggle high contrast'}).click();await page.waitForTimeout(400);}
 }
 const cdp=await page.context().newCDPSession(page);const ax=await cdp.send('Accessibility.getFullAXTree');
 await fs.writeFile(out+phase+'-ax-tree.json',JSON.stringify(ax,null,2));
 await fs.writeFile(out+'browser-probe-'+phase+'.json',JSON.stringify({phase,browser:await browser.version(),observations},null,2));
 console.log(JSON.stringify(observations,null,2));
}finally{await browser.close();}
