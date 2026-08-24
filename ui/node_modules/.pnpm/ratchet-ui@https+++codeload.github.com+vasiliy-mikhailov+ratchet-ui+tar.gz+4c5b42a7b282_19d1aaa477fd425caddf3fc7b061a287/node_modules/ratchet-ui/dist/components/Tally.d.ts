import type { ReactNode } from 'react';
import type { Style } from './style.js';
export type TallyProps = {
    /** A node, not a number: `40 / 356`, `6h 34m` and `—` are all legitimate values here. */
    value: ReactNode;
    label: string;
    /** A coloured value, for the two counts that mean better or worse. */
    tone?: 'plain' | 'good' | 'alarm';
};
/**
 * One count, in the strip of counts under the header.
 *
 * The shape, a box with a border, a 17px value over an 11px tertiary label, is what both dashboards
 * arrived at independently and is why this is here at all. Two tools behind one nav whose summary
 * numbers are set differently look like two tools, and this is the first thing on the page, so it
 * is the first chance to look like one.
 */
export declare function Tally({ value, label, tone }: TallyProps): import("react").JSX.Element;
/**
 * The strip those boxes sit in.
 *
 * Exported because both consumers had it, and one of them had it TWICE: two byte-identical private
 * copies of these three declarations in two components, each commented with the same reference to
 * the Java it came from. Three declarations written twice in one package is how a strip of counts
 * and the strip of counts under it start disagreeing about where the page begins.
 */
export declare const STRIP: Style;
//# sourceMappingURL=Tally.d.ts.map