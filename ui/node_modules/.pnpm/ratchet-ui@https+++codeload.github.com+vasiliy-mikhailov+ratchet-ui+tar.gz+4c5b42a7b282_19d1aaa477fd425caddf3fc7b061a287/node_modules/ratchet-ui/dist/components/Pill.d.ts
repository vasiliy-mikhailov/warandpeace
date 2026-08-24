import type { ReactNode } from 'react';
/**
 * SIX TONES, AND NOT ONE OF THEM IS A STATE.
 *
 * A `Pill` does not know what `FAIL_test_conservation` or `verified/pr-ready` means. A consuming
 * dashboard maps its own vocabulary onto these in one component of its own; nothing may pass a tone
 * straight through from a payload, because a tone arriving over the wire is a colour decision made
 * by a server that cannot be tested for it. That is not hypothetical: it is how one of these
 * dashboards ended up sending a verified marker to the infra red.
 */
export type PillTone = 'good' | 'warn' | 'quiet' | 'alarm' | 'running' | 'aside';
export type PillProps = {
    tone: PillTone;
    href?: string;
    /** Hover text. Where the MEANING of a verdict goes, since the pill itself has room for a word. */
    title?: string;
    children: ReactNode;
};
/**
 * The pill every verdict, role and count is shown in. The one purely presentational primitive.
 *
 * `running` keeps a pulsing dot, because a page that is live has to say which row is MOVING and a
 * static blue pill does not. A consumer's `prefers-reduced-motion` rule switches it off for readers
 * who asked.
 *
 * THE ONE CLASS NAME IN THIS PACKAGE, AND IT IS A TRAP FOR THE CONSUMER'S BUILD.
 *
 * `animate-pulse` is a Tailwind utility, because there is no token for motion and never should be.
 * Tailwind emits only the classes it finds by scanning the globs a project declares with `@source`,
 * and a project's globs cover its own source, not `node_modules`. Installed from here, this file is
 * outside them: the utility stops being emitted, the dot stops pulsing, and NOTHING FAILS. No error,
 * no warning, no red test, and the class does not even appear in the exported HTML, because a
 * running pill only exists at run time from fetched data.
 *
 * The fix is one line in the consumer's stylesheet, pointing at the built output of this package:
 *
 *     @source "../../../packages/ui/node_modules/ratchet-ui/dist";
 *
 * `dist` rather than `src`, because `files` in package.json ships the compiled output and only
 * `tokens.css` out of `src`, so a glob at `src` would scan one stylesheet and find nothing, silently
 * and in exactly the same way. `tsc` emits the class as a string literal, so the built file scans.
 *
 * And because a silent failure needs a check rather than a comment, the consumer asserts the class
 * is in its built stylesheet. ADOPTING.md carries the grep.
 */
export declare function Pill({ tone, href, title, children }: PillProps): import("react").JSX.Element;
//# sourceMappingURL=Pill.d.ts.map