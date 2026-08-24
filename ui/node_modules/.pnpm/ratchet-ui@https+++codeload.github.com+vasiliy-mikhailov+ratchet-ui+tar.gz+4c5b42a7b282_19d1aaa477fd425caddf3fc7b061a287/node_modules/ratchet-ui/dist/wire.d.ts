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
/**
 * THE STATE OF A WORK ITEM, AS A STRING, AND THE STRING IS THE WHOLE POINT.
 *
 * This is the one place where the obvious design is wrong, so it is worth being exact about why.
 *
 * The obvious design is a union of every state both pipelines use. It is wrong because these
 * vocabularies are not read by TypeScript. They are read by shell scripts, with `grep`, against the
 * settlement record on disk. Two live examples, both measured rather than remembered:
 *
 *   bump-java-version, `agent/run.sh`:
 *     grep -qvE '"state":"(bumping|requeued)"'
 *
 *   fix-java-svace-markers, `agent/entrypoint.sh`:
 *     DISPOSITIONS='"state":"(false-positive|by-design|unprovable|reproduced|needs-review|
 *                   verified/pr-ready|verified/pr-rejected)"'
 *
 * Both greps run against a file called `settlements.jsonl`, and both read a field called `state`.
 * The file shape is shared. The vocabulary is completely disjoint, and neither half can move: a
 * token added to one project's union and not to its shell script produces an item that is finished
 * on the page and unfinished to the loop that is supposed to stop working on it. That failure is
 * silent, and it costs a lane rather than a build.
 *
 * So a union here would be a union of two sets that must never be unioned in practice, and it would
 * hand each project a compiling reference to seven states its own runner would never recognise.
 *
 * The contract is therefore: the WIRE says `string`, and each project keeps its own union and
 * proves assignability with a one-line type test on its own side, like this:
 *
 *     import type { State } from 'ratchet-ui/wire'
 *     const _states: State = 'PASS' satisfies Verdict
 *
 * That gives each project exactly what it wants, an exhaustive union it can switch over, while the
 * shared type stays true: any pipeline's state is a string, and no pipeline's states are another's.
 */
export type State = string;
/**
 * WHAT SOMETHING MADE OF A FINDING, and a false friend worth naming before it bites.
 *
 * Both of the pipelines this was drawn from export a type called `Verdict`, and they mean different
 * things. bump-java-version's `Verdict` is a settlement state, `PASS` or `FAIL_build_post`, which
 * is this file's {@link State}. fix-java-svace-markers' `Verdict` is a critic's judgement on a
 * finding, `holds` or `unjudged` or `refuted`, which is this.
 *
 * The two are one rename away from being silently swapped, which is why they are separate names
 * here and why this comment exists. Same string-not-union reasoning as {@link State}.
 */
export type Verdict = string;
/**
 * THE KIND OF LINE IN A RECORD. String, for the third time, and for a slightly different reason.
 *
 * Kinds are not grepped by a runner, but they are appended to by whoever adds an agent, and the two
 * pipelines already disagree on nearly all of them: one has `exchange` and `applied`, the other has
 * `sent` and `metered` and `system`. Anything rendering a record must already cope with a kind it
 * has never seen, because the record is years older than the code reading it. A union would only
 * promise otherwise.
 */
export type EventKind = string;
/**
 * ONE ROW OF THE QUEUE: something the pipeline was asked to do, and where it got to.
 *
 * Deliberately small. Everything a particular pipeline knows about its own work items, which
 * repository at which commit, or which file at which line under which checker, belongs to that
 * pipeline and not here. What is shared is only this: work items have an identity, a state, a
 * reason, a count of what they recorded, and a time they last spoke.
 */
export type WorkItem = {
    /**
     * The identity used in every API path for this item, and the directory name on disk.
     *
     * A string, even where a pipeline's items are numbered, because a number in a URL invites a
     * reader to guess the next one and an id should not be guessable-by-increment.
     */
    id: string;
    state: State;
    /**
     * WHY IT IS IN THAT STATE, in one line, or NULL WHEN NOTHING HAS BEEN SAID YET.
     *
     * NULL, NOT ABSENT, and the distinction is load-bearing rather than pedantic. A server that emits
     * every field on every row and uses JSON `null` for "no value yet" is describing something
     * different from a server that omits the key. Typing this `?: string` says the KEY may be
     * missing, so a component written against it checks `=== undefined`, the null falls through as a
     * value, and the row renders an empty cell with the separators still drawn around it.
     *
     * That is not hypothetical. It shipped in bump-java-version, where a running bump drew a bare
     * arrow in the vulnerability column. The validator in `check.ts` enforces the distinction: a
     * missing key is a problem and an explicit null is not.
     */
    because: string | null;
    /** Record lines this item has written. Zero while it is only queued, never absent. */
    events: number;
    /**
     * Epoch milliseconds of THE LAST EVENT on this item, not of the last settlement.
     *
     * The difference decides whether this column can answer the question it exists for. A settlement
     * is written when an item starts and again when it ends, so an item grinding away for an hour
     * shows the moment it began, and a reader scanning for stalled work sees every running item
     * looking equally stale.
     */
    at: number;
    /**
     * Epoch milliseconds of when this item first spoke, where the pipeline records it.
     *
     * Optional because it is the one field of the six that a pipeline can honestly lack: an item
     * retried across several attempts may have no single start that means anything. With `at` it
     * answers what one timestamp cannot, since an item can be four hours old and still working or
     * four hours old and stopped, and those want different reactions from a reader.
     */
    startedAt?: number;
};
/**
 * EVERYTHING ONE ITEM'S PAGE NEEDS, IN ONE RESPONSE.
 *
 * One response rather than two, because the second request is the one that fails: a page that
 * fetches an item and then its events has two loading states, two failure states and a window in
 * which it can draw an item with no history and imply there is none.
 *
 * A pipeline will have more to say about its own items than this, its dependency scan or its chain
 * of agents, and it should say it in its own extension of this type rather than here.
 */
export type ItemDetail = {
    item: WorkItem;
    events: RecordEvent[];
};
/** One thing that happened, in the order it happened. */
export type RecordEvent = {
    at: number;
    kind: EventKind;
    /**
     * Which agent spoke, or NULL for a line no agent produced.
     *
     * Deterministic steps record events too, a build, a gate, a queue admission, and they have no
     * agent. Null says that; an absent key says the server forgot.
     */
    agent: string | null;
    /** Which tool was called, on the kinds that call one. */
    tool?: string;
    /**
     * The body of the line, where there is a single body.
     *
     * Optional because the two pipelines genuinely differ here and pretending otherwise would make
     * the contract a lie: one records a single `text` per line, the other splits a model call into
     * `prompt` and `reply` and holds both in full. A consumer that wants one string renders this when
     * it is present and falls back to whatever its own pipeline puts beside it.
     *
     * Long, frequently. Whole prompts run to tens of thousands of characters. Folding that is the
     * page's job, because a server that truncates has destroyed the record to save a scrollbar.
     */
    text?: string;
};
/**
 * WHAT SOMETHING NOTICED ACROSS ITEMS THAT NO SINGLE ITEM COULD SEE.
 *
 * The supervising half of an agent pipeline: the part that reads many items and says "these four
 * failed the same way". Separate from {@link WorkItem} because a finding is not work, it is a
 * claim about work, and it settles by being judged rather than by being finished.
 */
export type Finding = {
    /**
     * A STABLE ANCHOR, and stable is the requirement rather than a nicety.
     *
     * A findings page groups by verdict, so display order is not the record's order. An anchor
     * computed from display position moves the moment a critic answers something, which means a link
     * shared yesterday points at a different finding today. This must be derived from the finding's
     * position in the record, not from where it is drawn.
     */
    id: string;
    title: string;
    /** The argument. Prose, and as long as it needs to be. */
    body: string;
    verdict: Verdict;
    /** The ids of the work items this finding is about, possibly none. */
    items: string[];
};
/**
 * THE ONE DOCUMENT A SHELL NEEDS TO MOUNT A TOOL, served BY the tool.
 *
 * Served rather than configured, so it describes the version actually running instead of what
 * somebody wrote down once. This is the part of the wire that both pipelines already serve field
 * for field, so unlike everything above it, adopting it costs nobody a rename.
 */
export type Manifest = {
    id: string;
    name: string;
    description: string;
    /** The git commit of the running image, so a reader can tell two deployments apart. */
    version: string;
    /**
     * WHERE THE SHELL PUT THIS TOOL, which is configuration and never a constant.
     *
     * A zone with a hard-coded prefix can be mounted exactly one way, and the second tool that wants
     * the same prefix cannot be mounted at all.
     */
    basePath: string;
    assetPrefix: string;
    api: string;
    health: string;
    /**
     * The tool's own navigation, with paths RELATIVE TO `basePath`.
     *
     * The shell prefixes them. A tool does not know what the shell's URL bar says, and one that
     * assumes will work perfectly until the day it is mounted somewhere else.
     */
    nav: NavItem[];
    /**
     * HOW A COUNT REACHES THE SHELL'S NAVIGATION WITHOUT THE SHELL KNOWING WHAT IT COUNTS.
     *
     * The shell polls `endpoint` and reads `field`. That indirection is the entire value: when a
     * second tool wants a badge for something the shell has never heard of, the shell needs no
     * change. Keyed by the badge name a {@link NavItem} refers to.
     */
    badges: Record<string, Badge>;
};
export type NavItem = {
    label: string;
    path: string;
    /** The key into {@link Manifest.badges}, or null for an item that carries no count. */
    badge: string | null;
};
export type Badge = {
    endpoint: string;
    field: string;
};
/**
 * WHETHER THE TOOL CAN SERVE ITS RECORD. Not whether everything it might use is up.
 *
 * A dashboard is worth serving when the model endpoint is unreachable, because the whole record is
 * still readable and that is most of what anybody comes for. A health check that went red because a
 * model was down would have a shell hiding a tool that was working.
 */
export type Health = {
    ok: true;
    version: string;
} | {
    ok: false;
    why: string;
};
//# sourceMappingURL=wire.d.ts.map