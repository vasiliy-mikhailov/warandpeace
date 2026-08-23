package tech.mikhailov.wp.sections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tech.mikhailov.ratchet.llm.Chat;

import tech.mikhailov.ratchet.flow.Agent;
import tech.mikhailov.ratchet.flow.Flow;
import tech.mikhailov.wp.Chain;
import tech.mikhailov.wp.Text;

/**
 * WHAT IS ODD — AND THE WEAKEST VERIFIER IN THE SET, WHICH IS WHY THIS IS THE SECTION TO DISTRUST.
 *
 * <p>Fandom's own word for this section is minutiae: where dots are connected, where something is
 * highlighted. "At the end of chapter XXIV the will has NOT been opened, so Pierre is an expectant
 * heir and not yet Count Bezúkhov" is the shape wanted. So is a silence — he speaks twelve lines in
 * the whole of Book One and every one of them is in chapter V, none in the chapter where his life
 * changes.
 *
 * <p>SAY IT PLAINLY: THIS VERIFIER CANNOT CHECK THE THING THE SECTION IS FOR. An item has two
 * halves, a claim and its interest, and only the first half is checkable. That the will has not been
 * opened is a substring test against the chapter. That anybody should CARE is a judgement no test
 * here makes and no test here pretends to make. Every other section's verifier can reject a wrong
 * answer; this one can only reject an ungrounded one. A reader auditing these pages should start
 * here, and a fold that has to drop a section should drop this one.
 *
 * <p>THE OBVIOUS FIX WAS TRIED ON PAPER AND REJECTED: ask a model "is this interesting?" once per
 * item. A judge asked that question says yes — to a plot summary, to a restated span, to anything —
 * because there is no wrong answer to it. That buys one call per item, rejects nothing, and leaves a
 * check in the tree that a reader would count as a guard. A verifier that cannot reject is not a
 * verifier, and dressing this gap up as one is worse than the gap, because then nobody knows to
 * distrust the section. It is cheaper and more honest to check the half that is checkable and print
 * the other half's absence in this javadoc.
 *
 * <p>CROSS-CHAPTER NOTICINGS ARE NOT THIS NODE'S WORK. "Every one of his twelve lines is in chapter
 * V" cannot be grounded in a span of chapter XXIV, and this node reads one chapter. Connecting dots
 * between chapters is what {@code Chain.folded} keeps as a judged fold for {@code TRIVIA}; here an
 * item must rest on words that are on this page, or it is unfalsifiable at the only level that can
 * see it.
 */
public final class Trivia implements Work {

    /**
     * THREE, AND THE VERIFIER HOLDS THE DOER TO IT. The failure mode of this section is not a wrong
     * item, it is six mediocre ones — a model asked for oddities will always find another. A cap is
     * the only pressure available towards picking, since nothing here can rank.
     */
    private static final int MOST = 3;

    /**
     * A SPAN IS THE WORDS THAT CARRY THE FACT, NOT THE PASSAGE THEY SIT IN. Quoting half a page
     * passes the substring test while grounding nothing, which is the cheapest way to satisfy this
     * verifier without meaning it.
     */
    private static final int LONGEST = 300;

    private final Text text;
    private final Chat model;
    private String said = "";

    public Trivia(Text text, Chat model) {
        this.text = text;
        this.model = model;
    }

    /**
     * NO MODEL CALL. The instruction does not vary by chapter or by character, and 13,140 identical
     * paragraphs bought one at a time is the same paragraph at 13,140 times the price.
     *
     * <p>THE NAME TRAP IS CARRIED HERE BECAUSE NO VERIFIER IN THIS REPOSITORY CATCHES IT. "Count
     * Bezúkhov" is Pierre's father through Book One and only becomes Pierre after chapter XXIV;
     * "Bolkónski" is the old prince or his son. An oddity mined from those words and filed under the
     * wrong man reads as an insight and is confidently wrong, which the gold says is worse than a
     * gap — and trivia is exactly where a wrong attribution is most quotable and least questioned.
     * So the doer is told to drop what it cannot pin.
     */
    @Override
    public Agent planner(Chain.Hero hero, Chain.Chapter chapter) {
        return brief -> "Note what is ODD about " + hero.name() + " in this chapter: small, exact "
                + "detail of the kind one reader points out to another — a silence where speech was "
                + "expected, a thing that has NOT happened yet, a name the chapter withholds or "
                + "gives, a detail that is quietly strange. NOT a summary of what happens; what "
                + "happens belongs to the other sections. Each noticing must rest on words that are "
                + "on this page. A bare surname or a bare title — \"the count\", \"the prince\", "
                + "\"Bolkónski\" — often belongs to a father, a son or a namesake; if the chapter "
                + "does not make it clear those words are about " + hero.name() + ", leave the "
                + "noticing out. Most chapters hold nothing odd, and for those the right answer is "
                + "nothing at all.";
    }

    /**
     * A PLAIN FORMAT, NOT JSON. A small local model returns labelled lines nearly every time and
     * well-formed JSON considerably less often, and a triad that spends its three rounds arguing
     * about a bracket has extracted nothing. The JSON is assembled in {@link #body()}, by code that
     * cannot get it wrong.
     *
     * <p>NONE IS OFFERED FIRST AND MEANT. Every chapter is read for every character, so most of
     * these readings are of a chapter with nothing odd in it about this person. A doer with no way
     * to say "nothing" has exactly one way out — invent — and an invented oddity is the one output
     * of this pipeline that will be believed without being checked. An empty trivia list is the
     * normal answer here, not a failure to extract.
     */
    @Override
    public Flow.Doer doer(Chain.Hero hero, Chain.Chapter chapter) {
        return (plan, feedback) -> {
            String body = text.of(chapter);
            String ask = plan
                    + (feedback.isBlank() ? "" : "\n\nYour last answer was rejected: " + feedback)
                    + "\n\nIf nothing about " + hero.name() + " in this chapter is odd, answer the "
                    + "single word NONE and nothing else. NONE is a good answer and most chapters "
                    + "deserve it.\n\nOtherwise answer in pairs of lines and nothing else, at most "
                    + MOST + " pairs:\nODD: <the noticing, one sentence>\n"
                    + "SPAN: <the few words from the chapter it rests on, copied EXACTLY>\n\n"
                    + "Do not put quotation marks round the SPAN words. Do not pad the list.\n\n"
                    + "CHAPTER " + chapter.numeral() + " of " + chapter.book() + ":\n\n" + body;
            said = Ask.once(model, ask);
            return said;
        };
    }

    /** The workspace is the answer just given; this section reads nothing else. */
    @Override
    public String facts(Chain.Hero hero, Chain.Chapter chapter) {
        return said;
    }

    /**
     * GROUNDING ONLY, AND NOT ONE MODEL CALL.
     *
     * <p>Shape, the cap, "these words occur in this chapter", and "this noticing says more than the
     * words it copied" are all substring tests, and a model asked to perform them would be slower,
     * dearer and worse. What is left over — whether anybody should care — is the half this file's
     * javadoc admits is unguarded. Nothing below pretends otherwise.
     *
     * <p>NONE IS ACCEPTED UNCONDITIONALLY, and that is a decision, not an oversight. {@code
     * Personality} prices its NONE — if a chapter names the character five times it must show
     * something — because a chapter that features somebody does characterise them. Nothing of the
     * sort is true of oddity: a chapter can be entirely about Pierre and hold nothing worth pointing
     * at. Pressing a doer three times for trivia it has already said it does not have is a
     * three-call machine for manufacturing filler, in the section least able to detect it.
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
            List<String[]> items = items(said);
            if (items.isEmpty()) {
                return saysNone(said) ? "done"
                        : "again: I could not find a single ODD line. Answer either the one word "
                                + "NONE, or pairs of lines beginning ODD: and SPAN:.";
            }
            if (items.size() > MOST) {
                return "again: you listed " + items.size() + " noticings and the limit is " + MOST
                        + ". Keep the ones that are genuinely odd and drop the rest — dropping all "
                        + "of them and answering NONE is allowed.";
            }
            List<String> seen = new ArrayList<>();
            for (String[] pair : items) {
                String bad = wrong(pair[0], pair[1], body, seen);
                if (bad != null) {
                    return bad;
                }
                seen.add(letters(unquoted(pair[1])));
            }
            return "done";
        };
    }

    /**
     * WHAT IS WRONG WITH ONE ITEM, SAID SO THE MODEL CAN ACT ON IT.
     *
     * <p>STRIP THE QUOTES BEFORE TESTING THE SPAN. Not a nicety — the bug that ate a whole triad in
     * this repository. A verifier objected that the evidence did not occur in the chapter and quoted
     * it back WITH the quotation marks the model had wrapped round it; the model read its own words,
     * saw them as correct, and sent them again. Three calls, no progress, triad abandoned. The marks
     * are a delimiter, not part of the phrase, so they come off here and the objection names the
     * bare words and says outright not to add them back.
     *
     * <p>Both sides of the comparison are folded to one punctuation, because the Maude text writes
     * {@code can’t} and an em dash while a model writes {@code can't} and two hyphens. That mismatch
     * is not the model's mistake, and rejecting it spends the round budget on a difference nobody
     * can see.
     */
    private static String wrong(String note, String span, String body, List<String> seen) {
        if (note.isBlank()) {
            return "again: there is a SPAN line with no ODD line above it. Every SPAN belongs under "
                    + "the noticing it grounds.";
        }
        if (span.isBlank()) {
            return "again: this noticing has no SPAN line under it, so nothing in the chapter holds "
                    + "it up. Add the words it rests on, or drop it: " + bare(note);
        }
        String words = unquoted(span);
        if (words.split("\\s+").length < 2 || words.length() < 8) {
            return "again: SPAN must be words copied from the chapter, long enough to stand on "
                    + "their own. This is too short: " + bare(words);
        }
        if (words.length() > LONGEST) {
            return "again: SPAN is a whole passage. Copy only the few words that carry the "
                    + "noticing: " + bare(note);
        }
        if (!body.contains(flat(words))) {
            return "again: these words do not occur in this chapter, so copy them again from the "
                    + "page, exactly, and without adding quotation marks round them — the marks are "
                    + "not part of the words: " + bare(words);
        }
        // A NOTICING THAT FITS INSIDE ITS OWN SPAN HAS NOTICED NOTHING. It is the chapter's words
        // read back, which passes the substring test above while connecting no dots at all — the
        // one form of filler this section can actually catch, so it is caught. Only this direction:
        // a noticing that quotes its span and then says something about it is exactly right, and
        // the gold's own example ("the will has not yet been opened, so he is an expectant heir and
        // not yet Count Bezúkhov") is that shape.
        if (letters(words).contains(letters(note))) {
            return "again: this noticing only repeats the words it quotes. Say what is odd ABOUT "
                    + "them — what they imply, or what is missing — or drop it: " + bare(note);
        }
        if (note.split("\\s+").length < 4) {
            return "again: this noticing is too short to say anything: " + bare(note);
        }
        if (seen.contains(letters(words))) {
            return "again: two of your noticings rest on the same words. Keep one: " + bare(note);
        }
        return null;
    }

    /**
     * Pairs, and an unmatched half is kept rather than dropped so the verifier can name it.
     *
     * <p>Silently discarding an ODD line with no SPAN under it would let a half-answer through as a
     * shorter correct one, which is how a section quietly stops grounding anything.
     */
    private static List<String[]> items(String from) {
        List<String[]> out = new ArrayList<>();
        String pending = null;
        for (String raw : from.split("\\R")) {
            String l = raw.trim();
            String label = l.toUpperCase(Locale.ROOT);
            if (label.startsWith("ODD:")) {
                if (pending != null) {
                    out.add(new String[] {pending, ""});
                }
                pending = l.substring("ODD:".length()).trim();
            } else if (label.startsWith("SPAN:")) {
                out.add(new String[] {pending == null ? "" : pending,
                        l.substring("SPAN:".length()).trim()});
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

    /** Words only, for the one comparison that is about content rather than about characters. */
    private static String letters(String s) {
        return flat(s).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    /**
     * NO QUOTATION MARKS. Wrapping the model's own phrase in marks while telling it the phrase is
     * wrong is how it comes to believe the marks belong to the phrase.
     */
    private static String bare(String s) {
        return s.length() > 80 ? s.substring(0, 80) + "…" : s;
    }

    /**
     * What goes into the reading, as JSON. USUALLY EMPTY, AND THAT IS THE HEALTHY CASE — a chapter
     * with no trivia is the common one, and a run where this array is full everywhere is evidence
     * the doer is padding rather than evidence the novel is odd.
     */
    @Override
    public String body() {
        StringBuilder out = new StringBuilder("{\"trivia\":[");
        boolean first = true;
        for (String[] pair : items(said)) {
            if (pair[0].isBlank() || pair[1].isBlank()) {
                continue;
            }
            out.append(first ? "" : ",").append("{\"note\":").append(Ask.json(pair[0]))
                    .append(",\"span\":").append(Ask.json(unquoted(pair[1]))).append('}');
            first = false;
        }
        return out.append("]}").toString();
    }
}
