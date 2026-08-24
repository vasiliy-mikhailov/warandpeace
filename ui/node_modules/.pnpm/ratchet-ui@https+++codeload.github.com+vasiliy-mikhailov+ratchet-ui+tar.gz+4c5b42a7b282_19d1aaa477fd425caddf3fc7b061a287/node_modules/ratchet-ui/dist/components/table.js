/**
 * THE FOUR DECLARATIONS A TABLE IS MADE OF, AND BOTH DASHBOARDS HAD WRITTEN ALL FOUR.
 *
 * The column heading is the strongest evidence there is in this move: the two versions are
 * byte-identical, declaration for declaration, in the same order, down to `.06em` and the strong
 * rule under the row. Neither repository copied it from the other; both arrived at it from the same
 * server-rendered stylesheet and then kept it in a private constant in a component file.
 *
 * WHERE THE HAIRLINE GOES IS THE ONE THING THEY DISAGREED ABOUT, and it is settled here in favour
 * of the row. `ROW` carries a top border; the alternative was a bottom border on every cell. They
 * differ at the ends: a rule on the cell draws one under the last row of the table, closing it
 * against whatever follows, and a rule on the row does not, so the table ends where its content
 * ends. The row also spans the full width by construction, where a cell-borne rule is a row of
 * separate segments that only look continuous while every cell in the row is the same height.
 *
 * NINE PIXELS, AND TWENTY-FOUR ACROSS. The vertical measure is the header's, and a header set
 * tighter than the body it heads is a rule about nothing. The horizontal one is not a choice at
 * all: it is the page gutter, the same inset the page header, the tally strip and every section
 * heading use, and a cell that inset its content differently would make its column look like it
 * belonged to a different page from the heading above it.
 *
 * WHAT DID NOT COME WITH THEM. One consumer also has a monospace stack and a second, smaller scale
 * for a table folded inside another table's cell. Only one of the two has ever had a nested table,
 * so those stay in that repository: a shared package that carries a constant one consumer invented
 * and the other has no use for is a shared package that has started collecting things.
 */
/** Full width, collapsed, at the body size. Every table on both sites. */
export const TABLE = { width: '100%', borderCollapse: 'collapse', fontSize: '12.5px' };
/** The hairline between two rows. On the row, not the cell, so it spans the whole width. */
export const ROW = { borderTop: '1px solid var(--border-soft)' };
/** A column heading: small, uppercase, letterspaced, tertiary, on a strong rule. */
export const HEAD = {
    textAlign: 'left',
    color: 'var(--text-tertiary)',
    fontWeight: 500,
    fontSize: '11px',
    textTransform: 'uppercase',
    letterSpacing: '.06em',
    padding: '9px 24px',
    borderBottom: '1px solid var(--border-strong)',
};
/**
 * A body cell. Top-aligned because several columns carry two lines, a duration over an event count,
 * and a row whose cells centre themselves independently has no baseline at all.
 */
export const CELL = { padding: '9px 24px', verticalAlign: 'top' };
//# sourceMappingURL=table.js.map