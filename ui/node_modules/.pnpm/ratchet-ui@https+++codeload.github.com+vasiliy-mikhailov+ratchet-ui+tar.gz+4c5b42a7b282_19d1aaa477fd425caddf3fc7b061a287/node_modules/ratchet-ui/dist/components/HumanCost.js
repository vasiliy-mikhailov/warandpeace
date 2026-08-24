import { jsx as _jsx } from "react/jsx-runtime";
import { spellMinutes } from '../time.js';
const NOTHING = { color: 'var(--text-tertiary)' };
/**
 * WHAT THIS WOULD HAVE COST A PERSON, beside what it actually took.
 *
 * WHERE THE TWO HALVES CAME FROM, because the next reader will wonder. One dashboard had this as
 * six lines inside a `<td>` under a column heading reading "a person would have"; the other had a
 * component called `HumanCost`. The name is the other side's, since a column heading is not a name.
 * The behaviour is the inline cell's, and here that is not a formality: the two disagree about
 * zero, and the inline version is the one that prints it.
 *
 * PRINTING A ZERO IS WHAT SURFACES A PARSE FAILURE. The other version dashed on anything that was
 * not positive, on the stated grounds that its field cannot tell "never priced" from "priced at
 * nothing" because the parser turns an estimator who answered in prose into a 0. That is an
 * argument for showing the zero rather than hiding it: a dash says nobody asked, and a nought says
 * somebody answered and something ate the answer. Only one of those two sends anyone to look.
 *
 * AN ESTIMATE AND A MEASUREMENT ARE DIFFERENT CLAIMS AND STAY IN DIFFERENT COLUMNS. The pair is the
 * whole argument for a pipeline like this, which is exactly why they must not be added to each
 * other or to anything else: a guess folded into a measured total is a guess that has been
 * laundered into a number.
 */
export function HumanCost({ minutes }) {
    if (minutes === null) {
        return _jsx("span", { style: NOTHING, children: '—' });
    }
    return _jsx("span", { children: spellMinutes(minutes) });
}
//# sourceMappingURL=HumanCost.js.map