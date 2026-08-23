package tech.mikhailov.wp.sections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import dev.langchain4j.model.chat.ChatModel;

import tech.mikhailov.ratchet.flow.Agent;
import tech.mikhailov.ratchet.flow.Flow;
import tech.mikhailov.wp.Chain;
import tech.mikhailov.wp.Text;

/**
 * WHAT THIS CHAPTER STATES TO BE TRUE OF THEM — NOT WHAT THE NOVEL EVENTUALLY SETTLES ON.
 *
 * <p>Status, title, rank, house, occupation, relatives: the infobox rows, asked once per chapter
 * rather than once per character, because the answers move. Andrew is alive until Book Twelve and
 * Pierre is illegitimate until he is not. One infobox value would flatten a change into a snapshot
 * and lose which book it happened in; 365 of them keep the date on the change.
 *
 * <p>A "DEAD" MARKED EARLY IS THE WORST THING THIS SECTION CAN DO, because it does not stay here:
 * the fold carries it forward and every later chapter reads as posthumous, so the page announces a
 * death in chapter XXI for a man who is talking in chapter XXIV. That is the one answer the
 * verifier refuses to take on trust — the cited span has to say he died, and "dying" does not say
 * it. Count Bezúkhov is dying for four chapters and dead for two paragraphs, and the words that
 * settle it are "He is no more."
 *
 * <p>THE TITLE STANDING NEXT TO A NAME OFTEN BELONGS TO SOMEBODY ELSE. "Count Bezúkhov" is Pierre's
 * father through Book One and becomes Pierre only after chapter XXIV; "Bolkónski" is the old prince
 * or his son. A chapter that mentions the count's house, the count's papers and the count's death
 * while Pierre stands in the room will hand a careless extractor the title, the house and a status
 * of dead, all three wrong and all three about the same man. The instruction says so out loud, and
 * a relative whose name is the hero's own name is refused as arithmetic.
 *
 * <p>NOTHING STATED IS A NORMAL ANSWER. Most chapters say nothing about anybody's rank, and a
 * section that must produce a row will invent one. So {@code NONE} is a legal answer and an empty
 * list is a legal reading.
 */
public final class Facts implements Work {

    /** Closed, so a wrong field is a substring test rather than an opinion. */
    private static final List<String> FIELDS =
            List.of("status", "title", "rank", "house", "occupation", "relative");

    private static final List<String> STATUS = List.of("alive", "dead");

    /** Ways of writing "the chapter does not say", every one of which folds into the page as a row. */
    private static final List<String> EMPTY =
            List.of("none", "n/a", "na", "unknown", "unstated", "not stated", "not mentioned", "-");

    /**
     * Words that assert a death has happened. "no more" is in here for one chapter: Book One XXIV
     * announces the count's death as "He is no more...." and contains no other word for it near the
     * moment. A death list without that phrase rejects the only correct span in the most important
     * chapter this section has.
     */
    private static final Pattern DIED = Pattern.compile(
            "\\b(died|death|dead|deceased|expired|corpse|lifeless|killed|slain|buried|burial"
                    + "|funeral|perished)\\b|\\b(is|was|were|are) no more\\b|\\blast breath\\b"
                    + "|\\bpassed away\\b",
            Pattern.CASE_INSENSITIVE);

    /** Still alive, and the commonest way this section gets a death wrong by four chapters. */
    private static final Pattern DYING =
            Pattern.compile("\\b(dying|dies|die)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Openers at even indices, closers after them. A span arrives wrapped in whatever the model
     * chose as a delimiter, and the delimiter is not part of the span.
     */
    private static final String PAIRS = "\"\"''“”‘’«»";

    private final Text text;
    private final ChatModel model;
    private String said = "";

    public Facts(Text text, ChatModel model) {
        this.text = text;
        this.model = model;
    }

    /**
     * NO MODEL CALL. The question is the same 365 times over and the trap is the same 365 times
     * over, so the plan is a constant and paying for it would be paying to be told it again.
     *
     * <p>The last sentence is the expensive one. Without it the model reads "Count Bezúkhov's
     * death" in a room Pierre is standing in and gives Pierre the title, the house and the death.
     */
    @Override
    public Agent planner(Chain.Hero hero, Chain.Chapter chapter) {
        return brief -> "State what THIS CHAPTER says is true of " + hero.name() + " right now: "
                + "whether they are alive or dead, any title or rank it gives them, the house or "
                + "family they belong to, what they do, and the relatives it names. Nothing you "
                + "know from elsewhere in the novel, and nothing the chapter only implies. "
                + "A title or a death standing next to " + hero.name() + " often belongs to "
                + "somebody else in the room — leave it out unless the chapter applies it to "
                + hero.name() + ". If the chapter states none of these, say so.";
    }

    /**
     * Lines with two pipes, not JSON. A small local model asked for an object returns prose about
     * an object often enough that the parser becomes the least reliable part of the section, and
     * everything this section needs — a field, a value, a span — fits on one line.
     */
    @Override
    public Flow.Doer doer(Chain.Hero hero, Chain.Chapter chapter) {
        return (plan, feedback) -> {
            String body = text.of(chapter);
            String ask = plan
                    + (feedback.isBlank() ? "" : "\n\nYour last answer was rejected: " + feedback)
                    + "\n\nAnswer as one line per fact and nothing else:\n"
                    + "FACT: <field> | <value> | <words copied EXACTLY from the chapter that say so>"
                    + "\n\nfield is one of: " + String.join(", ", FIELDS)
                    + "\nstatus is exactly alive or dead, and dead only if the chapter says the "
                    + "death has happened.\nOne relative per line.\n"
                    + "If the chapter states none of these about " + hero.name()
                    + ", answer the single word NONE.\n\nCHAPTER " + chapter.numeral() + " of "
                    + chapter.book() + ":\n\n" + body;
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
     * ARITHMETIC, AND IT CAN REJECT.
     *
     * <p>Every check here is a test a model would be worse at than a substring search: the field is
     * one of six words, the status is one of two, the span occurs in the chapter or does not, and a
     * death is asserted by a word for death or by nothing.
     *
     * <p>THE FEEDBACK IS THE PRODUCT, not the verdict. A verifier that rejects with something the
     * model cannot act on spends three rounds and changes nothing — that happened on this corpus:
     * a span was quoted back WITH the quote marks the model had used as delimiters, so the model
     * could not see what it was supposed to fix and repeated itself until the budget ran out. Hence
     * two rules below: a matched pair of surrounding quotes is stripped before anything is checked,
     * and what was actually tested is shown between square brackets, which cannot be mistaken for
     * part of the text.
     */
    @Override
    public Agent verifier(Chain.Hero hero, Chain.Chapter chapter) {
        return judged -> {
            List<String[]> entries = entries();
            boolean none = declaresNone();
            if (entries.isEmpty()) {
                // Nothing stated is a real answer, but silence is not: an empty reply and a
                // considered NONE are indistinguishable on disk, and only one of them was read.
                return none ? "done" : "again: answer at least one FACT line in the form "
                        + "FACT: <field> | <value> | <span from the chapter>, or the single word "
                        + "NONE if this chapter states nothing about " + hero.name() + ".";
            }
            if (none) {
                return "again: you wrote NONE and also listed " + entries.size()
                        + (entries.size() == 1 ? " fact. " : " facts. ")
                        + "Drop the NONE line and keep the facts, or drop the facts.";
            }

            // Normalised once. The chapter is the Maude text with curly quotes and em dashes in it,
            // and a model retyping a span straightens them almost every time. Rejecting that is a
            // rejection about typography, which the model cannot see in its own output and cannot
            // repair — so both sides are flattened before they are compared.
            String body = flattened(text.of(chapter));
            String living = "";

            for (int i = 0; i < entries.size(); i++) {
                String[] parts = entries.get(i);
                int n = i + 1;
                if (parts.length < 3) {
                    return "again: fact " + n + " is not in the form "
                            + "FACT: <field> | <value> | <span>. It reads "
                            + shown(String.join(" | ", parts).trim()) + ". The span is not "
                            + "optional — a fact with nothing behind it cannot be checked and will "
                            + "not be kept.";
                }
                String field = parts[0].trim().toLowerCase(Locale.ROOT);
                String value = unquoted(parts[1]);
                String span = unquoted(parts[2]);

                if (!FIELDS.contains(field)) {
                    return "again: fact " + n + " has field " + shown(field) + ". Use one of "
                            + String.join(", ", FIELDS) + ".";
                }
                if (value.isBlank()) {
                    return "again: fact " + n + " has an empty value.";
                }
                // Before the EMPTY check, because "status | unknown" deserves the sharper of the two
                // messages: the field has exactly two legal values and naming them ends the round.
                if (field.equals("status") && !STATUS.contains(value.toLowerCase(Locale.ROOT))) {
                    return "again: fact " + n + " gives status " + shown(value)
                            + ". Status is exactly alive or dead. If the chapter does not say, "
                            + "leave the status line out.";
                }
                if (EMPTY.contains(value.toLowerCase(Locale.ROOT))) {
                    // A row saying "title: none" is not the absence of a title, it is a claim about
                    // one, and it folds into the page as a row. Absence is a line that was never
                    // written — which is why NONE exists for the whole answer and not per field.
                    return "again: fact " + n + " gives " + field + " as " + shown(value)
                            + ". Do not write a fact the chapter does not state — leave the line "
                            + "out. If the chapter states nothing at all, answer NONE.";
                }
                if (field.equals("relative")
                        && value.equalsIgnoreCase(hero.name())) {
                    // The Bezúkhov merge arriving as a row: the chapter's "the count" was read as
                    // the hero, so the hero came out as their own father.
                    return "again: fact " + n + " names " + shown(value) + " as a relative of "
                            + hero.name() + ", who is the same person. The chapter is talking "
                            + "about somebody else — name them as the chapter names them.";
                }

                if (span.length() < 4) {
                    return "again: fact " + n + " cites no span. Put the words from the chapter "
                            + "that state it after the second pipe.";
                }
                if (!body.contains(flattened(span))) {
                    return "again: fact " + n + " cites words that are not in this chapter. Copy a "
                            + "run of words straight out of the text. Quotation marks around the "
                            + "span are ignored, so they are not the problem. What was looked for, "
                            + "between brackets: " + shown(span);
                }

                if (field.equals("status") && value.equalsIgnoreCase("dead")
                        && !DIED.matcher(span).find()) {
                    // The check the section exists for. It bounds the damage rather than removing
                    // it — a span saying "all will end in death" would pass — but it stops the one
                    // failure that propagates, which is a man declared dead while he is on his feet.
                    return DYING.matcher(span).find()
                            ? "again: fact " + n + " says dead and cites " + shown(span)
                            + ". A man who is dying is alive. Cite the words that say the death has "
                            + "happened, or answer alive."
                            : "again: fact " + n + " says dead and cites " + shown(span)
                            + ", which does not mention a death. Cite the words that report it, or "
                            + "answer alive.";
                }
                if (field.equals("status")) {
                    String now = value.toLowerCase(Locale.ROOT);
                    if (!living.isEmpty() && !living.equals(now)) {
                        return "again: you gave status " + living + " and status " + now
                                + " for the same character in the same chapter. Keep the one the "
                                + "chapter states and drop the other.";
                    }
                    living = now;
                }
            }
            return "done";
        };
    }

    /**
     * Every FACT line, split but not validated, because the verifier has to complain about the
     * malformed ones and {@link #body} has to survive them — a rejected answer is still written to
     * disk, with {@code settled} false, and a section that threw while rendering it would lose the
     * evidence of what was rejected.
     */
    private List<String[]> entries() {
        List<String[]> out = new ArrayList<>();
        for (String l : said.split("\\R")) {
            String t = l.trim();
            if (t.toUpperCase(Locale.ROOT).startsWith("FACT:")) {
                out.add(t.substring("FACT:".length()).split("\\|", 3));
            }
        }
        return out;
    }

    /** A line that is the word NONE, however the model decorated it. */
    private boolean declaresNone() {
        for (String l : said.split("\\R")) {
            if (l.replaceAll("[^A-Za-z]", "").equalsIgnoreCase("NONE")) {
                return true;
            }
        }
        return false;
    }

    /**
     * THE DELIMITER IS NOT THE SPAN. A model told to copy words out of a chapter puts quotes round
     * them, and the chapter's own speech is already inside curly quotes, so a span can arrive
     * wrapped twice. Strip until there is no matched pair left, then test what remains.
     */
    private static String unquoted(String raw) {
        String t = raw.trim();
        while (t.length() > 1) {
            int at = PAIRS.indexOf(t.charAt(0));
            if (at < 0 || at % 2 != 0 || PAIRS.charAt(at + 1) != t.charAt(t.length() - 1)) {
                break;
            }
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    /** Typography folded and whitespace collapsed, so a comparison is about words. */
    private static String flattened(String s) {
        return s.replace('’', '\'').replace('‘', '\'')
                .replace('“', '"').replace('”', '"')
                .replace('—', '-').replace('–', '-')
                .replace("…", "...")
                .replaceAll("\\s+", " ").trim();
    }

    /** Brackets, not quotes, because the last verifier's quotes were read as part of the text. */
    private static String shown(String s) {
        return "[" + (s.length() > 70 ? s.substring(0, 70) + "…" : s) + "]";
    }

    /**
     * What goes into the reading, as JSON. The spans are stored stripped of their delimiters,
     * because that is the string the verifier actually found in the chapter, and a fold that
     * re-checks it must check the same one.
     */
    @Override
    public String body() {
        StringBuilder out = new StringBuilder("{\"facts\":[");
        boolean first = true;
        for (String[] parts : entries()) {
            if (parts.length < 3) {
                continue;
            }
            out.append(first ? "" : ",")
                    .append("{\"field\":")
                    .append(Ask.json(parts[0].trim().toLowerCase(Locale.ROOT)))
                    .append(",\"value\":").append(Ask.json(unquoted(parts[1])))
                    .append(",\"span\":").append(Ask.json(unquoted(parts[2])))
                    .append("}");
            first = false;
        }
        return out.append("]}").toString();
    }
}
