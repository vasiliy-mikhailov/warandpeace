import { jsx as _jsx } from "react/jsx-runtime";
/**
 * SEVENTY-TWO CHARACTERS, WHICH IS THE ONLY DECLARATION HERE WORTH ARGUING ABOUT.
 *
 * These pages are full bleed by house rule: there is no centred column, because the two dashboards
 * are mounted behind one nav and a zone that inset its content differently would read as a
 * different site. A paragraph with no measure of its own therefore runs the width of a monitor, and
 * a line of prose that long loses the reader on the way back to the left edge. So the measure is
 * the paragraph's rather than the page's.
 */
const ACCOUNT = { margin: '0 0 12px', fontSize: '13px', lineHeight: 1.6, maxWidth: '72ch' };
/**
 * The aside keeps the measure and gives up the size and the ink, which is the whole distinction.
 * It is the note under a card, and it is tertiary because it is the one paragraph on the page that
 * is allowed to be skipped.
 *
 * THESE FIVE DECLARATIONS ARE NOT INVENTED FOR THIS COMPONENT. They are, byte for byte, the
 * footnote style {@link SettingCard} already carried privately, which is why the quiet variant is
 * proven on both sides of the move rather than being one dashboard's idea adopted on trust.
 * `SettingCard` now renders its footnote through this component and stays pixel-identical.
 */
const QUIET = {
    ...ACCOUNT,
    margin: '8px 0 0',
    fontSize: '11.5px',
    color: 'var(--text-tertiary)',
};
/**
 * A PARAGRAPH EXPLAINING WHAT A CONTROL DOES AND WHY IT IS A CONTROL AT ALL.
 *
 * THE NAME AND THE SHAPE ARE ONE DASHBOARD'S, THE METRICS ARE THE OTHER'S, and the next reader
 * should know which is which. One side had fourteen call sites of a component called `Account` with
 * a `quiet` variant; the other had the same paragraph typed out inline four times and had never
 * named it. Naming it, and the `quiet` prop, are the first side's contribution and this component
 * would not exist without them. Every number below is the second side's, because that is the rule
 * this release follows, and the numbers are where the two actually differed: 13px against 15.2px,
 * a 1.6 line against a 1.7, a 72ch measure against 52em, and no colour at all against secondary.
 *
 * THE PROSE STAYS IN THE COMPONENT TREE AND NEVER GOES ON THE WIRE. It is not data: it does not
 * come from the record, it does not change per item, and an API that ships it is an API whose
 * payload grows every time somebody improves a sentence. The exception that earns itself is a
 * sentence that interpolates a number the component already holds, and that sentence belongs inside
 * that component rather than here.
 *
 * NO COLOUR ON THE PLAIN VARIANT, DELIBERATELY. It inherits, so it is whatever the page sets its
 * body text to, which is what a paragraph a reader is meant to read should be. Quieting it is the
 * job of the other variant, and a paragraph that is quiet by default is a paragraph nobody reads.
 *
 * THE MARGINS ARE THE COMMON CASE RATHER THAN THE ONLY CASE. Both are exported as {@link ACCOUNT}
 * and {@link ACCOUNT_QUIET} for the call site that leads straight into a code block and wants to
 * sit closer to it, which is one deviation stated out loud instead of a third prop on every use.
 */
export function Account({ children, quiet = false }) {
    return _jsx("p", { style: quiet ? QUIET : ACCOUNT, children: children });
}
export { ACCOUNT, QUIET as ACCOUNT_QUIET };
//# sourceMappingURL=Account.js.map