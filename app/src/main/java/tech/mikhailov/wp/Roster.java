package tech.mikhailov.wp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import tech.mikhailov.ratchet.record.Json;

/**
 * WHO THE SWEEP IS FOR — the one input to this pipeline that a person chooses.
 *
 * <p>THE LIST USED TO BE FOUR NAMES IN A {@code Map.of} INSIDE {@code Extract}, and everything else
 * about this project was configurable while the thing the whole run is about was not. A character
 * the reader wanted a page for had to be added in Java and the image rebuilt, which is the same
 * shape of defect the library underneath spent a week having reported against it: a value that is
 * obviously a setting, reachable only by editing the program.
 *
 * <p>IT IS NOT {@code /api/characters}, AND THE DIFFERENCE MATTERS. That endpoint answers "who has a
 * page", derived from what is on disk. This answers "who should have one", which is a decision. A
 * character can be on this list with nothing read for them yet — that is the normal state of a fresh
 * roster and the whole reason the settings page exists.
 *
 * <p>A SLUG AND A NAME, AND DELIBERATELY NOT A LIST OF WHAT ELSE THE TEXT CALLS THEM. The first
 * draft carried "variants" — nine strings for Pierre, seeded from {@code gold/} — on the reasoning
 * that matching only the canonical name would miss "the young man" and "the orator". That is right
 * about the problem and it makes it WORSE, which is the part the draft got wrong.
 *
 * <p>LOOK AT WHAT THOSE STRINGS ACTUALLY ARE, in the file the draft seeded from:
 *
 * <pre>
 * the young man (ch XXIV)
 * this young Jacobin (ch V, narrator via the vicomte)
 * my friend (ch XXIV, Prince Vasíli's address)
 * my dear boy / my dear friend (ch XXIV, Anna Mikháylovna's address)
 * </pre>
 *
 * <p>EVERY ONE IS SCOPED TO A CHAPTER AND OFTEN TO A SPEAKER. That file is a record of what was
 * observed, where, and in whose mouth — an OUTPUT. A roster strips the scope and asserts the forms
 * everywhere: "my friend" becomes Pierre in all 365 chapters, when in chapter XXIV it is Prince
 * Vasíli addressing him and everywhere else it is whoever anyone happens to be addressing. "The
 * young man" is a young man. Handed that list, a reading does not merely fail to find Pierre where
 * he is — it FINDS HIM WHERE HE IS NOT, and a false appearance is worse than a missing one because
 * nothing downstream can tell it from a real one.
 *
 * <p>THE {@code names} SECTION ALREADY DOES THIS, PER CHAPTER, WHICH IS THE ONLY SCOPE THAT IS
 * TRUE. Measured on the first live run, Book One chapter V, with nothing handed to it:
 *
 * <pre>
 * forms: Pierre (narrator, 17), Monsieur Pierre (Anna Pávlovna, 4),
 *        the orator (narrator, 1), this young Jacobin (the vicomte, 1)
 * not:   Monsieur — the vicomte, referring to Pierre in "how monsieur explains"
 * </pre>
 *
 * <p>It found the forms from context, attributed each to a speaker, counted them, and kept a
 * {@code not} list for a form that means somebody else here. A static list cannot express that last
 * line at all, and it is the line that decides whether a page is about the right person.
 *
 * <p>Nothing in the sweep ever read the field, which is how a harmful input hides: it cost nothing
 * at runtime, and the settings page asked a person to maintain it.
 */
public record Roster(List<Person> people) {

    /** One character: how the run refers to them, and how a reader does. */
    public record Person(String slug, String name) {

        public Person {
            if (slug == null || slug.isBlank()) {
                throw new IllegalArgumentException("a character needs a slug");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("a character needs a name: " + slug);
            }
        }

        String wire() {
            return Json.object(Json.field("slug", Json.string(slug)),
                    Json.field("name", Json.string(name)));
        }
    }

    public Roster {
        people = people == null ? List.of() : List.copyOf(people);
    }

    public String wire() {
        return Json.array(people, Person::wire);
    }

    public Person find(String slug) {
        return people.stream().filter(p -> p.slug().equals(slug)).findFirst().orElse(null);
    }

    /**
     * The roster as it stands: what was saved, or the seed if nothing has been.
     *
     * <p>SEEDING FROM {@code gold/} RATHER THAN FROM A LITERAL HERE. That file is twenty characters
     * with their variants, established by reading four chapters by hand to set a target for the
     * pipeline — it is the most carefully made list in the repository and duplicating a worse one
     * beside it would be strange.
     */
    public static Roster read(Path saved, Path gold) throws IOException {
        if (Files.isRegularFile(saved)) {
            return parse(Files.readString(saved, StandardCharsets.UTF_8));
        }
        Path seed = gold.resolve("characters.json");
        return Files.isRegularFile(seed)
                ? fromGold(Files.readString(seed, StandardCharsets.UTF_8))
                : new Roster(List.of());
    }

    public void write(Path saved) throws IOException {
        Files.createDirectories(saved.getParent());
        Files.writeString(saved, wire(), StandardCharsets.UTF_8);
    }

    /** Our own wire shape, read back. */
    static Roster parse(String json) {
        List<Person> people = new ArrayList<>();
        for (String one : objects(json)) {
            String slug = Json.read(one, "slug");
            String name = Json.read(one, "name");
            if (slug.isBlank() || name.isBlank()) {
                continue;
            }
            people.add(new Person(slug, name));
        }
        return new Roster(people);
    }

    /** The gold file's shape, which is {@code {"characters":[{"canonical":…,"variants":[…]}]}}. */
    static Roster fromGold(String json) {
        List<Person> people = new ArrayList<>();
        LinkedHashSet<String> taken = new LinkedHashSet<>();
        for (String one : objects(Json.part(json, "characters"))) {
            String canonical = Json.read(one, "canonical");
            if (canonical.isBlank()) {
                continue;
            }
            String slug = slug(canonical);
            // A SLUG IS A FILE PATH AND A JOURNAL KEY, so two characters cannot share one. The gold
            // list has none, and the settings page can produce them the moment somebody adds a
            // second "Princess Mary".
            String unique = slug;
            for (int n = 2; !taken.add(unique); n++) {
                unique = slug + "-" + n;
            }
            // The gold file's own variants are left where they are: that list is the hand-made
            // TARGET the pipeline is measured against, which is a different thing from an input.
            people.add(new Person(unique, canonical));
        }
        return new Roster(people);
    }

    /**
     * A NAME AS A PATH SEGMENT: accents folded, punctuation dropped, spaces hyphenated.
     *
     * <p>Folded rather than stripped, so {@code Bezúkhov} becomes {@code bezukhov} and not
     * {@code bezkhov}. Half this cast has an accent in it and the other half has a parenthetical.
     */
    public static String slug(String name) {
        String folded = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        String cut = folded.replaceAll("\\(.*?\\)", " ").replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return cut.isEmpty() ? "character" : cut;
    }

    /** The top-level objects of a JSON array. */
    private static List<String> objects(String array) {
        List<String> out = new ArrayList<>();
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

}
