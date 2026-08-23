package tech.mikhailov.wp.sections;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tech.mikhailov.ratchet.llm.Chat;

import tech.mikhailov.ratchet.flow.Agent;
import tech.mikhailov.ratchet.flow.Flow;
import tech.mikhailov.wp.Chain;
import tech.mikhailov.wp.Text;

/**
 * WHAT HAPPENED TO OR AROUND THEM HERE, IN ORDER, EVERY ENTRY NAILED TO A PARAGRAPH.
 *
 * <p>The deliverable. Fandom's guide puts {@code == Character arc ==} first and this is the section
 * that fills it, which is also why it is the section worth being strict with: everything else on the
 * page decorates a list of events, and a list of events nobody can check is a plausible-looking
 * invention.
 *
 * <p>EVENTS, NOT SPEECH. Most of what happens to a person in Tolstoy is narrated in sentences whose
 * subject is "he" or "she" — Pierre has eight entries in Book One chapter XXIV and does not speak one
 * word in it. A section that reported speech would hand his inheritance, his father's death and the
 * scene where the eldest princess turns on him nothing at all, and would look complete while doing
 * it. And an entry may be that the character is somewhere else while others argue about him: chapter
 * XXI is that and nothing else, and it is the most consequential chapter Pierre has in Book One.
 *
 * <p>The planner and the verifier are plain code, as in {@link Appearances}, because every question
 * this section asks of an answer is mechanical: the paragraph number is in range or it is not, the
 * span is inside that paragraph or it is not, the sentence names this character or it does not.
 * Nothing here is judgement, so nothing here should cost a call.
 */
public final class Arc implements Work {

    /** A one-word "anchor" anchors nothing. Three words is the shortest span that can only be one place. */
    private static final int MIN_WORDS = 3;

    private final Text text;
    private final Chat model;
    private String said = "";

    public Arc(Text text, Chat model) {
        this.text = text;
        this.model = model;
    }

    /**
     * NO MODEL CALL. What to look for does not change between chapters or between characters, so
     * planning it 13,140 times would be paying 13,140 times to be told the same paragraph.
     */
    @Override
    public Agent planner(Chain.Hero hero, Chain.Chapter chapter) {
        return brief -> "List what happens to or around " + hero.name() + " in this chapter, in the "
                + "order it happens. Events, not speech: what happens to a person here is usually "
                + "narrated about \"he\" or \"she\", not said out loud by anyone. Being talked about, "
                + "argued over or decided about while elsewhere is an event too — it may be the whole "
                + "of a chapter. A surname or a title alone can belong to somebody else in this book, "
                + "a father and a son sharing both; if an entry is not certainly about " + hero.name()
                + ", leave it out, because a gap is better than a confident mistake.";
    }

    /**
     * THE DOER IS SHOWN THE PARAGRAPH NUMBERS, because it is being asked to cite them.
     *
     * <p>{@link Text#paragraphs} is the numbering, on both sides: the prompt prints its list and the
     * verifier indexes into the same list, so "paragraph 12" means one thing. Numbering built here
     * instead would drift from the one the verifier trusts the first time either changed, and the
     * failure would be a correct entry rejected for citing the wrong index.
     */
    @Override
    public Flow.Doer doer(Chain.Hero hero, Chain.Chapter chapter) {
        return (plan, feedback) -> {
            String ask = plan
                    + (feedback.isBlank() ? "" : "\n\nYour last answer was rejected: " + feedback)
                    + "\n\nAnswer with one line per event and nothing else, each line in this form:\n"
                    + "ENTRY: <paragraph number> | <one sentence, naming " + hero.name() + "> | <at "
                    + "least " + MIN_WORDS + " words copied EXACTLY from that paragraph>\n\n"
                    + "The copied words must be inside the paragraph you numbered. Keep the events in "
                    + "the order they happen. Do not copy the same words twice. If nothing in this "
                    + "chapter happens to or around " + hero.name() + ", answer the single word NONE. "
                    + "If this chapter uses that name for a different person — a father and a son "
                    + "share one here — answer SOMEONE ELSE: <who the chapter means> instead.\n\n"
                    + "CHAPTER " + chapter.numeral() + " of " + chapter.book()
                    + ", numbered by paragraph:\n\n" + numbered(chapter);
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
     * THE ANCHOR IS THE INDEX AND THE SPAN AGREEING. Either alone is decoration.
     *
     * <p>A span that occurs somewhere in the chapter proves the words are Tolstoy's; it does not
     * prove the entry is about the moment it claims. A paragraph number on its own proves nothing at
     * all. Checked together they pin an entry to one place a reader can go and look, which is the
     * only property of this section anybody downstream can rely on.
     *
     * <p>EVERY REJECTION HERE NAMES THE ENTRY AND SAYS WHAT TO DO. The feedback is read by the model
     * on the next round and nothing else happens with it, so an objection it cannot act on costs
     * three calls and changes nothing. That is not hypothetical: this project has already lost a
     * triad to a verifier that complained a span was not in the chapter and echoed the span back
     * still wearing the quote marks the model had used as delimiters. So a matched pair of quotes
     * comes off before any comparison, and when a span is echoed it is echoed in brackets the text
     * never uses.
     */
    @Override
    public Agent verifier(Chain.Hero hero, Chain.Chapter chapter) {
        return judged -> {
            List<String> paragraphs;
            try {
                paragraphs = text.paragraphs(chapter);
            } catch (IOException unreadable) {
                return "again: the chapter could not be read: " + unreadable.getMessage();
            }
            List<Entry> entries = entries();
            if (entries.isEmpty()) {
                return empty(hero, paragraphs);
            }
            for (int n = 0; n < entries.size(); n++) {
                Entry entry = entries.get(n);
                String fault = fault(entry, n + 1, hero, paragraphs);
                if (fault != null) {
                    return fault;
                }
            }
            // ORDER AND REPETITION, which are properties of the list rather than of any one entry,
            // and are the same failure wearing different clothes: a list padded to look thorough.
            // Repetition is checked against every earlier entry, not just the one before, because
            // the cheap way to pad is to say one moment three times in three different sentences.
            for (int n = 1; n < entries.size(); n++) {
                Entry entry = entries.get(n);
                if (entry.paragraph() < entries.get(n - 1).paragraph()) {
                    return "again: the entry for paragraph " + entry.paragraph()
                            + " comes after one citing paragraph "
                            + entries.get(n - 1).paragraph() + ". List the events in the order the "
                            + "chapter tells them, so the paragraph numbers never go backwards.";
                }
                for (int m = 0; m < n; m++) {
                    if (fold(unquoted(entry.span())).equals(fold(unquoted(entries.get(m).span())))) {
                        return "again: entries " + (m + 1) + " and " + (n + 1) + " copy the same "
                                + "words, so they point at one moment. Drop one, or anchor it to the "
                                + "different words that make it a different event.";
                    }
                }
            }
            return "done";
        };
    }

    /**
     * NOTHING PARSED, WHICH IS TWO DIFFERENT ANSWERS AND ONLY ONE OF THEM IS ALLOWED.
     *
     * <p>NONE is the honest answer for the many chapters a given character is nowhere near, and
     * refusing it would buy invented entries for 300-odd chapters each. It is also the cheapest way
     * for a round to end without doing any work, so it is checked the only way code can check it: if
     * a paragraph says the name, there is something to say, and the rejection points at that
     * paragraph rather than asserting it in general.
     */
    private String empty(Chain.Hero hero, List<String> paragraphs) {
        if (elsewhere()) {
            return "done";
        }
        if (!none()) {
            return "again: no entry line parsed. Every line must read ENTRY: <paragraph number> | "
                    + "<what happened> | <words copied from that paragraph>, with the two bars, or the "
                    + "whole answer must be the single word NONE.";
        }
        for (int n = 0; n < paragraphs.size(); n++) {
            if (mentions(paragraphs.get(n), hero)) {
                return "again: you answered NONE, but paragraph " + (n + 1) + " names " + hero.name()
                        + ". Give at least one entry anchored there — or, if the chapter means a "
                        + "different person by that name, answer SOMEONE ELSE: <who it means>.";
            }
        }
        return "done";
    }

    /**
     * THE ONE ANSWER THIS VERIFIER MUST NOT ARGUE WITH, and it exists because the check above would
     * otherwise push the model into the corpus's worst mistake. "Nicholas" is Nicholas Rostóv and it
     * is also old Prince Nicholas Andréevich Bolkónski; "Bolkónski" is the old prince or his son;
     * "Count Bezúkhov" is Pierre's father until chapter XXIV. Told that paragraph 4 names Nicholas
     * and given no way to say WHICH Nicholas, a model does the obedient thing and writes an entry
     * about the wrong man — a page that is confidently wrong, which is worse than one with a gap.
     *
     * <p>It is not a free pass: NONE is a shrug and this is a claim, made in words that only get
     * written on purpose, and it is kept in the reading beside the empty list so a reader can see
     * the chapter was considered and why it was left out.
     */
    private boolean elsewhere() {
        return fold(said).startsWith("someone else");
    }

    /** What is wrong with one entry, or null. Ordered cheapest-to-say first. */
    private String fault(Entry entry, int at, Chain.Hero hero, List<String> paragraphs) {
        if (entry.paragraph() < 1 || entry.paragraph() > paragraphs.size()) {
            return "again: the entry for paragraph " + entry.paragraph() + " cites a paragraph"
                    + ", and this chapter has paragraphs 1 to " + paragraphs.size() + ".";
        }
        // THE ENTRY MUST SAY THE NAME. This is the whole of "reject an entry about someone else",
        // and it is a substring test because the alternative is asking a model to adjudicate the
        // trap it falls into: "Count Bezúkhov" is Pierre's FATHER until chapter XXIV, and an entry
        // reading "Count Bezúkhov made a will" is a true sentence about the wrong man. Made to name
        // Pierre, the same event comes back as "Pierre is left everything by his father's will",
        // which is the sentence the page needs and is checkable besides.
        if (!mentions(entry.what(), hero)) {
            return "again: the entry for paragraph " + entry.paragraph() + " never names "
                    + hero.name() + ", so it reads as an entry "
                    + "about somebody else. Say what happened to or around " + hero.name()
                    + " and use the name, not \"he\" or \"she\".";
        }
        String span = unquoted(entry.span());
        if (span.trim().split("\\s+").length < MIN_WORDS) {
            return "again: the entry for paragraph " + entry.paragraph() + " copies only "
                    + shown(span) + ", which is too little to find again. Copy at least " + MIN_WORDS
                    + " words from paragraph " + entry.paragraph() + ".";
        }
        String wanted = fold(span);
        if (fold(paragraphs.get(entry.paragraph() - 1)).contains(wanted)) {
            return null;
        }
        // THE MOST USEFUL REJECTION THIS FILE MAKES: the words are Tolstoy's and the number is not.
        // Saying which paragraph they came from turns three wasted calls into one correction.
        for (int n = 0; n < paragraphs.size(); n++) {
            if (fold(paragraphs.get(n)).contains(wanted)) {
                return "again: the entry for paragraph " + entry.paragraph() + " cites a paragraph"
                        + ", but the words it copies are in paragraph " + (n + 1)
                        + ". Change the number to " + (n + 1) + ", or copy words from paragraph "
                        + entry.paragraph() + " instead: " + shown(span);
            }
        }
        return "again: entry " + at + " copies words that are in no paragraph of this chapter, so the "
                + "entry is anchored to nothing. They may be your own wording, or they may run across a "
                + "paragraph break — copy from inside one paragraph. The words were: " + shown(span);
    }

    /**
     * ONE-BASED, because the number the model writes down is the number a reader of the reading will
     * look up, and nobody counts paragraphs from zero. The single conversion lives in the verifier.
     */
    private String numbered(Chain.Chapter chapter) throws IOException {
        List<String> paragraphs = text.paragraphs(chapter);
        StringBuilder out = new StringBuilder();
        for (int n = 0; n < paragraphs.size(); n++) {
            out.append('[').append(n + 1).append("] ").append(paragraphs.get(n)).append("\n\n");
        }
        return out.toString();
    }

    /** One event: where it is, what it was, and the words that prove both. */
    private record Entry(int paragraph, String what, String span) {
    }

    private List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        for (String raw : said.split("\\R")) {
            String line = raw.trim();
            if (!line.toUpperCase(Locale.ROOT).startsWith("ENTRY")) {
                continue;
            }
            String rest = line.substring("ENTRY".length()).trim();
            // "ENTRY:", "ENTRY 3:" and "ENTRY 3 |" all reach here. The colon is only a label
            // separator if it comes before the first bar; inside the sentence it is punctuation.
            int bar = rest.indexOf('|');
            int colon = rest.indexOf(':');
            if (colon >= 0 && (bar < 0 || colon < bar)) {
                rest = rest.substring(colon + 1);
            }
            String[] field = rest.split("\\|", 3);
            if (field.length < 3) {
                continue;
            }
            out.add(new Entry(number(field[0]), field[1].trim(), field[2].trim()));
        }
        return out;
    }

    /** The first run of digits, so "7", "[7]" and "para 7" all mean seven, and prose means nothing. */
    private static int number(String field) {
        String digits = field.replaceAll("^\\D*(\\d+).*$", "$1");
        return digits.matches("\\d+") ? Integer.parseInt(digits) : -1;
    }

    /**
     * NONE has to be recognised loosely or it is not an escape hatch at all: a model that adds a full
     * stop or a line of apology has still said the only thing there was to say, and rejecting that
     * spends three calls to be told it again. Loose is safe here because {@link #empty} makes NONE
     * pay for itself against the text.
     */
    private boolean none() {
        String answer = fold(said).replaceAll("[.!]", "").trim();
        return answer.equals("none") || answer.startsWith("none ") || answer.endsWith(" none");
    }

    /**
     * A MATCHED PAIR OF SURROUNDING QUOTES COMES OFF FIRST, and this is the line that this project
     * paid three model calls to learn. A model delimiting the words it copied is doing what it was
     * asked; a verifier that then tests the delimiters as though they were text rejects a correct
     * answer and, echoing it back, shows the model something identical to what it just sent.
     */
    private static String unquoted(String span) {
        String s = span.trim();
        while (s.length() > 1 && closes(s.charAt(0), s.charAt(s.length() - 1))) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private static boolean closes(char open, char close) {
        return (open == '"' && close == '"') || (open == '\'' && close == '\'')
                || (open == '“' && close == '”') || (open == '‘' && close == '’')
                || (open == '«' && close == '»');
    }

    /**
     * WHAT THE COMPARISON IS ALLOWED TO IGNORE, and the rule is: everything a printer chose, nothing
     * an author wrote. Line wrapping, curly quotes and the accents on Bezúkhov are the corpus's
     * typography, and a model retyping "Bezukhov" has copied the chapter correctly in every sense
     * that matters here. The words themselves are not folded away, so an invented span still fails.
     */
    private static String fold(String s) {
        String flat = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[“”„]", "\"")
                .replaceAll("[‘’]", "'")
                .replaceAll("[—–]", "-")
                .replaceAll("\\s+", " ");
        return flat.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Does this sentence talk about our character? The full name and the personal name both count,
     * because "Prince Andrew" is how the corpus introduces him and "Andrew" is how a sentence about
     * him usually runs.
     */
    private static boolean mentions(String sentence, Chain.Hero hero) {
        String line = fold(sentence);
        String full = fold(hero.name());
        if (full.isEmpty()) {
            return false;
        }
        if (line.contains(full)) {
            return true;
        }
        String personal = full.substring(full.lastIndexOf(' ') + 1);
        return personal.length() > 2 && line.contains(personal);
    }

    /**
     * Echoed in angle brackets and never in quote marks. The point of {@link #unquoted} is that a
     * span wearing delimiters cost this project a triad; echoing one back inside quote marks would
     * reintroduce the same ambiguity in the one place the model is reading for a difference.
     */
    private static String shown(String s) {
        return "<< " + (s.length() > 80 ? s.substring(0, 80) + "…" : s) + " >>";
    }

    /**
     * The ordered entries, which is what a fold concatenates and what the page prints.
     *
     * <p>The anchor travels with the entry rather than being verified and thrown away. A reading is
     * on disk for a long time and the fold that reads it later has no chapter in front of it, so an
     * entry that arrives without its paragraph and its span is an assertion nobody can re-check.
     *
     * <p>AN EMPTY LIST CARRIES ITS REASON. Chapters with no entries are the majority — one character
     * against 365 chapters — and an empty section with nothing beside it cannot be told apart from a
     * section that never ran. The note is the answer that emptied it, which is NONE, or the claim
     * that the name in this chapter belongs to somebody else.
     */
    @Override
    public String body() {
        StringBuilder out = new StringBuilder("{\"entries\":[");
        List<Entry> entries = entries();
        for (int n = 0; n < entries.size(); n++) {
            Entry entry = entries.get(n);
            out.append(n == 0 ? "" : ",")
                    .append("{\"paragraph\":").append(entry.paragraph())
                    .append(",\"what\":").append(Ask.json(entry.what()))
                    .append(",\"span\":").append(Ask.json(unquoted(entry.span())))
                    .append('}');
        }
        out.append("],\"count\":").append(entries.size());
        if (entries.isEmpty()) {
            out.append(",\"note\":").append(Ask.json(said.trim()));
        }
        return out.append('}').toString();
    }
}
