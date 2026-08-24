/**
 * A HOOK, ON THE SUBPATH FOR THINGS THAT NEED REACT.
 *
 * Nothing here renders, so it sits oddly beside the components. It sits here anyway because the
 * line this package's entry points draw is React and not JSX: what `ratchet-ui/components` promises
 * is that importing it costs a React resolution, and what the root entry promises is that importing
 * THAT does not. A hook is on the paying side of that line, and a third subpath for one file would
 * be a third thing for a consumer to learn.
 */
import { useState } from 'react';
/**
 * WHAT A REFUSAL SAYS WHEN THE SERVER WOULD NOT SAY.
 *
 * A control that reports nothing after a click that changed nothing is the worst of the four
 * states: the reader cannot tell a refusal from a click that missed. Both endpoints on this site
 * may answer with a refusal and no reason, so the sentence is here rather than typed out beside
 * each of them, where the two copies had already begun to matter: one page would have said
 * "declined" and the other nothing at all.
 */
export const NO_REASON = 'the server declined without saying why';
/**
 * A REFUSAL AND AN OUTAGE HAVE TO READ DIFFERENTLY, which is the whole reason this prefix exists.
 *
 * Both endpoints answer a refusal with 200 and a body, so the only thing that ever reaches the
 * catch is the network, the page being served without its api behind it, or a body that would not
 * parse. A wrong slug and an unreachable server are different problems with different next moves,
 * and a control that renders both as "it did not work" sends a reader to look at the wrong one.
 */
export const REQUEST_FAILED = 'the request itself failed: ';
/**
 * THE FOUR STATES OF ASKING A SERVER TO DO SOMETHING: idle, busy, done, refused.
 *
 * Two controls in the bump page's corner had this written out longhand, and they got it subtly
 * differently in three places, all of which are the refusal handling:
 *
 * <ol>
 *   <li>What counts as a refusal. One read a boolean off the answer; the other compared the state
 *       the server read back against the state that was asked for. Both are right for their own
 *       endpoint, which is why `read` is the caller's and everything else is not.</li>
 *   <li>What is cleared when a new ask starts. Both cleared the refusal, and they had to: a control
 *       still wearing the last refusal while a fresh ask is in flight is lying about the present.
 *       Only one of them cleared its "it worked" flag as well, and the other could briefly show
 *       done and busy at once.</li>
 *   <li>What a thrown request says. Both prefixed it, by hand, with the same sentence, which is
 *       to say the sentence was one edit away from being two sentences.</li>
 * </ol>
 *
 * NOTHING HERE IS DISABLED. That belongs to the control: `aria-disabled` rather than the attribute,
 * because the real one drops a button out of the tab order mid-action and strands a keyboard user
 * on the document body. This hook only says whether an ask is in flight; both controls check it
 * before calling `ask` again, and a second call while busy is the caller's to refuse.
 */
export function useAsk(how) {
    const [state, setState] = useState({ asks: 0, busy: false, landed: false, refused: '' });
    // Not memoised, and deliberately: it closes over THIS render's `how`, whose `send` and `onAnswer`
    // close over this render's state in turn. A callback held stable across renders would be asking
    // with a slug the page had already navigated away from.
    const ask = (input) => {
        setState((s) => ({ asks: s.asks + 1, busy: true, landed: false, refused: '' }));
        how
            .send(input)
            .then((answer) => {
            const landing = how.read(answer, input);
            setState((s) => ({
                ...s,
                busy: false,
                landed: landing.landed,
                refused: landing.landed ? '' : (landing.why ?? NO_REASON),
            }));
            how.onAnswer?.(answer, input);
        })
            .catch((e) => setState((s) => ({ ...s, busy: false, refused: `${REQUEST_FAILED}${e.message}` })));
    };
    return { ...state, ask };
}
//# sourceMappingURL=useAsk.js.map