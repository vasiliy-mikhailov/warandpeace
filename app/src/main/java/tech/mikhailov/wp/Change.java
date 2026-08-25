package tech.mikhailov.wp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tech.mikhailov.ratchet.record.Json;

/**
 * WHAT ONE CHAPTER DOES TO A CHARACTER'S PAGE — a change, not a new page.
 *
 * <p>THE MODEL EMITS THIS AND NOT THE WHOLE DOCUMENT, and the difference is the run. Measured
 * against a 262,144-token window with the page carried on every call:
 *
 * <pre>
 * re-emit the page each chapter   ceiling 126,026   dies at chapter  70   ~119,000,000 output tokens
 * emit a change                   ceiling 251,552   reaches      365      ~182,500 output tokens
 * </pre>
 *
 * <p>Six hundred and fifty times cheaper, and the expensive one cannot finish the book. Asking for
 * the whole page back also asks the model to copy three hundred chapters of its own earlier work
 * through a decoder on every call, which is three hundred chances to drop a line nobody would ever
 * notice was dropped.
 *
 * <p>THREE POLICIES, ONE PER KIND OF FIELD, and the kind is a fact about the field rather than a
 * setting. Prose is revised because a paragraph about who somebody is has one current answer.
 * Relationships and names are merged on a key because a hundred chapters of Anna Pávlovna are one
 * entry that gets better. Evidence is appended because dropping a line loses a fact.
 *
 * <p>AND CURATION IS THE ONE PLACE WORK CAN SLIDE BACKWARDS, which is what this whole library is
 * named against. {@code quotes} and {@code trivia} are capped, so a chapter that adds a fortieth
 * quote drops one — and a drop is the only operation here that makes a page hold LESS than it did.
 * {@link #applyTo} therefore reports what it evicted rather than doing it quietly, so the caller
 * can record it and a reader can see that chapter XL cost chapter V its best line.
 */
public record Change(String arc, String personality, List<Map<String, String>> relationships,
                     List<Map<String, String>> names, List<Map<String, String>> appearances,
                     List<Map<String, String>> facts, List<Map<String, String>> quotes,
                     List<Map<String, String>> trivia) {

    /** What merging keys on. A field not named here is appended or revised, never merged. */
    private static final Map<String, String> MERGE_ON = Map.of("relationships", "with",
            "names", "form");

    public Change {
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

    public static Change parse(String json) {
        return new Change(Json.read(json, "arc"), Json.read(json, "personality"),
                Page.list(Json.part(json, "relationships")), Page.list(Json.part(json, "names")),
                Page.list(Json.part(json, "appearances")), Page.list(Json.part(json, "facts")),
                Page.list(Json.part(json, "quotes")), Page.list(Json.part(json, "trivia")));
    }

    /** A change that asks for nothing, which is what a chapter the character is absent from wants. */
    public boolean isEmpty() {
        return arc.isBlank() && personality.isBlank() && relationships.isEmpty() && names.isEmpty()
                && appearances.isEmpty() && facts.isEmpty() && quotes.isEmpty() && trivia.isEmpty();
    }

    /** A page and what changing it cost, because the second half is not always nothing. */
    public record Applied(Page page, List<String> evicted) {
    }

    /**
     * The page after this chapter.
     *
     * <p>BLANK PROSE DOES NOT ERASE PROSE. A chapter with nothing to say about the arc returns an
     * empty string for it, and reading that as "the arc is now empty" would let one quiet chapter
     * delete everything the previous three hundred established. Only a non-blank revision replaces.
     */
    public Applied applyTo(Page page, String book, String chapter) {
        List<String> evicted = new ArrayList<>();
        return new Applied(new Page(page.character(),
                arc.isBlank() ? page.arc() : arc,
                personality.isBlank() ? page.personality() : personality,
                merged(page.relationships(), stamped(relationships, book, chapter), "with"),
                merged(page.names(), stamped(names, book, chapter), "form"),
                appended(page.appearances(), stamped(appearances, book, chapter)),
                appended(page.facts(), stamped(facts, book, chapter)),
                curated(appended(page.quotes(), stamped(quotes, book, chapter)),
                        Page.KEEP_QUOTES, "quotes", evicted),
                curated(appended(page.trivia(), stamped(trivia, book, chapter)),
                        Page.KEEP_TRIVIA, "trivia", evicted)),
                evicted);
    }

    /**
     * EVERY ENTRY CARRIES WHERE IT CAME FROM, stamped here rather than asked of the model.
     *
     * <p>The model is reading one chapter and knows which one; asking it to write the book and
     * chapter into every entry spends tokens on something the caller already holds, and invites the
     * one error that makes an entry untraceable. It is also what makes a flat list groupable back
     * into books on the way out.
     */
    private static List<Map<String, String>> stamped(List<Map<String, String>> rows, String book,
                                                     String chapter) {
        List<Map<String, String>> out = new ArrayList<>();
        for (Map<String, String> row : rows) {
            Map<String, String> copy = new LinkedHashMap<>(row);
            copy.put("book", book);
            copy.put("chapter", chapter);
            out.add(copy);
        }
        return out;
    }

    private static List<Map<String, String>> appended(List<Map<String, String>> was,
                                                      List<Map<String, String>> now) {
        List<Map<String, String>> out = new ArrayList<>(was);
        out.addAll(now);
        return out;
    }

    /**
     * MERGED ON A KEY, LAST WORD WINNING, and the earlier entry's origin kept.
     *
     * <p>A hundred chapters of Anna Pávlovna should be one entry that gets better, not a hundred
     * rows saying almost the same thing. What the newest chapter says about her supersedes what an
     * older one did — but {@code first} keeps where she was first seen, because a relationship that
     * has run since Book One and one that began in Book Twelve are different relationships and the
     * merged row would otherwise claim both started where they last changed.
     */
    private static List<Map<String, String>> merged(List<Map<String, String>> was,
                                                    List<Map<String, String>> now, String key) {
        Map<String, Map<String, String>> by = new LinkedHashMap<>();
        for (Map<String, String> row : was) {
            by.put(keyOf(row, key), new LinkedHashMap<>(row));
        }
        for (Map<String, String> row : now) {
            String k = keyOf(row, key);
            Map<String, String> merged = new LinkedHashMap<>(row);
            Map<String, String> before = by.get(k);
            if (before != null) {
                merged.put("first", before.getOrDefault("first",
                        before.getOrDefault("book", "") + " " + before.getOrDefault("chapter", "")));
            }
            by.put(k, merged);
        }
        return new ArrayList<>(by.values());
    }

    /** Case-folded, because "Anna Pávlovna" and "anna pávlovna" are one person and two keys. */
    private static String keyOf(Map<String, String> row, String key) {
        return row.getOrDefault(key, "").strip().toLowerCase(Locale.ROOT);
    }

    /**
     * THE CAP, PER BOOK, AND IT REPORTS WHAT IT DROPS.
     *
     * <p>PER BOOK RATHER THAN OVER THE WHOLE PAGE, and the first draft got this wrong in a way the
     * arithmetic showed immediately. A flat cap of forty, filled oldest-first, evicted 640 lines
     * across a 365-chapter run — so the finished page held forty quotes from the END of the novel
     * and nothing from Book One. That is not a curated page, it is a window on the last few
     * chapters, and for a character whose whole point is a transformation it drops precisely the
     * half that shows what he transformed FROM.
     *
     * <p>Capping within each book bounds the page the same way — seventeen books, so at most
     * seventeen times the per-book keep — while guaranteeing the early books survive the late ones.
     * Nothing in Book One can be evicted by anything in Book Fifteen, because they are not
     * competing.
     *
     * <p>Oldest first WITHIN a book, which is the only rule here that is not a judgement: any "keep
     * the best" needs a second opinion about what best means, and a page that silently reorders its
     * evidence by an unstated criterion is worse than one that says what it kept.
     *
     * <p>The eviction is returned rather than logged here, because this class must not decide
     * whether a drop is acceptable — {@code Settlement} and the caller do.
     */
    private static List<Map<String, String>> curated(List<Map<String, String>> rows, int keep,
                                                     String field, List<String> evicted) {
        Map<String, List<Map<String, String>>> byBook = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            byBook.computeIfAbsent(row.getOrDefault("book", ""), b -> new ArrayList<>()).add(row);
        }
        List<Map<String, String>> out = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, String>>> book : byBook.entrySet()) {
            List<Map<String, String>> mine = book.getValue();
            if (mine.size() > keep) {
                for (Map<String, String> gone : mine.subList(0, mine.size() - keep)) {
                    evicted.add(field + ": " + gone.getOrDefault("book", "?") + " "
                            + gone.getOrDefault("chapter", "?") + " — "
                            + first(gone, "said", "note", "fact", "what"));
                }
                mine = mine.subList(mine.size() - keep, mine.size());
            }
            out.addAll(mine);
        }
        return out;
    }

    private static String first(Map<String, String> row, String... keys) {
        for (String k : keys) {
            String v = row.get(k);
            if (v != null && !v.isBlank()) {
                return v.length() > 80 ? v.substring(0, 80) + "…" : v;
            }
        }
        return "";
    }
}
