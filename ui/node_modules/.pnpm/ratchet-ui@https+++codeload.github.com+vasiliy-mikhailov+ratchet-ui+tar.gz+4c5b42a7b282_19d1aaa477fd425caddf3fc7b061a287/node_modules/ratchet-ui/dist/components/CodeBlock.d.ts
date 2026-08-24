export type CodeBlockProps = {
    code: string;
    /**
     * Colour it as this language, or leave it uncoloured.
     *
     * ABSENT MEANS NO COLOURING, WHICH IS THE HONEST RENDER AND NOT A SHORTFALL. This lexer knows
     * Java and nothing else. Pointed at Kotlin, XML, a stack trace or a git diff it paints `int`,
     * `class` and `new` wherever those letters fall as words, and one attribute quote opens a string
     * that swallows the rest of the fragment. Colour is a claim about what a token IS, so a wrong
     * claim is worse than none: the reader cannot see that it was guessed. A default of `java` would
     * also invert who has to act, making every caller opt out of being lied to, and the caller who
     * forgets would get no error. That is the incident this prop exists because of: the sibling's
     * Java `block()` ran its colouriser over everything it was handed.
     */
    language?: 'java';
};
/**
 * A block of source, escaped by React and coloured by what the caller says it is.
 *
 * BLANK RENDERS NOTHING. An empty bordered box says there is source and that the source is empty,
 * which is a different statement and a wrong one: what actually happened is that nothing was
 * recorded.
 *
 * FROM THE SIBLING DASHBOARD'S REQUEST, WITH THE ARGUMENT THAT CARRIED IT. This project's rule one
 * asks that both dashboards wrote the thing AND that the versions differ in palette rather than
 * behaviour. These two differ in behaviour, so the rule excluded itself and then handed the case to
 * nothing, and what got written down as the reason to decline was a description of everything this
 * version does MORE. The amendment says where the versions differ in behaviour the shared one is
 * the version with call sites, which is this one: three, against an unused twenty-three lines that
 * had already drifted from its own inline copy.
 */
export declare function CodeBlock({ code, language }: CodeBlockProps): import("react").JSX.Element | null;
//# sourceMappingURL=CodeBlock.d.ts.map