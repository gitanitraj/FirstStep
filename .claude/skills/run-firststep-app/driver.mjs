// REPL driver for the First Step app (a static HTML/JS frontend served by
// the Spring Boot backend at http://localhost:8080). Designed for agents:
// wrap in tmux, send-keys commands, capture-pane output.
import { chromium } from 'playwright';
import * as readline from 'node:readline';
import * as fs from 'node:fs';
import * as path from 'node:path';

const APP_URL = process.env.APP_URL || 'http://localhost:8080';
const SHOT_DIR = process.env.SCREENSHOT_DIR || '/tmp/firststep-shots';
fs.mkdirSync(SHOT_DIR, { recursive: true });

let browser = null;
let page = null;
const consoleLog = [];

const COMMANDS = {
  async launch() {
    if (browser) return console.log('already launched');
    browser = await chromium.launch({ headless: true });
    const context = await browser.newContext();
    page = await context.newPage();
    page.on('console', msg => consoleLog.push({ type: msg.type(), text: msg.text() }));
    page.on('pageerror', err => consoleLog.push({ type: 'pageerror', text: err.message }));
    await page.goto(APP_URL, { waitUntil: 'domcontentloaded', timeout: 15_000 });
    console.log('launched.', APP_URL);
  },

  async nav(url) {
    if (!page) return console.log('ERROR: launch first');
    const target = url && url.startsWith('http') ? url : `${APP_URL}${url ? '/' + url.replace(/^\//, '') : ''}`;
    await page.goto(target, { waitUntil: 'domcontentloaded', timeout: 15_000 });
    console.log('nav →', target);
  },

  async ss(name) {
    if (!page) return console.log('ERROR: launch first');
    const f = path.join(SHOT_DIR, (name || `ss-${Date.now()}`) + '.png');
    await page.screenshot({ path: f, fullPage: true });
    console.log('screenshot:', f);
  },

  async click(sel) {
    if (!page) return console.log('ERROR: launch first');
    try { await page.click(sel, { timeout: 5_000 }); console.log('click', sel, '→ OK'); }
    catch (e) { console.log('click', sel, '→ ERROR:', e.message.split('\n')[0]); }
  },

  async 'click-text'(text) {
    if (!page) return console.log('ERROR: launch first');
    const r = await page.evaluate(t => {
      const els = [...document.querySelectorAll('button, a, [role="button"], .filter-chip, .news-item, .resource-card')];
      const el = els.find(e => e.textContent?.trim() === t)
              ?? els.find(e => e.textContent?.includes(t));
      if (!el) return 'NOT_FOUND';
      el.click(); return 'OK: ' + el.tagName + (el.className ? '.' + el.className.split(' ')[0] : '');
    }, text);
    console.log('click-text', JSON.stringify(text), '→', r);
  },

  async type(text) { if (page) await page.keyboard.type(text, { delay: 20 }); },
  async press(key) { if (page) await page.keyboard.press(key); },

  async fill(args) {
    if (!page) return console.log('ERROR: launch first');
    const [sel, ...rest] = args.split(' ');
    try { await page.fill(sel, rest.join(' '), { timeout: 5_000 }); console.log('fill', sel, '→ OK'); }
    catch (e) { console.log('fill', sel, '→ ERROR:', e.message.split('\n')[0]); }
  },

  async wait(sel) {
    if (!page) return console.log('ERROR: launch first');
    try { await page.waitForSelector(sel, { timeout: 10_000 }); console.log('found:', sel); }
    catch { console.log('TIMEOUT:', sel); }
  },

  async 'wait-text'(text) {
    if (!page) return console.log('ERROR: launch first');
    try {
      await page.waitForFunction(t => document.body.innerText.includes(t), text, { timeout: 10_000 });
      console.log('found text:', text);
    } catch { console.log('TIMEOUT waiting for text:', text); }
  },

  async eval(expr) {
    if (!page) return console.log('ERROR: launch first');
    try { console.log(JSON.stringify(await page.evaluate(expr))); }
    catch (e) { console.log('ERROR:', e.message); }
  },

  async text(sel) {
    if (!page) return console.log('ERROR: launch first');
    console.log(await page.evaluate(
      s => (s ? document.querySelector(s) : document.body)?.innerText ?? '(null)',
      sel || null));
  },

  async console(filter) {
    const entries = filter === '--errors'
      ? consoleLog.filter(e => e.type === 'error' || e.type === 'pageerror')
      : consoleLog;
    if (entries.length === 0) { console.log('(no console output' + (filter === '--errors' ? ', no errors' : '') + ')'); return; }
    entries.forEach(e => console.log(`[${e.type}] ${e.text}`));
  },

  async quit() { if (browser) await browser.close().catch(() => {}); browser = null; page = null; },
  help() { console.log('commands:', Object.keys(COMMANDS).join(', ')); },
};

const rl = readline.createInterface({ input: process.stdin, output: process.stdout, prompt: 'driver> ' });

// WHY: readline auto-closes as soon as piped/heredoc stdin hits EOF —
// which happens almost immediately, well before a multi-command script's
// earlier async commands (e.g. "launch") finish. By the time drain()'s
// loop reaches later commands, the interface may already be closed, and
// rl.prompt() throws ERR_USE_AFTER_CLOSE. Prompting is purely cosmetic, so
// swallow that specific case rather than let it crash the driver.
function safePrompt() { try { rl.prompt(); } catch { /* already closed (EOF) */ } }

// WHY a queue instead of rl.on('line', async ...) directly: readline
// buffers and emits 'line' events for every piped line synchronously
// (e.g. a whole heredoc arrives as one chunk), faster than an async
// command handler can await its Playwright calls — pause()/resume() on
// the readline interface does NOT reliably stop already-buffered lines
// from firing. Queueing lines and draining them one at a time with an
// explicit "busy" flag guarantees "launch" finishes before "ss" starts,
// regardless of how fast the lines arrive.
const queue = [];
let drainPromise = null;

// WHY drainPromise instead of a plain boolean: 'close' fires as soon as
// piped stdin hits EOF, which — for `echo`/heredoc input — is immediately
// after the last line is queued, often well before that line's async
// command (e.g. a Playwright page.goto) has actually finished. 'close'
// must await the SAME in-flight drain() call, not start a fresh one,
// or it races ahead and process.exit()s mid-launch.
function drain() {
  if (drainPromise) return drainPromise;
  drainPromise = (async () => {
    while (queue.length > 0) {
      const line = queue.shift();
      const [cmd, ...rest] = line.trim().split(/\s+/);
      if (!cmd) { safePrompt(); continue; }
      const fn = COMMANDS[cmd];
      if (!fn) { console.log('unknown:', cmd, '— try: help'); safePrompt(); continue; }
      try { await fn(rest.join(' ')); } catch (e) { console.log('ERROR:', e.message); }
      if (cmd === 'quit') { rl.close(); }
      else safePrompt();
    }
    drainPromise = null;
  })();
  return drainPromise;
}

rl.on('line', line => { queue.push(line); drain(); });
rl.on('close', async () => { await drain(); await COMMANDS.quit(); process.exit(0); });

console.log('First Step app driver — "help" for commands, "launch" to start');
safePrompt();
