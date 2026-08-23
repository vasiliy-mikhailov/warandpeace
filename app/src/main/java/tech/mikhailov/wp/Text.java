package tech.mikhailov.wp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * THE CORPUS ON DISK, AND THE ONLY PLACE THAT KNOWS ITS SHAPE.
 *
 * <p>365 chapters split off the Maude translation on the text's own {@code BOOK} and {@code CHAPTER}
 * markers, with the Gutenberg boilerplate stripped. A chapter is around 1,400 words, which is why
 * every extractor can read a whole one and why nothing here needs a window or a chunker.
 */
public final class Text {

    /** Paragraphs are the denominator. Coverage is how many were read, and a model cannot inflate it. */
    private static final Pattern BREAK = Pattern.compile("\\n\\s*\\n");

    /** Speech, for the one verifier that is exact rather than judged. */
    private static final Pattern SPOKEN = Pattern.compile("“[^”]{2,}”", Pattern.DOTALL);

    private final Path root;

    public Text(Path root) {
        this.root = root;
    }

    public List<Chain.Book> books() throws IOException {
        List<Chain.Book> out = new ArrayList<>();
        for (String line : Files.readAllLines(root.resolve("chapters.jsonl"))) {
            String book = field(line, "book");
            if (!book.isBlank() && out.stream().noneMatch(b -> b.title().equals(book))) {
                out.add(new Chain.Book(book));
            }
        }
        return out;
    }

    public List<Chain.Chapter> chaptersIn(Chain.Book book) throws IOException {
        List<Chain.Chapter> out = new ArrayList<>();
        for (String line : Files.readAllLines(root.resolve("chapters.jsonl"))) {
            if (book != null && !book.title().equals(field(line, "book"))) {
                continue;
            }
            out.add(new Chain.Chapter(field(line, "slug"), field(line, "book"), field(line, "chapter")));
        }
        return out;
    }

    public String of(Chain.Chapter chapter) throws IOException {
        return Files.readString(root.resolve("chapters").resolve(chapter.slug() + ".txt"),
                StandardCharsets.UTF_8);
    }

    /** Numbered, because a verifier that says "paragraph 12" must mean the same 12 as the extractor. */
    public List<String> paragraphs(Chain.Chapter chapter) throws IOException {
        List<String> out = new ArrayList<>();
        for (String part : BREAK.split(of(chapter))) {
            if (part.trim().split("\\s+").length > 3) {
                out.add(part.trim().replaceAll("\\s+", " "));
            }
        }
        return out;
    }

    public List<String> spoken(Chain.Chapter chapter) throws IOException {
        List<String> out = new ArrayList<>();
        Matcher m = SPOKEN.matcher(of(chapter));
        while (m.find()) {
            out.add(m.group().replaceAll("\\s+", " "));
        }
        return out;
    }

    /**
     * A minimal reader for the one file this class owns.
     *
     * <p>Not a JSON parser and not pretending to be one: {@code chapters.jsonl} is written by a
     * script in this repository with no nesting and no escapes, so a general parser here would be a
     * dependency bought to read our own output.
     */
    private static String field(String line, String name) {
        Matcher m = Pattern.compile("\"" + name + "\":\\s*\"?([^\",}]*)\"?").matcher(line);
        return m.find() ? m.group(1) : "";
    }
}
