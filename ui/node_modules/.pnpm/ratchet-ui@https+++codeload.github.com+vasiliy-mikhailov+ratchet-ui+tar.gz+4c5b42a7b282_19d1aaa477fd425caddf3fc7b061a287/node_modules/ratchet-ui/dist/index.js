/**
 * ratchet-ui: the parts of an agent-pipeline dashboard that do not move at UI speed.
 *
 * WHAT THIS ENTRY EXPORTS AND WHAT IT DELIBERATELY DOES NOT.
 *
 * Everything here is a shape, a validator or a type. There is no React reachable from this file and
 * that is a promise rather than an accident: a consumer that wants the wire types for its server, or
 * the validators for a test, must never have to resolve a React version to get them.
 *
 * THE COMPONENTS ARE ON `ratchet-ui/components`, and version 0.1.0 said they would never exist. That
 * paragraph is worth quoting rather than deleting, because it was right about the failure and wrong
 * about the conclusion: moving a component into `node_modules` moves it outside the `@source` globs
 * a Tailwind consumer scans, the utility classes it uses silently stop being emitted, and the
 * component renders with no error and no failing test. What changed is that the failure was
 * reproduced, measured and given a tested remedy, which is one `@source` line in each consumer
 * pointing at this package's built output, plus a grep over the consumer's built stylesheet. See
 * `components/Pill.tsx` for the mechanics and ADOPTING.md for the line to paste.
 *
 * SUBPATHS ARE THE POINT, so prefer them to this file. `ratchet-ui/wire` is types with no runtime
 * whatsoever, so importing from it costs a consumer nothing at all at run time. Importing from here
 * pulls in the validators too. Both are tiny; the distinction still matters, because a types-only
 * dependency is one a consumer never has to think about again.
 */
export { duration, spellMinutes } from './time.js';
export { checkFinding, checkHealth, checkItemDetail, checkManifest, checkRecordEvent, checkWorkItem, checkWorkItems, describe, } from './check.js';
//# sourceMappingURL=index.js.map