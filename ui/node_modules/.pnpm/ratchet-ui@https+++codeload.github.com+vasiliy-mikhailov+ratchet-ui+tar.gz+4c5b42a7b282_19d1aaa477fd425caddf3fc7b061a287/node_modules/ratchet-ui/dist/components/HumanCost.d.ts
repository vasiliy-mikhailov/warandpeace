export type HumanCostProps = {
    /**
     * WHAT A PERSON WOULD HAVE SPENT, in whole minutes, or `null` when nobody has priced it.
     *
     * NULL AND ZERO ARE DIFFERENT ANSWERS HERE, unlike in {@link TimeSpent}, and the difference is
     * the reason this takes `number | null` rather than a plain number. Nothing priced is nothing to
     * report and gets the dash. Priced at zero is a claim somebody made, and printing it is how a
     * reader finds out that an estimator answered in prose and something turned that into a 0.
     */
    minutes: number | null;
};
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
export declare function HumanCost({ minutes }: HumanCostProps): import("react").JSX.Element;
//# sourceMappingURL=HumanCost.d.ts.map