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
export {};
//# sourceMappingURL=style.js.map