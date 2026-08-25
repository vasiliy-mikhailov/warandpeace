package tech.mikhailov.wp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import tech.mikhailov.ratchet.flow.Agent;
import tech.mikhailov.ratchet.flow.Flow;
import tech.mikhailov.ratchet.record.Journal;
import tech.mikhailov.ratchet.record.Trace;

/**
 * PRINTS THE SHAPE, AND PRINTS NOTHING ELSE.
 *
 * <p>Run it to see what the program is. It builds the same tree {@code main} runs and hands it to
 * {@link Flow#shape}, so there is no second copy of the answer to keep in step. The corpus it walks
 * is a stand-in — a handful of heroes and books, enough to show the nesting — because the SHAPE does
 * not depend on how many chapters there are, only on how they are composed.
 */
public final class Picture {

    private Picture() {
    }

    public static void main(String[] args) throws IOException {
        Chain chain = new Chain(new Sketch(), new Journal(Path.of("/tmp/wp-shape.journal.jsonl")),
                QUIET, new Unbuilt());

        System.out.println("── one pass, the page carried ─────────────────────");
        System.out.print(Flow.shape(chain.read()));
        System.out.println();
        System.out.println("── what it replaced: pass 1 ───────────────────────");
        System.out.print(Flow.shape(chain.extract()));
        System.out.println();
        System.out.println("── what it replaced: pass 2 ───────────────────────");
        System.out.print(Flow.shape(chain.compact()));
        System.out.println();
        System.out.println("named nodes of the page pass, in the order it reaches them:");
        for (String name : Flow.names(chain.read())) {
            System.out.println("  " + name);
        }
    }

    /** Printing the shape runs nothing, so nothing is recorded. */
    private static final Trace QUIET = new Trace() {
        public void asked(String a, String p, String r) { }
        public void applied(String s, String w) { }
        public void tool(String a, String t, String g, String r) { }
        public void thought(String f, String t, String c) { }
        public void built(String p, Trace.Outcome r) { }
        public void settled(String k, String s, String w, boolean b, boolean a) { }
        public void failed(String k, Throwable c) { }
        public void progress(String k, String n) { }
        public void priced(String k, String m, String i) { }
    };

    /** Two heroes, two books, two chapters each. Enough to show nesting; the shape is not a count. */
    private static final class Sketch implements Chain.Corpus {
        public List<Chain.Hero> heroes() {
            return List.of(new Chain.Hero("pierre", "Pierre"), new Chain.Hero("andrew", "Prince Andrew"));
        }

        public List<Chain.Book> books() {
            return List.of(new Chain.Book("BOOK ONE"), new Chain.Book("BOOK TWO"));
        }

        public List<Chain.Chapter> chaptersIn(Chain.Book book) {
            return List.of(new Chain.Chapter("ch-i", book.title(), "I"),
                    new Chain.Chapter("ch-ii", book.title(), "II"));
        }
    }

    /**
     * The triads, not yet written.
     *
     * <p>It throws rather than returning something plausible: a shape printed off stubs that quietly
     * answered would be a picture of a program that does not exist, which is the failure this whole
     * file is arranged against.
     */
    private static final class Unbuilt implements Chain.Sections {
        public Agent planner(Chain.Section s, Chain.Hero h, Chain.Chapter c) {
            return task -> {
                throw new IllegalStateException("no planner for " + s.stage() + " yet");
            };
        }

        public Flow.Doer doer(Chain.Section s, Chain.Hero h, Chain.Chapter c) {
            return (plan, feedback) -> {
                throw new IllegalStateException("no doer for " + s.stage() + " yet");
            };
        }

        public Agent verifier(Chain.Section s, Chain.Hero h, Chain.Chapter c) {
            return task -> {
                throw new IllegalStateException("no verifier for " + s.stage() + " yet");
            };
        }

        public Agent pagePlanner(Chain.Hero h, Chain.Chapter c) {
            return task -> {
                throw new IllegalStateException("no page planner yet");
            };
        }

        public Flow.Doer pageDoer(Chain.Hero h, Chain.Chapter c) {
            return (plan, feedback) -> {
                throw new IllegalStateException("no page doer yet");
            };
        }

        public Agent pageVerifier(Chain.Hero h, Chain.Chapter c) {
            return task -> {
                throw new IllegalStateException("no page verifier yet");
            };
        }

        public String page(Chain.Hero h) {
            throw new IllegalStateException("no page yet");
        }

        public String facts(Chain.Section s, Chain.Hero h, Chain.Chapter c) {
            throw new IllegalStateException("no facts for " + s.stage() + " yet");
        }

        public String arrange(Chain.Section s, Chain.Hero h, String level) {
            throw new IllegalStateException("no arrangement for " + s.stage() + " yet");
        }

        public Agent foldPlanner(Chain.Section s, Chain.Hero h, String level) {
            return task -> { throw new IllegalStateException("no fold planner for " + s.stage()); };
        }

        public Flow.Doer foldDoer(Chain.Section s, Chain.Hero h, String level) {
            return (plan, feedback) -> { throw new IllegalStateException("no fold doer for " + s.stage()); };
        }

        public Agent accounting(Chain.Section s, Chain.Hero h, String level) {
            return task -> { throw new IllegalStateException("no accounting for " + s.stage()); };
        }

        public String fragments(Chain.Section s, Chain.Hero h, String level) {
            throw new IllegalStateException("no fragments for " + s.stage() + " yet");
        }
    }
}
