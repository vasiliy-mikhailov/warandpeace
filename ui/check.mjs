// RUN ratchet-ui's OWN VALIDATORS OVER WHAT THIS SERVER REALLY SENT.
//
// The fixtures beside this file are captured from a running Dash, not written by hand — a fixture
// somebody typed is a test of their typing. `capture.sh` re-takes them; if a re-take turns this
// red, that is the news rather than the problem.
//
//   node ui/check.mjs
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))

// THE PACKAGE, WHEREVER IT IS. Preferred order: an explicit path, then an installed copy, then a
// sibling checkout. Said out loud because a check that silently validated against the wrong version
// of the contract would be worse than no check.
const roots = [
  process.env.RATCHET_UI,
  join(here, 'node_modules', 'ratchet-ui'),
  join(here, '..', '..', 'ratchet-architect', 'ratchet-ui'),
]
let check, from
for (const root of roots) {
  if (!root) continue
  for (const entry of ['src/check.ts', 'dist/check.js']) {
    try {
      check = await import(join(root, entry))
      from = join(root, entry)
      break
    } catch { /* try the next */ }
  }
  if (check) break
}
if (!check) {
  console.error('ui/check.mjs: no ratchet-ui found. Set RATCHET_UI, or clone it beside this repo.')
  process.exit(2)
}

const load = (name) => JSON.parse(readFileSync(join(here, 'fixtures', name), 'utf8'))
const runs = [
  ['manifest', check.checkManifest(load('manifest.json'))],
  ['health', check.checkHealth(load('health.json'))],
  ['items', check.checkWorkItems(load('items.json'))],
  ['item-detail', check.checkItemDetail(load('item-detail.json'))],
]

console.log('contract checked against ' + from + '\n')
let bad = 0
for (const [name, problems] of runs) {
  const said = check.describe(problems)
  if (problems.length) bad += problems.length
  console.log('  ' + name.padEnd(13) + said)
}

// THE PATH HALF, WHICH checkManifest DOES NOT COVER. It walks every nav item's badge and refuses one
// naming a badge the manifest does not define; nothing walks the path. A sibling repository shipped
// two nav items pointing at routes no page served, with a live correct badge beside one of them.
const manifest = load('manifest.json')
const served = ['/', '/chapters', '/dashboard']
const dangling = manifest.nav.filter((n) => !served.includes(n.path))
console.log('  ' + 'nav paths'.padEnd(13)
  + (dangling.length ? dangling.map((n) => n.label + ' -> ' + n.path).join(', ') : 'no problems'))
bad += dangling.length

process.exit(bad ? 1 : 0)
