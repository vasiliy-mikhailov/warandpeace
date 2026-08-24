import type { ReactNode } from 'react';
import type { Style } from './style.js';
/**
 * WHICH WAY A COLUMN READS, set once for the heading and its cells together.
 *
 * One prop rather than two, because the only thing worse than a right-aligned column is a
 * right-aligned column under a left-aligned heading, and that is what two props eventually produce.
 */
export type Align = 'left' | 'right';
export type Column<Row> = {
    /**
     * THE HEADING, IN THE WORDS A READER NEEDS: "a person would have", not "minutes".
     *
     * Both dashboards had already decided this independently and one of them wrote it down: the
     * headings are prose, not field names. Two of them agree word for word across the two tables,
     * which is a large part of why this shell is shareable.
     */
    head: string;
    /**
     * WHAT THE COLUMN MEANS, ON THE HEADING, where a notation has to be explained once.
     *
     * On the heading rather than on every cell, because it is a fact about the column. A title
     * repeated on each row is the same sentence rendered fifty times into a document a reader may be
     * searching.
     */
    headTitle?: string;
    /** Which way this column reads, heading and cells together. Default 'left'. */
    align?: Align;
    /**
     * COLOUR AND FONT ONLY. THE INSET AND THE ALIGNMENT BELONG TO THE TABLE, and that is enforced
     * here rather than asked for: this is spread UNDER the shared cell, so a `padding` in it does
     * nothing at all. Every table on both sites is set at one inset, one consumer has a test that
     * fails the moment two of its tables disagree about it, and an override prop that could reach the
     * padding is how that test starts failing for a reason nobody can find.
     */
    cellStyle?: Style;
    cell: (row: Row) => ReactNode;
};
export type DataTableProps<Row> = {
    rows: readonly Row[];
    columns: readonly Column<Row>[];
    /**
     * NEVER THE INDEX. React reuses DOM by key, and a cell in either of these tables holds open state
     * that rides on it: a fold in a cell, keyed by position, hands its open state to a different row
     * the moment the rows are re-ordered or one settles above another. Both dashboards have hit that,
     * one of them twice, which is why this is a required function rather than an optional one with a
     * helpful default.
     */
    rowKey: (row: Row) => string;
    /**
     * WHAT TO SAY INSTEAD OF AN EMPTY BODY.
     *
     * A table with headings and nothing under them is indistinguishable from a table that has not
     * finished loading. Omitting this is allowed and gives exactly that: the headings alone, which is
     * the right answer for a table whose caller has already said something above it.
     */
    empty?: ReactNode;
    /**
     * A CLASS FOR EVERY ROW, and the one thing in this package that must be written down in the
     * CONSUMER'S OWN SOURCE rather than here.
     *
     * A utility class only exists if a stylesheet generator saw the literal string somewhere it was
     * scanning. This package is installed into `node_modules`, which a consumer's globs may or may
     * not cover, and a class that goes missing costs nothing visible: no error, no failing test, the
     * rule is simply never emitted. A hover band written in the consumer's own file is scanned by the
     * globs that consumer already has, and cannot go quiet.
     */
    rowClassName?: string;
};
/**
 * THE SHELL OF A TABLE, WITH THE CELLS LEFT TO THE CALLER.
 *
 * THE TWO DASHBOARDS FACTORED THEIR INDEX TABLE ON OPPOSITE AXES, and this is the joint that lets
 * both keep the half they got right. One factored the SHELL: one set of table, heading, row and
 * cell styles shared across three tables, with the cells written out inline. The other factored the
 * CELLS: six components, each owning its own dash rule and its own tooltip, over a shell copied
 * privately into two files and shared with nothing. Converged, the shell is the first one's and the
 * cells stay with whoever owns the vocabulary in them.
 *
 * WHY THE RENDER PROP IS PER COLUMN AND NEVER PER ROW. A `renderRow` would hand back the `<tr>`,
 * the key and the hairline, which is exactly the shell that was worth factoring; two callers would
 * then own four copies of it again. A `cell` per column hands back only the part that knows what a
 * severity or a verdict is, and that part cannot be shared: it reaches for a vocabulary each server
 * owns and neither has agreed with the other.
 *
 * WHY THERE IS A TYPE PARAMETER AS WELL AS A RENDER PROP. They do different jobs, and dropping the
 * parameter is the tempting simplification that quietly deletes something. `Row` is what makes
 * `cell` type-check against the caller's own summary type at the call site; without it every cell
 * takes `any`, and the inline `<td>`s this replaces were type-checked.
 *
 * THERE IS DELIBERATELY NO `sort`. One consumer sorts before rendering and the other has a written
 * decision NOT to sort, because its table's order is the run's plan and a table sorted by state
 * groups everything nobody has reached at one end, which looks like progress. Order belongs to
 * whoever owns the rows.
 */
export declare function DataTable<Row>({ rows, columns, rowKey, empty, rowClassName, }: DataTableProps<Row>): import("react").JSX.Element;
//# sourceMappingURL=DataTable.d.ts.map