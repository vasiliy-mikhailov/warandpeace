import { jsx as _jsx } from "react/jsx-runtime";
const LAMP = {
    width: '9px',
    height: '9px',
    borderRadius: '50%',
    display: 'block',
    flex: '0 0 auto',
    borderWidth: '1px',
    borderStyle: 'solid',
};
function lampStyle(lit, reached, colour) {
    if (lit) {
        return {
            ...LAMP,
            // Set once and read three times, so the fill, the edge and the glow cannot drift apart. The
            // property is declared here rather than in the contract because no consumer can usefully
            // define it: it holds whatever that caller passed for that lamp.
            '--lamp': colour,
            background: 'var(--lamp)',
            borderColor: 'var(--lamp)',
            boxShadow: '0 0 5px color-mix(in srgb, var(--lamp) 40%, transparent)',
        };
    }
    if (reached) {
        // DIM IS STILL MADE OF THE CALLER'S COLOUR, which is the whole difference from hollow. A stage
        // that ran and answered no is still ABOUT that colour; a wash inside a solid ring says the lamp
        // is there and is off. No glow, because the glow is what "the build said so" looks like.
        return {
            ...LAMP,
            '--lamp': colour,
            background: 'color-mix(in srgb, var(--lamp) 18%, transparent)',
            borderColor: 'color-mix(in srgb, var(--lamp) 55%, transparent)',
        };
    }
    // HOLLOW IS NOT "NO", AND IT CARRIES NONE OF THE CALLER'S COLOUR. Dashed, empty and neutral,
    // because a stage that was never reached has nothing to say in that colour. Three signals part it
    // from dim, colour and fill and stroke, so a reader who cannot separate two washes still has the
    // dash.
    return {
        ...LAMP,
        background: 'transparent',
        borderColor: 'var(--border-strong)',
        borderStyle: 'dashed',
    };
}
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
export function Lamp({ lit, reached, colour, label }) {
    return _jsx("span", { role: "img", "aria-label": label, title: label, style: lampStyle(lit, reached, colour) });
}
//# sourceMappingURL=Lamp.js.map