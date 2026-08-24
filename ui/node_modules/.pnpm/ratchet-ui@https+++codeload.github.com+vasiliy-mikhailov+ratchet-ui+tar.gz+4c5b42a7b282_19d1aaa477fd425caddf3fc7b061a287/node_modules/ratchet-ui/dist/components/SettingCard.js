import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { Account } from './Account.js';
const CARD = {
    border: '1px solid var(--border-soft)',
    borderRadius: '8px',
    background: 'var(--bg-card)',
    padding: '14px 16px',
};
/**
 * The edited card, which differs by a wash and a left rule and by nothing else.
 *
 * TWO SIGNALS RATHER THAN ONE, because either alone is a colour difference a reader has to be told
 * about. The rule gives the card an edge the others do not have, so a column of cards shows which
 * one was touched at a glance rather than on inspection.
 */
const CARD_CHANGED = {
    ...CARD,
    borderLeft: '2px solid var(--accent-primary)',
    background: 'var(--accent-soft)',
};
const TITLE = { fontWeight: 600, fontSize: '13px' };
const PROVENANCE = {
    fontSize: '10px',
    textTransform: 'uppercase',
    letterSpacing: '.06em',
    color: 'var(--text-tertiary)',
    marginLeft: '8px',
};
/** The provenance of a card somebody edited says so in the accent rather than in a second word. */
const PROVENANCE_CHANGED = { ...PROVENANCE, color: 'var(--accent-primary)' };
/**
 * ONE SETTING, IN THE CARD BOTH DASHBOARDS SHOW ONE IN.
 *
 * The two versions were a card and a row, and the pairing between them is exact rather than
 * approximate: `title` against `name`, `provenance` against `state`, both drawn as a small
 * uppercase letterspaced tertiary word beside the heading, at 10px and 10.5px respectively. The
 * markup, the metrics and the prop names here are the card's, per the rule this release follows.
 * The two optional props are the row's, they default off, and a caller that omits them gets the
 * card byte for byte as it was.
 *
 * THE FOOTNOTE IS A QUIET {@link Account} AND NOT A PRIVATE STYLE. The five declarations it used to
 * carry are the same five, so the note under a card and the note under a control are now one thing
 * that can only be restyled once.
 */
export function SettingCard({ title, provenance, changed = false, id, children, footnote, }) {
    return (_jsxs(_Fragment, { children: [_jsxs("section", { id: id, style: changed ? CARD_CHANGED : CARD, children: [_jsxs("h3", { style: { margin: '0 0 8px' }, children: [_jsx("span", { style: TITLE, children: title }), provenance === undefined ? null : (_jsx("span", { style: changed ? PROVENANCE_CHANGED : PROVENANCE, children: provenance }))] }), children] }), footnote === undefined ? null : _jsx(Account, { quiet: true, children: footnote })] }));
}
//# sourceMappingURL=SettingCard.js.map