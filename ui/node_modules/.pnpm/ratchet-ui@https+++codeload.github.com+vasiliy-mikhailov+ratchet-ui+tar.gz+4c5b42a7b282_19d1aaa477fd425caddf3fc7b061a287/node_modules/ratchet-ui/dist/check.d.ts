/**
 * WHAT MAKES THE CONTRACT REAL RATHER THAN ASPIRATIONAL.
 *
 * A type in `wire.ts` is a promise between two compilers. It says nothing whatever about the bytes
 * a running server actually sends, and the interesting failures all live in that gap: a field typed
 * `string | null` that arrives absent, a count that arrives as the string `"0"`, a nav entry whose
 * badge names a badge the manifest does not define. TypeScript will happily let a page render all
 * three, because by the time the JSON is parsed the types are gone.
 *
 * These functions close that gap. A backend either serves the shape or it does not, and a test can
 * say which, in one line, against the real response.
 *
 * THREE RULES, EACH OF WHICH IS A DECISION.
 *
 * They RETURN PROBLEMS, they do not throw. A validator that throws can only be used inside a
 * `try`, which means the caller writes the same four lines everywhere and the natural mistake is to
 * catch and ignore. A returned list can be asserted empty in a test, logged in production, and
 * counted, and the empty case needs no syntax at all.
 *
 * They report EVERY problem, not the first. A server being brought up to the contract wants the
 * list, because fixing one field and re-running to discover the next is a slow way to learn six
 * things. The cost is that a validator cannot stop early, which for documents this size is nothing.
 *
 * They have NO DEPENDENCIES. This package exists to be adopted by other people's tools, and a
 * schema library in the dependency tree is a version negotiation with everybody who adopts it.
 * Hand-written checks are more code here and no code at all for a consumer.
 */
/**
 * One thing wrong with a document.
 *
 * `path` is written the way a reader would say it out loud, `items[3].because`, so that a problem
 * found in a list of four hundred can be looked up in the response without counting.
 */
export type Problem = {
    path: string;
    /** What is wrong, phrased as what was expected and what arrived. */
    says: string;
};
/** Checks one row of a queue. See {@link WorkItem}. */
export declare function checkWorkItem(value: unknown, at?: string): Problem[];
/** Checks a whole queue, naming each bad row by its index. */
export declare function checkWorkItems(value: unknown, at?: string): Problem[];
/** Checks one line of a record. See {@link RecordEvent}. */
export declare function checkRecordEvent(value: unknown, at?: string): Problem[];
/** Checks one item's page payload: the item, and everything it recorded. See {@link ItemDetail}. */
export declare function checkItemDetail(value: unknown, at?: string): Problem[];
/** Checks one claim about work. See {@link Finding}. */
export declare function checkFinding(value: unknown, at?: string): Problem[];
/**
 * Checks the document a shell reads to mount a tool. See {@link Manifest}.
 *
 * This one does more than check kinds, because the manifest has an internal reference that can be
 * wrong while every field is individually the right type: a nav item may name a badge, and the
 * badge may not be there. A shell that follows the name gets `undefined`, polls nothing and shows
 * no count, and nobody finds out until somebody notices a number that never appears. That is worth
 * one extra rule.
 */
export declare function checkManifest(value: unknown, at?: string): Problem[];
/**
 * Checks a health response. See {@link Health}.
 *
 * The only union in the contract, and it is checked as one: `ok: true` requires a version and
 * `ok: false` requires a reason. Checking both halves loosely, by asking only that one of the two
 * strings be present, would accept `{ok: false, version: 'abc'}`, which is a tool reporting that it
 * is broken and declining to say why.
 */
export declare function checkHealth(value: unknown, at?: string): Problem[];
/**
 * One line for a test or a log: what is wrong, in the order it was found.
 *
 * Deliberately not thrown and deliberately not coloured. A caller decides whether a malformed
 * response is worth failing a test over or worth one warning line in a server that must stay up.
 */
export declare function describe(problems: Problem[]): string;
//# sourceMappingURL=check.d.ts.map