package tech.mikhailov.wp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.mikhailov.ratchet.record.Json;

/**
 * ONE CHARACTER'S WIKI PAGE, CARRIED THROUGH THE BOOK AND CHANGED BY EVERY CHAPTER.
 *
 * <p>This replaces a design in which every chapter wrote eight separate readings and two later
 * passes folded them. The fold is now continuous: a chapter is read WITH the page so far, and what
 * comes back is a change to it. There is no compact pass, because there is nothing left to compact.
 *
 * <p>THE PAGE IS A PAGE AND NOT AN ARCHIVE, and that is the constraint the whole shape turns on. It
 * travels in the model's context on every one of 365 calls, so its size is the run's ceiling.
 * Measured on the first live extraction — Pierre, Book One chapter V — one chapter produced 1,792
 * tokens across the eight sections. Kept whole, that is 654,080 tokens over the novel against a
 * 251,552 ceiling: the run walks into a wall around chapter 140, and quotes alone (671 a chapter)
 * would nearly fill it unaided.
 *
 * <p>So each field has a POLICY, and three of them exist:
 *
 * <ul>
 * <li><strong>revise</strong> — {@link #arc} and {@link #personality} are prose the next chapter
 *     rewrites. Bounded by what a paragraph is, not by how many chapters have gone past.
 * <li><strong>merge</strong> — {@link #relationships} keys on the other person and {@link #names}
 *     on the form, so a hundred chapters of Anna Pávlovna are one entry that gets better. These
 *     converge rather than grow.
 * <li><strong>append</strong> — {@link #appearances}, {@link #facts}, {@link #quotes} and
 *     {@link #trivia} accumulate, because losing one loses evidence. The first two are one short
 *     line per chapter and stay small; the last two are curated, which is the one place in this
 *     design where something can be dropped and therefore the one place that needs a gate.
 * </ul>
 *
 * <p>THE PER-CHAPTER EVIDENCE IS NOT LOST BY ANY OF THIS. It is written to {@code readings/} as it
 * always was, whole, and that is the audit trail. What the page holds is the wiki page.
 *
 * <p>STORED FLAT, GROUPED ON RENDER, and this is a deliberate departure from the sketch this was
 * built from — which nested {@code appearances} as book, then chapter, then the entry. The
 * information is identical either way: every entry carries its own {@code book} and {@code chapter},
 * so a reader groups them in one pass. What differs is that arbitrary object keys cannot be
 * enumerated by the scanner in {@code ratchet-core}'s {@code Json}, which has no tree model on
 * purpose, so nesting would mean a second JSON reader in this repository to hold the same facts.
 */
public record Page(String character, String arc, String personality,
                   List<Map<String, String>> relationships, List<Map<String, String>> names,
                   List<Map<String, String>> appearances, List<Map<String, String>> facts,
                   List<Map<String, String>> quotes, List<Map<String, String>> trivia) {

    /**
     * What a growing list may hold PER BOOK before the curator drops something.
     *
     * <p>Per book, so seventeen books put the real ceiling at 17 x these. A flat page-wide cap was
     * the first draft and it kept the last forty quotes in the novel and nothing from Book One —
     * for a character whose page is about a transformation, exactly the half that shows what he
     * transformed from.
     */
    public static final int KEEP_QUOTES = 4;
    public static final int KEEP_TRIVIA = 4;

    /** The fields a chapter may change, in the order the page renders them. */
    public static final List<String> FIELDS = List.of("arc", "personality", "relationships",
            "names", "appearances", "facts", "quotes", "trivia");

    public Page {
        character = character == null ? "" : character;
        arc = arc == null ? "" : arc;
        personality = personality == null ? "" : personality;
        relationships = copy(relationships);
        names = copy(names);
        appearances = copy(appearances);
        facts = copy(facts);
        quotes = copy(quotes);
        trivia = copy(trivia);
    }

    private static List<Map<String, String>> copy(List<Map<String, String>> rows) {
        return rows == null ? List.of() : List.copyOf(rows);
    }

    /** A page nobody has read a chapter into yet. */
    public static Page blank(String character) {
        return new Page(character, "", "", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of());
    }

    /**
     * WHAT THE MODEL IS SHOWN AND WHAT IS KEPT ON DISK, which are the same document.
     *
     * <p>One representation, not two. A prompt-facing rendering that differed from the stored one
     * would be a second schema nobody validates, and the model would be revising something other
     * than what it is revising.
     */
    public String wire() {
        return Json.object(
                Json.field("character", Json.string(character)),
                Json.field("arc", Json.string(arc)),
                Json.field("personality", Json.string(personality)),
                Json.field("relationships", rows(relationships)),
                Json.field("names", rows(names)),
                Json.field("appearances", rows(appearances)),
                Json.field("facts", rows(facts)),
                Json.field("quotes", rows(quotes)),
                Json.field("trivia", rows(trivia)));
    }

    private static String rows(List<Map<String, String>> list) {
        return Json.array(list, row -> Json.map(entries(row)));
    }

    private static Map<String, String> entries(Map<String, String> row) {
        Map<String, String> quoted = new LinkedHashMap<>();
        row.forEach((k, v) -> quoted.put(k, Json.string(v)));
        return quoted;
    }

    public static Page read(Path file, String character) throws IOException {
        return Files.isRegularFile(file)
                ? parse(Files.readString(file, StandardCharsets.UTF_8))
                : blank(character);
    }

    public void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, wire(), StandardCharsets.UTF_8);
    }

    public static Page parse(String json) {
        return new Page(Json.read(json, "character"), Json.read(json, "arc"),
                Json.read(json, "personality"),
                list(Json.part(json, "relationships")), list(Json.part(json, "names")),
                list(Json.part(json, "appearances")), list(Json.part(json, "facts")),
                list(Json.part(json, "quotes")), list(Json.part(json, "trivia")));
    }

    /** The objects of an array, each as a flat map. */
    static List<Map<String, String>> list(String array) {
        List<Map<String, String>> out = new ArrayList<>();
        for (String one : objects(array)) {
            Map<String, String> row = Json.row(one);
            if (!row.isEmpty()) {
                out.add(row);
            }
        }
        return out;
    }

    /** Top-level objects of a JSON array, brackets counted with strings respected. */
    static List<String> objects(String array) {
        List<String> out = new ArrayList<>();
        if (array == null) {
            return out;
        }
        int depth = 0;
        int from = -1;
        boolean inString = false;
        for (int i = 0; i < array.length(); i++) {
            char c = array.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                if (depth++ == 0) {
                    from = i;
                }
            } else if (c == '}' && --depth == 0 && from >= 0) {
                out.add(array.substring(from, i + 1));
            }
        }
        return out;
    }

    /** How much of the model's context this page will occupy, near enough to decide with. */
    public int roughTokens() {
        return wire().length() / 4;
    }
}
