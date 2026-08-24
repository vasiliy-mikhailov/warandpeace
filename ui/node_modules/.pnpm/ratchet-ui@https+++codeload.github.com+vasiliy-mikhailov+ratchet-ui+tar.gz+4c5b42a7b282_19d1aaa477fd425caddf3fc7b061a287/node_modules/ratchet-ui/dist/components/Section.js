import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * THE PAGE'S GUTTER, as a padding pair.
 *
 * 24px, the same inset the page header, the tally strip and every table cell use. Two dashboards
 * behind one nav have to agree about where the page starts; a block that chose its own inset reads
 * as a different site the moment a reader scrolls past it.
 */
export const PAGE_GUTTER = '0 24px';
/**
 * A SECTION HEADING, WRITTEN DOWN ONCE FOR BOTH DASHBOARDS.
 *
 * Small, uppercase, letterspaced, tertiary. One repository had it typed out in full in four
 * separate places, one of which was dead code; the other had these same five declarations in four
 * private constants of its own, differing only in the margin above them. Eight copies of one
 * heading, none of which had drifted yet, which is the only reason nobody had noticed.
 *
 * THE WEIGHT IS PART OF THE HEADING AND IS THE ONE THING THE COPIES DID NOT AGREE ON. Four of them
 * left it out, which is not the same as choosing it: an `h2` or `h3` with no weight declared takes
 * the browser's bold, so those four render at 700 beside four that render at 500. Naming it here is
 * what makes the eight one heading rather than two that look alike from a distance.
 *
 * The margin travels with it because it is part of the rhythm rather than part of the type, and it
 * is the one declaration a caller legitimately overrides: {@link Section} takes two of the values
 * itself, and a heading at the top of an already-indented block wants no leading at all.
 */
export const HEADING = {
    fontSize: '11px',
    textTransform: 'uppercase',
    letterSpacing: '.06em',
    color: 'var(--text-tertiary)',
    fontWeight: 500,
    margin: '18px 24px 10px',
};
/**
 * A HEADING AND THE BLOCK UNDER IT. Two pages had defined this, and they had defined it differently.
 *
 * One version inset the whole section and the other inset only the heading, and both were right for
 * what they contained: one holds cards, the other holds full-bleed tables. That difference is the
 * `gutter` prop. Everything else about the two, the 22px trailing margin, the heading treatment,
 * the leading above it, was the same fact written twice.
 */
export function Section({ title, gutter = 'body', id, children }) {
    const box = {
        margin: '0 0 22px',
        ...(id === undefined ? null : { scrollMarginTop: '12px' }),
        ...(gutter === 'body' ? { padding: PAGE_GUTTER } : null),
    };
    return (_jsxs("section", { id: id, style: box, children: [_jsx("h2", { style: gutter === 'body' ? { ...HEADING, margin: '18px 0 10px' } : HEADING, children: title }), children] }));
}
//# sourceMappingURL=Section.js.map