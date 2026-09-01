#!/usr/bin/env bash
# Rebuilds the offline preview and both print PDFs from the committed sources.
#
#   ./build.sh
#
# Requires: node with playwright-core, and a Chromium binary.
#   CHROME=/path/to/chrome ./build.sh
set -euo pipefail
cd "$(dirname "$0")"
CHROME="${CHROME:-/opt/pw-browsers/chromium-1194/chrome-linux/chrome}"

# preview.html — index.html with the web fonts served from ./fonts instead of Google Fonts,
# so the profile renders correctly with no network access.
sed 's#<link href="https://fonts.googleapis.com/css2.*rel="stylesheet">#<link rel="stylesheet" href="fonts/fonts.css">#' \
  index.html > preview.html

node - "$CHROME" <<'JS'
const { chromium } = require('playwright-core');
const chrome = process.argv[2];
const dir = process.cwd() + '/';
(async () => {
  const b = await chromium.launch({ executablePath: chrome, args: ['--no-sandbox'] });
  const url = 'file://' + dir + 'print.html';

  // Press build: A4 trim + 3mm bleed + corner crop marks.
  const p = await b.newPage();
  await p.goto(url);
  await p.evaluate(() => {
    document.querySelectorAll('.furniture,.marks').forEach(e => e.remove());
    const pages = document.querySelectorAll('.page'), total = pages.length;
    pages.forEach((page, i) => {
      const f = document.createElement('div');
      f.className = 'furniture';
      f.innerHTML = '<span>JS Law Firm — Cairo</span>' +
        '<span>Regulatory position verified 31 August 2026</span>' +
        '<span class="pn">' + (i + 1) + ' / ' + total + '</span>';
      page.appendChild(f);
      const m = document.createElement('div');
      m.className = 'marks';
      [['h','top:3mm;left:0'],['v','top:0;left:3mm'],['h','top:3mm;right:0'],['v','top:0;right:3mm'],
       ['h','bottom:3mm;left:0'],['v','bottom:0;left:3mm'],['h','bottom:3mm;right:0'],['v','bottom:0;right:3mm']]
        .forEach(x => { const s = document.createElement('span'); s.className = x[0]; s.setAttribute('style', x[1]); m.appendChild(s); });
      page.appendChild(m);
    });
  });
  await p.pdf({ path: dir + 'JS-Law-Firm-Profile-A4-bleed.pdf', width: '216mm', height: '303mm',
                printBackground: true, margin: { top:'0', bottom:'0', left:'0', right:'0' } });

  // Desk build: plain A4, no bleed, no crop marks.
  const t = await b.newPage();
  await t.goto(url);
  await t.addStyleTag({ content: ':root{--bleed:0mm}.page{width:210mm;height:297mm;margin-bottom:0}' });
  await t.waitForTimeout(200);
  await t.pdf({ path: dir + 'JS-Law-Firm-Profile-A4.pdf', width: '210mm', height: '297mm',
                printBackground: true, margin: { top:'0', bottom:'0', left:'0', right:'0' } });

  await b.close();
})();
JS
echo "built: preview.html, JS-Law-Firm-Profile-A4.pdf, JS-Law-Firm-Profile-A4-bleed.pdf"
