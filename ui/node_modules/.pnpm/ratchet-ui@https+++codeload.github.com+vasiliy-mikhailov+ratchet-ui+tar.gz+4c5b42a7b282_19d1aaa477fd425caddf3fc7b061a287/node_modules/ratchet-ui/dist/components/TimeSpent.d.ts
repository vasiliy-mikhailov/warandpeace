export type TimeSpentProps = {
    /**
     * HOW LONG THE MACHINE HAS BEEN AT THIS, in milliseconds, or `null` for a job that has not
     * started.
     *
     * NULL IS NOT ZERO AND THE COMPONENT WILL NOT PRETEND IT IS. A queued item has no span, and "0s"
     * states a duration for work nobody has done; the em dash says there is nothing to report. Both
     * dashboards had reached that rule and expressed it differently, one testing whether a start
     * stamp existed and the other testing whether the span was positive, so it is a prop here rather
     * than a comparison inside: only the caller knows which of its own fields means "not started".
     *
     * Wall clock, not machine time. A job that waited eleven minutes for a daemon spent eleven
     * minutes, and the figure a person compares against an estimate is the one that includes them.
     */
    ms: number | null;
    /** How many lines of the record this job has. Its own answer to "is anything happening". */
    events: number;
};
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
export declare function TimeSpent({ ms, events }: TimeSpentProps): import("react").JSX.Element;
//# sourceMappingURL=TimeSpent.d.ts.map