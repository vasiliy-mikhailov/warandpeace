/**
 * THE COMPONENTS, AND WHY THEY ARE ON A SUBPATH OF THEIR OWN.
 *
 * Everything here needs React. The root entry of this package does not, and must not: a consumer
 * that wants `ratchet-ui/wire` for its server, `ratchet-ui/check` for a test or `ratchet-ui/time`
 * to spell a duration into a log line should never have to resolve a React version to get them.
 * Keeping the components behind `ratchet-ui/components` is what makes that true by construction
 * rather than by care. One thing here does not render at all, `useAsk`, and it is here anyway
 * because the line these entry points draw is React and not JSX.
 *
 * WHAT QUALIFIES ONE OF THESE TO BE HERE, IN THREE RULES RATHER THAN TWO.
 *
 * The first is 0.2.0's and is unchanged: both consuming dashboards had written it, and the
 * difference between their two versions was the palette rather than the behaviour. That rule is
 * narrower than it sounds and it excluded more components than it admitted: two versions of the
 * same fold, one of which remembers whether the reader opened it, are not one component written
 * twice, and the one that remembers would be deleted by sharing.
 *
 * The second is 0.3.0's and covers the case the first cannot see. Where one dashboard had a
 * COMPONENT and the other had the same thing written out inline, the behaviour taken is the inline
 * one's, because that is the rule this project settled on before any of this started, and the name
 * taken is the component's, because a name is what the side that extracted it actually contributed
 * and a column heading is not a component name. `TimeSpent`, `HumanCost`, `Account`, `KeyStatus`
 * and `SettingCard`'s two optional props all arrived that way, and each file says so in its own
 * header, because the next reader will find half of it familiar and half of it new.
 *
 * The third is 0.4.0's and exists because the first one had a hole in it. Rule one asks for TWO
 * things, that both dashboards wrote it and that the difference is palette rather than behaviour.
 * When the second half fails, rule one excludes itself and then hands the case to nothing, and what
 * got written down in `ADOPTING.md` as the reason to decline `CodeBlock` was a list of everything
 * the other version does MORE. So: where both repositories wrote it and the versions differ in
 * BEHAVIOUR rather than palette, the shared one is the version WITH CALL SITES. Where neither has
 * call sites, neither moves. That is the same standard rule two already applied, which took the
 * behaviour from whichever side had it and the name from whichever side named it; this finishes it
 * rather than adding to it. `CodeBlock` and `Lamp` arrived under it, from the sibling dashboard's
 * request, and each says so in its own header.
 *
 * WHAT NO RULE ADMITS is a component only one side has ever had. There is no version of it to be
 * canonical, and adopting the other's wholesale is a decision this package is not the place to
 * make. It stays where it is. `Semaphore`, the two-lamp component `Lamp` was lifted out of, is
 * exactly that case and was deliberately not offered: what red and green mean inside it is one
 * pipeline's vocabulary, so the lamp came and its meanings did not.
 *
 * TWO TAB ROWS EXIST AND ONLY ONE OF THEM IS HERE. `SectionTabs` is the ruled bar across the top of
 * a page, which lights the section being read with a filled pill. The other kind, an underline row
 * that sits inside a page and lights with a 2px rule, belongs to the consumer that has one; a
 * barrel exporting both under names a hurried reader could confuse is how a page ends up claiming
 * its subsection is the page.
 *
 * NONE OF THESE CARRIES A COLOUR. Every colour is a custom property the consumer defines, listed in
 * `tokens.css`, and a component reaching for a name the contract does not promise fails
 * `tokens.test.ts` rather than rendering invisibly.
 */
export { ACCOUNT, ACCOUNT_QUIET, Account } from './Account.js';
export { CodeBlock } from './CodeBlock.js';
export { DataTable, } from './DataTable.js';
export { EmptyNote } from './EmptyNote.js';
export { HumanCost } from './HumanCost.js';
export { KeyStatus } from './KeyStatus.js';
export { Lamp } from './Lamp.js';
export { Loaded } from './Loaded.js';
export { CORNER, CORNER_BUSY, CORNER_BUTTON, CORNER_MARK, CORNER_REFUSED, HEADER_NOTE, PageHeader, } from './PageHeader.js';
export { Pill } from './Pill.js';
export { ProgressBar } from './ProgressBar.js';
export { HEADING, PAGE_GUTTER, Section } from './Section.js';
export { SectionTabs, } from './SectionTabs.js';
export { SettingCard } from './SettingCard.js';
export { CELL, HEAD, ROW, TABLE } from './table.js';
export { STRIP, Tally } from './Tally.js';
export { TimeSpent } from './TimeSpent.js';
export { NO_REASON, REQUEST_FAILED, useAsk, } from './useAsk.js';
//# sourceMappingURL=index.js.map