import { jsx as _jsx } from "react/jsx-runtime";
const TRACK = {
    height: '4px',
    margin: 0,
    background: 'var(--bg-subtle)',
    // NOT CLAMPED HERE. A caller handing this 140 has counted more settled items than it was given,
    // and that is worth seeing in a test rather than smoothing over in a component; `overflow` keeps
    // the mistake from breaking the page while it stays a mistake.
    overflow: 'hidden',
};
/**
 * TWO TOKENS FOR ONE GRADIENT, and neither of them is named after a state.
 *
 * Both consuming dashboards drew this bar from the colour of "working on it" to the colour of
 * "finished", and each wrote its own two state names into the gradient. Neither pair can live in a
 * shared package: one of them means `bumping` and `PASS` and the other means `proving` and
 * `verified/pr-ready`, and a package that picked either would be telling the other tool what its
 * vocabulary is. So the gradient reads two names that describe the BAR, and each consumer points
 * them at whichever of its own states it already uses.
 */
const FILL = {
    display: 'block',
    height: '100%',
    background: 'linear-gradient(90deg, var(--state-progress-from), var(--state-progress-to))',
};
/** How much of the run has settled. The bar directly under the header. */
export function ProgressBar({ pct }) {
    return (_jsx("div", { style: TRACK, role: "progressbar", "aria-valuenow": pct, "aria-valuemin": 0, "aria-valuemax": 100, "aria-label": "settled", children: _jsx("i", { style: { ...FILL, width: `${pct}%` } }) }));
}
//# sourceMappingURL=ProgressBar.js.map