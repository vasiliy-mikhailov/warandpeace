import type { ReactNode } from 'react';
export type KeyStatusProps = {
    /** Is there a key at all. */
    keyed: boolean;
    /**
     * WHERE THE KEY CAME FROM, IN THE CONSUMER'S OWN WORDS: "the environment", "this page".
     *
     * Only meaningful when there is one: with no key at all there is no source to report. It is a
     * string rather than a union because the set of places a key can come from is a fact about the
     * deployment rather than about this component, and one of the two consumers has two of them where
     * the other has one.
     */
    keySource: string;
    /**
     * WHAT HAVING NO KEY MEANS HERE, and the one thing about this component that cannot be shared.
     *
     * The two consumers disagree, and it is a stated policy on both sides rather than drift. One
     * settings page can set a key, so its answer is about what to do next. The other deliberately
     * renders no key field at all, on the grounds that a portal several developers reach should not
     * put a credential on screen, so its answer is that every call will be refused until somebody
     * sets one on the container. Neither sentence is true of the other page, so the sentence belongs
     * to the caller.
     */
    whenAbsent: ReactNode;
};
/**
 * WHETHER THE ENDPOINT HAS A KEY, AND WHOSE.
 *
 * THE NAME IS ONE DASHBOARD'S AND THE BEHAVIOUR IS THE OTHER'S, which is worth saying because this
 * file will read as an import to one of them and as a rewrite to the other. One had a component
 * called `KeyStatus`; the other had twelve lines inside its model section and no name for them. The
 * pill's two labels, "key set" and "no key", and the good and alarm tones under them, were already
 * identical in both, which is what made the pair worth settling at all.
 *
 * THE SOURCE SENTENCE SITS BESIDE THE PILL AT READING SIZE rather than under it as an aside. It is
 * the answer to the question the pill raises, and a reader who has just been told there is a key is
 * about to ask whose; an aside two sizes down is where a thing goes when nobody has asked.
 *
 * NO KEY IS EVER RENDERED, and that is not this component's decision to make. It is handed a
 * boolean and a sentence; there is nothing here that could show a credential even if a caller
 * wanted one shown.
 */
export declare function KeyStatus({ keyed, keySource, whenAbsent }: KeyStatusProps): import("react").JSX.Element;
//# sourceMappingURL=KeyStatus.d.ts.map