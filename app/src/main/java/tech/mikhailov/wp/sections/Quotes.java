package tech.mikhailov.wp.sections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tech.mikhailov.ratchet.llm.Chat;

import tech.mikhailov.ratchet.flow.Agent;
import tech.mikhailov.ratchet.flow.Flow;
import tech.mikhailov.wp.Chain;
import tech.mikhailov.wp.Text;

/**
 * WHAT HE SAYS, AND THE ONE VERIFIER HERE THAT CANNOT BE ARGUED WITH.
 *
 * <p>A quotation is in the book or it is not. So nothing in this section needs a second model to
 * judge it: every span the doer hands back is looked up in {@link Text#spoken}, and an invented
 * line dies on a substring test that costs no call at all. That is why this is the section to
 * believe when the others disagree — the others are opinions about a chapter, and this one is the
 * chapter.
 *
 * <p>A SPLIT SPEECH IS ONE SPEECH. Tolstoy puts the tag inside it — {@code “From what I have
 * heard,” said Pierre, blushing and breaking into the conversation, “almost all the aristocracy
 * has already gone over to Bonaparte’s side.”} — so a reader who counts quoted spans gets two
 * lines where the man said one thing. Pierre's twelve spans in Book One chapter V are eight
 * speeches, and a page that prints twelve has him stammering. The answer therefore groups: SAID
 * opens a speech, ALSO continues one. The grouping is not taken on trust — the gap between two
 * spans is right there in the chapter, and reading it is free.
 *
 * <p>SILENCE IS AN ANSWER AND HAS TO SURVIVE. Pierre speaks not one word in Book One chapter
 * XXIV, the chapter in which his life changes, and that silence is the characterisation. A
 * section that felt obliged to come back with something would come back with somebody else's
 * lines, so the only thing this verifier holds against an empty answer is the chapter printing
 * "said Pierre" while the answer says nothing.
 */
public final class Quotes implements Work {

    /**
     * THE SAME PASSAGES AS {@link Text#spoken}, BUT WITH THEIR POSITIONS.
     *
     * <p>Duplicated deliberately and it is the only duplication here. {@code Text} owns what counts
     * as speech and stays the authority for whether a span is real; this copy exists because
     * {@code spoken()} hands back whitespace-collapsed strings, and a collapsed string cannot be
     * found again in the raw chapter, so the offsets are gone. Every check below that reads the
     * TAG — who the text says is talking, whether a gap splits one speech or separates two — needs
     * an offset. The two are held together by a size comparison before any of those checks run: if
     * the definitions ever drift apart, the positional checks stand down rather than run against a
     * map of a different chapter.
     */
    private static final Pattern SPOKEN = Pattern.compile("“[^”]{2,}”", Pattern.DOTALL);

    /** A blank line is a new paragraph and therefore, in dialogue, a new speaker. */
    private static final Pattern BREAK = Pattern.compile("\\n\\s*\\n");

    /**
     * The verbs an attributive tag is built from. Maude is consistent about this, which is what
     * makes the tag mechanically readable at all.
     */
    private static final String VERBS = "said|says|replied|answered|asked|remarked|declared"
            + "|continued|added|cried|exclaimed|whispered|murmured|muttered|shouted|repeated"
            + "|suggested|began|interrupted|rejoined|returned|observed|objected|retorted"
            + "|insisted|urged|responded|concluded|explained|screamed|sobbed|went on|put in"
            + "|chimed in";

    /** Up to three capitalised words, so "Monsieur Pierre" and "Princess Anna Mikháylovna" hold. */
    private static final String NAME = "\\p{Lu}[\\p{L}’'.-]*(?:\\s+\\p{Lu}[\\p{L}’'.-]*){0,2}";

    private static final Pattern TAG_VERB_FIRST =
            Pattern.compile("\\b(?:" + VERBS + ")\\s+(" + NAME + ")");
    private static final Pattern TAG_NAME_FIRST =
            Pattern.compile("(" + NAME + ")\\s+(?:" + VERBS + ")\\b");

    /** A name OR a description. Never rejects; see {@link #disputed}. */
    private static final Pattern TAG_ANY_VERB_FIRST = Pattern.compile(
            "\\b(?:" + VERBS + ")\\s+(the\\s+\\p{L}+|" + NAME + ")");

    /** How far past a closing quote mark the tag can be. Past this it is the next sentence. */
    private static final int TAG = 140;

    /** The longest gap that is still a tag rather than narration. Chapter V has one of 185. */
    private static final int SPLIT = 220;

    /** How many objections one rejection carries. Three rounds is the budget; five is generous. */
    private static final int OBJECTIONS = 5;

    private final Text text;
    private final Chat model;
    private String said = "";
    private List<List<String>> kept = List.of();

    public Quotes(Text text, Chat model) {
        this.text = text;
        this.model = model;
    }

    /**
     * NO MODEL CALL. Like {@code appearances}, the plan does not vary: the question is the same
     * question in all 365 chapters, and the two things worth saying — rejoin a split speech, take
     * only what the text gives him — are the same two things every time. Buying that sentence
     * 13,140 times would be paying to be told what is written here.
     */
    @Override
    public Agent planner(Chain.Hero hero, Chain.Chapter chapter) {
        return brief -> "List every speech " + hero.name() + " makes in this chapter, in the order "
                + "they occur, copied word for word from the text.\n\nTolstoy interrupts a speech "
                + "with a tag, and the interruption is not a second speech: “From what I have "
                + "heard,” said Pierre, blushing, “almost all the aristocracy...” is ONE thing "
                + "Pierre says, printed as two quoted pieces. Give the first piece as SAID and the "
                + "second as ALSO.\n\nTake only what the text gives " + hero.name() + ". A line the "
                + "chapter tags with another name belongs to that person, and a chapter in which "
                + hero.name() + " says nothing is a real answer.";
    }

    @Override
    public Flow.Doer doer(Chain.Hero hero, Chain.Chapter chapter) {
        return (plan, feedback) -> {
            String body = text.of(chapter);
            String ask = plan
                    + (feedback.isBlank() ? "" : "\n\nYour last answer was rejected: " + feedback)
                    + "\n\nAnswer as lines and nothing else:\nSAID: <the words spoken, copied "
                    + "EXACTLY from the chapter, quotation marks and all>\nALSO: <the rest of that "
                    + "same speech, when a tag splits it — otherwise leave this line out>\n\nOne "
                    + "SAID for each speech. If " + hero.name() + " speaks nothing in this chapter, "
                    + "answer the single word NONE.\n\nCHAPTER " + chapter.numeral() + " of "
                    + chapter.book() + ":\n\n" + body;
            said = Ask.once(model, ask);
            return said;
        };
    }

    /**
     * THE WORKSPACE IS THE CHAPTER'S OWN DIALOGUE, NUMBERED, and that is not decoration.
     *
     * <p>{@code Flow.triad} splices this into the doer's next prompt after a rejection, so the list
     * is absent on the round that costs nothing and present on the round after the model has
     * demonstrably failed to copy something. A small model that cannot transcribe a sentence out of
     * 1,400 words of prose can pick one out of a numbered list of forty, and putting the list in
     * the first prompt would pay for it every time to fix the answers that were already right.
     */
    @Override
    public String facts(Chain.Hero hero, Chain.Chapter chapter) throws IOException {
        StringBuilder out = new StringBuilder(said);
        out.append("\n\nEvery passage this chapter prints between quotation marks, in order. A "
                + "speech of ").append(hero.name()).append("'s is one of these, copied exactly:");
        int n = 0;
        for (String passage : text.spoken(chapter)) {
            out.append("\n").append(++n).append(". ").append(passage);
        }
        return out.toString();
    }

    /**
     * SEVEN CHECKS, NO MODEL, AND EVERY FAULT REPORTED AT ONCE.
     *
     * <p>The triad has three rounds. A verifier that reveals one fault per round can repair three
     * faults at most, and this one routinely finds more than three in one answer — a fabricated
     * line, two speeches that should be one, and a line of the vicomte's, all in the same reply.
     * So it says everything it has, and the model spends its rounds on the work rather than on
     * discovering what else was wrong.
     *
     * <p>NOTHING GOES OUT WRAPPED IN QUOTE MARKS. This project has already lost a triad to that:
     * a verifier objected that a span did not occur in the chapter and printed the span inside the
     * quote marks it had used as delimiters, so the model read the objection as being about a
     * string it had never written, repeated itself twice, and the budget ran out having changed
     * nothing. A span here is printed bare, last in its sentence, carrying only the “ ” the book
     * put around it. And a matched pair of surrounding quotes is stripped from anything the model
     * sends IN, so the same argument cannot come back the other way round.
     *
     * <p>WHAT IT WILL NOT REJECT ON is a description. "said the vicomte" beside a line claimed for
     * Pierre looks like a catch and is not a safe one: this corpus calls Pierre "the young man" for
     * half of chapter XXIV, and a verifier that treated a description as a foreign speaker would
     * reject the truth in exactly the chapter that matters most. Which description denotes whom is
     * the {@code names} section's whole job. Here only a capitalised NAME in the tag rejects.
     */
    @Override
    public Agent verifier(Chain.Hero hero, Chain.Chapter chapter) {
        return judged -> {
            String body = text.of(chapter);
            List<String> spoken = text.spoken(chapter);
            List<Span> spans = spansOf(body);
            List<List<String>> claims = utterances(said);
            List<String> wrong = new ArrayList<>();

            if (claims.isEmpty() && !declaresNone(said)) {
                return "again: nothing came back in the form the plan asked for. One line per "
                        + "speech — SAID: to open a speech, ALSO: for the rest of one a tag splits "
                        + "— or the single word NONE if " + hero.name() + " speaks nothing here.";
            }
            if (orphanAlso(said)) {
                wrong.add("an ALSO: line arrived before any SAID: line. ALSO continues the speech "
                        + "above it, so the first line of an answer is always a SAID");
            }

            // Every claim resolved to the chapter's own string, or reported as not being one.
            List<List<Integer>> at = new ArrayList<>();
            List<List<String>> canonical = new ArrayList<>();
            for (List<String> utterance : claims) {
                List<Integer> found = new ArrayList<>();
                List<String> exact = new ArrayList<>();
                for (String claim : utterance) {
                    int i = locate(spoken, claim);
                    found.add(i);
                    exact.add(i < 0 ? claim : spoken.get(i));
                    if (i < 0) {
                        wrong.add(flat(body).contains(key(claim))
                                ? "these words are in the chapter but they are not something "
                                        + "anybody says — a speech is what stands between “ and ”, "
                                        + "and the tag between two halves of one is not part of "
                                        + "it: " + show(claim)
                                : "these words do not occur in this chapter: " + show(claim));
                    }
                }
                at.add(found);
                canonical.add(exact);
            }
            // THE READING KEEPS THE BOOK'S STRING, NEVER THE MODEL'S COPY OF IT. A span is
            // located case- and whitespace-insensitively, so a transcription wobble does not cost
            // a round, and what is stored is the passage as Tolstoy's translator printed it.
            kept = canonical;

            Set<Integer> seen = new LinkedHashSet<>();
            for (List<Integer> found : at) {
                for (int i : found) {
                    if (i >= 0 && !seen.add(i)) {
                        wrong.add("this speech is listed twice, which would print it twice on the "
                                + "page: " + show(spoken.get(i)));
                    }
                }
            }

            // The positional checks stand down rather than run against a map of another chapter.
            if (spans.size() == spoken.size()) {
                for (List<Integer> found : at) {
                    for (int k = 0; k < found.size(); k++) {
                        int i = found.get(k);
                        if (i < 0) {
                            continue;
                        }
                        String other = foreignSpeaker(body, spans, i, hero, k > 0);
                        if (other != null) {
                            wrong.add("the chapter tags this line as " + other + "'s, not "
                                    + hero.name() + "'s, so it is not his to quote: "
                                    + show(spoken.get(i)));
                        }
                        if (k == found.size() - 1 && splits(body, spans, i)
                                && !claimed(at, i + 1) && !disputed(body, spans, i, hero)) {
                            wrong.add("a tag splits that speech and you kept only the first half. "
                                    + "Add the rest of it as an ALSO: line under it: "
                                    + show(spoken.get(i + 1)));
                        }
                        if (k > 0 && !(found.get(k - 1) >= 0 && i == found.get(k - 1) + 1
                                && splits(body, spans, found.get(k - 1)))) {
                            wrong.add("this ALSO: does not continue the line above it — what "
                                    + "separates them in the chapter ends a sentence, so they are "
                                    + "two speeches. Give it its own SAID: " + show(spoken.get(i)));
                        }
                    }
                }
                for (int u = 1; u < at.size(); u++) {
                    int last = last(at.get(u - 1));
                    int first = at.get(u).isEmpty() ? -1 : at.get(u).get(0);
                    if (last >= 0 && first == last + 1 && splits(body, spans, last)) {
                        wrong.add("these two SAID: lines are one speech — the chapter puts nothing "
                                + "between them but a tag that does not end the sentence. The "
                                + "second one is an ALSO: " + show(spoken.get(first)));
                    }
                }
                // THE CHECK AGAINST GIVING UP. Everything above catches a line that should not be
                // there; without this one, an answer of NONE passes every test ever written, and
                // an empty page reads exactly like a chapter the character is not in.
                for (int i = 0; i < spans.size(); i++) {
                    String tagged = heroSpeaker(body, spans, i, hero);
                    if (tagged != null && !claimed(at, i)) {
                        wrong.add("the chapter names " + tagged + " in the tag beside this line, so "
                                + hero.name() + " is the one saying it and it is missing from your "
                                + "answer: " + show(spoken.get(i)));
                    }
                }
            }

            if (wrong.isEmpty()) {
                return "done";
            }
            StringBuilder out = new StringBuilder("again: ").append(wrong.size() == 1
                    ? "one thing is wrong." : wrong.size() + " things are wrong.");
            for (int i = 0; i < Math.min(wrong.size(), OBJECTIONS); i++) {
                out.append("\n- ").append(wrong.get(i));
            }
            if (wrong.size() > OBJECTIONS) {
                out.append("\n- and ").append(wrong.size() - OBJECTIONS)
                        .append(" more faults of the same kinds.");
            }
            return out.toString();
        };
    }

    /**
     * WHETHER THE GAP BETWEEN TWO SPANS IS A TAG OR A SENTENCE, which is the whole rejoin rule.
     *
     * <p>Three tests and each one earns its place against the real text. A blank line is a new
     * paragraph and so a new speaker — in Book One chapter XXIV eleven pairs of spans touch with
     * nothing but a paragraph break between them, and joining those would put the eldest princess's
     * words in Prince Vasíli's mouth. A full stop ends the tag's sentence, which is what separates
     * {@code “Catiche has had tea served,” said Prince Vasíli to Anna Mikháylovna. “Go and take
     * something...”} — two speeches, and the gold set indexes them apart — from {@code “From what I
     * have heard,” said Pierre, blushing, “almost all...”}, which is one. And a length cap, because
     * a tag is a tag; chapter V's longest is 185 characters and the cap sits just above it.
     */
    private static boolean splits(String body, List<Span> spans, int i) {
        if (i < 0 || i + 1 >= spans.size()) {
            return false;
        }
        String gap = body.substring(spans.get(i).to(), spans.get(i + 1).from());
        if (gap.isBlank() || gap.length() > SPLIT || BREAK.matcher(gap).find()) {
            return false;
        }
        return gap.indexOf('.') < 0 && gap.indexOf('!') < 0 && gap.indexOf('?') < 0;
    }

    /** The tag names somebody, and that somebody is not our man. Null when the tag names nobody. */
    private static String foreignSpeaker(String body, List<Span> spans, int i, Chain.Hero hero,
            boolean continuation) {
        String who = namedSpeaker(after(body, spans, i));
        if (who == null && continuation && i > 0) {
            who = namedSpeaker(body.substring(spans.get(i - 1).to(), spans.get(i).from()));
        }
        return who != null && !isHero(who, hero) ? who : null;
    }

    /** The tag names our man outright, which is what makes leaving the line out a fault. */
    private static String heroSpeaker(String body, List<Span> spans, int i, Chain.Hero hero) {
        String who = namedSpeaker(after(body, spans, i));
        return who != null && isHero(who, hero) ? who : null;
    }

    /**
     * THE TAG, WHICH STOPS AT THE END OF ITS OWN SENTENCE.
     *
     * <p>Cutting there is not tidiness. After chapter V's {@code “Won’t you come over to the other
     * table?”} the text runs {@code suggested Anna Pávlovna. But Pierre continued his speech...} —
     * a window that ran past the full stop would find "Pierre continued", read it as a tag, and
     * demand that Anna Pávlovna's question be filed as one of Pierre's speeches. That is a verifier
     * rejecting a correct answer, which costs three rounds and teaches the model something false.
     */
    private static String after(String body, List<Span> spans, int i) {
        int from = spans.get(i).to();
        int to = Math.min(body.length(), from + TAG);
        if (i + 1 < spans.size()) {
            to = Math.min(to, spans.get(i + 1).from());
        }
        String window = body.substring(from, to);
        Matcher paragraph = BREAK.matcher(window);
        if (paragraph.find()) {
            window = window.substring(0, paragraph.start());
        }
        for (int c = 0; c < window.length(); c++) {
            if (window.charAt(c) == '.' || window.charAt(c) == '!' || window.charAt(c) == '?') {
                return window.substring(0, c);
            }
        }
        return window;
    }

    /** Collapsed, because the corpus is hard-wrapped and "Prince\nVasíli" is one name. */
    private static String namedSpeaker(String tag) {
        Matcher m = TAG_VERB_FIRST.matcher(tag);
        if (m.find()) {
            return m.group(1).replaceAll("\\s+", " ");
        }
        m = TAG_NAME_FIRST.matcher(tag);
        return m.find() ? m.group(1).replaceAll("\\s+", " ") : null;
    }

    /**
     * SOMEBODY ELSE IS DESCRIBED IN THE TAG — ENOUGH TO STOP ASKING FOR MORE, NEVER TO REJECT.
     *
     * <p>A description cannot reject, for the reason the class javadoc gives: "the young man" is
     * Pierre. But it can still stop this verifier making things worse. Claim the vicomte's
     * {@code “Liberty and equality,”} for Pierre and the split-speech check, reading only the
     * chapter, would helpfully point out that the second half is missing — telling the model to
     * quote MORE of the vicomte. So a tag that describes somebody other than our man silences the
     * demand for the rest of the speech, and says nothing about the claim itself.
     */
    private static boolean disputed(String body, List<Span> spans, int i, Chain.Hero hero) {
        Matcher m = TAG_ANY_VERB_FIRST.matcher(after(body, spans, i));
        return m.find() && !isHero(m.group(1).replaceAll("\\s+", " "), hero);
    }

    /** "Monsieur Pierre" is Pierre and "Prince Vasíli" is not Prince Andrew; the last word decides. */
    private static boolean isHero(String who, Chain.Hero hero) {
        String name = hero.name().toLowerCase(Locale.ROOT).trim();
        String seen = who.toLowerCase(Locale.ROOT);
        if (name.isEmpty() || seen.contains(name)) {
            return true;
        }
        String surname = name.substring(name.lastIndexOf(' ') + 1);
        return surname.length() > 2 && seen.contains(surname);
    }

    private static boolean claimed(List<List<Integer>> at, int i) {
        return at.stream().anyMatch(found -> found.contains(i));
    }

    private static int last(List<Integer> found) {
        return found.isEmpty() ? -1 : found.get(found.size() - 1);
    }

    /**
     * Which of the chapter's spoken passages this claim is, or -1.
     *
     * <p>Exact first, then containment, because a model that copies half a passage has made a
     * transcription slip rather than a finding: the unit of this section is the passage, and
     * {@code Text.spoken} is what says where one starts and stops.
     */
    private static int locate(List<String> spoken, String claim) {
        String want = key(claim);
        if (want.length() < 2) {
            return -1;
        }
        for (int i = 0; i < spoken.size(); i++) {
            if (key(spoken.get(i)).equals(want)) {
                return i;
            }
        }
        for (int i = 0; i < spoken.size(); i++) {
            if (key(spoken.get(i)).contains(want)) {
                return i;
            }
        }
        return -1;
    }

    private List<Span> spansOf(String body) {
        List<Span> out = new ArrayList<>();
        Matcher m = SPOKEN.matcher(body);
        while (m.find()) {
            out.add(new Span(m.start(), m.end()));
        }
        return out;
    }

    private static List<List<String>> utterances(String answer) {
        List<List<String>> out = new ArrayList<>();
        for (String raw : answer.split("\\R")) {
            String line = raw.strip();
            String label = line.toUpperCase(Locale.ROOT);
            if (label.startsWith("SAID:")) {
                List<String> one = new ArrayList<>();
                add(one, line.substring(5));
                out.add(one);
            } else if (label.startsWith("ALSO:") && !out.isEmpty()) {
                add(out.get(out.size() - 1), line.substring(5));
            }
        }
        out.removeIf(List::isEmpty);
        return out;
    }

    private static void add(List<String> to, String span) {
        String one = span.strip();
        if (!one.isEmpty() && !one.equalsIgnoreCase("none")) {
            to.add(one);
        }
    }

    private static boolean orphanAlso(String answer) {
        for (String raw : answer.split("\\R")) {
            String label = raw.strip().toUpperCase(Locale.ROOT);
            if (label.startsWith("SAID:")) {
                return false;
            }
            if (label.startsWith("ALSO:")) {
                return true;
            }
        }
        return false;
    }

    private static boolean declaresNone(String answer) {
        for (String raw : answer.split("\\R")) {
            if (raw.strip().replaceAll("[.\\s]+$", "").equalsIgnoreCase("none")) {
                return true;
            }
        }
        return false;
    }

    /**
     * STRIP A MATCHED PAIR OF SURROUNDING QUOTES, and go on stripping while they match.
     *
     * <p>The model is asked to copy the passage with its quotation marks, and it will sometimes
     * put its own around that. Both readings have to compare equal to the chapter, because the
     * alternative is a rejection whose content is "your quote marks are wrong", which is not a
     * fault anybody can act on and is how three rounds get spent on punctuation.
     */
    private static String unquoted(String s) {
        String out = s.trim();
        while (out.length() > 1 && (out.charAt(0) == '"' || out.charAt(0) == '\'')
                && out.charAt(out.length() - 1) == out.charAt(0)) {
            out = out.substring(1, out.length() - 1).trim();
        }
        return out;
    }

    /** Curly and straight are the same mark, and a model normalises them without being asked. */
    private static String flat(String s) {
        return s.replace('“', '"').replace('”', '"')
                .replace('‘', '\'').replace('’', '\'')
                .replace("…", "...")
                .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static String key(String s) {
        return unquoted(flat(s));
    }

    /** Bare, truncated, and last in its sentence. Adding delimiters here is the trap above. */
    private static String show(String s) {
        String one = s.replaceAll("\\s+", " ").trim();
        return one.length() > 80 ? one.substring(0, 80) + "…" : one;
    }

    /**
     * WHAT THE PAGE PRINTS AND WHAT A LATER PASS CAN RE-CHECK, which are not the same string.
     *
     * <p>{@code said} is the speech as a reader wants it, with the tag taken out of the middle.
     * {@code spans} is what the book actually prints, and every one of them survives the substring
     * test on its own — so the fold can verify a quote it inherited without going back to the
     * chapter for the rejoining rule.
     */
    @Override
    public String body() {
        StringBuilder out = new StringBuilder("{\"count\":").append(kept.size())
                .append(",\"quotes\":[");
        for (int i = 0; i < kept.size(); i++) {
            List<String> spans = kept.get(i);
            out.append(i == 0 ? "" : ",")
                    .append("{\"said\":").append(Ask.json(String.join(" ", spans)))
                    .append(",\"spans\":[");
            for (int j = 0; j < spans.size(); j++) {
                out.append(j == 0 ? "" : ",").append(Ask.json(spans.get(j)));
            }
            out.append("]}");
        }
        return out.append("]}").toString();
    }

    /** One spoken passage, with the offsets {@link Text#spoken} normalises away. */
    private record Span(int from, int to) {
    }
}
