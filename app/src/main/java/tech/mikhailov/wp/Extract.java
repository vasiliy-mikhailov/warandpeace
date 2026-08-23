package tech.mikhailov.wp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import dev.langchain4j.model.chat.ChatModel;

import tech.mikhailov.ratchet.flow.Agent;
import tech.mikhailov.ratchet.flow.Flow;
import tech.mikhailov.ratchet.llm.Model;
import tech.mikhailov.ratchet.record.Journal;
import tech.mikhailov.ratchet.record.JsonlTrace;
import tech.mikhailov.ratchet.record.Trace;
import tech.mikhailov.wp.sections.Appearances;
import tech.mikhailov.wp.sections.Arc;
import tech.mikhailov.wp.sections.Facts;
import tech.mikhailov.wp.sections.Names;
import tech.mikhailov.wp.sections.Personality;
import tech.mikhailov.wp.sections.Quotes;
import tech.mikhailov.wp.sections.Relationships;
import tech.mikhailov.wp.sections.Trivia;
import tech.mikhailov.wp.sections.Work;

/**
 * ONE SECTION, ONE CHAPTER, ONE CHARACTER — RUN FOR REAL.
 *
 * <p>Deliberately not the whole chain yet. The eight sections are independent and the chain is a
 * loop over them, so the risk is never in the loop; it is in whether a triad against a real endpoint
 * behaves the way the design says. This runs one and writes what it got, so the next seven are built
 * against something that has been observed rather than something that has been argued.
 *
 * <p>Usage: {@code Extract <character> <chapter-slug> <section>}
 */
public final class Extract {

    private Extract() {
    }

    public static void main(String[] args) throws IOException {
        String who = args.length > 0 ? args[0] : "pierre";
        String slug = args.length > 1 ? args[1] : "024-book-one-chxxiv";
        String want = args.length > 2 ? args[2] : "appearances";

        needs();
        Text text = new Text(Path.of("corpus"));
        Chain.Chapter chapter = text.chaptersIn(null).stream()
                .filter(c -> c.slug().equals(slug)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no chapter " + slug));
        Chain.Hero hero = new Chain.Hero(who, name(who));

        Path records = Files.createDirectories(Path.of("records"));
        Trace trace = new JsonlTrace(records.resolve(who + ".trace.jsonl"),
                records.resolve("settlements.jsonl"), who);
        Journal journal = new Journal(records.resolve(who + ".journal.jsonl"));

        ChatModel model = Model.forProducer(trace);
        Work work = sectionFor(want, text, model);

        List<String> paragraphs = text.paragraphs(chapter);
        System.out.println(hero.name() + " · " + chapter.book() + " ch " + chapter.numeral()
                + " · " + want + " · " + paragraphs.size() + " paragraphs");

        String key = who + "#" + slug + "#" + want;
        Agent node = Flow.resumable(
                Flow.triad(want, work.planner(hero, chapter), work.doer(hero, chapter),
                        work.verifier(hero, chapter), () -> work.facts(hero, chapter),
                        trace, key, 3),
                journal, () -> key);

        long began = System.currentTimeMillis();
        String answer = node.run("Reading " + chapter.book() + " chapter " + chapter.numeral()
                + " for the character " + hero.name() + ".");
        long took = (System.currentTimeMillis() - began) / 1000;

        System.out.println("\n── what it answered ──\n" + answer);

        // DID IT ACTUALLY SETTLE? `Flow.triad` returns the doer's last attempt either way — on a
        // spent round budget it records "N rounds spent, last word was again" to the trace and
        // hands the answer back regardless, which is right, because a triad cannot invent success.
        // But the caller is given no way to tell the two apart, and the first live run here wrote a
        // three-times-rejected answer to disk with no mark on it AND journalled the key as done, so
        // a resume would have skipped that chapter forever.
        //
        // Re-asking a verifier that is plain code costs nothing, so that is what this does. Where a
        // verifier is a model call it would cost one, which is the argument for ratchet returning
        // the verdict rather than only tracing it. Filed as a gap rather than worked around silently.
        boolean settled = "done".equals(work.verifier(hero, chapter).run("").trim());
        System.out.println("── verdict ── " + (settled ? "settled" : "NOT settled — stored as rejected"));

        new Reading(who, chapter.book(), slug, want, attempt(records, who, chapter, want),
                work.body(), paragraphs.size(), paragraphs.size(), settled).append(Path.of("readings"));

        System.out.println("\n── written ──");
        System.out.println("  " + Reading.path(Path.of("readings"), who, chapter.book(), slug, want));
        System.out.println("  took " + took + "s");
    }

    /**
     * ONE PLACE THAT KNOWS WHICH CLASS IS WHICH SECTION.
     *
     * <p>Keyed on {@link Chain.Section#stage()} rather than on a string written here as well, so a
     * section renamed in the enum cannot keep working under its old name in this switch. The
     * default throws with the list rather than falling back to something plausible: a typo that
     * silently ran the wrong extractor would write a reading under a section it is not.
     */
    static Work sectionFor(String stage, Text text, ChatModel model) {
        for (Chain.Section section : Chain.Section.values()) {
            if (!section.stage().equals(stage)) {
                continue;
            }
            return switch (section) {
                case APPEARANCES -> new Appearances(text, model);
                case ARC -> new Arc(text, model);
                case QUOTES -> new Quotes(text, model);
                case RELATIONSHIPS -> new Relationships(text, model);
                case PERSONALITY -> new Personality(text, model);
                case TRIVIA -> new Trivia(text, model);
                case NAMES -> new Names(text, model);
                case FACTS -> new Facts(text, model);
            };
        }
        StringBuilder known = new StringBuilder();
        for (Chain.Section section : Chain.Section.values()) {
            known.append(known.length() == 0 ? "" : ", ").append(section.stage());
        }
        throw new IllegalArgumentException("no section '" + stage + "'. There are: " + known);
    }

    /** A re-reading is attempt n+1, and the earlier ones stay on disk. */
    private static int attempt(Path records, String who, Chain.Chapter chapter, String section)
            throws IOException {
        return Reading.all(Reading.path(Path.of("readings"), who, chapter.book(),
                chapter.slug(), section)).size() + 1;
    }

    private static String name(String slug) {
        return Map.of("pierre", "Pierre", "andrew", "Prince Andrew",
                "natasha", "Natásha", "nicholas", "Nicholas").getOrDefault(slug, slug);
    }

    /**
     * THE JVM CANNOT SET ITS OWN ENVIRONMENT, so this refuses rather than pretending.
     *
     * <p>The {@code .env} in this repository names the model {@code LLM_BASE_URL} /
     * {@code LLM_MODEL} / {@code LLM_API_KEY}. ratchet's {@code Model} reads {@code RATCHET_BASE} /
     * {@code RATCHET_MODEL} / {@code RATCHET_KEY} through {@code Env}, which goes to
     * {@code System.getenv} — so a system property set here would be read by nobody. An earlier
     * version of this method set three of them, with a javadoc explaining the mismatch it was
     * failing to fix.
     *
     * <p>The bridge belongs in the launcher, which is where a sibling repository puts it and
     * documents why. Until this repository has one, the export line is the instruction.
     */
    private static void needs() {
        if (System.getenv("RATCHET_BASE") != null && !System.getenv("RATCHET_BASE").isBlank()) {
            return;
        }
        throw new IllegalStateException("""
                RATCHET_BASE is not set, so no model can be built.

                The .env here names the endpoint LLM_BASE_URL, and ratchet reads RATCHET_BASE from
                the process environment. Bridge them in the shell that starts the JVM:

                  set -a; . ./.env; set +a
                  export RATCHET_BASE="$LLM_BASE_URL" RATCHET_MODEL="$LLM_MODEL" RATCHET_KEY="$LLM_API_KEY"
                """);
    }
}
