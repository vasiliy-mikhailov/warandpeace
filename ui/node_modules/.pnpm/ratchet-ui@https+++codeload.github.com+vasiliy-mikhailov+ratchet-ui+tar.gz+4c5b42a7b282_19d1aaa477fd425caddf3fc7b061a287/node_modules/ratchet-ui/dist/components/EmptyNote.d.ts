import type { ReactNode } from 'react';
export type EmptyNoteProps = {
    children: ReactNode;
};
/**
 * WHAT AN EMPTY THING SAYS, said the same way everywhere.
 *
 * An empty table that renders as nothing is indistinguishable from a table that failed to load, and
 * the reader's next move differs completely between the two. So emptiness is stated.
 *
 * THE COPY IS NOT A PROP DEFAULT AND MUST NOT BECOME ONE. There is no `children` fallback here, and
 * the reason is a bug both consuming dashboards shipped: a sentence baked into the component gets
 * shown on the one screen nobody wrote copy for, and a wrong reassurance ("nothing traced for this
 * marker", on a page that has no marker) is worse than a blank space. The sentence is the screen's
 * to write, every time.
 *
 * AN INLINE NOTE, NOT A FULL-PAGE ONE. This draws a line of italic text where the missing content
 * would have been, because most empty states sit beside content that is present and a block of
 * whitespace tall enough to fill a page pushes that content off the fold. A screen where the note
 * IS the whole page wants the roomy version, and gets it by wrapping this in its own padded
 * element, which is one line at the few call sites that want it rather than a prop at all of them.
 */
export declare function EmptyNote({ children }: EmptyNoteProps): import("react").JSX.Element;
//# sourceMappingURL=EmptyNote.d.ts.map