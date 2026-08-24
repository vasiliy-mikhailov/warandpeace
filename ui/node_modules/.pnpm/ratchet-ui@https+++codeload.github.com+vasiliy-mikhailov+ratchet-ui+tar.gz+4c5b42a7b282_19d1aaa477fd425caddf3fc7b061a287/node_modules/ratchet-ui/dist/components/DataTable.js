import { Fragment as _Fragment, jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { CELL, HEAD, ROW, TABLE } from './table.js';
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
export function DataTable({ rows, columns, rowKey, empty, rowClassName, }) {
    if (rows.length === 0 && empty !== undefined) {
        return _jsx(_Fragment, { children: empty });
    }
    return (_jsx("div", { style: { overflowX: 'auto' }, children: _jsxs("table", { style: TABLE, children: [_jsx("thead", { children: _jsx("tr", { children: columns.map((column) => (
                        // `scope` because a heading that does not say what it heads is a heading a screen
                        // reader has to guess at, and one of the two originals already had it.
                        _jsx("th", { scope: "col", style: column.align === 'right' ? { ...HEAD, textAlign: 'right' } : HEAD, title: column.headTitle, children: column.head }, column.head))) }) }), _jsx("tbody", { children: rows.map((row) => (_jsx("tr", { style: ROW, className: rowClassName, children: columns.map((column) => (_jsx("td", { style: {
                                ...column.cellStyle,
                                ...CELL,
                                ...(column.align === 'right' ? { textAlign: 'right' } : null),
                            }, children: column.cell(row) }, column.head))) }, rowKey(row)))) })] }) }));
}
//# sourceMappingURL=DataTable.js.map