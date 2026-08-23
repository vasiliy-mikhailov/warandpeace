package tech.mikhailov.wp.sections;

import java.io.IOException;

import tech.mikhailov.ratchet.flow.Agent;
import tech.mikhailov.ratchet.flow.Flow;
import tech.mikhailov.wp.Chain;

/**
 * ONE SECTION'S TRIAD, AND WHAT IT LEAVES BEHIND.
 *
 * <p>One implementation per section, in its own file, because the eight are independent — nothing
 * any of them produces is an input to another — and because eight authors editing one class is how
 * a merge conflict becomes a design.
 */
public interface Work {

    Agent planner(Chain.Hero hero, Chain.Chapter chapter);

    Flow.Doer doer(Chain.Hero hero, Chain.Chapter chapter);

    Agent verifier(Chain.Hero hero, Chain.Chapter chapter);

    String facts(Chain.Hero hero, Chain.Chapter chapter) throws IOException;

    /** The section's answer as JSON, for the line appended to its reading file. */
    String body();
}
