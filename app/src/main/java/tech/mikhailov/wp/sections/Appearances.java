package tech.mikhailov.wp.sections;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import dev.langchain4j.model.chat.ChatModel;

import tech.mikhailov.ratchet.flow.Agent;
import tech.mikhailov.ratchet.flow.Flow;
import tech.mikhailov.wp.Chain;
import tech.mikhailov.wp.Text;

/**
 * PRESENT, MENTIONED-ONLY, OR ABSENT — and the verifier is arithmetic.
 *
 * <p>The cheapest of the eight and the one worth building first, because everything it needs to
 * check is checkable without asking anybody. A verdict is one of three words; the evidence is a span
 * that either occurs in the chapter or does not; the paragraph count either covers the chapter or
 * does not. So the planner and the verifier are plain code and only the doer costs a model call.
 *
 * <p>MENTIONED-ONLY IS NOT A ROUNDING OF ABSENT. Book One chapter XXI is Pierre's entire inheritance
 * argued over by other people while he is elsewhere: he is in no scene and the chapter is about
 * nothing else. A pipeline with two verdicts loses that chapter from his page, and it is one of the
 * most important chapters he has.
 */
public final class Appearances implements Work {

    private static final List<String> VERDICTS = List.of("present", "mentioned-only", "absent");

    private final Text text;
    private final ChatModel model;
    private String said = "";

    public Appearances(Text text, ChatModel model) {
        this.text = text;
        this.model = model;
    }

    /**
     * NO MODEL CALL. The plan for this section does not vary by chapter or by character, so asking
     * for it would be paying to be told the same thing 13,140 times.
     */
    @Override
    public Agent planner(Chain.Hero hero, Chain.Chapter chapter) {
        return brief -> "Decide whether " + hero.name() + " is PRESENT in this chapter (in a scene, "
                + "acting or spoken to), MENTIONED-ONLY (talked about by others while elsewhere), or "
                + "ABSENT. Quote the words that settle it.";
    }

    @Override
    public Flow.Doer doer(Chain.Hero hero, Chain.Chapter chapter) {
        return (plan, feedback) -> {
            String body = text.of(chapter);
            String ask = plan
                    + (feedback.isBlank() ? "" : "\n\nYour last answer was rejected: " + feedback)
                    + "\n\nAnswer as three lines and nothing else:\nVERDICT: <present|mentioned-only|absent>"
                    + "\nEVIDENCE: <a short phrase copied EXACTLY from the chapter>"
                    + "\nWHY: <one sentence>\n\nCHAPTER " + chapter.numeral() + " of " + chapter.book()
                    + ":\n\n" + body;
            said = Ask.once(model, ask);
            return said;
        };
    }

    /** The workspace is the answer just given; there is nothing else this section touches. */
    @Override
    public String facts(Chain.Hero hero, Chain.Chapter chapter) {
        return said;
    }

    /**
     * ARITHMETIC, NOT JUDGEMENT.
     *
     * <p>Three things, each of which a model would be worse at than a substring test: the verdict is
     * one of three words, the evidence occurs in the chapter verbatim, and a verdict of ABSENT is not
     * compatible with the name occurring in the text. That last one is the check that matters — it is
     * the only automatic way to catch the model deciding a chapter is not about somebody it is about.
     */
    @Override
    public Agent verifier(Chain.Hero hero, Chain.Chapter chapter) {
        return judged -> {
            String verdict = line(said, "VERDICT:").toLowerCase(Locale.ROOT).trim();
            String evidence = line(said, "EVIDENCE:").trim();
            if (!VERDICTS.contains(verdict)) {
                return "again: VERDICT must be exactly one of " + VERDICTS + ", not " + quoted(verdict);
            }
            String body;
            try {
                body = text.of(chapter);
            } catch (IOException unreadable) {
                return "again: the chapter could not be read: " + unreadable.getMessage();
            }
            if (evidence.length() < 4) {
                return "again: EVIDENCE must be a phrase copied from the chapter";
            }
            if (!body.replaceAll("\\s+", " ").contains(evidence.replaceAll("\\s+", " "))) {
                return "again: that EVIDENCE does not occur in the chapter. Copy it exactly, "
                        + "including punctuation: " + quoted(evidence);
            }
            if (verdict.equals("absent") && body.contains(hero.name())) {
                return "again: you answered absent, but the chapter names " + quoted(hero.name())
                        + ". Decide between present and mentioned-only.";
            }
            return "done";
        };
    }

    private static String line(String from, String label) {
        for (String l : from.split("\\R")) {
            if (l.trim().toUpperCase(Locale.ROOT).startsWith(label)) {
                return l.trim().substring(label.length());
            }
        }
        return "";
    }

    private static String quoted(String s) {
        return "\"" + (s.length() > 60 ? s.substring(0, 60) + "…" : s) + "\"";
    }

    /** What goes into the reading, as JSON. */
    @Override
    public String body() {
        return "{\"verdict\":\"" + line(said, "VERDICT:").trim()
                + "\",\"evidence\":" + Ask.json(line(said, "EVIDENCE:").trim())
                + ",\"why\":" + Ask.json(line(said, "WHY:").trim()) + "}";
    }
}
