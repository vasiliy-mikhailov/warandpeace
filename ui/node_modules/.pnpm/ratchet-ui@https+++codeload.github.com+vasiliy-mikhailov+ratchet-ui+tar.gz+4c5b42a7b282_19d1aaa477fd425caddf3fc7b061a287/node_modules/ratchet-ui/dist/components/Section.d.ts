import type { ReactNode } from 'react';
import type { Style } from './style.js';
/**
 * THE PAGE'S GUTTER, as a padding pair.
 *
 * 24px, the same inset the page header, the tally strip and every table cell use. Two dashboards
 * behind one nav have to agree about where the page starts; a block that chose its own inset reads
 * as a different site the moment a reader scrolls past it.
 */
export declare const PAGE_GUTTER = "0 24px";
/**
 * A SECTION HEADING, WRITTEN DOWN ONCE FOR BOTH DASHBOARDS.
 *
 * Small, uppercase, letterspaced, tertiary. One repository had it typed out in full in four
 * separate places, one of which was dead code; the other had these same five declarations in four
 * private constants of its own, differing only in the margin above them. Eight copies of one
 * heading, none of which had drifted yet, which is the only reason nobody had noticed.
 *
 * THE WEIGHT IS PART OF THE HEADING AND IS THE ONE THING THE COPIES DID NOT AGREE ON. Four of them
 * left it out, which is not the same as choosing it: an `h2` or `h3` with no weight declared takes
 * the browser's bold, so those four render at 700 beside four that render at 500. Naming it here is
 * what makes the eight one heading rather than two that look alike from a distance.
 *
 * The margin travels with it because it is part of the rhythm rather than part of the type, and it
 * is the one declaration a caller legitimately overrides: {@link Section} takes two of the values
 * itself, and a heading at the top of an already-indented block wants no leading at all.
 */
export declare const HEADING: Style;
export type SectionProps = {
    title: string;
    /**
     * WHERE THE GUTTER IS PAID, which is the whole difference between the two shapes of section and
     * the reason there were two copies of this component.
     *
     * `body` insets the whole block, and is what a section full of cards or prose wants: everything
     * inside it lines up under the heading.
     *
     * `heading` insets the heading alone and lets the body run to the edges of the page, which is
     * what a section containing a table wants: a table's own cells carry the gutter, and a table
     * inset a second time would sit 48px in while every other table on the site sits at 24.
     */
    gutter?: 'body' | 'heading';
    /**
     * An anchor, for a section something links straight to.
     *
     * It brings `scroll-margin-top` with it rather than leaving that to the caller, because a section
     * you can link to and a section that needs room above it when the browser jumps to it are the
     * same section, and the one time they were written separately the room was left off.
     */
    id?: string;
    children: ReactNode;
};
/**
 * A HEADING AND THE BLOCK UNDER IT. Two pages had defined this, and they had defined it differently.
 *
 * One version inset the whole section and the other inset only the heading, and both were right for
 * what they contained: one holds cards, the other holds full-bleed tables. That difference is the
 * `gutter` prop. Everything else about the two, the 22px trailing margin, the heading treatment,
 * the leading above it, was the same fact written twice.
 */
export declare function Section({ title, gutter, id, children }: SectionProps): import("react").JSX.Element;
//# sourceMappingURL=Section.d.ts.map