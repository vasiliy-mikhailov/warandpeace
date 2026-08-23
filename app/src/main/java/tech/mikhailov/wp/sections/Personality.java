package tech.mikhailov.wp.sections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.langchain4j.model.chat.ChatModel;

import tech.mikhailov.ratchet.flow.Agent;
import tech.mikhailov.ratchet.flow.Flow;
import tech.mikhailov.wp.Chain;
import tech.mikhailov.wp.Text;

/**
 * WHAT THIS CHAPTER SHOWS THEM TO BE LIKE — never what the novel eventually decides they are.
 *
 * <p>THE SECTION MOST PRONE TO INVENTION, and the reason is in the question itself. Ask a model what
 * a character is like and it answers from everything it has ever read about War and Peace: Pierre is
 * searching for meaning, Andrew is disillusioned. Those sentences are true of the book and are not
 * in the chapter, so nothing can check them and every one of them lands on the page with the same
 * confidence as a fact. So the question asked here is narrower than the section's name: what does
 * THIS chapter demonstrate, and which words demonstrate it. "Looks solemnly at his audience over his
 * spectacles" is extractable. "He is searching for meaning" is not, and asking for it is asking for
 * invention.
 *
 * <p>CONTRADICTIONS BETWEEN CHAPTERS ARE NOT ERRORS HERE. Chapter V Pierre blurts out everything in
 * his mind; Book Fourteen Pierre does not. Nothing in this file may resolve that, and
 * {@code Chain.folded} keeps the fold for this section out of a model's hands for the same reason —
 * accumulate, do not collapse. The contradiction IS the arc, and a fold that smooths it produces a
 * character who was the same person for fifteen hundred pages.
 *
 * <p>Nearly everything the verifier does is arithmetic — a span occurs in the chapter or it does
 * not. One thing is not: whether a phrase is something a scene showed or a verdict on a life. That
 * is the one model call, asked last, of the traits alone.
 */
public final class Personality implements Work {

    /** At most six, and the verifier holds the doer to it, because a long list is padding. */
    private static final int MOST = 6;

    /**
     * PHRASES THAT REACH OUTSIDE THE CHAPTER, caught for free before anything is paid for.
     *
     * <p>Deliberately short. Every entry here must be a phrase that CANNOT be right about a single
     * scene, because a false rejection costs three calls and teaches the model nothing. "Always" and
     * "never" are the tempting additions and are left out on purpose: "never once looks at the man
     * he is answering" is exactly the kind of observation this section wants.
     */
    private static final List<String> BEYOND = List.of(
            "the novel", "the book", "the whole story", "over the course", "by the end",
            "eventually", "later on", "in later chapters", "will become", "comes to realize",
            "comes to understand", "his life", "her life", "deep down", "at heart",
            "fundamentally", "as a person", "in general", "his journey", "her journey",
            "searching for meaning", "throughout");

    private final Text text;
    private final ChatModel model;
    private String said = "";

    public Personality(Text text, ChatModel model) {
        this.text = text;
        this.model = model;
    }

    /**
     * NO MODEL CALL. The instruction is the same for every chapter and every character, so buying it
     * 13,140 times would buy the same paragraph 13,140 times.
     *
     * <p>The warning about bare surnames and bare titles is carried here rather than in the verifier
     * because no verifier in this repository can check it. "The count" in Book One is Pierre's
     * father, not Pierre; "Bolkónski" is the old prince or his son. A trait mined from those words
     * and filed under the wrong man is confidently wrong, and the gold is explicit that a page with a
     * gap beats a page that is confidently wrong. So the doer is told to drop what it cannot pin.
     */
    @Override
    public Agent planner(Chain.Hero hero, Chain.Chapter chapter) {
        return brief -> "Say what THIS chapter shows " + hero.name() + " to be like — only what "
                + "happens on this page, never what you know of them from the rest of the novel. "
                + "Each trait needs the words from the chapter that show it. A bare surname or a "
                + "bare title — \"the count\", \"the prince\", \"Bolkónski\" — often belongs to a "
                + "father, a son or a namesake; if the chapter does not make it clear those words "
                + "are about " + hero.name() + ", leave the trait out. If the chapter shows nothing "
                + "about what " + hero.name() + " is like, say so rather than reaching for what you "
                + "already know.";
    }

    /**
     * A PLAIN FORMAT, NOT JSON. A small local model produces labelled lines almost every time and
     * well-formed JSON considerably less often, and a triad that spends its three rounds arguing
     * about a bracket has extracted nothing.
     *
     * <p>NONE IS AN ANSWER AND IS OFFERED FIRST. Every chapter is read for every character, so most
     * of the 13,140 readings are of a chapter that shows this person nothing at all. A doer with no
     * way to say so has one way out, which is to invent — in the section where invention is already
     * the failure mode. The escape is not free: the verifier prices it below.
     */
    @Override
    public Flow.Doer doer(Chain.Hero hero, Chain.Chapter chapter) {
        return (plan, feedback) -> {
            String body = text.of(chapter);
            String ask = plan
                    + (feedback.isBlank() ? "" : "\n\nYour last answer was rejected: " + feedback)
                    + "\n\nIf this chapter shows nothing about what " + hero.name() + " is like, "
                    + "answer the single word NONE and nothing else.\n\nOtherwise answer in pairs "
                    + "of lines and nothing else, at most " + MOST + " pairs:\n"
                    + "TRAIT: <what this chapter shows " + hero.name() + " to be like, in a few words>\n"
                    + "SHOWN: <the words from the chapter that show it, copied EXACTLY>\n\n"
                    + "Do not put quotation marks round the SHOWN words. Do not write what "
                    + hero.name() + " is like in the rest of the novel.\n\nCHAPTER "
                    + chapter.numeral() + " of " + chapter.book() + ":\n\n" + body;
            said = Ask.once(model, ask);
            return said;
        };
    }

    /** The workspace is the answer just given; this section writes nothing else. */
    @Override
    public String facts(Chain.Hero hero, Chain.Chapter chapter) {
        return said;
    }

    /**
     * ARITHMETIC FIRST, JUDGEMENT LAST, AND THE JUDGEMENT IS PAID FOR ONLY ONCE THE ARITHMETIC IS
     * CLEAN.
     *
     * <p>The order is the whole design. Shape, span length, and "these words occur in this chapter"
     * are substring tests, and a model asked to perform them would be slower, dearer and worse. Only
     * the last question — is this phrase something the scene showed, or a verdict on a life — needs
     * anybody's opinion, and it is asked of the traits alone. Handing it the chapter as well invites
     * it to redo the extraction instead of answering the one question.
     */
    @Override
    public Agent verifier(Chain.Hero hero, Chain.Chapter chapter) {
        return judged -> {
            String body;
            try {
                body = flat(text.of(chapter));
            } catch (IOException unreadable) {
                return "again: the chapter could not be read: " + unreadable.getMessage();
            }
            List<String[]> traits = traits(said);
            if (traits.isEmpty()) {
                return nothing(hero, body);
            }
            if (traits.size() > MOST) {
                return "again: you listed " + traits.size() + " traits and the limit is " + MOST
                        + ". Keep the ones this chapter actually shows and drop the rest.";
            }
            for (String[] pair : traits) {
                String bad = wrong(pair[0], pair[1], body);
                if (bad != null) {
                    return bad;
                }
            }
            return vague(traits);
        };
    }

    /**
     * NONE IS NOT FREE, BUT IT IS CHEAP, and that asymmetry is deliberate.
     *
     * <p>A verifier that accepts NONE unconditionally has handed the doer a way out of every chapter.
     * One that rejects NONE whenever the name occurs punishes the passing mention — "Pierre was not
     * there" — and a doer pressed three times over a chapter that genuinely shows nothing will
     * eventually invent something, which is the outcome this whole file exists to prevent. So the
     * bar is being named repeatedly: a character the chapter returns to five times is a character
     * the chapter is showing.
     *
     * <p>The gap this leaves is known and accepted. Book One chapter XXIV calls Pierre "the young
     * man" for stretches and NONE would pass there on a light count. A missing trait is a gap; an
     * invented one is a page that is confidently wrong, and the gold says which of those is worse.
     */
    private String nothing(Chain.Hero hero, String body) {
        if (!saysNone(said)) {
            return "again: I could not find a single TRAIT line. Answer either the one word NONE, "
                    + "or pairs of lines beginning TRAIT: and SHOWN:.";
        }
        int named = occurrences(body, hero.name());
        if (named >= 5) {
            return "again: you answered NONE, but this chapter names " + hero.name() + " " + named
                    + " times, so it is showing us something about him. Give at least one TRAIT "
                    + "with the SHOWN words that demonstrate it.";
        }
        return "done";
    }

    /**
     * WHAT IS WRONG WITH ONE TRAIT, SAID SO THE MODEL CAN FIX IT.
     *
     * <p>STRIP THE QUOTES BEFORE TESTING THE SPAN. This is not a nicety, it is the bug that ate a
     * whole triad in this repository: a verifier objected that the evidence did not occur in the
     * chapter and quoted it back WITH the quotation marks the model had wrapped round it, so the
     * model read its own words, saw them as correct, and sent them again — three calls, no progress,
     * triad abandoned. The marks are a delimiter, not part of the phrase, so they come off here and
     * the objection names the bare words and says outright not to add them back.
     *
     * <p>The comparison folds typographic punctuation on BOTH sides for the same reason. The Maude
     * text writes {@code can’t} and an em dash; a model writes {@code can't} and two hyphens. That
     * mismatch is not the model's mistake and rejecting it would be three rounds spent on a
     * character it cannot see.
     */
    private static String wrong(String trait, String span, String body) {
        if (trait.isBlank()) {
            return "again: there is a SHOWN line with no TRAIT line above it. Every SHOWN belongs "
                    + "under the TRAIT it demonstrates.";
        }
        if (span.isBlank()) {
            return "again: this TRAIT has no SHOWN line under it: " + bare(trait) + ". Every trait "
                    + "needs the words from the chapter that show it.";
        }
        String words = unquoted(span);
        if (words.split("\\s+").length < 2 || words.length() < 8) {
            return "again: SHOWN must be a phrase from the chapter, long enough to show something "
                    + "on its own. This is too short: " + bare(words);
        }
        if (!body.contains(flat(words))) {
            return "again: these words do not occur in this chapter, so copy them again from the "
                    + "page, exactly, and without adding quotation marks round them — the marks are "
                    + "not part of the words: " + bare(words);
        }
        if (trait.split("\\s+").length > 25) {
            return "again: this TRAIT is a summary of what happens, not a few words saying what he "
                    + "is like: " + bare(trait);
        }
        String lower = trait.toLowerCase(Locale.ROOT);
        for (String reach : BEYOND) {
            if (lower.contains(reach)) {
                return "again: the words \"" + reach + "\" make this a claim about the whole novel, "
                        + "which this one chapter cannot show. Rewrite it as what happens on this "
                        + "page, or drop it: " + bare(trait);
            }
        }
        return null;
    }

    /**
     * THE ONE MODEL CALL, and the only check here that is genuinely a judgement.
     *
     * <p>An unreadable verdict from the judge is treated as acceptance. A doer cannot act on
     * feedback derived from an answer nobody could parse, so charging it for one would cost the
     * three rounds and change nothing — the exact failure this section's verifier is built around.
     * When the judge is incoherent the cost falls on the judge, which is to say on nobody.
     */
    private String vague(List<String[]> traits) {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < traits.size(); i++) {
            list.append(i + 1).append(": ").append(traits.get(i)[0]).append('\n');
        }
        String ask = "Each numbered line below is supposed to say what ONE CHAPTER of a novel showed "
                + "a character to be like. A good one describes what the character did or how they "
                + "behaved on that page. A bad one is a verdict about the whole person or the whole "
                + "novel, which no single scene could show.\n\nAnswer one line per number and "
                + "nothing else, either\n<number>: scene\nor\n<number>: verdict\n\n" + list;
        String ruling = Ask.once(model, ask);
        for (int i = 0; i < traits.size(); i++) {
            if (verdict(ruling, i + 1)) {
                return "again: this reads as a verdict about the whole person rather than something "
                        + "this chapter shows. Say what he does or how he behaves on this page, or "
                        + "drop it: " + bare(traits.get(i)[0]);
            }
        }
        return "done";
    }

    private static boolean verdict(String ruling, int n) {
        for (String l : ruling.split("\\R")) {
            String t = l.trim().toLowerCase(Locale.ROOT);
            if (t.startsWith(n + ":") || t.startsWith(n + ".") || t.startsWith(n + ")")) {
                return t.contains("verdict");
            }
        }
        return false;
    }

    /**
     * Pairs, and an unmatched half is kept rather than dropped so the verifier can name it.
     *
     * <p>Silently discarding a TRAIT with no SHOWN under it would let a half-answer through as a
     * shorter correct one, which is how a section quietly stops extracting anything.
     */
    private static List<String[]> traits(String from) {
        List<String[]> out = new ArrayList<>();
        String pending = null;
        for (String raw : from.split("\\R")) {
            String l = raw.trim();
            String label = l.toUpperCase(Locale.ROOT);
            if (label.startsWith("TRAIT:")) {
                if (pending != null) {
                    out.add(new String[] {pending, ""});
                }
                pending = l.substring("TRAIT:".length()).trim();
            } else if (label.startsWith("SHOWN:")) {
                out.add(new String[] {pending == null ? "" : pending,
                        l.substring("SHOWN:".length()).trim()});
                pending = null;
            }
        }
        if (pending != null) {
            out.add(new String[] {pending, ""});
        }
        return out;
    }

    private static boolean saysNone(String from) {
        for (String l : from.split("\\R")) {
            if (l.trim().replaceAll("[.\\s]", "").equalsIgnoreCase("NONE")) {
                return true;
            }
        }
        return false;
    }

    /** One matched pair at a time, straight or typographic, because models nest them. */
    private static String unquoted(String s) {
        String t = s.trim();
        while (t.length() > 1) {
            char open = t.charAt(0);
            char close = t.charAt(t.length() - 1);
            boolean matched = (open == '"' && close == '"') || (open == '\'' && close == '\'')
                    || (open == '“' && close == '”') || (open == '‘' && close == '’');
            if (!matched) {
                return t;
            }
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    /**
     * One shape for both sides of every comparison.
     *
     * <p>The chapter files are hard-wrapped at seventy columns, so a phrase the model copied as one
     * line contains a newline in the source and no naive substring test would ever match it.
     */
    private static String flat(String s) {
        return s.replace('‘', '\'').replace('’', '\'')
                .replace('“', '"').replace('”', '"')
                .replace('–', '-').replace('—', '-')
                .replace("…", "...")
                .replaceAll("\\s+", " ").trim();
    }

    private static int occurrences(String body, String name) {
        int found = 0;
        for (int at = body.indexOf(name); at >= 0; at = body.indexOf(name, at + 1)) {
            found++;
        }
        return found;
    }

    /**
     * NO QUOTATION MARKS. Wrapping the model's own phrase in marks when telling it the phrase is
     * wrong is how it comes to believe the marks belong to the phrase.
     */
    private static String bare(String s) {
        return s.length() > 80 ? s.substring(0, 80) + "…" : s;
    }

    /** What goes into the reading, as JSON. Empty when the chapter showed nothing, and that is fine. */
    @Override
    public String body() {
        StringBuilder out = new StringBuilder("{\"traits\":[");
        boolean first = true;
        for (String[] pair : traits(said)) {
            if (pair[0].isBlank() || pair[1].isBlank()) {
                continue;
            }
            out.append(first ? "" : ",").append("{\"trait\":").append(Ask.json(pair[0]))
                    .append(",\"shown_by\":").append(Ask.json(unquoted(pair[1]))).append('}');
            first = false;
        }
        return out.append("]}").toString();
    }
}
