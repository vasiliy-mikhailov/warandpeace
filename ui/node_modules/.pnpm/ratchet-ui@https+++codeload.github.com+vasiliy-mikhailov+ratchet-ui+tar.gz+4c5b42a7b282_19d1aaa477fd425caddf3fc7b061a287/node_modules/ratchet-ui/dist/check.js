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
function isBag(value) {
    // `typeof null` is 'object', which is the oldest trap in the language, and an array is an object
    // too. A document that arrives as `[]` where an object was expected should be told so plainly
    // rather than reported as forty missing fields.
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}
function kindOf(value) {
    if (value === null)
        return 'null';
    if (value === undefined)
        return 'undefined';
    if (Array.isArray(value))
        return 'an array';
    return `a ${typeof value}`;
}
/**
 * Whether a field is absent, treating a key holding `undefined` as absent.
 *
 * `JSON.parse` never produces an `undefined` value, so on a parsed document this is exactly the
 * `in` check. It differs on a document assembled in memory, where `{because: undefined}` is a key
 * that exists holding nothing, and there the two are worth collapsing: a server template that
 * interpolated a missing value and a server that omitted the field have made the same mistake and
 * want the same message. Reporting one of them as "expected a string, got undefined" would be
 * technically accurate and would send a reader looking for a field they would find is not there.
 */
function absent(bag, key) {
    return !(key in bag) || bag[key] === undefined;
}
function root(value, at, what) {
    return isBag(value) ? [] : [{ path: at, says: `expected ${what} object, got ${kindOf(value)}` }];
}
function str(bag, key, at) {
    if (absent(bag, key))
        return [{ path: `${at}.${key}`, says: 'is missing, expected a string' }];
    const value = bag[key];
    if (typeof value === 'string')
        return [];
    return [{ path: `${at}.${key}`, says: `expected a string, got ${kindOf(value)}` }];
}
/**
 * A string that MAY BE NULL BUT MAY NOT BE ABSENT, which is the rule this whole file is here for.
 *
 * `null` means the server has nothing to say yet. A missing key means the server forgot the field,
 * and the two want different reactions: the first is drawn as a blank, the second is a bug in the
 * server. A page cannot tell them apart after `JSON.parse` unless something checked.
 */
function nullableStr(bag, key, at) {
    if (absent(bag, key)) {
        return [{ path: `${at}.${key}`, says: 'is missing, expected a string or null' }];
    }
    const value = bag[key];
    if (typeof value === 'string' || value === null)
        return [];
    return [{ path: `${at}.${key}`, says: `expected a string or null, got ${kindOf(value)}` }];
}
/**
 * A finite number.
 *
 * Finite rather than merely `typeof 'number'`, because `NaN` passes that test and then propagates
 * silently through every sum and comparison it touches. JSON has no literal for it, but a document
 * assembled in memory before being handed here can carry one, and that is exactly the case where
 * catching it early is worth something.
 */
function num(bag, key, at) {
    if (absent(bag, key))
        return [{ path: `${at}.${key}`, says: 'is missing, expected a number' }];
    const value = bag[key];
    if (typeof value !== 'number') {
        return [{ path: `${at}.${key}`, says: `expected a number, got ${kindOf(value)}` }];
    }
    if (!Number.isFinite(value)) {
        return [{ path: `${at}.${key}`, says: 'expected a finite number, got ' + String(value) }];
    }
    return [];
}
/** A count: a number that is finite, whole and not negative. */
function count(bag, key, at) {
    const problems = num(bag, key, at);
    if (problems.length > 0)
        return problems;
    const value = bag[key];
    if (!Number.isInteger(value) || value < 0) {
        return [{ path: `${at}.${key}`, says: `expected a whole count of 0 or more, got ${value}` }];
    }
    return [];
}
/** An optional field: absent is fine, present and wrong is not. Undefined counts as absent. */
function optional(bag, key, at, check) {
    if (absent(bag, key))
        return [];
    return check(bag, key, at);
}
function arrayAt(bag, key, at) {
    if (absent(bag, key)) {
        return { problems: [{ path: `${at}.${key}`, says: 'is missing, expected an array' }] };
    }
    const value = bag[key];
    if (!Array.isArray(value)) {
        return { problems: [{ path: `${at}.${key}`, says: `expected an array, got ${kindOf(value)}` }] };
    }
    return { items: value };
}
/**
 * The wrapper every exported check goes through, so that "never throws" is a guarantee and not an
 * intention.
 *
 * Reading a property is the only thing any rule above does, and on a `JSON.parse` result that
 * cannot throw. It can throw on an object that was not parsed but assembled, with a getter on it,
 * and the caller of a validator is very often the `catch` of the fetch that produced the document.
 * A validator that throws there replaces a precise list of problems with a stack trace about the
 * validator, which is the least useful thing it could possibly say.
 */
function safely(at, run) {
    try {
        return run();
    }
    catch (thrown) {
        return [{ path: at, says: `could not be read: ${String(thrown)}` }];
    }
}
/** Checks one row of a queue. See {@link WorkItem}. */
export function checkWorkItem(value, at = 'item') {
    return safely(at, () => {
        const bad = root(value, at, 'a work item');
        if (bad.length > 0)
            return bad;
        const bag = value;
        return [
            ...str(bag, 'id', at),
            ...str(bag, 'state', at),
            ...nullableStr(bag, 'because', at),
            ...count(bag, 'events', at),
            ...num(bag, 'at', at),
            ...optional(bag, 'startedAt', at, num),
        ];
    });
}
/** Checks a whole queue, naming each bad row by its index. */
export function checkWorkItems(value, at = 'items') {
    return safely(at, () => {
        if (!Array.isArray(value)) {
            return [{ path: at, says: `expected an array of work items, got ${kindOf(value)}` }];
        }
        return value.flatMap((item, i) => checkWorkItem(item, `${at}[${i}]`));
    });
}
/** Checks one line of a record. See {@link RecordEvent}. */
export function checkRecordEvent(value, at = 'event') {
    return safely(at, () => {
        const bad = root(value, at, 'a record event');
        if (bad.length > 0)
            return bad;
        const bag = value;
        return [
            ...num(bag, 'at', at),
            ...str(bag, 'kind', at),
            ...nullableStr(bag, 'agent', at),
            ...optional(bag, 'tool', at, str),
            ...optional(bag, 'text', at, str),
        ];
    });
}
/** Checks one item's page payload: the item, and everything it recorded. See {@link ItemDetail}. */
export function checkItemDetail(value, at = 'detail') {
    return safely(at, () => {
        const bad = root(value, at, 'an item detail');
        if (bad.length > 0)
            return bad;
        const bag = value;
        const problems = checkWorkItem(bag['item'], `${at}.item`);
        const events = arrayAt(bag, 'events', at);
        if ('problems' in events)
            return [...problems, ...events.problems];
        return [
            ...problems,
            ...events.items.flatMap((event, i) => checkRecordEvent(event, `${at}.events[${i}]`)),
        ];
    });
}
/** Checks one claim about work. See {@link Finding}. */
export function checkFinding(value, at = 'finding') {
    return safely(at, () => {
        const bad = root(value, at, 'a finding');
        if (bad.length > 0)
            return bad;
        const bag = value;
        const problems = [
            ...str(bag, 'id', at),
            ...str(bag, 'title', at),
            ...str(bag, 'body', at),
            ...str(bag, 'verdict', at),
        ];
        const items = arrayAt(bag, 'items', at);
        if ('problems' in items)
            return [...problems, ...items.problems];
        return [
            ...problems,
            ...items.items.flatMap((id, i) => typeof id === 'string'
                ? []
                : [{ path: `${at}.items[${i}]`, says: `expected an item id string, got ${kindOf(id)}` }]),
        ];
    });
}
function checkNavItem(value, at) {
    const bad = root(value, at, 'a nav item');
    if (bad.length > 0)
        return bad;
    const bag = value;
    return [...str(bag, 'label', at), ...str(bag, 'path', at), ...nullableStr(bag, 'badge', at)];
}
/**
 * Checks the document a shell reads to mount a tool. See {@link Manifest}.
 *
 * This one does more than check kinds, because the manifest has an internal reference that can be
 * wrong while every field is individually the right type: a nav item may name a badge, and the
 * badge may not be there. A shell that follows the name gets `undefined`, polls nothing and shows
 * no count, and nobody finds out until somebody notices a number that never appears. That is worth
 * one extra rule.
 */
export function checkManifest(value, at = 'manifest') {
    return safely(at, () => {
        const bad = root(value, at, 'a manifest');
        if (bad.length > 0)
            return bad;
        const bag = value;
        const problems = [
            ...str(bag, 'id', at),
            ...str(bag, 'name', at),
            ...str(bag, 'description', at),
            ...str(bag, 'version', at),
            ...str(bag, 'basePath', at),
            ...str(bag, 'assetPrefix', at),
            ...str(bag, 'api', at),
            ...str(bag, 'health', at),
        ];
        const badgeNames = new Set();
        const badges = bag['badges'];
        if (absent(bag, 'badges')) {
            problems.push({ path: `${at}.badges`, says: 'is missing, expected an object of badges' });
        }
        else if (!isBag(badges)) {
            problems.push({
                path: `${at}.badges`,
                says: `expected an object of badges, got ${kindOf(badges)}`,
            });
        }
        else {
            for (const name of Object.keys(badges)) {
                badgeNames.add(name);
                const where = `${at}.badges.${name}`;
                const badge = badges[name];
                if (!isBag(badge)) {
                    problems.push({ path: where, says: `expected a badge object, got ${kindOf(badge)}` });
                    continue;
                }
                problems.push(...str(badge, 'endpoint', where), ...str(badge, 'field', where));
            }
        }
        const nav = arrayAt(bag, 'nav', at);
        if ('problems' in nav)
            return [...problems, ...nav.problems];
        nav.items.forEach((item, i) => {
            const where = `${at}.nav[${i}]`;
            const itemProblems = checkNavItem(item, where);
            problems.push(...itemProblems);
            if (itemProblems.length > 0)
                return;
            const badge = item['badge'];
            if (typeof badge === 'string' && !badgeNames.has(badge)) {
                problems.push({
                    path: `${where}.badge`,
                    says: `names the badge '${badge}', which this manifest does not define`,
                });
            }
        });
        return problems;
    });
}
/**
 * Checks a health response. See {@link Health}.
 *
 * The only union in the contract, and it is checked as one: `ok: true` requires a version and
 * `ok: false` requires a reason. Checking both halves loosely, by asking only that one of the two
 * strings be present, would accept `{ok: false, version: 'abc'}`, which is a tool reporting that it
 * is broken and declining to say why.
 */
export function checkHealth(value, at = 'health') {
    return safely(at, () => {
        const bad = root(value, at, 'a health response');
        if (bad.length > 0)
            return bad;
        const bag = value;
        if (absent(bag, 'ok'))
            return [{ path: `${at}.ok`, says: 'is missing, expected true or false' }];
        if (bag['ok'] === true)
            return str(bag, 'version', at);
        if (bag['ok'] === false)
            return str(bag, 'why', at);
        return [{ path: `${at}.ok`, says: `expected true or false, got ${kindOf(bag['ok'])}` }];
    });
}
/**
 * One line for a test or a log: what is wrong, in the order it was found.
 *
 * Deliberately not thrown and deliberately not coloured. A caller decides whether a malformed
 * response is worth failing a test over or worth one warning line in a server that must stay up.
 */
export function describe(problems) {
    if (problems.length === 0)
        return 'no problems';
    return problems.map((problem) => `${problem.path} ${problem.says}`).join('\n');
}
//# sourceMappingURL=check.js.map