import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
const BAR = {
    display: 'flex',
    alignItems: 'center',
    gap: '4px',
    padding: '10px 24px',
    borderBottom: '1px solid var(--border-soft)',
    background: 'var(--bg-panel)',
};
function tabStyle(current) {
    return {
        padding: '5px 11px',
        borderRadius: '6px',
        fontSize: '12.5px',
        textDecoration: 'none',
        whiteSpace: 'nowrap',
        color: current ? 'var(--text-primary)' : 'var(--text-tertiary)',
        background: current ? 'var(--state-selected-bg)' : 'transparent',
        fontWeight: current ? 600 : 400,
    };
}
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
export function SectionTabs({ tabs, trailing, label }) {
    return (_jsxs("nav", { style: BAR, "aria-label": label, children: [tabs.map((tab) => (_jsx("a", { href: tab.href, "aria-current": tab.current ? 'page' : undefined, style: tabStyle(tab.current), children: tab.label }, tab.href))), (trailing ?? []).map((tab, index) => (_jsx("a", { href: tab.href, "aria-current": tab.current ? 'page' : undefined, style: index === 0 ? { ...tabStyle(tab.current), marginLeft: 'auto' } : tabStyle(tab.current), children: tab.label }, tab.href)))] }));
}
//# sourceMappingURL=SectionTabs.js.map