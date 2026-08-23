package tech.mikhailov.wp.sections;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import tech.mikhailov.ratchet.llm.Chat;

import tech.mikhailov.ratchet.flow.Agent;
import tech.mikhailov.ratchet.flow.Flow;
import tech.mikhailov.wp.Chain;
import tech.mikhailov.wp.Text;

/**
 * WHO ACTS ON WHOM — A SYMMETRIC EDGE IS A DELETED CHARACTER FACT.
 *
 * <p>In Book One chapter XXIV, Anna Mikháylovna, Prince Vasíli and Princess Catiche all speak at
 * Pierre, and he answers none of them: he is led by the hand, gripped by the elbow, wept over and
 * shouted at, and does not say one word in the whole chapter. "Speaks with" records all three and
 * loses the only thing that chapter says about him. So a relation carries a direction, and a
 * relation that arrives without one is REJECTED rather than defaulted — a default is how the
 * symmetric edge comes back in through the verifier.
 *
 * <p>THE OTHER PARTY MUST BE A NAME THIS CHAPTER ITSELF USES. Two rules, both mechanical, both
 * guarding the same failure. The name must occur in this chapter verbatim, because a model that
 * knows the novel will helpfully upgrade "Prince Vasíli" to "Prince Vasíli Kurágin" — a surname
 * chapter XXIV never says — and once it is upgrading it will also import somebody who is not here
 * at all. And the name must be a NAME: "the princess" is Catiche in chapter XXIV and her middle
 * sister in chapter XXI, "Bolkónski" is the old prince or his son, and "Count Bezúkhov" is Pierre's
 * father until chapter XXIV and Pierre after it. A description accepted as an identity is how two
 * people become one, which is worse than a page with a hole in it.
 *
 * <p>The cost of that second rule is real and visible: the vicomte is Pierre's whole chapter V and
 * chapter V never gives him a surname, so this section drops him. That is the trade taken
 * deliberately — the fold downstream can be told about a gap, but it cannot un-merge two people.
 *
 * <p>The planner and the verifier are plain code. Everything asked here is checkable without asking
 * anybody: three direction words, a name that occurs or does not, a span that occurs or does not.
 * What this verifier does NOT check is whether the direction is TRUE — that is judgement, it would
 * cost a second model call per round, and the check that catches the damage is the naming one.
 */
public final class Relationships implements Work {

    private static final List<String> DIRECTIONS = List.of("hero-to-other", "other-to-hero", "mutual");

    /**
     * A capitalised word that is only a title, a kin term or a description does not make a name.
     *
     * <p>Without this list "the eldest princess", "the count" and "the German" all pass a naive
     * has-a-capital test the moment they start a line, and each of them denotes a different person
     * in a different chapter of this corpus.
     */
    private static final Set<String> NOT_A_NAME = Set.of(
            "the", "a", "an", "this", "that", "these", "those", "his", "her", "their", "my", "our",
            "your", "its", "and", "of", "with", "to", "at", "in", "for", "by", "who", "none",
            "unknown", "nobody", "someone", "everyone", "all", "one", "other", "another",
            "prince", "princess", "count", "countess", "duke", "duchess", "baron", "baroness",
            "emperor", "empress", "tsar", "king", "queen", "general", "colonel", "captain", "major",
            "doctor", "doctors", "dr", "monsieur", "madame", "mademoiselle", "mister", "mr", "mrs",
            "miss", "sir", "madam", "lady", "lord", "vicomte", "viscount", "marshal", "aide",
            "father", "mother", "son", "daughter", "brother", "sister", "sisters", "uncle", "aunt",
            "cousin", "wife", "husband", "nephew", "niece", "family", "heir", "heirs",
            "man", "woman", "men", "women", "boy", "girl", "youth", "guest", "guests", "host",
            "hostess", "servant", "servants", "footman", "maid", "priest", "visitor", "people",
            "crowd", "company", "room", "others", "narrator",
            "old", "young", "eldest", "elder", "younger", "second", "third", "little", "elderly",
            "dying", "sick", "dear", "poor", "good", "same", "whole",
            "german", "french", "russian", "austrian", "english");

    private final Text text;
    private final Chat model;
    private String said = "";

    public Relationships(Text text, Chat model) {
        this.text = text;
        this.model = model;
    }

    /**
     * NO MODEL CALL. The plan is the same instruction for every chapter and every character, so
     * buying it 13,140 times buys nothing; what varies is the chapter, and the doer already has it.
     *
     * <p>The direction rule is stated here rather than in the doer's format block because it is the
     * judgement being asked for, not a detail of the reply. "They speak and the hero does not
     * answer" has to be spelled out or every relation comes back mutual.
     */
    @Override
    public Agent planner(Chain.Hero hero, Chain.Chapter chapter) {
        String who = hero.name();
        return brief -> "List everyone " + who + " interacts with IN THIS CHAPTER, and which way "
                + "each interaction runs. hero-to-other: " + who + " speaks to them or acts on them. "
                + "other-to-hero: they speak to or act on " + who + " and " + who + " does not "
                + "answer. mutual: both sides speak or act. Name each person in words THIS CHAPTER "
                + "uses for them — never a fuller name from elsewhere in the novel — and prefer a "
                + "proper name over a description, because a description like \"the princess\" is a "
                + "different woman in a different chapter. Leave out anyone who is merely named near "
                + who + " without any interaction.";
    }

    @Override
    public Flow.Doer doer(Chain.Hero hero, Chain.Chapter chapter) {
        return (plan, feedback) -> {
            String body = text.of(chapter);
            String ask = plan
                    + (feedback.isBlank() ? "" : "\n\nYour last answer was rejected: " + feedback)
                    + "\n\nAnswer as one line per relation and nothing else. Four fields, separated "
                    + "by | , and no | inside a field:\nRELATION: <the other person, named exactly "
                    + "as this chapter names them> | <hero-to-other|other-to-hero|mutual> | <what "
                    + "happened, one clause> | <a phrase copied EXACTLY from the chapter that shows "
                    + "it>\n\nKeep each relation on ONE line, including the copied phrase. If "
                    + hero.name() + " interacts with nobody here, answer the single word NONE."
                    + "\n\nCHAPTER " + chapter.numeral() + " of " + chapter.book()
                    + ":\n\n" + body;
            said = Ask.once(model, ask);
            return said;
        };
    }

    /** The workspace is the answer just given; this section reads nothing else and writes nothing. */
    @Override
    public String facts(Chain.Hero hero, Chain.Chapter chapter) {
        return said;
    }

    /**
     * FOUR WAYS TO REJECT, AND EVERY ONE OF THEM IS A SUBSTRING TEST OR A LIST MEMBERSHIP.
     *
     * <p>The direction is one of three words; the other party is a name rather than a description;
     * that name occurs in this chapter; the span occurs in this chapter. NONE is accepted without
     * argument, and that is deliberate rather than a hole: a chapter where the hero is absent or
     * only talked about has no relations, and a verifier that demanded one would be paying a model
     * to invent them. Chapter XXI is Pierre's inheritance argued over by other people while he is
     * elsewhere — he interacts with nobody in it, and NONE is the correct reading.
     *
     * <p>EVERY REJECTION HAS TO TELL THE MODEL WHAT TO DO NEXT, because the feedback is the next
     * round's prompt and a round it cannot act on costs three calls and changes nothing. That is not
     * hypothetical here: an earlier verifier in this repository rejected a span and quoted it back
     * WITH the quote marks the model had used as delimiters, so the model could only repeat itself
     * until the budget ran out. Hence {@link #unquoted}, and hence saying out loud, in the message,
     * that the quotation marks belong to the message.
     */
    @Override
    public Agent verifier(Chain.Hero hero, Chain.Chapter chapter) {
        return judged -> {
            String body;
            try {
                body = norm(text.of(chapter));
            } catch (IOException unreadable) {
                return "again: the chapter could not be read: " + unreadable.getMessage();
            }
            List<String[]> relations = relations(said);
            if (relations.isEmpty()) {
                if (none(said)) {
                    return "done";
                }
                return "again: no relation line could be read. Give ONE line per relation, four "
                        + "fields separated by | : the other person | one of " + DIRECTIONS
                        + " | what happened | a phrase copied from the chapter. If " + hero.name()
                        + " interacts with nobody here, answer the single word NONE.";
            }
            for (String[] relation : relations) {
                String with = relation[0];
                String direction = relation[1].toLowerCase(Locale.ROOT);
                String what = relation[2];
                String span = unquoted(relation[3]);

                if (!DIRECTIONS.contains(direction)) {
                    return "again: the relation with " + quoted(with) + " has no usable direction. "
                            + "The second field must be exactly one of " + DIRECTIONS + ", not "
                            + quoted(direction) + ". Use other-to-hero when they speak to or act on "
                            + hero.name() + " and he does not answer.";
                }
                if (with.isBlank()) {
                    return "again: a relation has no other party in its first field.";
                }
                if (sameAs(with, hero.name())) {
                    return "again: " + quoted(with) + " names " + hero.name() + " himself. A "
                            + "relation runs between " + hero.name() + " and somebody else, so name "
                            + "the other person by their own name, not by their relation to him.";
                }
                if (!named(with)) {
                    return "again: " + quoted(with) + " is a description, not a name, and a "
                            + "description means different people in different chapters — \"the "
                            + "princess\" is one sister here and another one elsewhere. Give the "
                            + "proper name this chapter uses for them; if this chapter never names "
                            + "them, drop the relation rather than guessing.";
                }
                if (!body.contains(norm(with))) {
                    return "again: " + quoted(with) + " does not occur anywhere in this chapter, so "
                            + "it has been brought in from elsewhere in the novel. Use only the "
                            + "words this chapter prints, and no fuller a name than it prints.";
                }
                if (what.trim().split("\\s+").length < 2) {
                    return "again: the relation with " + quoted(with) + " does not say what "
                            + "happened. The third field is one clause describing the interaction.";
                }
                if (span.length() < 8 || sameAs(span, with)) {
                    return "again: the relation with " + quoted(with) + " cites no evidence. The "
                            + "fourth field is a phrase copied from the chapter that shows the "
                            + "interaction happening, not the person's name again.";
                }
                if (!body.contains(norm(span))) {
                    return "again: this phrase does not occur in the chapter, so the relation with "
                            + quoted(with) + " is unsupported. Copy a phrase word for word out of "
                            + "the chapter text instead: " + quoted(span) + " — the quotation marks "
                            + "around it in this message are mine, not part of what you copied.";
                }
            }
            return "done";
        };
    }

    /**
     * ONE LINE, FOUR FIELDS, AND FORGIVING ABOUT EVERYTHING THAT IS NOT THE ANSWER.
     *
     * <p>A small model asked for a plain format still opens with "Here are the relations:" and still
     * bullets or numbers its lines, and it will label the fields it was told to leave bare. None of
     * that is a wrong answer, so none of it should cost a round: anything without four fields is
     * skipped, and a leading bullet, number or field label is stripped. Splitting with a limit of
     * four keeps a stray | inside the copied span where it belongs, in the span.
     */
    private static List<String[]> relations(String answer) {
        List<String[]> out = new ArrayList<>();
        for (String raw : answer.split("\\R")) {
            String line = raw.trim()
                    .replaceFirst("^[-*•]\\s*", "")
                    .replaceFirst("^\\d+[.)]\\s*", "")
                    .replaceFirst("(?i)^relations?\\s*:\\s*", "")
                    .trim();
            String[] fields = line.split("\\|", 4);
            if (fields.length == 4) {
                out.add(new String[] {
                        label(fields[0]), label(fields[1]), label(fields[2]), label(fields[3])});
            }
        }
        return out;
    }

    private static String label(String field) {
        return field.trim()
                .replaceFirst("(?i)^(with|who|whom|person|direction|dir|what|why|span|evidence"
                        + "|quote|proof)\\s*:\\s*", "")
                .trim();
    }

    /** An answer of NONE, however the model dressed it up. */
    private static boolean none(String answer) {
        for (String raw : answer.split("\\R")) {
            String letters = raw.trim()
                    .replaceFirst("(?i)^relations?\\s*:\\s*", "")
                    .replaceAll("[^\\p{L}]", "");
            if (letters.equalsIgnoreCase("none")) {
                return true;
            }
        }
        return false;
    }

    /**
     * A name is a capitalised word that is not merely a title, a kin term or a description.
     *
     * <p>Tokenised on non-letters so a possessive or a comma cannot hide the word, and so the
     * accented forms this translation uses — Mikháylovna, Bezúkhov, Vasíli — are single tokens.
     */
    private static boolean named(String with) {
        for (String token : with.split("[^\\p{L}]+")) {
            if (!token.isEmpty() && Character.isUpperCase(token.charAt(0))
                    && !NOT_A_NAME.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameAs(String one, String other) {
        return norm(one).equalsIgnoreCase(norm(other));
    }

    /**
     * THE CORPUS IS GUTENBERG'S TYPOGRAPHY AND THE MODEL'S KEYBOARD IS NOT.
     *
     * <p>Three differences that are not disagreements about the text, each of which cost a rejection
     * the model could not act on when this was tried without them. Curly quotes, apostrophes and em
     * dashes: a model that types {@code don't} where the page says {@code don’t} has copied the
     * words perfectly and would be rejected forever over a character it cannot see. Line wrapping:
     * "Anna Mikháylovna" is split across two lines all over this corpus, so whitespace collapses
     * first. And the accents — the Maude translation prints Vasíli, Mikháylovna, Bezúkhov, and a
     * small model drops those marks constantly. Rejecting {@code Mikhaylovna} would have told it the
     * name "does not occur in this chapter", which is both false and unfixable.
     *
     * <p>THE COST IS PAID IN THE RECORD, NOT IN THE CHECK. What {@link #body} writes is what the
     * model typed, so an unaccented spelling reaches the fold as written and the fold has to merge
     * it. That is a merge a model can do safely, because both spellings are the same person; the
     * merge this section refuses to allow is the one between two different people.
     */
    private static String norm(String s) {
        String flat = s.replace('‘', '\'').replace('’', '\'')
                .replace('“', '"').replace('”', '"')
                .replace('—', '-').replace('–', '-')
                .replace("…", "...")
                .replaceAll("\\s+", " ").trim();
        StringBuilder out = new StringBuilder(flat.length());
        for (int i = 0; i < flat.length(); i++) {
            // Per character, so an accent is dropped without the string changing length or shape:
            // NFD over the whole string would splice combining marks in as separate characters and
            // leave the rest of this class comparing two different alphabets.
            out.append(Normalizer.normalize(String.valueOf(flat.charAt(i)), Normalizer.Form.NFD)
                    .charAt(0));
        }
        return out.toString();
    }

    /**
     * STRIP THE DELIMITERS BEFORE LOOKING FOR THE SPAN. Asked to copy a phrase, a model wraps it in
     * quotes; the chapter's own dialogue is wrapped in quotes too. Either way the marks are not part
     * of what has to be found, and a verifier that searches for them rejects a correct answer and
     * then quotes the marks back, which is a round nobody can act on.
     */
    private static String unquoted(String s) {
        String out = s.trim();
        while (out.length() > 1 && closer(out.charAt(0)) == out.charAt(out.length() - 1)) {
            out = out.substring(1, out.length() - 1).trim();
        }
        // AN UNMATCHED MARK IS STILL A DELIMITER. A model that opens a quotation and copies a phrase
        // running past the closing one leaves a lone “ on the front, and the matched-pair loop above
        // does not touch it. Dropping it cannot let a fabricated span through — every word still has
        // to occur in the chapter — and not dropping it rejects an answer that is otherwise right.
        while (out.length() > 1 && closer(out.charAt(0)) != 0) {
            out = out.substring(1).trim();
        }
        while (out.length() > 1 && mark(out.charAt(out.length() - 1))) {
            out = out.substring(0, out.length() - 1).trim();
        }
        return out;
    }

    private static char closer(char opener) {
        return switch (opener) {
            case '"' -> '"';
            case '\'' -> '\'';
            case '“' -> '”';
            case '‘' -> '’';
            case '«' -> '»';
            default -> 0;
        };
    }

    private static boolean mark(char c) {
        return c == '"' || c == '\'' || c == '”' || c == '’' || c == '»' || closer(c) != 0;
    }

    private static String quoted(String s) {
        return "\"" + (s.length() > 60 ? s.substring(0, 60) + "…" : s) + "\"";
    }

    /**
     * What goes into the reading, as JSON. An empty list is a real answer here, not a failure: it
     * says this chapter was read for this character and there was no interaction in it.
     */
    @Override
    public String body() {
        StringBuilder out = new StringBuilder("{\"relations\":[");
        List<String[]> relations = relations(said);
        for (int i = 0; i < relations.size(); i++) {
            String[] relation = relations.get(i);
            out.append(i == 0 ? "" : ",")
                    .append("{\"with\":").append(Ask.json(relation[0]))
                    .append(",\"direction\":").append(Ask.json(relation[1].toLowerCase(Locale.ROOT)))
                    .append(",\"what\":").append(Ask.json(relation[2]))
                    .append(",\"span\":").append(Ask.json(unquoted(relation[3])))
                    .append("}");
        }
        return out.append("]}").toString();
    }
}
