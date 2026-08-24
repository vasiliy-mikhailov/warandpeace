import { Fragment as _Fragment, jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { EmptyNote } from './EmptyNote.js';
import { PAGE_GUTTER } from './Section.js';
/**
 * THE THREE STATES OF A THING BEING READ, in the one order that is correct.
 *
 * Both dashboards wrote this out nine times before extracting it, one across five files and one
 * across six, as a pair of early returns in most of them and a nested ternary in the rest. The
 * wording drifted, the shape of the wrapper drifted, and one of the eighteen copies had the two
 * states the wrong way round for a while: a screen that checks emptiness before failure reports a
 * failed read as "still loading", and a reader waits for something that is never coming.
 *
 * FAILURE BEATS EMPTINESS. A read that failed also has no value, so testing the value first
 * describes every failure as a wait. The order here is the whole logic of the component and it is
 * the reason this is a component rather than a snippet.
 *
 * EMPTINESS IS STATED RATHER THAN DRAWN AS NOTHING, which is {@link EmptyNote}'s argument and this
 * inherits it: a page that renders nothing while it waits is indistinguishable from a page that
 * finished and found nothing.
 */
export function Loaded({ what, subject, failed, value, header, children }) {
    if (failed === null && value !== null) {
        return _jsx(_Fragment, { children: children(value) });
    }
    const note = (_jsx(EmptyNote, { children: failed === null
            ? `Reading the ${what}…`
            : `${subject ?? `The ${what}`} could not be read: ${failed}` }));
    if (header === undefined) {
        return note;
    }
    return (_jsxs(_Fragment, { children: [header, _jsx("div", { style: { padding: PAGE_GUTTER }, children: note })] }));
}
//# sourceMappingURL=Loaded.js.map