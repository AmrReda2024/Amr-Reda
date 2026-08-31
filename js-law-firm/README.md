# JS Law Firm — company profile

Two deliverables built from one design system:

| File | What it is |
| --- | --- |
| `index.html` | The interactive profile. Single file — the founder's photograph is embedded as a data URI; only the web fonts are fetched from Google Fonts. Open it directly in a browser. |
| `JS-Law-Firm-Profile-A4.pdf` | 10-page A4 print build, no bleed. The version to email or print on a desk printer. |
| `JS-Law-Firm-Profile-A4-bleed.pdf` | Same 10 pages at A4 trim plus 3 mm bleed and corner crop marks, for a commercial printer. |
| `print.html` | Source of both PDFs. Paginated A4 layout, fonts loaded from `fonts/`. |
| `build.sh` | Regenerates `preview.html` and both PDFs. |
| `fonts/` | Newsreader and IBM Plex Sans (latin subsets), so the print build renders without network access. |
| `jacqueline-saad.jpg` | Source photograph. |

## Rebuilding

```sh
npm install playwright-core          # once, in this directory
CHROME=/path/to/chromium ./build.sh
```

`build.sh` also writes `preview.html` — `index.html` with the fonts served from `fonts/`
instead of Google Fonts, for viewing the interactive profile offline.

## Design notes

- **Palette.** Field green `#0E3129` (cover, German Desk, contact), field deep `#0A231D`
  (entry sequence, selected matters), paper `#FBFAF7`, stone `#DEDCD3` / `#EFEDE6`,
  ink `#17201C` / `#4A5450`, brass `#9C7833` with `#7A5C24` for small text on light
  surfaces (contrast) and `#C9A85E` on dark. Display type Newsreader, body IBM Plex Sans.
- **Where the boldness is spent.** The six-gate entry sequence. Numbered markers appear
  there and nowhere else, because entry genuinely is a sequence.
- **Regulatory content** is dated: everything in the profile was verified 31 August 2026.
  The countdown on the live-position section is computed in the browser from the
  31 October 2026 PDPL deadline, so it stays honest as the date passes.
- **Fill-in fields.** Facts that were not available are marked visibly (dashed brass
  fields) rather than filled with plausible substitutes: the sector-authorisation
  instrument, three matter outcomes awaiting client clearance, and the fee figures.
  Search `class="fill"` in either HTML file.
- **Accessibility.** Keyboard-operable gate accordions with `aria-expanded`, visible
  focus rings, `prefers-reduced-motion` respected, skip link, no horizontal scroll at
  390 px. Print build keeps reversed type at 9 pt or larger.
