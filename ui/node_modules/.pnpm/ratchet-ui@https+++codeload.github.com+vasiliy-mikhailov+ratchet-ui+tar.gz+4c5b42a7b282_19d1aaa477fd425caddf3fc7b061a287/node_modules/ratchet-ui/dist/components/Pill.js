import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * Tone → token. The names on the left are what a pill is FOR; the names on the right are the
 * contract, and the gap between the two columns is the only place a colour lives.
 *
 * The right-hand column used to be each consumer's own state names, which is exactly why this
 * component could not be shared before: the two dashboards had the same six tones written against
 * disjoint vocabularies. Six contract names, six aliases in each consumer's own stylesheet, and the
 * colours never enter this package.
 */
const TONE = {
    good: 'var(--state-good)',
    warn: 'var(--state-warn)',
    quiet: 'var(--state-quiet)',
    alarm: 'var(--state-alarm)',
    running: 'var(--state-running)',
    aside: 'var(--state-aside)',
};
function pillStyle(tone) {
    return {
        // Set once and read three times, so the text, the wash and the edge cannot drift apart.
        '--pill-tone': TONE[tone],
        display: 'inline-block',
        padding: '2px 9px',
        borderRadius: '20px',
        fontSize: '11px',
        whiteSpace: 'nowrap',
        textDecoration: 'none',
        color: 'var(--pill-tone)',
        background: 'color-mix(in srgb, var(--pill-tone) 14%, transparent)',
        border: '1px solid color-mix(in srgb, var(--pill-tone) 32%, transparent)',
    };
}
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
export function Pill({ tone, href, title, children }) {
    const body = (_jsxs(_Fragment, { children: [tone === 'running' ? (_jsx("span", { className: "animate-pulse", "aria-hidden": "true", children: '● ' })) : null, children] }));
    if (href === undefined) {
        return (_jsx("span", { style: pillStyle(tone), title: title, children: body }));
    }
    return (_jsx("a", { href: href, style: pillStyle(tone), title: title, children: body }));
}
//# sourceMappingURL=Pill.js.map