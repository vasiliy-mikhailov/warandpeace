import type { ReactNode } from 'react';
import type { Style } from './style.js';
/**
 * Somewhere to go back to.
 *
 * A LABEL IS NOT A DESTINATION, which is what the server-rendered original assumed: it took the
 * words and hard-coded the href. Every caller happened to mean "the list", so the coincidence never
 * bit, and a screen whose back went anywhere else would have shipped a link that lied. Both fields
 * travel.
 */
export type Crumb = {
    label: string;
    href: string;
};
export type PageHeaderProps = {
    title: string;
    /**
     * A NODE, NOT A STRING. Screens compose entities and pills into their subtitle, a verdict beside
     * a repository name or a hop beside a sha, and a `string` prop forces the caller to either flatten
     * that or reach for markup. Reaching for markup is how the original ended up appending the
     * subtitle unescaped.
     */
    subtitle: ReactNode;
    back?: Crumb;
    /**
     * THE CORNER CONTROLS, AND THEY ARE THE CALLER'S RATHER THAN THIS COMPONENT'S.
     *
     * A shared header cannot know that this dashboard has a findings button and that one has a mail
     * link, so what it owns is the ROW: pushed to the far edge, centred, at one gap. A consumer that
     * wants the same three controls on every screen writes them once in a component of its own and
     * passes it here, which costs it one file and keeps the discipline it had when the header supplied
     * them.
     */
    actions?: ReactNode;
};
/**
 * The header every screen wears.
 *
 * FULL BLEED WITH A RULE UNDER IT, not a centred column. Two tools behind one nav must agree about
 * where the page starts, and a zone that insets its content by a different amount reads as a
 * different site the moment a reader crosses the boundary.
 *
 * There is no product name and no logo: mounted in a shell, the shell already said which tool this
 * is, and a zone that repeats it spends the one line above the fold saying nothing.
 */
export declare function PageHeader({ title, subtitle, back, actions }: PageHeaderProps): import("react").JSX.Element;
/**
 * A SENTENCE HANGING UNDER THE HEADER, which is where every state and every refusal ends up.
 *
 * A REASON IS A SENTENCE, and a sentence does not belong in the right-aligned row of corner
 * controls, where it either truncates to nothing or deforms the header. So it goes into the
 * subtitle, under the title, measured to sixty characters because a line longer than that stops
 * being read across a full-bleed header.
 *
 * The bold word carries the state and the rest is prose, wherever this is used: the fact must not
 * be carried by colour alone.
 */
export declare const HEADER_NOTE: Style;
/**
 * The gear-shaped corner link.
 *
 * Both dashboards had these six declarations, in this order, one of them under the name `GEAR` and
 * private to its own header. It is exported here because the corner controls are now the caller's,
 * and a caller that has to guess the metrics of the row it is filling will guess differently on the
 * second screen.
 */
export declare const CORNER: Style;
/**
 * THE BUTTON TWIN OF THE CORNER GEAR.
 *
 * It spreads CORNER rather than restating its numbers, so every corner control shares one box by
 * construction and they cannot drift apart the next time one of them is adjusted. A button does not
 * inherit fontFamily, and the `font` shorthand would take CORNER's fontSize down with it, so the
 * family is named on a line of its own.
 *
 * The border is reserved transparent and paid for out of the padding: a refusal can turn it red
 * without moving the gear beside it by a pixel. No hover, because the gear has none and matching
 * that corner is the whole argument for this shape.
 */
export declare const CORNER_BUTTON: Style;
/** An ask is in flight. Dimmed rather than removed, so the row does not reflow under the pointer. */
export declare const CORNER_BUSY: Style;
/** The last ask was refused. Reserved border, so this costs no layout. */
export declare const CORNER_REFUSED: Style;
/**
 * 1.25em, MEASURED OFF THE RENDERED PAGE RATHER THAN REASONED ABOUT.
 *
 * The gear these sit beside is emoji-presented: U+2699 with no variation selector falls through to
 * the colour emoji face, which is why it is blue in a monochrome header, and an emoji glyph
 * overshoots its em. Measured on the real page at 1.25rem, the gear's ink is 22.2px square while a
 * 1em drawing came out at 12. Guessing from a text face said the opposite, which is why this number
 * is taken from a screenshot of the thing itself.
 *
 * 1.25em puts a drawn mark at about 85 per cent of the gear's diameter, where a stroked mark reads
 * as the same weight as a filled one rather than as a larger, thinner ring. If the gear ever loses
 * its emoji presentation this drops back to roughly 0.8em, so re-measure rather than trusting the
 * number. Vertical padding is nil because the drawing is already taller than the glyph's line box.
 */
export declare const CORNER_MARK: Style;
//# sourceMappingURL=PageHeader.d.ts.map