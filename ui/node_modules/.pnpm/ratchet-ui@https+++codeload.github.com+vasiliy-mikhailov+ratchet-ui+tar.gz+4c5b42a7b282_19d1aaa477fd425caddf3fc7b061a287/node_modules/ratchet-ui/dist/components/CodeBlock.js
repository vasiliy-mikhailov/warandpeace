import { jsx as _jsx } from "react/jsx-runtime";
/**
 * Java's words, its strings and its comments, in ONE ALTERNATION SCANNED ONCE.
 *
 * THE GUARANTEE IS CONSUMPTION, NOT PRECEDENCE, and this is the sentence to read before editing.
 * A global pattern finds the earliest match position, takes the whole token there, and resumes
 * AFTER it. `// the "public" API` matches the comment branch at index 0 and eats to end of line,
 * so the `"` seven characters along is never offered to the scanner and `public` is never a
 * starting position. That is why a keyword inside a string stays inside the string and a quote
 * inside a comment cannot open one. Four separate replacements over the same text have no such
 * property, and they are how that line comes back as half a comment with a stray keyword in it.
 *
 * THE ORDER IS THE INTENDED PRECEDENCE AND IS NOT WHAT DOES THE WORK TODAY. The four branches have
 * disjoint first characters: `/` for a comment, a quote for a string, a letter for a word, a digit
 * for a number. Order only decides ties at one index and there are no ties, which was checked by
 * running this alternation and its exact reverse over seven adversarial inputs and getting
 * identical tokens from both. It is kept because it states what precedence should be, and because
 * the first branch anyone adds whose opening character overlaps another, an annotation `@\w+` or a
 * text block, will need it. What it is not is the reason strings work, and a reader who believes it
 * is will go and reorder branches the day one breaks.
 *
 * ONLY EVER HANDED TO `matchAll`. Never `exec`, never `test`. See {@link colourJava}.
 *
 * FROM THE SIBLING DASHBOARD'S REQUEST. The one-alternation design, the branch set and the
 * `language` prop are theirs; the argument that carried it is that colouring by four passes cannot
 * keep a token's insides out of another token's reach, and that a rule which excluded itself from
 * deciding this pair had handed the case to nothing.
 */
const JAVA = /(?<comment>\/\/[^\n]*|\/\*[\s\S]*?\*\/)|(?<string>"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')|(?<word>\b(?:abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|var|void|volatile|while|true|false|null|record|sealed|yield)\b)|(?<number>\b\d[\w.]*)/g;
/** The four token colours, from the contract. A colour never appears here as a literal. */
const COLOUR = {
    comment: 'var(--code-comment)',
    string: 'var(--code-string)',
    word: 'var(--code-keyword)',
    number: 'var(--code-number)',
};
function kindOf(groups) {
    return groups['comment'] !== undefined
        ? 'comment'
        : groups['string'] !== undefined
            ? 'string'
            : groups['word'] !== undefined
                ? 'word'
                : 'number';
}
/**
 * The source cut into coloured spans and the plain text between them.
 *
 * `matchAll` AND NOTHING ELSE, AND NOT FOR THE REASON IT LOOKS LIKE. It does not reset the pattern's
 * cursor; it clones the pattern, copies the current `lastIndex` into the clone, and advances only
 * the clone. What it therefore never does is WRITE the shared `lastIndex` back, so a lexing pass
 * cannot leave a cursor behind for the next one, and every block starts at zero because nothing
 * moves it off zero. An `exec` loop over this same module-level pattern has the opposite property
 * and fails silently: any early exit leaves the cursor mid-source and the NEXT block on the page
 * begins scanning from the middle of its own text with its opening tokens uncoloured. Measured, so
 * that this is a fact rather than a worry: after one `JAVA.exec('int a; int b;')` the cursor sits at
 * 3, and `[...'int a;'.matchAll(JAVA)]` then returns the empty array. Two blocks are the ordinary
 * case rather than the edge one, and a development-mode double render is a second way in.
 */
function colourJava(source) {
    const out = [];
    let at = 0;
    for (const match of source.matchAll(JAVA)) {
        // A match always carries both. The fallbacks are for the type, which cannot know that.
        const text = match[0] ?? '';
        const start = match.index ?? at;
        if (start > at) {
            out.push(source.slice(at, start));
        }
        const kind = kindOf(match.groups ?? {});
        out.push(_jsx("span", { 
            // Italic is the fourth channel, and it is the one that survives a monochrome display and a
            // reader who cannot separate two greys. It costs no token.
            style: { color: COLOUR[kind], fontStyle: kind === 'comment' ? 'italic' : 'normal' }, children: text }, start));
        at = start + text.length;
    }
    out.push(source.slice(at));
    return out;
}
/**
 * The shell the block renders into.
 *
 * THE SHELL IS bump-java-version's AND THE MARGIN BELONGS TO NEITHER. Both repositories wrote this
 * box and the difference between the two is chrome, which is the case rule one governs and which it
 * gives to bump-java-version. The one declaration that is genuinely per-site is the margin, which
 * disagrees THREE ways across the two repositories, so it is not here at all. That is the
 * arrangement `HEADING` already ships under: the shell is the library's and the margin stays the
 * caller's, in a wrapper.
 *
 * THE FONT STACK IS NAMED RATHER THAN LEFT TO THE BROWSER, and it is here because of what its
 * absence proved. bump-java-version's component carried it and the inline copy on its settings page
 * had dropped it, which is an unused component drifting from its own duplicate: exactly the
 * evidence that decided this case. Shipping the shared version without it would keep the loser of
 * that drift.
 *
 * ESCAPING LIVES IN THIS COMPONENT. It emits text nodes and spans it builds itself. Never
 * `dangerouslySetInnerHTML`, and never pre-coloured markup from a server, because either puts the
 * escaping back in the hands of every caller and one caller will hand it a diff with a `<` in it.
 */
const BLOCK = {
    padding: '10px 12px',
    overflowX: 'auto',
    borderRadius: '6px',
    background: 'var(--bg-subtle)',
    border: '1px solid var(--border-soft)',
    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
    fontSize: '12px',
    lineHeight: 1.5,
    color: 'var(--text-secondary)',
    margin: 0,
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
export function CodeBlock({ code, language }) {
    if (code.trim().length === 0) {
        return null;
    }
    return _jsx("pre", { style: BLOCK, children: language === 'java' ? colourJava(code) : code });
}
//# sourceMappingURL=CodeBlock.js.map