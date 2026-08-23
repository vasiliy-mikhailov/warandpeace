package tech.mikhailov.wp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * WHAT ONE CHAPTER YIELDED ABOUT ONE CHARACTER, IN ONE SECTION.
 *
 * <p>The name is the gate said out loud: <strong>a second reading must not find less than the
 * first</strong>. That sentence only works with this noun. A "fragment" cannot find anything, and a
 * "witness" is a different observer — but two readings are the same fixed text read twice, which is
 * exactly what makes monotonicity mean something here.
 *
 * <p>ONE FILE PER SECTION, NOT PER CHAPTER, and the journal key said so before the storage did:
 * {@code {hero}#{chapter}#{section}} is what {@link Chain} already journals, so that is the grain
 * the output should have. Bundling all eight into one file would make every fold read and discard
 * seven eighths of its input, and would make a prompt change to one section invalidate the other
 * seven — which matters because {@code Resume} carries one version per lane and cannot express the
 * difference.
 *
 * <p>APPENDED, NEVER OVERWRITTEN. A re-reading adds a line. When the gate keeps the better of two,
 * the rejected one stays on disk, which is what makes the ratchet auditable rather than merely
 * enforced: you can go and see that chapter XXI was read twice and the second pass found five
 * fewer entries. {@code Settlement} is append-only for the same reason and says so — a process
 * that gets killed leaves a torn last line, and that is the normal case rather than a fault.
 */
public record Reading(String character, String book, String chapter, String section,
                      int attempt, String body, int paragraphsRead, int paragraphsTotal,
                      boolean settled) {

    /** Where a reading lives. The tree is the filesystem, so a fold is a glob. */
    public static Path path(Path root, String character, String book, String chapter, String section) {
        return root.resolve(character).resolve(slug(book)).resolve(chapter).resolve(section + ".jsonl");
    }

    /** Every reading of this section, oldest first. The gate compares the newest against the best. */
    public static List<String> all(Path file) throws IOException {
        return Files.exists(file) ? Files.readAllLines(file, StandardCharsets.UTF_8) : List.of();
    }

    public void append(Path root) throws IOException {
        Path file = path(root, character, book, chapter, section);
        Files.createDirectories(file.getParent());
        Files.writeString(file, line() + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * COVERAGE IS RECORDED WITH THE READING, not inferred from it.
     *
     * <p>Pass one's gate is that every paragraph was read, and the only honest place for that number
     * is beside what the reading found. Kept separate from the body so a fold can count coverage
     * without parsing what a section happens to hold.
     *
     * <p>{@code settled} IS HERE BECAUSE THE FIRST LIVE RUN WROTE A REJECTED ANSWER AS THOUGH IT HAD
     * PASSED. {@link tech.mikhailov.ratchet.flow.Flow#triad} returns the doer's last attempt when the
     * round budget runs out and records that it never settled — which is right, since a triad cannot
     * invent success. What was wrong was downstream: the reading was appended with no mark, and the
     * journal recorded the key as done, so a resume would skip that chapter forever. A verifier that
     * fired three times and changed nothing is the shape this whole project exists to argue against,
     * and it appeared here within an hour of the loop being wired.
     */
    private String line() {
        return "{\"attempt\":" + attempt
                + ",\"settled\":" + settled
                + ",\"character\":\"" + character + "\""
                + ",\"book\":\"" + esc(book) + "\""
                + ",\"chapter\":\"" + chapter + "\""
                + ",\"section\":\"" + section + "\""
                + ",\"read\":" + paragraphsRead
                + ",\"of\":" + paragraphsTotal
                + ",\"body\":" + body
                + "}";
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String slug(String book) {
        return book.toLowerCase().replace(' ', '-');
    }
}
