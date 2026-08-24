import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
const BOX = {
    padding: '6px 12px',
    border: '1px solid var(--border-soft)',
    borderRadius: '6px',
    background: 'var(--bg-card)',
};
const VALUE = { fontSize: '17px', display: 'block', fontWeight: 600 };
const LABEL = { color: 'var(--text-tertiary)', fontSize: '11px' };
/**
 * TONE TO TOKEN, AND `plain` IS THE ORDINARY TEXT COLOUR RATHER THAN NO COLOUR AT ALL.
 *
 * Writing the primary text token here instead of leaving the colour to inherit is what makes the
 * default safe to adopt: a page whose body already sets `color: var(--text-primary)` renders the
 * same value either way, and a page mounted somewhere that does not gets the count in the colour a
 * count is supposed to be rather than in whatever it inherited.
 *
 * THESE ARE NOT THE PILL'S TONE NAMES, and the duplication is deliberate. A tally that moved and a
 * pill that says PASS are different claims: one is arithmetic getting better or worse, the other is
 * a settled outcome. Both consumers already draw that distinction in their own vocabularies, and
 * collapsing the two pairs here would delete it for both of them to save four lines.
 */
const TONE = {
    plain: 'var(--text-primary)',
    good: 'var(--state-count-good)',
    alarm: 'var(--state-count-alarm)',
};
/**
 * One count, in the strip of counts under the header.
 *
 * The shape, a box with a border, a 17px value over an 11px tertiary label, is what both dashboards
 * arrived at independently and is why this is here at all. Two tools behind one nav whose summary
 * numbers are set differently look like two tools, and this is the first thing on the page, so it
 * is the first chance to look like one.
 */
export function Tally({ value, label, tone = 'plain' }) {
    return (_jsxs("div", { style: BOX, children: [_jsx("b", { style: { ...VALUE, color: TONE[tone] }, children: value }), _jsx("span", { style: LABEL, children: label })] }));
}
/**
 * The strip those boxes sit in.
 *
 * Exported because both consumers had it, and one of them had it TWICE: two byte-identical private
 * copies of these three declarations in two components, each commented with the same reference to
 * the Java it came from. Three declarations written twice in one package is how a strip of counts
 * and the strip of counts under it start disagreeing about where the page begins.
 */
export const STRIP = { display: 'flex', flexWrap: 'wrap', gap: '8px', padding: '14px 24px' };
//# sourceMappingURL=Tally.js.map