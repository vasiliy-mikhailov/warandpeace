package tech.mikhailov.wp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * WHAT THE RUN LOOKS LIKE WHILE IT IS RUNNING, AND WHAT IT PRODUCED.
 *
 * <p>Two things are served from one host, and pairing them is the argument: the WIKI, which is the
 * product, and the DASHBOARD, which is the machine that made it. A character page that nobody can
 * trace back to a chapter is a claim; the same page beside the coverage that produced it is a
 * measurement.
 *
 * <p>THE VOCABULARY IS {@code ratchet-ui}'s AND NOT OURS. {@code WorkItem}, {@code ItemDetail},
 * {@code Manifest}, {@code NavItem}, {@code Badge}, {@code Health} and {@code Finding} are its types,
 * with a validator apiece, so what this serves is checkable rather than merely plausible. Three
 * documents are this pipeline's own — a chapter, a character, and a reading — and those are the only
 * three it invents.
 *
 * <p>EVERY NAV PATH MUST RESOLVE TO A PAGE THAT EXISTS, and that is asserted rather than assumed.
 * {@code checkManifest} polices the BADGE half of the nav contract and nothing polices the PATH
 * half, which is how a sibling repository ended up publishing two nav items pointing at routes no
 * page served — a live, correct badge count beside a dead link. The check lives in
 * {@link #danglingNav()} and the test calls it.
 */
public final class Dash {

    private static final String VERSION = "0.1.0";

    private final Path records;
    private final Path readings;
    private final Path wiki;
    private final Text text;

    /**
     * THE BUILT UI, SERVED BY THE PROCESS THAT SERVES THE API.
     *
     * <p>No node in the runtime image and no second container. This process is already an HTTP
     * server with the record mounted into it; a {@code static/} directory is one handler, where a
     * Node process beside it would be a second thing to keep alive and a second place for the
     * dashboard to be dark exactly when the sweep it reports on has died — which is the same
     * argument the compose file makes for splitting the reader from the sweep.
     */
    private final Path assets;

    public Dash(Path records, Path readings, Path wiki, Text text) {
        this(records, readings, wiki, text, Path.of("static"));
    }

    public Dash(Path records, Path readings, Path wiki, Text text, Path assets) {
        this.records = records;
        this.readings = readings;
        this.wiki = wiki;
        this.text = text;
        this.assets = assets;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8087;
        Dash dash = new Dash(Path.of("records"), Path.of("readings"), Path.of("wiki"),
                new Text(Path.of("corpus")), Path.of("static"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", dash::handle);
        server.setExecutor(null);
        server.start();
        System.out.println("warandpeace: serving on http://127.0.0.1:" + port);
        System.out.println("  the wiki      /            one page per character");
        System.out.println("  the run       /dashboard   lanes, coverage, the record");
    }

    void handle(HttpExchange exchange) {
        try {
            String path = exchange.getRequestURI().getPath();
            switch (path) {
                case "/api/health" -> send(exchange, 200, health());
                case "/api/manifest" -> send(exchange, 200, manifest());
                case "/api/items" -> send(exchange, 200, items());
                case "/api/characters" -> send(exchange, 200, characters());
                case "/api/chapters" -> send(exchange, 200, chapters());
                case "/api/badges" -> send(exchange, 200, badges());
                default -> {
                    if (path.startsWith("/api/items/")) {
                        send(exchange, 200, detail(path.substring("/api/items/".length())));
                    } else if (SERVED.contains(path)) {
                        // ONE DOCUMENT FOR EVERY PAGE. The bundle routes on the path it was loaded
                        // at, so the server's job is to hand the same index.html to each of them
                        // and let the client decide. A path NOT in SERVED is a 404 here rather than
                        // a page, so a link to a route nobody built fails loudly instead of
                        // rendering an empty shell that looks like a slow load.
                        page(exchange);
                    } else if (path.startsWith("/assets/")) {
                        asset(exchange, path);
                    } else {
                        send(exchange, 404, "{\"ok\":false,\"why\":\"no such endpoint: "
                                + esc(path) + "\"}");
                    }
                }
            }
        } catch (RuntimeException | IOException failed) {
            try {
                send(exchange, 500, "{\"ok\":false,\"why\":" + json(String.valueOf(failed)) + "}");
            } catch (IOException gone) {
                // The client left. Nothing to say and nobody to say it to.
            }
        }
    }

    /**
     * THE NAV, AND EVERY PATH ON IT IS SERVED.
     *
     * <p>Kept in one list rather than written into the JSON by hand, so {@link #danglingNav()} can
     * walk the same list the manifest publishes. Two copies of a route table is how one of them
     * starts advertising a page that was never built.
     */
    static final List<String[]> NAV = List.of(
            new String[]{"Characters", "/", "characters"},
            new String[]{"Chapters", "/chapters", "chapters"},
            new String[]{"The run", "/dashboard", "reading"});

    /**
     * THE PAGES THIS SERVER ANSWERS FOR — and this list IS the routing, not a claim about it.
     *
     * <p>IT USED TO BE A SECOND COPY OF THE ROUTE TABLE AND THAT MADE THE GUARD ABOVE VACUOUS. The
     * handler served {@code /api/*} and nothing else; {@link #NAV} named three pages; this named the
     * same three; {@link #danglingNav()} compared the two hand-written lists, found them in perfect
     * agreement, and reported the nav healthy while the server returned 404 for every path on it.
     * The javadoc on {@code danglingNav} says it exists because a sibling repository published nav
     * items pointing at routes no page served. It had exactly that bug, one level up, because it
     * checked a list against a list instead of against the routing.
     *
     * <p>So {@link #handle} now dispatches on THIS. Delete a path from here and the server stops
     * answering it, which is what makes comparing the nav against it mean anything.
     */
    static final List<String> SERVED = List.of("/", "/chapters", "/dashboard");

    /**
     * Which nav paths point nowhere. Empty is the only acceptable answer, and a test asserts it.
     *
     * <p>This is the check a sibling repository did not have. Its manifest published
     * {@code /findings} and {@code /corpus}; its export had neither; both type-checked and both
     * validated clean, because the badge half of the contract is policed and the path half is not.
     */
    static List<String> danglingNav() {
        List<String> dead = new ArrayList<>();
        for (String[] item : NAV) {
            if (!SERVED.contains(item[1])) {
                dead.add(item[0] + " -> " + item[1]);
            }
        }
        return dead;
    }

    String manifest() {
        StringBuilder nav = new StringBuilder();
        for (String[] item : NAV) {
            if (nav.length() > 0) {
                nav.append(',');
            }
            nav.append("{\"label\":").append(json(item[0]))
               .append(",\"path\":").append(json(item[1]))
               .append(",\"badge\":").append(item[2] == null ? "null" : json(item[2])).append('}');
        }
        return "{\"id\":\"warandpeace\""
                + ",\"name\":\"War and Peace\""
                + ",\"description\":\"A fandom wiki built one chapter at a time\""
                + ",\"version\":\"" + VERSION + "\""
                + ",\"basePath\":\"\",\"assetPrefix\":\"\""
                + ",\"api\":\"/api\",\"health\":\"/api/health\""
                + ",\"nav\":[" + nav + "]"
                + ",\"badges\":{"
                + "\"characters\":{\"endpoint\":\"/api/badges\",\"field\":\"characters\"},"
                + "\"chapters\":{\"endpoint\":\"/api/badges\",\"field\":\"chapters\"},"
                + "\"reading\":{\"endpoint\":\"/api/badges\",\"field\":\"reading\"}}}";
    }

    String health() {
        List<String> dead = danglingNav();
        return dead.isEmpty()
                ? "{\"ok\":true,\"version\":\"" + VERSION + "\"}"
                : "{\"ok\":false,\"why\":" + json("nav points nowhere: " + dead) + "}";
    }

    String badges() throws IOException {
        return "{\"characters\":" + charactersOnDisk().size()
                + ",\"chapters\":" + text.chaptersIn(null).size()
                + ",\"reading\":" + readingFiles().size() + "}";
    }

    /**
     * A LANE IS A CHARACTER AND A BOOK, which is what a wall-clock budget is spent on.
     *
     * <p>Not (character, chapter, section) — that is the JOURNAL key, 13,140 of them, and a queue
     * nobody can read is not a queue. Not the character alone either, because a character is
     * fourteen books of work and a page that says "Pierre: running" for six hours says nothing.
     */
    String items() throws IOException {
        List<String> out = new ArrayList<>();
        for (String who : charactersOnDisk()) {
            for (Chain.Book book : text.books()) {
                List<Path> read = readingsFor(who, book);
                if (read.isEmpty()) {
                    continue;
                }
                out.add("{\"id\":" + json(who + "/" + slug(book.title()))
                        + ",\"state\":\"" + (read.size() >= expected(book) ? "read" : "reading") + "\""
                        + ",\"because\":" + json(read.size() + " of " + expected(book) + " readings")
                        + ",\"events\":" + read.size()
                        + ",\"at\":" + newest(read) + "}");
            }
        }
        return "[" + String.join(",", out) + "]";
    }

    String detail(String id) throws IOException {
        int slash = id.indexOf('/');
        if (slash < 0) {
            return "{\"ok\":false,\"why\":\"an item id is character/book\"}";
        }
        String who = id.substring(0, slash);
        String bookSlug = id.substring(slash + 1);
        Chain.Book book = text.books().stream()
                .filter(b -> slug(b.title()).equals(bookSlug)).findFirst().orElse(null);
        if (book == null) {
            return "{\"ok\":false,\"why\":" + json("no book " + bookSlug) + "}";
        }
        List<Path> read = readingsFor(who, book);
        List<String> events = new ArrayList<>();
        for (Path file : read) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                events.add("{\"at\":" + Files.getLastModifiedTime(file).toMillis()
                        + ",\"kind\":\"reading\""
                        + ",\"agent\":" + json(file.getFileName().toString().replace(".jsonl", ""))
                        + ",\"text\":" + json(line) + "}");
            }
        }
        return "{\"item\":{\"id\":" + json(id)
                + ",\"state\":\"" + (read.size() >= expected(book) ? "read" : "reading") + "\""
                + ",\"because\":" + json(read.size() + " of " + expected(book) + " readings")
                + ",\"events\":" + events.size()
                + ",\"at\":" + newest(read) + "}"
                + ",\"events\":[" + String.join(",", events) + "]}";
    }

    String characters() throws IOException {
        List<String> out = new ArrayList<>();
        for (String who : charactersOnDisk()) {
            Path page = wiki.resolve(who + ".wiki");
            out.add("{\"id\":" + json(who)
                    + ",\"readings\":" + readingFiles().stream()
                            .filter(p -> p.toString().contains("/" + who + "/")).count()
                    + ",\"page\":" + (Files.exists(page) ? "true" : "false") + "}");
        }
        return "[" + String.join(",", out) + "]";
    }

    String chapters() throws IOException {
        List<String> out = new ArrayList<>();
        for (Chain.Chapter chapter : text.chaptersIn(null)) {
            out.add("{\"slug\":" + json(chapter.slug())
                    + ",\"book\":" + json(chapter.book())
                    + ",\"chapter\":" + json(chapter.numeral())
                    + ",\"paragraphs\":" + text.paragraphs(chapter).size() + "}");
        }
        return "[" + String.join(",", out) + "]";
    }

    // ---------------------------------------------------------------- what is on disk

    private List<String> charactersOnDisk() throws IOException {
        if (!Files.isDirectory(readings)) {
            return List.of();
        }
        try (var entries = Files.list(readings)) {
            return entries.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    private List<Path> readingFiles() throws IOException {
        if (!Files.isDirectory(readings)) {
            return List.of();
        }
        try (var walk = Files.walk(readings)) {
            return walk.filter(p -> p.toString().endsWith(".jsonl")).sorted().toList();
        }
    }

    private List<Path> readingsFor(String who, Chain.Book book) throws IOException {
        Path under = readings.resolve(who).resolve(slug(book.title()));
        if (!Files.isDirectory(under)) {
            return List.of();
        }
        try (var walk = Files.walk(under)) {
            return walk.filter(p -> p.toString().endsWith(".jsonl")).sorted().toList();
        }
    }

    /** Eight sections for every chapter of the book. The denominator a reader is owed. */
    private int expected(Chain.Book book) throws IOException {
        return text.chaptersIn(book).size() * Chain.Section.values().length;
    }

    private long newest(List<Path> files) {
        return files.stream().map(p -> {
            try {
                return Files.getLastModifiedTime(p).toMillis();
            } catch (IOException unreadable) {
                return 0L;
            }
        }).max(Comparator.naturalOrder()).orElse(0L);
    }

    static String slug(String book) {
        return book.toLowerCase().replace(' ', '-');
    }

    // ---------------------------------------------------------------- plumbing

    /**
     * The single-page document, for every path the nav can reach.
     *
     * <p>A MISSING BUNDLE IS SAID IN WORDS. An image built without the UI stage would otherwise
     * serve an empty 200, and an empty 200 is the one failure a reader cannot tell from a slow
     * network. The API keeps working either way, which is deliberate: the record is the thing that
     * must not go dark.
     */
    private void page(HttpExchange exchange) throws IOException {
        Path index = assets.resolve("index.html");
        if (!Files.isRegularFile(index)) {
            byte[] said = ("The API is serving but the interface was not built into this image. "
                    + "Run `pnpm build` in ui/ and rebuild, or read /api/manifest directly.")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(503, said.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(said);
            }
            return;
        }
        write(exchange, 200, "text/html; charset=utf-8", Files.readAllBytes(index));
    }

    /**
     * One built asset, by exact name.
     *
     * <p>THE PATH IS REBUILT FROM ITS OWN FILE NAME RATHER THAN TRUSTED. {@code normalize} on a
     * caller's string is the wrong instrument — it resolves {@code ..} but happily walks out of the
     * directory when the prefix check is written the obvious way. Taking only the final segment
     * cannot escape, and the bundle's names are flat by construction.
     */
    private void asset(HttpExchange exchange, String path) throws IOException {
        String name = path.substring(path.lastIndexOf('/') + 1);
        Path file = assets.resolve("assets").resolve(name);
        if (name.isEmpty() || !Files.isRegularFile(file)) {
            send(exchange, 404, "{\"ok\":false,\"why\":\"no such asset: " + esc(path) + "\"}");
            return;
        }
        String type = name.endsWith(".css") ? "text/css; charset=utf-8"
                : name.endsWith(".js") ? "text/javascript; charset=utf-8"
                : name.endsWith(".svg") ? "image/svg+xml"
                : "application/octet-stream";
        // The names are content-hashed by the bundler, so a year is safe and a reload is free.
        exchange.getResponseHeaders().add("Cache-Control", "public, max-age=31536000, immutable");
        write(exchange, 200, type, Files.readAllBytes(file));
    }

    private void write(HttpExchange exchange, int status, String type, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().add("Content-Type", type);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static String json(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
