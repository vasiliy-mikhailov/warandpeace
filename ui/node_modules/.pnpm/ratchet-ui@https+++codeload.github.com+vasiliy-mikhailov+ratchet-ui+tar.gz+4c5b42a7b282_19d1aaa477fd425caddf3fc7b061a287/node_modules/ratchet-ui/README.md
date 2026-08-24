# ratchet-ui

The parts of an agent-pipeline dashboard that do not move at UI speed: the shapes a backend serves,
the runtime checks that say whether it really served them, and a token contract that names what a
component needs without deciding what colour it is.

Apache 2.0. No dependencies. React is a peer, and only the `./components` entry needs it.

```sh
# not on npm yet; depend on a release tag
pnpm add github:vasiliy-mikhailov/ratchet-ui#v0.4.0
```

## Why this exists

Two tools grew the same dashboard twice. Both work a queue with an agent chain, both write a
`settlements.jsonl`, both serve a list of work items, an item with its recorded events, findings
from a supervising pass, and a manifest so a shell can mount them side by side. Neither knew what
the other had called any of it.

The duplication that hurts is not the components. Components are cheap to write twice and they are
supposed to differ, because the two tools are showing different things. What is expensive to have
twice is the part where the two must agree: the vocabulary, and whether a given server actually
speaks it. That is what is in here, and nothing else is.

## What ships

| File | What it is |
| --- | --- |
| `src/wire.ts` | The six documents an agent pipeline serves, as types. No imports, no runtime. |
| `src/check.ts` | Hand-written validators returning a list of problems, never throwing. |
| `src/tokens.css` | The custom-property names a component may reference, with placeholder values. |
| `src/style.ts` | Adding CSS custom properties to a style object without depending on React. |
| `src/time.ts` | How long something took, spelled one way: `8m 45s`, `2h`. No React. |
| `src/components/` | Sixteen React components, one table shell, four table style objects, one hook. |
| `tsconfig.base.json` | The compiler settings both consuming repositories already had, byte for byte. |

## The types are only half of a contract

A type is a promise between two compilers. It says nothing about the bytes a running server sends,
and that gap is where the interesting failures live: a field typed `string | null` that arrives
absent, a count that arrives as the string `"0"`, a nav item naming a badge the manifest does not
define. All three type-check, because by the time the JSON is parsed the types are gone.

So every shape has a validator, and the validators are how a backend proves it serves the contract:

```ts
import { checkWorkItems, describe } from 'ratchet-ui'

const problems = checkWorkItems(await (await fetch('/api/items')).json())
expect(describe(problems)).toBe('no problems')
```

They return problems rather than throwing, they report every problem rather than the first, and
they never throw on any input including the ones you would pass from a `catch`.

The rule they exist for is that **`null` and absent are different**. `null` means the server has
nothing to say yet and the page draws a blank. A missing key means the server forgot the field. A
page cannot tell them apart after `JSON.parse`, and the version that gets shipped is the one where a
running item draws an empty cell with the separators still around it.

## States are strings, deliberately

The one design decision worth arguing about. `State`, `Verdict` and `EventKind` are `string`, not
unions of everything both pipelines use, because these vocabularies are not read by TypeScript. They
are read by shell scripts, with `grep`, against the settlement record on disk:

```sh
# one pipeline's runner
grep -qvE '"state":"(bumping|requeued)"'

# the other's
DISPOSITIONS='"state":"(false-positive|by-design|unprovable|reproduced|needs-review|...)"'
```

Same filename, same field, disjoint vocabularies, and neither can move: a token added to one
project's union and not to its shell script produces an item that is finished on the page and
unfinished to the loop that is supposed to stop working on it. Silently, and it costs a lane.

So the wire says `string`, and each project keeps its own union and proves assignability on its own
side in one line:

```ts
import type { State } from 'ratchet-ui/wire'

type Verdict = 'PASS' | 'FAIL_build_post' | 'queued'
const _assignable: State = 'PASS' satisfies Verdict
```

Each project gets the exhaustive union it can switch over. The shared type stays true.

## The token contract carries no palette

`src/tokens.css` is a list of thirty-seven custom-property **names**. The values in it are a
deliberately drab grey ramp, chosen for this file, belonging to nobody. They are there so an
unthemed consumer sees something legible and a component's tests have something to compute against,
and they are chosen to look unfinished on purpose. A default that looked good would get shipped by
somebody in a hurry, and this package would have made a design decision for a product it knows
nothing about.

This is a licence position as much as a design one. A palette usually belongs to somebody: a design
system, an employer, a product with a brand. Copying one into a shared library is how a colour with
an owner ends up in a dozen repositories that have no right to it. **No product's palette is ever
added to this file** — not as a default, not as a theme, not as an example. A component that needs a
colour it does not have gets a new *name* here, never somebody's value.

Override by importing first and defining the same names afterwards. Later wins at equal specificity:

```css
@import 'ratchet-ui/tokens.css';

:root {
  --bg-canvas: /* your value */;
  --accent-action: /* your value */;
}

.dark {
  --bg-canvas: /* your dark value */;
}
```

Overriding every name is the expected case. A consumer whose tokens are declared under a licence
that does not permit redistribution should keep them exactly where they are; supporting that is what
this arrangement is for.

Dark theme switches on `.dark` at the root rather than `prefers-color-scheme`, because a dashboard
mounted inside a shell has to follow the shell's choice and the shell needs something it can set.

## The components

`ratchet-ui/components` ships sixteen: `Account`, `CodeBlock`, `DataTable`, `EmptyNote`,
`HumanCost`, `KeyStatus`, `Lamp`, `Loaded`, `PageHeader`, `Pill`, `ProgressBar`, `Section`,
`SectionTabs`, `SettingCard`, `Tally` and `TimeSpent`, plus the style objects they sit in, the four
declarations a table is made of, the `useAsk` hook and the `Style` type they are written against.
React is a peer at `>=19`, declared for this entry alone. Nothing under `.`, `./wire`, `./check`,
`./style` or `./time` imports React, so a consumer that only wants the contract still pays nothing.

Three rules admit a component here and all three are narrow.

The first is 0.2.0's: both dashboards had written it, and with the comments stripped the two
versions differed by the palette rather than by the behaviour.

The second is 0.3.0's, and it covers the pairs the first cannot see. Where one dashboard had a
COMPONENT and the other had the same thing written out inline, the behaviour taken is the inline
one's and the name taken is the component's. The behaviour, because which repository wins a
disagreement was settled before any of this started. The name, because naming it is what the side
that extracted it actually contributed, and a column heading is not a component name. Five arrived
that way and each says so in its own header, since a file like that reads as an import to one
repository and as a rewrite to the other.

The third is 0.4.0's, and it is there because the first rule had a hole in it. Rule one asks for two
things at once: that both dashboards wrote the component, and that the difference between the two
versions is the palette rather than the behaviour. When the second half fails, rule one excludes
itself and then hands the case to nothing. `CodeBlock` was declined that way, and the reason written
down for declining it was a description of everything the other version does *more*. So: **where
both repositories wrote it and the versions differ in behaviour rather than palette, the shared one
is the version with call sites. Where neither has call sites, neither moves.** That is the standard
rule two already applied, which takes the behaviour from whichever side has it and the name from
whichever side named it; this finishes that principle rather than adding a new one. `CodeBlock` and
`Lamp` came in under it.

No rule admits a component only one side has ever had. There is no second version for the rule to
choose between, and adopting one wholesale is a decision a shared package is the wrong place to
make. `Semaphore`, the two-lamp component `Lamp` was lifted out of, is that case and was
deliberately not offered: what red and green mean inside it is one pipeline's vocabulary. `Lamp`
takes its colour and its whole label as props for exactly that reason, so no build vocabulary
travels with it.

The rule is checked rather than asserted: `tokens.test.ts` fails the build on a component that
reaches for a custom property the contract does not list, which is exactly what happened the first
time a pill arrived carrying `var(--state-pass)`.

So a tone is a name, and a colour is the consumer's:

```css
/* your own stylesheet, your own values, your own repository */
:root {
  --state-good: /* what "this worked" looks like in your product */;
  --state-warn: /* … */;
  --state-quiet: /* … */;
  --state-alarm: /* … */;
  --state-running: /* … */;
  --state-aside: /* … */;
}
```

### One line your build needs, and it fails silently without it

`Pill` renders `className="animate-pulse"` on the dot that marks a moving row. It is the only class
name in the package, because there is no token for motion and there should not be. Tailwind emits
the classes it finds by scanning the globs a project declares with `@source`, a project's globs
cover its own source, and installed from here this file is under `node_modules`. The utility stops
being emitted, the dot stops pulsing, and **nothing fails**: no error, no warning, no red test, and
the class is not even in the exported HTML, because a running pill only exists at run time from
fetched data.

Add the glob, pointing at the built output:

```css
@source "../../../packages/ui/node_modules/ratchet-ui/dist";
```

`dist` and not `src`, because `files` ships the compiled output and only `tokens.css` out of `src`,
so a glob at `src` would scan one stylesheet and find nothing, silently and in the same way.
`@source` paths resolve relative to the stylesheet, not to the project root, and Tailwind does not
warn about a glob that matches nothing, so a path that is wrong by one directory looks exactly like
a path that is right.

Then check it, in the repository that would lose the dot:

```sh
grep -o '\.animate-pulse{[^}]*}' apps/web/out/_next/static/chunks/*.css
```

[ADOPTING.md](ADOPTING.md) is the full bill for a second dashboard taking these, component by
component, including what each one costs.

## What is deliberately not here

**No component that behaves differently in the two dashboards.** Version 0.1.0 shipped no
components at all and said their absence was a decision. Half of that was right and is quoted under
[the components](#the-components): the Tailwind trap is real, was reproduced, and now has a tested
remedy. The other half has been narrowed rather than dropped. What is still refused is a component
whose two versions differ in what they can DO: a fold that remembers whether the reader opened it, a
timestamp that ticks on its own, a field that owns its input and wires `aria-describedby` to its
help. Eleven such pairs were examined and ten stayed where they were, because moving one deletes a
capability its author built on purpose, and no amount of parameterisation puts it back without
turning the shared file into both versions bolted together.

0.3.0 refused four more on that rule and two on a new one. A paragraph of prose that one side
renders with the writer's line breaks intact, and the other parses as a markdown subset that
deliberately joins those lines, is not one component written twice: the two make opposite decisions
about the only thing the input carries. And a page-corner nav whose two versions share one style
object and no behaviour has nothing left to move, because the style object shipped in 0.2.0.

**No schema library.** The validators are hand-written because a schema dependency here is a version
negotiation with everybody who adopts this. More code in this repository, none in yours.

**No settings, no theme switcher, no data fetching.** Those move at UI speed. This package is for
the parts that do not.

## Adopting it

Nothing here requires a rewrite. The manifest and health shapes are already served field for field
by both pipelines and cost nobody a rename. The rest is a request rather than an instruction, and
[ADOPTING.md](ADOPTING.md) is that request written out: what a tool would rename, what it would keep,
and which parts are worth taking on their own even if the rest is declined.

## Depending on it

Over a git reference, until there is a reason to be on npm:

```json
"dependencies": {
  "ratchet-ui": "github:vasiliy-mikhailov/ratchet-ui#v0.4.0"
}
```

pnpm resolves that through codeload with no token and no registry, and records the resolved commit
and an integrity hash in `pnpm-lock.yaml`. That is the point of pinning a tag rather than a branch:
`pnpm install --frozen-lockfile` then fails loudly on a reference that has moved, instead of
shipping whatever `main` happens to say today.

**Depend on a tag, never on `main`.** A release tag is a complete package and `main` is not: the tag
carries `dist/`, which `main` gitignores. That looks backwards and is the only arrangement that
works. A codeload tarball is the repository rather than an npm package, so a gitignored build output
is simply absent from it, and the npm answer to that, a `prepare` script, is refused by pnpm 10 for
a git dependency unless the consumer allowlists it **by resolved commit sha**. Writing this
package's sha into a second file in your repository would undo the one guarantee the lockfile is
there to give. `release.sh` carries the full reasoning and is what cuts a tag.

What the arrangement buys a consumer is nothing to do: no allowlist entry, no `transpilePackages`,
no build step, and `.d.ts` files that `skipLibCheck` covers rather than somebody else's TypeScript
source compiled under your compiler settings.

## Development

```sh
pnpm install
pnpm typecheck
pnpm test
pnpm build
```

There is no host Node requirement beyond version 22. CI runs the same four commands, because a build
that only works on the author's machine is the one thing a shared package must not be.
