import { jsxs as _jsxs, jsx as _jsx } from "react/jsx-runtime";
const HEADER = {
    display: 'flex',
    alignItems: 'flex-start',
    gap: '12px',
    padding: '16px 24px',
    borderBottom: '1px solid var(--border-soft)',
};
const TITLE = { margin: 0, fontSize: '14px', fontWeight: 600 };
const SUB = { color: 'var(--text-tertiary)', fontSize: '12px', marginTop: '3px' };
const CRUMB = {
    display: 'inline-block',
    marginBottom: '6px',
    fontSize: '12px',
    color: 'var(--text-tertiary)',
    textDecoration: 'none',
};
/**
 * A FLEX ROW RATHER THAN THREE MEASURED OFFSETS. The original positioned its corner controls
 * absolutely at three right offsets, so adding a fourth meant re-measuring the other three, and the
 * last rule in that stylesheet is the same selector repeated after somebody lost that argument once.
 */
const ACTIONS = { marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '2px' };
/**
 * The header every screen wears.
 *
 * FULL BLEED WITH A RULE UNDER IT, not a centred column. Two tools behind one nav must agree about
 * where the page starts, and a zone that insets its content by a different amount reads as a
 * different site the moment a reader crosses the boundary.
 *
 * There is no product name and no logo: mounted in a shell, the shell already said which tool this
 * is, and a zone that repeats it spends the one line above the fold saying nothing.
 */
export function PageHeader({ title, subtitle, back, actions }) {
    return (_jsxs("header", { style: HEADER, children: [_jsxs("div", { children: [back === undefined ? null : (_jsxs("a", { href: back.href, style: CRUMB, children: ['← ', back.label] })), _jsx("h1", { style: TITLE, children: title }), _jsx("div", { style: SUB, children: subtitle })] }), actions === undefined ? null : _jsx("div", { style: ACTIONS, children: actions })] }));
}
/**
 * A SENTENCE HANGING UNDER THE HEADER, which is where every state and every refusal ends up.
 *
 * A REASON IS A SENTENCE, and a sentence does not belong in the right-aligned row of corner
 * controls, where it either truncates to nothing or deforms the header. So it goes into the
 * subtitle, under the title, measured to sixty characters because a line longer than that stops
 * being read across a full-bleed header.
 *
 * The bold word carries the state and the rest is prose, wherever this is used: the fact must not
 * be carried by colour alone.
 */
export const HEADER_NOTE = {
    marginTop: '4px',
    maxWidth: '60ch',
    color: 'var(--text-secondary)',
};
/**
 * The gear-shaped corner link.
 *
 * Both dashboards had these six declarations, in this order, one of them under the name `GEAR` and
 * private to its own header. It is exported here because the corner controls are now the caller's,
 * and a caller that has to guess the metrics of the row it is filling will guess differently on the
 * second screen.
 */
export const CORNER = {
    fontSize: '1.25rem',
    lineHeight: 1,
    color: 'var(--text-tertiary)',
    textDecoration: 'none',
    padding: '0.2rem 0.35rem',
    borderRadius: '5px',
};
/**
 * THE BUTTON TWIN OF THE CORNER GEAR.
 *
 * It spreads CORNER rather than restating its numbers, so every corner control shares one box by
 * construction and they cannot drift apart the next time one of them is adjusted. A button does not
 * inherit fontFamily, and the `font` shorthand would take CORNER's fontSize down with it, so the
 * family is named on a line of its own.
 *
 * The border is reserved transparent and paid for out of the padding: a refusal can turn it red
 * without moving the gear beside it by a pixel. No hover, because the gear has none and matching
 * that corner is the whole argument for this shape.
 */
export const CORNER_BUTTON = {
    ...CORNER,
    display: 'inline-flex',
    alignItems: 'center',
    appearance: 'none',
    background: 'none',
    border: '1px solid transparent',
    padding: '0 calc(0.35rem - 1px)',
    margin: 0,
    fontFamily: 'inherit',
    cursor: 'pointer',
    transition: 'color 120ms ease, border-color 120ms ease',
};
/** An ask is in flight. Dimmed rather than removed, so the row does not reflow under the pointer. */
export const CORNER_BUSY = { opacity: 0.55, cursor: 'progress' };
/** The last ask was refused. Reserved border, so this costs no layout. */
export const CORNER_REFUSED = { color: 'var(--danger)', borderColor: 'var(--danger)' };
/**
 * 1.25em, MEASURED OFF THE RENDERED PAGE RATHER THAN REASONED ABOUT.
 *
 * The gear these sit beside is emoji-presented: U+2699 with no variation selector falls through to
 * the colour emoji face, which is why it is blue in a monochrome header, and an emoji glyph
 * overshoots its em. Measured on the real page at 1.25rem, the gear's ink is 22.2px square while a
 * 1em drawing came out at 12. Guessing from a text face said the opposite, which is why this number
 * is taken from a screenshot of the thing itself.
 *
 * 1.25em puts a drawn mark at about 85 per cent of the gear's diameter, where a stroked mark reads
 * as the same weight as a filled one rather than as a larger, thinner ring. If the gear ever loses
 * its emoji presentation this drops back to roughly 0.8em, so re-measure rather than trusting the
 * number. Vertical padding is nil because the drawing is already taller than the glyph's line box.
 */
export const CORNER_MARK = { width: '1.25em', height: '1.25em', display: 'block' };
//# sourceMappingURL=PageHeader.js.map