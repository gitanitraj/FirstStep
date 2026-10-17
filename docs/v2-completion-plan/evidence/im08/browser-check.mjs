import { chromium } from '/Users/composedmedia/.npm/_npx/e41f203b7505f1fb/node_modules/playwright/index.mjs';
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const out = new URL('./', import.meta.url).pathname;
const base = 'http://127.0.0.1:4178';
const path = '/app-next/category/housing?communityId=wilmington-de';
const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
const sourceRequests = []; context.on('request', r => sourceRequests.push(r.url()));
const page = await context.newPage();
const errors = []; page.on('pageerror', error => errors.push(error.message));
const results = { browser: await browser.version(), viewportChecks: [], journeys: [], limitations: 'Browser smoke and measured layout, not exhaustive WCAG or provider website audit.' };
try {
  await page.goto(base + path);
  await page.getByRole('heading', { name: 'Featured topics', exact: true }).waitFor();
  const response = await page.request.get(base + '/api/category/housing?communityId=wilmington-de');
  assert.equal(response.status(), 200); const api = await response.json();
  await fs.writeFile(out + 'packaged-category-api.json', JSON.stringify(api, null, 2));
  const featured = page.getByRole('region', { name: 'Featured topics', exact: true });
  assert.deepEqual(await featured.getByRole('heading', { level: 3 }).allTextContents(), ['Transitional Housing', 'Emergency Shelter', 'Homeownership', 'Rental Assistance']);
  assert.equal(await featured.getByRole('listitem').count(), 12);
  assert(!/FALLBACK|DURABLE|TIMELY|VISUAL/.test(await featured.innerText()));
  const expectedLinks = api.data.destination.featuredTopics.flatMap(t => t.items.map(p => p.href));
  assert.deepEqual(await featured.locator('li a').evaluateAll(links => links.map(a => a.getAttribute('href'))), expectedLinks);
  for (const width of [375, 1280, 1440]) {
    await page.setViewportSize({ width, height: 1000 }); await page.evaluate(() => document.fonts.ready);
    const metrics = await page.evaluate(() => {
      const rail = document.querySelector('aside[aria-labelledby="context-title"]');
      const primary = document.querySelector('section[aria-labelledby="organizations-title"]');
      const list = document.querySelector('section[aria-labelledby="featured-title"] ul');
      return { width: innerWidth, scrollWidth: document.documentElement.scrollWidth,
        rail: rail.getBoundingClientRect().toJSON(), primaryBottom: primary.getBoundingClientRect().bottom,
        featuredCardWidth: list.getBoundingClientRect().width };
    });
    assert(metrics.scrollWidth <= width, JSON.stringify(metrics));
    if (width === 375) assert(metrics.rail.top >= metrics.primaryBottom, 'rail follows primary on mobile');
    assert(metrics.featuredCardWidth >= 200, 'cards retain readable width');
    await page.screenshot({ path: out + `housing-${width}.png`, fullPage: true });
    results.viewportChecks.push(metrics);
  }
  // Actual keyboard activation opens the card's original source. No remote content certification.
  const sourceLink = featured.locator('li a').first();
  await sourceLink.focus(); assert.equal(await sourceLink.evaluate(el => el === document.activeElement), true);
  const popupPromise = page.waitForEvent('popup'); await sourceLink.press('Enter');
  const popup = await popupPromise; await popup.waitForURL(url => url.href.startsWith(expectedLinks[0]), { timeout: 10000 }).catch(() => {});
  results.journeys.push({ action: 'keyboard featured source click', expected: expectedLinks[0], opened: popup.url() });
  assert(popup.url().startsWith(expectedLinks[0]) || sourceRequests.includes(expectedLinks[0])); await popup.close();
  await featured.getByRole('link', { name: 'Transitional Housing', exact: true }).click();
  await page.getByRole('heading', { name: 'Transitional Housing', exact: true, level: 1 }).waitFor();
  assert(page.url().includes('communityId=wilmington-de')); await page.reload();
  await page.getByRole('heading', { name: 'Transitional Housing', exact: true, level: 1 }).waitFor();
  await page.goBack(); await page.getByRole('heading', { name: 'Featured topics', exact: true }).waitFor();
  results.journeys.push({ action: 'featured Topic, refresh, browser back', final: page.url() });
  await page.getByRole('region', { name: 'Get help', exact: true }).getByRole('link', { name: 'All Housing Help' }).click();
  await page.getByRole('heading', { name: 'All Housing Help', exact: true, level: 1 }).waitFor();
  const helpRegion = page.getByRole('region', { name: 'Get help', exact: true });
  assert.equal(await helpRegion.getByRole('listitem').count(), 4);
  await helpRegion.getByRole('button', { name: /more$/ }).click();
  assert.equal(await helpRegion.getByRole('listitem').count(), 36);
  assert(page.url().includes('communityId=wilmington-de'));
  results.journeys.push({ action: 'All Help full expansion', visible: 36, url: page.url() });
  await page.goBack(); await page.getByRole('heading', { name: 'Featured topics', exact: true }).waitFor();
  await page.getByRole('button', { name: 'Flyers', exact: true }).click();
  const flyer = page.getByRole('region', { name: 'What to know', exact: true });
  assert.equal(await flyer.getByRole('img').count(), 1);
  const image = flyer.getByRole('img').first(); await image.scrollIntoViewIfNeeded();
  await expectImage(image); results.journeys.push({ action: 'contextual unactivated Flyer', src: await image.getAttribute('src') });
  await page.getByRole('button', { name: 'First Rate News', exact: true }).click();
  const article = flyer.getByRole('link').first(); await article.click();
  await page.locator('article').waitFor(); assert(page.url().includes('/originals/OR-003?communityId=wilmington-de'));
  results.journeys.push({ action: 'approved original article read', url: page.url() });
  await page.goBack(); await page.getByRole('heading', { name: 'Featured topics', exact: true }).waitFor();
  const org = page.getByRole('region', { name: 'Organizations', exact: true }).getByRole('link').first();
  const orgHref = await org.getAttribute('href'); await org.click();
  await page.getByRole('heading', { name: 'What they offer', level: 2 }).waitFor().catch(() => {});
  assert(page.url().includes('communityId=wilmington-de')); results.journeys.push({ action: 'scoped organization', href: orgHref });
  await page.goto(base + path); await page.getByRole('heading', { name: 'Featured topics', exact: true }).waitFor();
  await page.setViewportSize({ width: 375, height: 1000 });
  const switcher = page.getByRole('navigation', { name: 'Choose a civic information collection' });
  const lawButton = switcher.getByRole('button', { name: 'Law', exact: true }); await lawButton.focus(); await lawButton.press('Enter');
  assert.equal(await lawButton.getAttribute('aria-pressed'), 'true');
  const focus = await lawButton.evaluate(el => ({ outline: getComputedStyle(el).outlineStyle, width: getComputedStyle(el).outlineWidth }));
  assert.notEqual(focus.outline, 'none'); results.keyboardFocus = focus;
  await page.getByRole('button', { name: 'Toggle high contrast' }).click();
  assert(await page.locator('body').evaluate(el => el.classList.contains('high-contrast')));
  await page.screenshot({ path: out + 'housing-375-high-contrast.png', fullPage: true });
  await page.getByRole('button', { name: 'Switch language' }).click();
  await page.getByRole('heading', { name: 'Temas destacados', exact: true }).waitFor();
  assert.equal(await page.locator('html').getAttribute('lang'), 'es');
  await page.screenshot({ path: out + 'housing-375-spanish.png', fullPage: true });
  await page.getByRole('button', { name: /idioma/i }).click();
  await page.goto(base + '/app-next/category/housing/eviction-prevention?communityId=wilmington-de');
  await page.getByRole('heading', { name: 'Eviction Prevention', exact: true, level: 1 }).waitFor();
  assert(await page.getByRole('status').textContent());
  await page.goto(base + '/app-next/category/food?communityId=bear-de');
  await page.getByRole('heading', { name: 'Featured topics', exact: true }).waitFor();
  assert.equal(await page.getByRole('region', { name: 'Featured topics', exact: true }).getByRole('heading', { level: 3 }).first().textContent(), 'Food Pantry');
  results.journeys.push({ action: 'real alternate Category/Community', url: page.url() });
  await page.goto(base + '/app-next/category/unknown?communityId=wilmington-de'); await page.getByRole('alert').waitFor();
  results.journeys.push({ action: 'unknown Category accessible alert' });
  assert.deepEqual(errors, []); results.pageErrors = errors; results.status = 'PASS';
} catch (error) { results.status = 'FAIL'; results.error = String(error); await page.screenshot({ path: out + 'browser-failure.png', fullPage: true }); throw error; }
finally { await fs.writeFile(out + 'browser-results.json', JSON.stringify(results, null, 2)); await browser.close(); }
async function expectImage(locator) { await locator.evaluate(async img => { if (!img.complete) await new Promise(resolve => { img.onload = resolve; img.onerror = resolve; }); }); assert(await locator.evaluate(img => img.naturalWidth > 0)); }
