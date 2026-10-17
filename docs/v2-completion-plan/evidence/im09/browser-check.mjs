import { chromium } from '/Users/composedmedia/.npm/_npx/e41f203b7505f1fb/node_modules/playwright/index.mjs';
import fs from 'node:fs/promises';
import assert from 'node:assert/strict';
const out = new URL('./', import.meta.url).pathname;
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
const errors = []; page.on('pageerror', e => errors.push(e.message));
const checks = [];
try {
  await page.goto('http://127.0.0.1:4179/app-next/category/housing?communityId=wilmington-de');
  await page.getByRole('heading', { name: 'Featured topics', exact: true }).waitFor();
  const button = page.getByRole('button', { name: 'First Rate News', exact: true });
  await button.focus(); await button.press('Enter');
  const region = page.locator('#active-collection');
  await region.getByRole('button', { name: /more$/ }).click();
  assert.equal(await region.getByRole('listitem').count(), 4);
  const response = await page.request.get('http://127.0.0.1:4179/api/category/housing?communityId=wilmington-de');
  const body = await response.json();
  const items = body.data.collections.find(c => c.key === 'first-rate-news').items;
  assert.deepEqual(items.map(i => i.id), ['OR-003', 'FAQ-002', 'EA-001', 'EA-005']);
  for (const item of items) {
    const title = region.getByText(item.title, { exact: true });
    assert.equal(await title.count(), 1);
    if (item.contentType === 'EXPERT') assert.equal(await region.getByRole('link', { name: item.title, exact: true }).count(), 0);
  }
  assert.equal(await region.locator('a[href*="/originals/EA-"]').count(), 0);
  assert.equal(await region.locator('a[href*="/originals/FAQ-"]').count(), 0);
  for (const width of [375, 1280]) {
    await page.setViewportSize({ width, height: 1000 });
    const metrics = await page.evaluate(() => ({ width: innerWidth, scroll: document.documentElement.scrollWidth }));
    assert(metrics.scroll <= width); checks.push(metrics);
    await page.screenshot({ path: out + `first-rate-news-${width}.png`, fullPage: true });
  }
  await region.getByRole('link', { name: items[0].title, exact: true }).click();
  await page.getByRole('heading', { name: items[0].title, exact: true, level: 1 }).waitFor();
  assert(page.url().includes('/originals/OR-003?communityId=wilmington-de'));
  assert.deepEqual(errors, []);
  await fs.writeFile(out + 'browser-result.json', JSON.stringify({ status: 'PASS', browser: await browser.version(),
    membership: items.map(i => i.id), checks, articleUrl: page.url(), errors,
    limitation: 'Focused built-app keyboard, membership, navigation and layout smoke; not exhaustive accessibility verification.' }, null, 2));
} finally { await browser.close(); }
