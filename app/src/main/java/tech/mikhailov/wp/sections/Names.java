package tech.mikhailov.wp.sections;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dev.langchain4j.model.chat.ChatModel;

import tech.mikhailov.ratchet.flow.Agent;
import tech.mikhailov.ratchet.flow.Flow;
import tech.mikhailov.wp.Chain;
import tech.mikhailov.wp.Text;

/**
 * WHAT THIS CHAPTER CALLS THEM — THE SECTION THAT MAKES IDENTITY COMPUTABLE RATHER THAN LUCKY.
 *
 * <p>The other seven ask what happened. This one asks what the character was named while it
 * happened: "Pierre", "Monsieur Pierre", "the young man", "this young Jacobin", "the orator". On its
 * own that is a list of words. Written down for every chapter it is the thing that turns the book's
 * worst hazard into arithmetic — one surface form appearing under two characters is a CONFLICT that
 * code finds at fold time, instead of an error a reader has to happen to notice. "Count Bezúkhov" is
 * Pierre's father through chapter XXI and Pierre from XXIV, and that is only findable because both
 * chapters wrote the form down.
 *
 * <p>So the doer is one model call and everything around it is plain code: the plan does not vary,
 * and "does this phrase occur in the chapter" is a substring test that no model would do better.
 *
 * <p>A FORM THE CHAPTER DOES NOT CONTAIN IS AN INVENTED ALIAS, AND AN INVENTED ALIAS IS HOW TWO
 * PEOPLE BECOME ONE PAGE. That is the whole of what the verifier is for; the rest of it is care
 * taken not to reject for reasons the model cannot see.
 */
public final class Names implements Work {

    /** Alone, each of these points at whoever is nearest. A page cannot be built out of them. */
    private static final Set<String> PRONOUNS = Set.of("he", "him", "his", "she", "her", "hers",
            "it", "its", "they", "them", "their", "theirs", "i", "me", "my", "mine", "we", "us",
            "our", "you", "your", "yours", "one", "who", "himself", "herself", "themselves");

    /**
     * A HEAD NOUN WITH NOTHING IN FRONT OF IT IS NOT A NAME. The test cannot be "does it contain a
     * capital letter", because the best forms this book has for Pierre — "the young man", "the
     * orator", "this young Jacobin" — have none. It is whether anything at all narrows the noun:
     * "the young man" picks somebody out of the room and "the man" does not.
     */
    private static final Set<String> BARE = Set.of("man", "woman", "boy", "girl", "lady",
            "gentleman", "person", "people", "fellow", "child", "figure", "guest", "visitor",
            "speaker", "voice", "other", "others");

    private static final Set<String> DETERMINERS = Set.of("the", "a", "an", "this", "that", "these",
            "those", "his", "her", "their", "my", "our", "your", "its");

    /** A form is a way of naming somebody. Past this, the model has pasted a clause to get past the
     * occurrence test — which it would pass, because the clause is genuinely in the chapter. */
    private static final int MOST_WORDS = 8;

    /** More objections than this is a wall of text nobody acts on; the next round can have the rest. */
    private static final int MOST_OBJECTIONS = 4;

    private final Text text;
    private final ChatModel model;
    private String said = "";
    private String prose = "";
    private String proseOf = "";

    public Names(Text text, ChatModel model) {
        this.text = text;
        this.model = model;
    }

    /**
     * NO MODEL CALL. The question is the same for every chapter and every character, and the one
     * thing a plan could usefully add — which forms are traps — is a standing fact about this book
     * rather than a per-chapter discovery, so paying to be told it 13,140 times buys nothing.
     */
    @Override
    public Agent planner(Chain.Hero hero, Chain.Chapter chapter) {
        return brief -> "List every way this chapter names " + hero.name() + ": the plain name, "
                + "every form of address, and every phrase the narration uses for them — "
                + "\"the young man\", \"this young Jacobin\", \"the orator\". Copy the words exactly "
                + "as the chapter writes them. LEAVE OUT ANY FORM THAT HERE DENOTES SOMEBODY ELSE: "
                + "in this book a bare surname or a bare title usually belongs to the father and not "
                + "the son. A form you leave out is a gap; a form you get wrong merges two people.";
    }

    @Override
    public Flow.Doer doer(Chain.Hero hero, Chain.Chapter chapter) {
        return (plan, feedback) -> {
            String body = proseOf(chapter);
            String ask = plan
                    + (feedback.isBlank() ? "" : "\n\nYour last answer was rejected: " + feedback)
                    + "\n\nAnswer as one line per form and nothing else:"
                    + "\nNAME: <the words, copied from the chapter> | BY: <who calls them that, or narrator>"
                    + "\nNOT: <a form here that looks like theirs but is somebody else> — <who it is>"
                    + "\n\nIf the chapter never names " + hero.name() + ", answer with the single "
                    + "word NONE, plus any NOT: lines that say why it looks as though it does."
                    + "\n\nCHAPTER " + chapter.numeral() + " of " + chapter.book() + ":\n\n" + body;
            said = Ask.once(model, ask);
            return said;
        };
    }

    /** The workspace is the answer just given; this section reads the chapter and nothing else. */
    @Override
    public String facts(Chain.Hero hero, Chain.Chapter chapter) {
        return said;
    }

    /**
     * A SUBSTRING TEST, AND THE CARE IS ALL IN WHAT IT REFUSES TO REJECT FOR.
     *
     * <p>Four rejections, each of them something code settles and a model would only have an opinion
     * about: a form that does not occur in the chapter is invented; a form that is only a pronoun or
     * a bare common noun denotes everybody; a form the length of a sentence is a clause pasted in to
     * satisfy the occurrence test; and a chapter that plainly contains the character's name while the
     * answer contains no form at all has not been read.
     *
     * <p>THE LAST ONE HAS AN HONEST WAY OUT, AND IT HAD TO. Demanding a form because the string
     * "Nicholas" occurs would push the model into answering "Nicholas Andréevich" in a chapter about
     * the old prince — the merge this whole section exists to catch, manufactured by its own
     * verifier. So the objection is answerable either by adding the form or by naming, on a NOT:
     * line, who that occurrence actually is. A gap stated is worth keeping; a gap silently kept is
     * indistinguishable from a chapter nobody read.
     */
    @Override
    public Agent verifier(Chain.Hero hero, Chain.Chapter chapter) {
        return judged -> {
            Index index = Index.of(proseOf(chapter));
            List<String[]> forms = lines("NAME:");
            List<String[]> nots = lines("NOT:");
            boolean named = index.has(hero.name());

            if (forms.isEmpty() && nots.isEmpty() && !saidNone()) {
                return "again: answer with one NAME: line per form, or the single word NONE if the "
                        + "chapter never names " + hero.name() + ".";
            }

            List<String> wrong = new ArrayList<>();
            for (String[] form : forms) {
                String f = form[0];
                if (words(f).size() > MOST_WORDS) {
                    wrong.add(quoted(f) + " is a sentence, not a name — give only the words that "
                            + "stand for the character");
                } else if (denotesAnybody(f)) {
                    wrong.add(quoted(f) + " would fit anybody in the room; a form has to pick one "
                            + "person out of it");
                } else if (!index.has(f)) {
                    wrong.add(quoted(f) + " does not occur in the chapter — copy the words from the "
                            + "text, or drop the form");
                } else if (!form[1].isBlank() && !isNarrator(form[1]) && !index.has(form[1])) {
                    wrong.add("BY " + quoted(form[1]) + " for " + quoted(f) + " is not in the "
                            + "chapter — name the speaker as the chapter writes it, or write narrator");
                }
            }
            for (String[] not : nots) {
                if (!index.has(not[0])) {
                    wrong.add("NOT " + quoted(not[0]) + " does not occur in the chapter either, so "
                            + "there is nothing to rule out — drop the line");
                }
            }
            if (named && !covers(forms, hero.name()) && !covers(nots, hero.name())) {
                wrong.add("the chapter contains " + quoted(hero.name()) + " and none of your forms "
                        + "does. Add it as a NAME: line, or, if that occurrence is a different "
                        + "person, as a NOT: line saying who");
            }
            return wrong.isEmpty() ? "done" : "again: " + joined(wrong);
        };
    }

    /**
     * What goes into the reading, as JSON — with the forms spelled AS THE CHAPTER SPELLS THEM.
     *
     * <p>{@code times} is counted here rather than asked for: the model has no reason to count
     * accurately and the fold has every reason to know which form is the chapter's habitual one and
     * which is a single aside.
     */
    @Override
    public String body() {
        Index index = Index.of(prose);
        // TWO SPELLINGS OF ONE FORM ARE ONE FORM, and folding them here is cheaper than saying so.
        // A duplicate is not a mistake the model needs told about, and telling it costs a call.
        Map<String, String[]> kept = new LinkedHashMap<>();
        for (String[] form : lines("NAME:")) {
            String spelling = index.spelling(form[0]);
            kept.putIfAbsent(Index.key(spelling), new String[] {spelling, form[1]});
        }
        StringBuilder out = new StringBuilder("{\"forms\":[");
        boolean first = true;
        for (String[] form : kept.values()) {
            out.append(first ? "" : ",").append("{\"form\":").append(Ask.json(form[0]))
                    .append(",\"by\":")
                    .append(Ask.json(form[1].isBlank() ? "narrator" : form[1]))
                    .append(",\"times\":").append(index.count(form[0])).append('}');
            first = false;
        }
        out.append("],\"not\":[");
        first = true;
        for (String[] not : lines("NOT:")) {
            out.append(first ? "" : ",").append("{\"form\":")
                    .append(Ask.json(index.spelling(not[0]))).append(",\"who\":")
                    .append(Ask.json(not[1].isBlank() ? "somebody else" : not[1])).append('}');
            first = false;
        }
        return out.append("]}").toString();
    }

    /**
     * Read once per chapter, and KEYED BY THE SLUG rather than merely cached. One {@code Work} may
     * be handed chapter after chapter, and a cache without the key would verify chapter twelve's
     * answer against chapter eleven's text — wrong in exactly the direction nobody looks at.
     */
    private String proseOf(Chain.Chapter chapter) throws IOException {
        if (!chapter.slug().equals(proseOf)) {
            prose = text.of(chapter);
            proseOf = chapter.slug();
        }
        return prose;
    }

    /** One line per form, and anything that is not one of ours is the model clearing its throat. */
    private List<String[]> lines(String label) {
        List<String[]> out = new ArrayList<>();
        for (String raw : said.split("\\R")) {
            String line = raw.trim();
            if (!line.toUpperCase(Locale.ROOT).startsWith(label)) {
                continue;
            }
            String rest = line.substring(label.length()).trim();
            String who = "";
            int cut = separator(rest);
            if (cut >= 0) {
                who = rest.substring(cut + 1).trim();
                rest = rest.substring(0, cut).trim();
                if (who.toUpperCase(Locale.ROOT).startsWith("BY:")) {
                    who = who.substring("BY:".length()).trim();
                }
            }
            String form = unquote(rest);
            if (!form.isBlank()) {
                out.add(new String[] {form, unquote(who)});
            }
        }
        return out;
    }

    private static int separator(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '|' || c == '—' || c == '–') {
                return i;
            }
        }
        return -1;
    }

    private boolean saidNone() {
        for (String raw : said.split("\\R")) {
            if (raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "").equals("NONE")) {
                return true;
            }
        }
        return false;
    }

    private static boolean covers(List<String[]> forms, String name) {
        for (String[] form : forms) {
            if (Index.key(form[0]).contains(Index.key(name))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNarrator(String who) {
        String w = who.toLowerCase(Locale.ROOT);
        return w.contains("narrat") || w.contains("author") || w.equals("the text");
    }

    /** Determiners come off first, because they are what "the young man" has over "the man". */
    private static boolean denotesAnybody(String form) {
        List<String> words = words(form);
        if (words.isEmpty()) {
            return true;
        }
        if (words.size() == 1 && PRONOUNS.contains(words.get(0))) {
            return true;
        }
        int at = 0;
        while (at < words.size() && DETERMINERS.contains(words.get(at))) {
            at++;
        }
        return at == words.size() || (at == words.size() - 1 && BARE.contains(words.get(at)));
    }

    private static List<String> words(String form) {
        List<String> out = new ArrayList<>();
        for (String word : form.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{M}']+")) {
            if (!word.isBlank()) {
                out.add(word);
            }
        }
        return out;
    }

    /**
     * STRIP A MATCHED PAIR OF SURROUNDING QUOTES BEFORE COMPARING ANYTHING. The quotes are the
     * model's delimiters, not part of the name. A verifier in this repository once objected that a
     * span did not occur in the chapter and quoted the span back WITH those delimiters still on it;
     * the model read its own words, could see nothing wrong with them, and said the same thing twice
     * more until the round budget ran out. Three paid calls, no change.
     */
    private static String unquote(String value) {
        String out = value.trim();
        while (out.length() > 1 && closes(out.charAt(0), out.charAt(out.length() - 1))) {
            out = out.substring(1, out.length() - 1).trim();
        }
        return out;
    }

    private static boolean closes(char open, char close) {
        return (open == '"' && close == '"')
                || (open == '\'' && close == '\'')
                || (open == '“' && close == '”')
                || (open == '‘' && close == '’')
                || (open == '«' && close == '»');
    }

    private static String joined(List<String> objections) {
        int shown = Math.min(objections.size(), MOST_OBJECTIONS);
        String out = String.join("; ", objections.subList(0, shown));
        return shown == objections.size() ? out
                : out + "; and " + (objections.size() - shown) + " more of the same kind";
    }

    private static String quoted(String value) {
        return "\"" + (value.length() > 60 ? value.substring(0, 60) + "…" : value) + "\"";
    }

    /**
     * THE CHAPTER, FOLDED FOR COMPARISON, WITH A WAY BACK TO ITS OWN SPELLING.
     *
     * <p>Every check here is "does the chapter contain this", so every accident of typing that is
     * not the model's fault has to come out of the question first — otherwise the verifier rejects
     * for something the model cannot see and cannot act on, which is three calls and no change. The
     * prose is hard wrapped, so a two-word form crosses a line break. It is set with curly quotes
     * and em dashes that a model retypes as ASCII. It is full of Bezúkhov, Natásha and Anna
     * Mikháylovna, and a small local model drops the accents. And a form the chapter only ever uses
     * at the start of a sentence is capitalised there and nowhere else. None of those is invention.
     *
     * <p>So the match runs on a folded copy, and {@link #spelling} hands back the run of ORIGINAL
     * characters that matched. The reading keeps the chapter's spelling rather than the model's,
     * which is the same reason the match was made loosely: the text is the authority on itself.
     */
    private static final class Index {

        private static final String SINGLES = "‘’‚‛′´`";
        private static final String DOUBLES = "“”„«»″";
        private static final String DASHES = "‐‑‒–—―";

        private final String raw;
        private final String flat;
        private final String loose;
        private final int[] from;
        private final int[] to;

        private Index(String raw, String loose, int[] from, int[] to) {
            this.raw = raw;
            this.flat = raw.replaceAll("\\s+", " ");
            this.loose = loose;
            this.from = from;
            this.to = to;
        }

        static Index of(String raw) {
            StringBuilder loose = new StringBuilder(raw.length());
            int[] from = new int[raw.length() + 8];
            int[] to = new int[raw.length() + 8];
            for (int i = 0; i < raw.length(); i++) {
                String folded = fold(raw.charAt(i));
                if (folded.equals(" ")
                        && (loose.length() == 0 || loose.charAt(loose.length() - 1) == ' ')) {
                    continue;
                }
                for (int k = 0; k < folded.length(); k++) {
                    if (loose.length() >= from.length) {
                        from = Arrays.copyOf(from, from.length * 2);
                        to = Arrays.copyOf(to, to.length * 2);
                    }
                    from[loose.length()] = i;
                    to[loose.length()] = i + 1;
                    loose.append(folded.charAt(k));
                }
            }
            return new Index(raw, loose.toString(), from, to);
        }

        boolean has(String form) {
            return find(key(form), 0) >= 0;
        }

        int count(String form) {
            String key = key(form);
            int seen = 0;
            for (int at = find(key, 0); at >= 0; at = find(key, at + key.length())) {
                seen++;
            }
            return seen;
        }

        /**
         * A WHOLE WORD, BECAUSE "the count" IS INSIDE "the countess" AND THEY ARE DIFFERENT PEOPLE.
         *
         * <p>A plain {@code indexOf} would report the form attested and then count every countess in
         * the chapter as an occurrence of it — which is not a nuisance in this book, it is the exact
         * error the section exists to prevent, arriving through the tool that was meant to prevent
         * it. The edges are only guarded where the form itself ends in a letter, so a possessive
         * ("Pierre's") still counts as an occurrence of the name and a form ending in punctuation is
         * not made unfindable.
         */
        private int find(String key, int start) {
            if (key.isEmpty()) {
                return -1;
            }
            for (int at = loose.indexOf(key, start); at >= 0; at = loose.indexOf(key, at + 1)) {
                if (open(key.charAt(0), at - 1) && open(key.charAt(key.length() - 1), at + key.length())) {
                    return at;
                }
            }
            return -1;
        }

        private boolean open(char edge, int outside) {
            if (!Character.isLetterOrDigit(edge) || outside < 0 || outside >= loose.length()) {
                return true;
            }
            return !Character.isLetterOrDigit(loose.charAt(outside));
        }

        /** The chapter's own spelling of a form; the model's, unchanged, when it is not in there. */
        String spelling(String form) {
            String want = form.replaceAll("\\s+", " ").trim();
            if (flat.contains(want)) {
                return want;
            }
            String key = key(form);
            int at = find(key, 0);
            return at < 0 ? want
                    : raw.substring(from[at], to[at + key.length() - 1]).replaceAll("\\s+", " ");
        }

        static String key(String value) {
            StringBuilder out = new StringBuilder(value.length());
            for (int i = 0; i < value.length(); i++) {
                String folded = fold(value.charAt(i));
                if (folded.equals(" ") && (out.length() == 0 || out.charAt(out.length() - 1) == ' ')) {
                    continue;
                }
                out.append(folded);
            }
            return out.toString().trim();
        }

        private static String fold(char c) {
            // The no-break space is not whitespace to `Character`, and it is what typeset
            // prose puts between a title and a name. Left alone it splits one form into two.
            if (Character.isWhitespace(c) || c == '\u00a0') {
                return " ";
            }
            if (SINGLES.indexOf(c) >= 0) {
                return "'";
            }
            if (DOUBLES.indexOf(c) >= 0) {
                return "\"";
            }
            if (DASHES.indexOf(c) >= 0) {
                return "-";
            }
            StringBuilder out = new StringBuilder(1);
            for (char part : Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD)
                    .toCharArray()) {
                if (Character.getType(part) != Character.NON_SPACING_MARK) {
                    out.append(Character.toLowerCase(part));
                }
            }
            return out.toString();
        }
    }
}
