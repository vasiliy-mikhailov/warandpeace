/** One destination in the bar. `href` is a prop: whoever knows the URL passes the URL. */
export type SectionTab = {
    href: string;
    label: string;
    /** Is the reader on this one now. */
    current: boolean;
};
export type SectionTabsProps = {
    tabs: SectionTab[];
    /**
     * LINKS THAT LEAVE THIS ROW'S SET rather than choose within it, pushed to the right.
     *
     * Both dashboards wrote this and both wrote down the same argument for it: a thing that watches
     * the run is not a setting, and putting it in the same run of tabs as "the model" invites a
     * reader to look for a value to change in it. The auto margin lands on the first of them, so
     * several departures stay together at the right rather than spreading out.
     *
     * A DEPARTURE IS LIT BY ITS OWN `current` LIKE ANY OTHER TAB, and the two versions differed here.
     * The other one never lights a departure, on the grounds that lighting it would claim the reader
     * is already there. That is right when the link really leaves the page and wrong when it is a
     * section of this one wearing a divider, which is what it is on the page this arrangement was
     * taken from. A caller that wants the other behaviour passes `current: false` and gets it.
     */
    trailing?: SectionTab[];
    /** The nav's accessible name. Required, because a page with two navs in it needs both named. */
    label: string;
};
/**
 * THE SECTIONS OF A PAGE, AS A RULED BAR ACROSS THE TOP OF IT.
 *
 * TWO FILES THAT NEVER MET AGREED ON FIVE DECLARATIONS. The tab inset `5px 11px`, the `6px` radius,
 * `var(--state-selected-bg)` under the current one, the bar's `10px 24px` and the soft rule under
 * it are the same in both, and both wrote the trailing departure with `marginLeft: 'auto'` and the
 * same sentence explaining why. That is what makes this shareable at all; everything else about the
 * two pages, the routes and the words on the tabs, is the caller's and stays there.
 *
 * NOT TO BE CONFUSED WITH AN UNDERLINE TAB ROW, and one consumer has both. This is the bar at the
 * top of a page, which lights the current section with a filled pill on a panel background. The
 * other kind sits inside a page and lights with a 2px underline. They are different components with
 * different jobs and neither is a variant of the other; a page that used this one inside itself
 * would be claiming its subsection is the page.
 *
 * `aria-current` GOES ON THE LIT TAB, which neither original did. It is what tells a screen reader
 * which of eight links is the page it is already on, and adding it here is a correction rather than
 * a preference: the alternative is that the row says where you are in colour alone.
 */
export declare function SectionTabs({ tabs, trailing, label }: SectionTabsProps): import("react").JSX.Element;
//# sourceMappingURL=SectionTabs.d.ts.map