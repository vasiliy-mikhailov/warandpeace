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
 * <p>THE VARIANTS TRAVEL WITH THE NAME because a regex over a canonical name finds a fraction of the
 * appearances: Pierre is "Pierre", "Monsieur Pierre", "the young man", "the orator" and six more, and
 * a sweep that matched only the first would build a page with holes in it and no way to know. The
 * seed carries the ones already established by hand in {@code gold/}.
 */
public record Roster(List<Person> people) {

    /** One character: how the run refers to them, how a reader does, and what the text calls them. */
    public record Person(String slug, String name, List<String> variants) {

        public Person {
            if (slug == null || slug.isBlank()) {
                throw new IllegalArgumentException("a character needs a slug");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("a character needs a name: " + slug);
            }
            variants = variants == null ? List.of() : List.copyOf(variants);
        }

        String wire() {
            return Json.object(Json.field("slug", Json.string(slug)),
                    Json.field("name", Json.string(name)),
                    Json.field("variants", Json.array(variants, Json::string)));
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
            people.add(new Person(slug, name, strings(Json.part(one, "variants"))));
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
            people.add(new Person(unique, canonical, strings(Json.part(one, "variants"))));
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

    /** The strings of a JSON array of strings. */
    private static List<String> strings(String array) {
        List<String> out = new ArrayList<>();
        boolean inString = false;
        StringBuilder one = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            char c = array.charAt(i);
            if (inString) {
                if (c == '\\' && i + 1 < array.length()) {
                    char next = array.charAt(++i);
                    one.append(switch (next) {
                        case 'n' -> '\n';
                        case 't' -> '\t';
                        case 'r' -> '\r';
                        case 'u' -> (char) Integer.parseInt(array, i + 1, i += 4, 16);
                        default -> next;
                    });
                } else if (c == '"') {
                    out.add(one.toString());
                    one.setLength(0);
                    inString = false;
                } else {
                    one.append(c);
                }
            } else if (c == '"') {
                inString = true;
            }
        }
        return out;
    }
}
