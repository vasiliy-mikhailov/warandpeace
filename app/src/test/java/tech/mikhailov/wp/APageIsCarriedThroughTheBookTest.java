package tech.mikhailov.wp;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE FOLD IS THE READING NOW, so the page's arithmetic is where this design is right or wrong.
 *
 * <p>Every chapter is read WITH the page so far and returns a change to it. There is no compact
 * pass. That makes the page's growth the run's ceiling: it travels in the model's context on all
 * 365 calls, and measured on the first live extraction one chapter produced 1,792 tokens across the
 * eight sections — 654,080 over the novel, against a 251,552 ceiling. So the policies below are not
 * tidiness, they are what lets the run finish.
 */
class APageIsCarriedThroughTheBookTest {

    private static Map<String, String> row(String... kv) {
        var m = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private static Change change(String arc, List<Map<String, String>> quotes,
                                 List<Map<String, String>> relationships) {
        return new Change(arc, "", relationships, List.of(), List.of(), List.of(), quotes,
                List.of());
    }

    @Test
    void aChapterWithNothingToSayAboutTheArcDoesNotEraseIt() {
        // THE BUG THIS DESIGN INVITES MOST. A blank field in a change means "no opinion", and
        // reading it as "the value is now blank" lets one quiet chapter delete what three hundred
        // established. Pierre is absent from long stretches of Book Nine.
        Page after = change("Pierre arrives in Petersburg", List.of(), List.of())
                .applyTo(Page.blank("Pierre"), "BOOK ONE", "I").page();

        Page later = change("", List.of(), List.of()).applyTo(after, "BOOK NINE", "IV").page();

        assertEquals("Pierre arrives in Petersburg", later.arc(),
                "a chapter he is not in must not blank the arc");
    }

    @Test
    void proseIsReplacedWhenTheChapterHasSomethingToSay() {
        Page after = change("early Pierre", List.of(), List.of())
                .applyTo(Page.blank("Pierre"), "BOOK ONE", "I").page();

        Page later = change("Pierre after Borodinó", List.of(), List.of())
                .applyTo(after, "BOOK TEN", "XX").page();

        assertEquals("Pierre after Borodinó", later.arc(), "revise means replace, not append");
    }

    @Test
    void ahundredChaptersOfOnePersonAreOneEntryThatGetsBetter() {
        // MERGE, NOT APPEND. Anna Pávlovna recurs from the first page to the last; a row per
        // chapter would be a hundred near-identical rows and would blow the ceiling on its own.
        Page page = Page.blank("Pierre");
        page = change("", List.of(), List.of(row("with", "Anna Pávlovna", "how", "she manages him")))
                .applyTo(page, "BOOK ONE", "I").page();
        page = change("", List.of(), List.of(row("with", "Anna Pávlovna", "how", "she avoids him")))
                .applyTo(page, "BOOK FOUR", "II").page();
        page = change("", List.of(), List.of(row("with", "Hélène", "how", "married")))
                .applyTo(page, "BOOK FOUR", "II").page();

        assertEquals(2, page.relationships().size(), "two people, not three entries");
        Map<String, String> anna = page.relationships().get(0);
        assertEquals("she avoids him", anna.get("how"), "the newest chapter's word wins");
        assertTrue(anna.get("first").contains("BOOK ONE"),
                "and where it STARTED survives the merge, because a relationship running since "
                        + "Book One is not the same as one that began in Book Four: " + anna);
    }

    @Test
    void theSameNameInDifferentCaseIsOnePerson() {
        Page page = Page.blank("Pierre");
        page = change("", List.of(), List.of(row("with", "Anna Pávlovna", "how", "a")))
                .applyTo(page, "BOOK ONE", "I").page();
        page = change("", List.of(), List.of(row("with", "anna pávlovna", "how", "b")))
                .applyTo(page, "BOOK ONE", "II").page();

        assertEquals(1, page.relationships().size(), "one person, two spellings: "
                + page.relationships());
    }

    @Test
    void evidenceAccumulatesAndCarriesWhereItCameFrom() {
        // The stamp is applied here rather than asked of the model: it already knows which chapter
        // it is reading, and an entry that loses its origin cannot be traced back to the text.
        Page page = new Change("", "", List.of(), List.of(),
                List.of(row("verdict", "present", "span", "Pierre wished to make a remark")),
                List.of(), List.of(), List.of())
                .applyTo(Page.blank("Pierre"), "BOOK ONE", "V").page();

        assertEquals(1, page.appearances().size());
        assertEquals("BOOK ONE", page.appearances().get(0).get("book"));
        assertEquals("V", page.appearances().get(0).get("chapter"));
        assertEquals("present", page.appearances().get(0).get("verdict"));
    }

    @Test
    void theCapReportsEveryLineItDropsRatherThanDroppingItQuietly() {
        // THE ONE PLACE WORK CAN SLIDE BACKWARDS, in a library named against exactly that. A
        // fifth quote in one book evicts that book's first, and a page that held something and now
        // does not is the event this project exists to make visible.
        Page page = Page.blank("Pierre");
        for (int i = 1; i <= Page.KEEP_QUOTES; i++) {
            page = change("", List.of(row("said", "quote " + i)), List.of())
                    .applyTo(page, "BOOK ONE", "ch" + i).page();
        }
        assertEquals(Page.KEEP_QUOTES, page.quotes().size());

        // A DIFFERENT BOOK EVICTS NOTHING, which is the whole reason the cap is per book: Book
        // Fifteen must not be able to delete Book One's evidence of who he was to begin with.
        Change.Applied elsewhere = change("", List.of(row("said", "in another book")), List.of())
                .applyTo(page, "BOOK TWO", "I");
        assertEquals(List.of(), elsewhere.evicted(), "books do not compete: " + elsewhere.evicted());
        assertEquals(Page.KEEP_QUOTES + 1, elsewhere.page().quotes().size());

        Change.Applied applied = change("", List.of(row("said", "one too many")), List.of())
                .applyTo(page, "BOOK ONE", "last");

        assertEquals(Page.KEEP_QUOTES, applied.page().quotes().size(), "the cap holds within a book");
        assertEquals(1, applied.evicted().size(), "and the eviction is reported");
        assertTrue(applied.evicted().get(0).contains("quote 1"),
                "oldest first, named so it can be recorded: " + applied.evicted());
        assertTrue(applied.evicted().get(0).contains("BOOK ONE"),
                "with where it came from: " + applied.evicted());
    }

    @Test
    void nothingIsEvictedWhileThereIsRoom() {
        Change.Applied applied = change("", List.of(row("said", "the first")), List.of())
                .applyTo(Page.blank("Pierre"), "BOOK ONE", "I");

        assertEquals(List.of(), applied.evicted(),
                "a page under the cap loses nothing, so the gate has nothing to judge");
    }

    @Test
    void aPageSurvivesADiskRoundTripWithAwkwardText(@TempDir Path root) throws Exception {
        // Tolstoy is full of quotation marks and the page is written by a hand-rolled JSON writer.
        Page page = new Change("He said “no” — and meant it.\nTwice.", "", List.of(), List.of(),
                List.of(), List.of(), List.of(row("said", "\"From what I have heard,\"")), List.of())
                .applyTo(Page.blank("Pierre Bezúkhov"), "BOOK ONE", "V").page();

        Path file = root.resolve("pierre.json");
        page.write(file);
        Page back = Page.read(file, "Pierre Bezúkhov");

        assertEquals(page.arc(), back.arc());
        assertEquals("Pierre Bezúkhov", back.character());
        assertEquals(1, back.quotes().size());
        assertEquals("\"From what I have heard,\"", back.quotes().get(0).get("said"));
    }

    @Test
    void anAbsentChapterAsksForNothing() {
        assertTrue(Change.parse("{\"arc\":\"\",\"quotes\":[]}").isEmpty(),
                "a chapter the character is not in should cost the page nothing");
        assertTrue(!Change.parse("{\"arc\":\"something happened\"}").isEmpty());
    }

    @Test
    void aFinishedPageFitsTheContextItHasToTravelIn() {
        // THE MEASUREMENT THAT CHOSE THIS DESIGN, kept as an assertion so a future field cannot
        // quietly break it. 365 chapters of appearances and facts, both capped lists full, and
        // prose at a generous length.
        Page page = Page.blank("Pierre Bezúkhov");
        for (int i = 1; i <= 365; i++) {
            page = new Change(i == 1 ? "x".repeat(6000) : "", i == 1 ? "y".repeat(2400) : "",
                    List.of(), List.of(),
                    List.of(row("verdict", "present", "span", "a span of about this length here")),
                    List.of(row("fact", "a fact")),
                    List.of(row("said", "a quote of roughly the length quotes run to in practice")),
                    List.of(row("note", "a note about this long", "span", "and its span")))
                    .applyTo(page, "BOOK " + (i % 17), "ch" + i).page();
        }
        int tokens = page.roughTokens();

        assertTrue(tokens < 60_000,
                "the page travels on every one of 365 calls; the ceiling is 251,552 and the worst "
                        + "call adds a 5,692-token chapter, 4,000 of thinking and the change. "
                        + "This page: " + tokens + " tokens");
        // Seventeen books at four apiece, and crucially spread ACROSS them rather than forty
        // from the end of the novel.
        assertEquals(17 * Page.KEEP_QUOTES, page.quotes().size());
        assertEquals(17 * Page.KEEP_TRIVIA, page.trivia().size());
        assertEquals(365, page.appearances().size(), "appearances are never dropped");
        assertTrue(page.quotes().stream().anyMatch(q -> "BOOK 1".equals(q.get("book"))),
                "the first book still has quotes on the finished page, which a flat cap lost");
    }
}
