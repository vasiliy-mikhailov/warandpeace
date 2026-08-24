package tech.mikhailov.wp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE LIST THE WHOLE RUN IS ABOUT, and until now it was four names in a {@code Map.of}.
 *
 * <p>Every other input to this pipeline was a setting — the endpoint, the retry, the patience, the
 * sampling, the thinking budget — and the one thing a person actually decides, WHO THE WIKI IS FOR,
 * required editing Java and rebuilding an image. That is the same defect the library underneath had
 * reported against it six times in a week, in the consumer that files those reports.
 */
class TheRosterIsTheOneThingAPersonChoosesTest {

    @Test
    void theSeedIsTheHandMadeGoldListRatherThanALiteralBesideIt(@TempDir Path root)
            throws Exception {
        Roster seeded = Roster.read(root.resolve("nothing-saved.json"), Path.of("gold"));

        assertEquals(20, seeded.people().size(),
                "the twenty characters established by reading four chapters by hand");
        Roster.Person pierre = seeded.find("pierre-bezukhov");
        assertNotNull(pierre, "slugs are folded, not stripped: "
                + seeded.people().stream().map(Roster.Person::slug).toList());
        assertEquals("Pierre Bezúkhov", pierre.name());
        assertTrue(pierre.variants().size() >= 9,
                "THE VARIANTS TRAVEL WITH THE NAME. A regex over the canonical name alone finds a "
                        + "fraction of the appearances — Pierre is also the young man, the orator "
                        + "and seven more — and a sweep matching only the first builds a page with "
                        + "holes in it and no way to know: " + pierre.variants());
    }

    @Test
    void anAccentIsFoldedAndNotDropped() {
        // bezkhov, not bezukhov, is what stripping combining marks the obvious way produces, and
        // half this cast has an accent in it.
        assertEquals("pierre-bezukhov", Roster.slug("Pierre Bezúkhov"));
        assertEquals("natasha-rostova", Roster.slug("Natásha Rostóva"));
        // A parenthetical is an aside to a reader, not part of a path.
        assertEquals("the-little-princess", Roster.slug("The little princess (Lise)"));
    }

    @Test
    void twoCharactersCannotShareASlugBecauseASlugIsAPath(@TempDir Path root) throws Exception {
        // A slug is a directory under readings/ AND the first field of a journal key, so a
        // collision does not read as a collision: the second character's readings land on the
        // first's and the journal reports the work already done.
        Path saved = root.resolve("roster.json");
        Roster gold = Roster.fromGold("{\"characters\":["
                + "{\"canonical\":\"Princess Mary\",\"variants\":[]},"
                + "{\"canonical\":\"Princess Mary\",\"variants\":[]}]}");

        assertEquals(List.of("princess-mary", "princess-mary-2"),
                gold.people().stream().map(Roster.Person::slug).toList());
        gold.write(saved);
        assertEquals(2, Roster.read(saved, Path.of("gold")).people().size());
    }

    @Test
    void whatIsSavedIsWhatComesBack(@TempDir Path root) throws Exception {
        Path saved = root.resolve("roster.json");
        Roster mine = new Roster(List.of(
                new Roster.Person("pierre", "Pierre", List.of("the young man", "the orator")),
                new Roster.Person("andrew", "Prince Andrew", List.of())));

        mine.write(saved);
        Roster back = Roster.read(saved, Path.of("gold"));

        assertEquals(2, back.people().size(), "and NOT the twenty-character seed");
        assertEquals("Pierre", back.find("pierre").name());
        assertEquals(List.of("the young man", "the orator"), back.find("pierre").variants());
        assertNull(back.find("natasha"));
    }

    @Test
    void aNameWithAQuoteInItSurvivesTheRoundTrip(@TempDir Path root) throws Exception {
        // The cast includes `Count Bezúkhov (Pierre's father)`. An apostrophe is harmless; a
        // double quote or a newline pasted into the settings page is what breaks a hand-written
        // JSON writer, and this file is written by one.
        Path saved = root.resolve("roster.json");
        String awkward = "The \"old\" prince\nand a second line";
        new Roster(List.of(new Roster.Person("odd", awkward, List.of("a \"variant\""))))
                .write(saved);

        Roster back = Roster.read(saved, Path.of("gold"));

        assertEquals(awkward, back.find("odd").name());
        assertEquals(List.of("a \"variant\""), back.find("odd").variants());
    }

    @Test
    void aCharacterWithoutANameIsRefusedRatherThanSavedAsItsSlug() {
        // The settings page can produce this with one empty field, and a character called
        // "pierre-bezukhov" on its own page is a row nobody would notice was wrong.
        assertThrows(IllegalArgumentException.class,
                () -> new Roster.Person("pierre", "  ", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Roster.Person("", "Pierre", List.of()));
    }

    @Test
    void anUnreadableRosterIsNotWrittenOverAGoodOne(@TempDir Path root) throws Exception {
        Path saved = root.resolve("roster.json");
        new Roster(List.of(new Roster.Person("pierre", "Pierre", List.of()))).write(saved);
        String before = Files.readString(saved);

        // What Dash.saveRoster refuses: a body that parses to nothing but was not meant as empty.
        assertEquals(0, Roster.parse("this is not json at all").people().size());
        assertEquals(before, Files.readString(saved), "and the good one is still on disk");
    }
}
