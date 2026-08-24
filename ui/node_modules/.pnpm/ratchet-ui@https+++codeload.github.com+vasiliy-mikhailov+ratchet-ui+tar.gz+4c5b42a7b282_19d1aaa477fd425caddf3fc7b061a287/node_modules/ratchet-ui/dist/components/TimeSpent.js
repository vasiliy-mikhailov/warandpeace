import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { duration } from '../time.js';
/** A measurement, not a finding, so the second line is set down out of the way. */
const COUNT = { color: 'var(--text-tertiary)', fontSize: '11px' };
/** Nothing to report reads as nothing to report, in the colour of a thing that is absent. */
const NOTHING = { color: 'var(--text-tertiary)' };
/**
 * HOW LONG THE MACHINE TOOK OVER THIS, AND HOW MUCH IT SAID WHILE IT DID.
 *
 * HALF OF THIS CAME FROM EACH SIDE, AND THE NEXT READER WILL WONDER WHICH HALF. One dashboard had
 * it as ten lines inside a `<td>` and had never named it; the other had extracted a component and
 * called it `TimeSpent`. The behaviour below is the inline cell's, declaration for declaration,
 * because that is the rule this whole release follows. The insight that it is a component at all,
 * and the name it goes by, are the other side's, because a column heading is not a component name
 * and the side that had already extracted it had nothing else to offer here. So a reader comparing
 * this against either repository finds half of it familiar and half of it new, and that is the
 * merge rather than a mistake.
 *
 * TWO FACTS IN ONE CELL BECAUSE THEY ARE READ TOGETHER. A long span with a handful of events is
 * stuck; a long span with thousands is slow; and the reader deciding which one they are looking at
 * should not have to cross the table to do it. That is also why the count is not a column of its
 * own and why an item with no span still deserves the dash rather than an empty cell.
 *
 * THE SPAN IS THE PAGE'S OWN INK AND ONLY THE COUNT IS SET DOWN. The other version quieted the
 * whole cell, which makes the two lines one fact; the span is a measurement the reader came for and
 * the count is the footnote to it.
 *
 * THE COUNT IS GROUPED. A record of `1834` events and one of `18340` differ by a digit that is easy
 * to miss in a right-aligned column, and the separator is the cheapest way to stop a reader
 * misreading an order of magnitude.
 */
export function TimeSpent({ ms, events }) {
    if (ms === null) {
        return _jsx("span", { style: NOTHING, children: '—' });
    }
    return (_jsxs(_Fragment, { children: [_jsx("span", { children: duration(ms) }), _jsxs("div", { style: COUNT, children: [events.toLocaleString(), " event(s)"] })] }));
}
//# sourceMappingURL=TimeSpent.js.map