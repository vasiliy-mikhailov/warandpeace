import type { CSSProperties } from 'react';
import type { WithTokens } from '../style.js';
/**
 * THE STYLE TYPE THE COMPONENTS IN THIS DIRECTORY AUTHOR AGAINST.
 *
 * `../style.ts` exports the half of this that does not need React, {@link WithTokens}, and explains
 * at length why the finished type cannot live there: the root entry of this package is types and
 * validators, and a consumer that only wants `ratchet-ui/wire` must not have to resolve a React
 * version to get it.
 *
 * That argument holds for the root entry and stops at this directory. Everything under
 * `./components` is React by definition, so the base type is already in the dependency tree of
 * anyone who imports from here, and naming it costs them nothing they were not already paying.
 *
 * It is exported rather than kept private because a consumer writing its own components beside
 * these wants the same type, and the alternative is that every such repository restates this one
 * line. Both consuming dashboards had restated it, identically, which is how it got noticed.
 */
export type Style = WithTokens<CSSProperties>;
//# sourceMappingURL=style.d.ts.map