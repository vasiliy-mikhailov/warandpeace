/**
 * THE VOCABULARY TWO AGENT PIPELINES CAN BOTH SERVE.
 *
 * An agent pipeline that works a queue produces the same six documents whatever it is working on: a
 * list of work items, one item in detail, the events that item recorded, the findings something
 * noticed across items, and the manifest and health a shell needs to mount the dashboard. The
 * shapes below are those six, written once so two tools can be checked against one description
 * instead of drifting into two.
 *
 * TYPES ONLY, AND THAT IS A REQUIREMENT RATHER THAN AN ACCIDENT. There is no `import` in this file
 * and there is no runtime value in it, so `import type { WorkItem } from 'ratchet-ui/wire'` erases
 * completely at compile time and no bundler ever loads anything. A single `export const` here would
 * turn a types-only dependency into a runtime one for every consumer, and a dashboard that pays for
 * a module at runtime to describe shapes it already knows statically has bought nothing.
 *
 * ADDITIVE CHANGES ONLY. A shell, and a reader's open tab, will be older than the server as often
 * as not. Adding a field is safe. Renaming one breaks a page nobody rebuilt.
 *
 * NOTHING HERE IS A COLOUR, A LABEL OR A WIDTH. A state is `PASS`, not green. The moment a server
 * sends a tone over the wire it has made a design decision that cannot be tested for, and the page
 * has stopped being the only place that knows what a state looks like.
 */
export {};
//# sourceMappingURL=wire.js.map