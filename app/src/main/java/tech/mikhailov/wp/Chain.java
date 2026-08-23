package tech.mikhailov.wp;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import tech.mikhailov.ratchet.flow.Agent;
import tech.mikhailov.ratchet.flow.Flow;
import tech.mikhailov.ratchet.record.Journal;
import tech.mikhailov.ratchet.record.Trace;

/**
 * THE PROGRAM, AND THEREFORE THE SHAPE.
 *
 * <p>There is no diagram of this pipeline anywhere, deliberately. {@link Flow#shape} walks the tree
 * this class builds, which is the same object the runtime executes, so a stage cannot be advertised
 * after it is deleted — deleting it deletes the node the picture was reading. An earlier revision of
 * this design was an HTML page drawn beside the program, and it went stale inside one revision,
 * which is the argument {@code Shape}'s own javadoc makes at length.
 *
 * <p>THE SIX SECTIONS OF A FANDOM CHARACTER PAGE ARE SIX TRIADS. That is not a rendering decision
 * that leaked into the pipeline; it is the other way round. Fandom's guide prescribes the sections
 * and puts {@code == Character arc ==} first, so the page is the specification and each section is
 * an extraction question asked of every chapter, with its own verifier.
 *
 * <p>They run in {@link Flow#seq} rather than in parallel because they depend on each other:
 * {@code Quotes} needs the attribution, {@code Appearances} needs the arc, {@code Relationships}
 * needs to know who is present.
 */
public final class Chain {

    /**
     * The six, in dependency order, with the strength of each verifier written beside it.
     *
     * <p>The order of this enum is the order of the sequence, and the two at the bottom are the ones
     * to distrust: a triad whose verifier cannot reject is a triad only in shape.
     */
    public enum Section {
        /** Present, mentioned-only, or absent. Verify: mechanical — the name occurs, an arc entry exists. */
        APPEARANCES("appearances"),
        /** What happened to them here. Verify: every entry anchored to a paragraph in this chapter. */
        ARC("character-arc"),
        /** What they said. Verify: mechanical and exact — the string occurs verbatim in the chapter. */
        QUOTES("quotes"),
        /** With whom, and which direction. Verify: both parties real, the relation directional. */
        RELATIONSHIPS("relationships"),
        /** What this chapter shows them to be like. Verify: each trait cites a span that supports it. */
        PERSONALITY("personality"),
        /** What is odd. Verify: true against a span — but "interesting" is not checkable. */
        TRIVIA("trivia"),
        /**
         * What this chapter CALLS them. Verify: mechanical — the surface form occurs in the text.
         *
         * <p>The seventh, and the one that makes identity computable rather than lucky. Every other
         * section asks what happened; this asks what the character was named while it happened —
         * "Pierre", "Monsieur Pierre", "the young man", "this young Jacobin". Grounded, and the
         * verifier is a substring test.
         */
        NAMES("names"),
        /**
         * What is true of them as this chapter states it: status, rank, house, relatives.
         *
         * <p>The infobox asks who they are, and nothing else here answers that. Extracted PER
         * CHAPTER on purpose, because the answers move: Andrew is alive until Book Twelve, and
         * Pierre is illegitimate until he is not. A single infobox value would flatten a change
         * into a snapshot and lose which book it happened in.
         */
        FACTS("facts");

        private final String stage;

        Section(String stage) {
            this.stage = stage;
        }

        public String stage() {
            return stage;
        }
    }

    /** How many rounds a verifier may hold one section's triad before the round is given up. */
    static final int ROUNDS = 3;

    private final Corpus corpus;
    private final Journal journal;
    private final Trace trace;
    private final Sections sections;

    public Chain(Corpus corpus, Journal journal, Trace trace, Sections sections) {
        this.corpus = corpus;
        this.journal = journal;
        this.trace = trace;
        this.sections = sections;
    }

    /**
     * PASS ONE. Additive: every node writes and nothing reconciles, so a lane can stop between any
     * two chapters and lose only the chapter it was in.
     *
     * <p>EVERY CHAPTER IS READ FOR EVERY CHARACTER, and there is no cheap skip in front of it any
     * more. There was one — a regex asking whether the name occurred, which removed 72% of the
     * triads — and it was removed because it can only be wrong in the direction nobody sees. A
     * chapter that calls Pierre "the young man" and never "Pierre" does not match, and chapter XXIV
     * of Book One is exactly such a chapter for part of its length. The skip left no record, so a
     * wrong skip was indistinguishable from a chapter nobody had reached.
     *
     * <p>The cost of that honesty is real: 6 heroes x 365 chapters x 6 sections is 13,140 triads
     * rather than 3,720. If it needs paying down later, the gate to reach for is {@code appearances}
     * itself — it already answers present / mentioned-only / absent, it reads the prose rather than
     * matching a string, and its verdict can guard the other five. That trades a silent regex for a
     * recorded judgement, which is the same speedup bought honestly.
     */
    // A NODE IS NAMED FOR WHAT IT ITERATES, NOT FOR THE PHASE IT BELONGS TO. Naming this one
    // "extract" made the shape print `extract -> book -> chapter`, and a reader correctly concluded
    // the outer loop was the book. The structure was right and the label lied, which is the failure
    // Shape exists to prevent, arriving through the one door it does not watch.
    //
    // The record is `Hero` rather than `Character` on purpose: a nested `Chain.Character` shadows
    // `java.lang.Character` everywhere inside this class, which is a trap for whoever writes the
    // first `Character.isDigit` here. The node carries the wiki's word; the type avoids the clash.
    public Agent extract() {
        return Flow.each("character", corpus::heroes, Hero::slug, hero ->
                Flow.each("book", corpus::books, Book::title, book ->
                        Flow.each("chapter", () -> corpus.chaptersIn(book), Chapter::slug, chapter ->
                                Flow.seq("",
                                        section(Section.APPEARANCES, hero, chapter),
                                        section(Section.ARC, hero, chapter),
                                        section(Section.QUOTES, hero, chapter),
                                        section(Section.RELATIONSHIPS, hero, chapter),
                                        section(Section.PERSONALITY, hero, chapter),
                                        section(Section.TRIVIA, hero, chapter),
                                        section(Section.NAMES, hero, chapter),
                                        section(Section.FACTS, hero, chapter)))));
    }

    /**
     * PASS TWO. Lossy, so the gate cannot be preservation. Chapters fold into a book and books fold
     * into a page, which is one operation used at two levels rather than two operations.
     *
     * <p>Every fragment must reach a recorded decision — kept, merged into a named output, or
     * dropped with a reason. Pierre has around eight hundred of them, and a fold that quietly
     * forgets Book Nine produces a page that looks perfectly plausible.
     */
    public Agent compact() {
        return Flow.each("character", corpus::heroes, Hero::slug, hero ->
                Flow.seq("fold",
                        Flow.each("book", corpus::books, Book::title, book ->
                                foldInto("fold-book", hero, of(book))),
                        foldInto("fold-page", hero, "page")));
    }

    /**
     * ONE FOLD, AND IT IS SIX FOLDS, BECAUSE COMPACTION IS NOT ONE OPERATION.
     *
     * <p>An earlier version of this method was a single {@link Flow#code} step, which is a
     * {@code Step} and therefore has NO VERIFIER. That is the whole of what was wrong with it: the
     * design said at length that a lossy fold is gated by accounting — every fragment kept, merged
     * into a named output, or dropped with a reason — and then composed it as a node that cannot
     * reject anything. A gate described in prose and absent from the tree is not a gate.
     *
     * <p>Splitting it by section shows the second thing. Three of the six do not need a model at
     * all: {@code character-arc} and {@code appearances} concatenate in chapter order, and
     * {@code personality} accumulates and must NOT collapse — the contradiction between chapter V
     * Pierre and Book Fourteen Pierre IS the arc. Those are {@code code}, honestly marked, and they
     * are the sections that matter most. Only the three that exercise judgement — merging a
     * recurring pair into one relationship, selecting what is outstanding — are triads.
     */
    private Agent foldInto(String name, Hero hero, String level) {
        return Flow.seq(name,
                folded(Section.ARC, hero, level),
                folded(Section.APPEARANCES, hero, level),
                folded(Section.PERSONALITY, hero, level),
                folded(Section.RELATIONSHIPS, hero, level),
                folded(Section.QUOTES, hero, level),
                folded(Section.TRIVIA, hero, level),
                folded(Section.NAMES, hero, level),
                folded(Section.FACTS, hero, level));
    }

    /**
     * Whether a section's fold is a decision or an arrangement.
     *
     * <p>{@code NAMES} IS DELIBERATELY NOT A DECISION. Folding aliases is a set union, and the thing
     * worth finding — one surface form denoting two different people — is a CONFLICT, which code
     * detects and a model would only be invited to explain away. "Count Bezúkhov" maps to Pierre's
     * father in chapters I to XXI and to Pierre from XXIV; that is two entries under one key, found
     * by comparison, and asking a model to reconcile them is asking it to delete the finding.
     */
    private static boolean judged(Section section) {
        return section == Section.RELATIONSHIPS || section == Section.QUOTES
                || section == Section.TRIVIA || section == Section.FACTS;
    }

    private Agent folded(Section section, Hero hero, String level) {
        Supplier<String> key = () -> of(hero) + "#" + level + "#fold-" + section.stage();
        Agent node = judged(section)
                ? Flow.triad(section.stage(),
                        sections.foldPlanner(section, hero, level),
                        sections.foldDoer(section, hero, level),
                        sections.accounting(section, hero, level),
                        () -> sections.fragments(section, hero, level),
                        trace, key.get(), ROUNDS)
                : Flow.code(section.stage(), task -> sections.arrange(section, hero, level));
        return Flow.resumable(node, journal, key);
    }

    /**
     * ONE SECTION OF ONE CHAPTER FOR ONE HERO, AND THE JOURNAL KEY IS ALL THREE.
     *
     * <p>THE WRAPPING LEVEL IS THE WHOLE DECISION HERE. {@link Flow#resumable} writes its row only
     * once the wrapped node returns and nothing inside a node survives — measured on ratchet at
     * seven paid model calls destroyed by a failure on round three of a triad, with the journal file
     * never created. Wrapping the CHAPTER would mean a failure in the sixth section discards five
     * finished ones, about fifteen calls. Wrapping each SECTION costs about three. Same code, five
     * times the loss, one line apart.
     */
    private Agent section(Section section, Hero hero, Chapter chapter) {
        // NULL-TOLERANT BECAUSE THE PICTURE IS DRAWN BEFORE ANYTHING RUNS. `Flow.each` builds its
        // body with a null item to walk the shape, and says so: a shape is what the program CAN do,
        // not what one execution did. So the key renders as a template here and as a key at run time.
        Supplier<String> key = () -> of(hero) + "#" + of(chapter) + "#" + section.stage();
        return Flow.resumable(
                Flow.triad(section.stage(),
                        sections.planner(section, hero, chapter),
                        sections.doer(section, hero, chapter),
                        sections.verifier(section, hero, chapter),
                        () -> sections.facts(section, hero, chapter),
                        trace, key.get(), ROUNDS),
                journal, key);
    }

    private static String of(Hero hero) {
        return hero == null ? "{hero}" : hero.slug();
    }

    private static String of(Book book) {
        return book == null ? "{book}" : book.title();
    }

    private static String of(Chapter chapter) {
        return chapter == null ? "{chapter}" : chapter.slug();
    }

    /** What the run reads. Named here so the shape can be walked without a corpus on disk. */
    public interface Corpus {
        List<Hero> heroes();

        List<Book> books();

        List<Chapter> chaptersIn(Book book);

    }

    /** What each triad is made of. One implementation per {@link Section}. */
    public interface Sections {
        Agent planner(Section section, Hero hero, Chapter chapter);

        Flow.Doer doer(Section section, Hero hero, Chapter chapter);

        Agent verifier(Section section, Hero hero, Chapter chapter);

        String facts(Section section, Hero hero, Chapter chapter) throws IOException;

        /** For the three folds that are arrangements, not decisions. No model is asked. */
        String arrange(Section section, Hero hero, String level) throws IOException;

        Agent foldPlanner(Section section, Hero hero, String level);

        Flow.Doer foldDoer(Section section, Hero hero, String level);

        /** The gate: every fragment kept, merged into a named output, or dropped with a reason. */
        Agent accounting(Section section, Hero hero, String level);

        String fragments(Section section, Hero hero, String level) throws IOException;
    }

    public record Hero(String slug, String name) {
    }

    public record Book(String title) {
    }

    public record Chapter(String slug, String book, String numeral) {
    }
}
