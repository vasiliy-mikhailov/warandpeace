import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Pill } from './Pill.js';
const SAID = { marginLeft: '8px', fontSize: '12.5px', color: 'var(--text-secondary)' };
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
export function KeyStatus({ keyed, keySource, whenAbsent }) {
    return (_jsxs("div", { style: { margin: '0 0 12px' }, children: [_jsx(Pill, { tone: keyed ? 'good' : 'alarm', children: keyed ? 'key set' : 'no key' }), _jsx("span", { style: SAID, children: keyed ? `the agents are using the key from ${keySource}` : whenAbsent })] }));
}
//# sourceMappingURL=KeyStatus.js.map