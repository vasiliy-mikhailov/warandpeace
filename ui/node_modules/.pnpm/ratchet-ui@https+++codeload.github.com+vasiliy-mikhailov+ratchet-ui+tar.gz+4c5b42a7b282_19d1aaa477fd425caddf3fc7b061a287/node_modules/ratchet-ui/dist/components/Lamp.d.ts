export type LampProps = {
    /** The build said so. */
    lit: boolean;
    /** This stage was got to at all. */
    reached: boolean;
    /**
     * The caller's colour, as a value or as one of its own custom properties.
     *
     * IT IS A PROP SO THAT NO BUILD VOCABULARY TRAVELS. What red and green MEAN belongs to the
     * pipeline drawing the lamps, and the two pipelines using this do not mean the same thing by
     * either word. One of them means a test that failed on purpose; the other means a suite that was
     * already whole before anything was touched. A component that named those would have chosen one
     * pipeline's vocabulary for both.
     */
    colour: string;
    /**
     * The whole sentence, and the caller owns what its lamps mean.
     *
     * NOT A FRAGMENT THIS COMPONENT COMPLETES. The obvious design has the caller name the lamp and
     * this component append an appearance word, and it is wrong twice over: it fixes the wording of
     * the three states here, and it makes every label a sentence assembled by two authors who cannot
     * read each other. The caller varies the whole sentence by state, which is the same number of
     * strings and one author.
     */
    label: string;
};
/**
 * One lamp: lit, dim, or hollow.
 *
 * THREE APPEARANCES, AND THE MIDDLE ONE IS THE ENTIRE POINT. Lit means the build said so. Dim means
 * the stage was reached and it did not happen. Hollow means it was never got to. A lamp that was
 * only ever on or off would tell a reader that a stage which ran and answered no and a stage that
 * never ran are the same answer, and they are not: one is a fact about the work and the other is a
 * fact about how far the work got.
 *
 * WHAT SEPARATES DIM FROM HOLLOW IS THREE SIGNALS RATHER THAN ONE. Colour identity: dim is a wash
 * of the caller's colour inside a ring of it, hollow carries none of it and is drawn in the neutral
 * contract border. Fill: dim has something inside the ring, hollow is empty. Stroke: dim's ring is
 * continuous, hollow's is dashed. Three, so that a reader who cannot separate two washes of one
 * colour still has the dash and a reader looking at a monochrome print still has fill against no
 * fill. Opacity on the whole element was the obvious alternative and is the wrong one: it fades the
 * ring and the fill by the same factor, so a faint dim lamp and a hollow lamp converge into the
 * same circle, which is exactly the collapse the three states exist to prevent.
 *
 * LIT WINS OVER `reached`. A lamp cannot be lit without having been reached, so `lit && !reached` is
 * contradictory input rather than a fourth state, and the lit fact is the one a build actually
 * recorded. Resolved in that order, silently and on purpose.
 *
 * `role="img"` WITH THE WHOLE SENTENCE AS ITS LABEL, not a bare `title` on an empty element. These
 * labels carry the standard of proof, and a decorative element with a tooltip is invisible to
 * anyone not holding a mouse. `title` is kept alongside for the reader who does have one.
 *
 * FROM THE SIBLING DASHBOARD'S REQUEST, INCLUDING THE LINE THEY DREW AROUND IT. They offered this
 * and explicitly declined to offer the two-lamp component it sits inside, on the grounds that what
 * red and green mean there is their pipeline's vocabulary: a red lamp is a test that failed on
 * purpose, green is reached only when red went red, and a marker nobody has judged has no lamps at
 * all. That stays theirs. What is here is the one thing with no vocabulary in it, which is why
 * `colour` and `label` are props.
 */
export declare function Lamp({ lit, reached, colour, label }: LampProps): import("react").JSX.Element;
//# sourceMappingURL=Lamp.d.ts.map