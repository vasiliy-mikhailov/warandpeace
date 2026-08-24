/**
 * A STYLE OBJECT THAT MAY ALSO CARRY CSS CUSTOM PROPERTIES.
 *
 * React's `CSSProperties` has no index signature, so `{'--tone': tone}` is a type error without
 * help. The help is worth having because of what it enables: a component sets a token ONCE and
 * refers to it from three declarations, text and background and border, instead of repeating the
 * same `var(--state-pass)` three times and letting one of the three drift the day the tone changes.
 *
 * WHY THE COMBINATOR RATHER THAN A FINISHED TYPE, which is the whole design of this file.
 *
 * The obvious version of this file is `export type Style = CSSProperties & Record<...>`, and it
 * cannot be written here: this package has no dependency on React and no peer dependency on it
 * either. That is deliberate, and the reason is that a peer dependency on React for the sake of one
 * type would make every consumer of this package resolve a React version, including the consumers
 * that never render anything.
 *
 * The next obvious version is to restate `CSSProperties` locally. That is worse than it looks. A
 * faithful restatement is a large thing to maintain and to be wrong about, and the cheap
 * approximation, an index signature over every string key, accepts `{colour: 'red'}` and every
 * other typo along with it. A type that accepts everything is not a weaker version of a type that
 * accepts the right things; it is the absence of one, wearing its name.
 *
 * So this file exports the piece that genuinely does not depend on React, {@link WithTokens}, and
 * lets a consumer supply the base type it already has in its own dependency tree. A React consumer
 * writes `WithTokens<CSSProperties>` and gets the precise thing, with no version negotiation. A
 * consumer that is not rendering React writes {@link Style} and gets the loose thing knowingly.
 */
/**
 * The custom properties: any `--name`, holding a string or a number.
 *
 * A number is allowed because a token can be a unitless quantity, an opacity or a z-index or a
 * flex-grow, and pushing those through `String()` at every call site buys nothing.
 */
export type Tokens = Record<`--${string}`, string | number>;
/**
 * Add CSS custom properties to whatever style type a consumer already has.
 *
 * The intended use, from a project that has React in its dependencies:
 *
 *     import type { CSSProperties } from 'react'
 *     import type { WithTokens } from 'ratchet-ui'
 *
 *     type Style = WithTokens<CSSProperties>
 *
 * That is exactly the type this would have exported if it were allowed to import React, and it
 * costs the consumer one line in one file.
 */
export type WithTokens<Base> = Base & Tokens;
/**
 * The same thing for a consumer with no `CSSProperties` to hand.
 *
 * KNOWINGLY LOOSE. The base half accepts any string key, so it will not catch a misspelled property
 * name the way {@link WithTokens} over React's type would. It is here so that code which only needs
 * to pass a style object through, a layout helper or a test fixture, does not have to invent a
 * type; anything that is actually authoring styles should prefer `WithTokens<CSSProperties>`.
 */
export type Style = WithTokens<{
    [property: string]: string | number | undefined;
}>;
//# sourceMappingURL=style.d.ts.map