package tech.mikhailov.wp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE NAV IS CHECKED AGAINST THE SERVER, NOT AGAINST A SECOND LIST THAT AGREES WITH IT.
 *
 * <p>{@link EveryNavPathIsServedTest} compares {@code Dash.NAV} with {@code Dash.SERVED} and its
 * javadoc explains that it exists because a sibling repository published nav items pointing at
 * routes no page served. It had exactly that bug itself, one level up, for a whole release:
 * {@code SERVED} was a hand-written second copy of the route table, the handler answered
 * {@code /api/*} and nothing else, the two lists agreed perfectly, {@code danglingNav()} returned
 * empty, {@code /api/health} reported {@code ok:true}, and the site answered
 * {@code {"ok":false,"why":"no such endpoint: /"}} at its own front door. Nobody found it from
 * inside the process. It was found by opening the site.
 *
 * <p>THE LESSON IS NOT "ADD ANOTHER LIST". A guard that compares two things the same author wrote
 * on the same afternoon proves they were written by one author. So this test starts the real
 * server, on a real port, and asks it — the only reading that could not have agreed with itself.
 *
 * <p>{@code SERVED} is now what {@link Dash#handle} dispatches on, so the older test is no longer
 * vacuous either and both are kept: that one fails fast at build time on a typo, this one fails on
 * a route that stopped resolving for any reason at all.
 */
class EveryNavPathIsActuallyAnsweredTest {

    @Test
    void everyNavPathAnswersWithThePageAndNotWithA404(@TempDir Path root) throws Exception {
        Path assets = builtUi(root);

        withServer(root, assets, base -> {
            for (String[] item : Dash.NAV) {
                HttpResponse<String> answered = get(base + item[1]);
                assertEquals(200, answered.statusCode(),
                        "the nav offers " + item[0] + " at " + item[1] + " and the server said "
                                + answered.statusCode() + ": " + answered.body());
                assertTrue(answered.body().contains("<div id=\"root\">"),
                        item[1] + " answered 200 with something that is not the page: "
                                + answered.body());
            }
        });
    }

    @Test
    void aPathNobodyBuiltIsStillA404RatherThanAnEmptyShell(@TempDir Path root) throws Exception {
        // THE OTHER HALF, AND IT IS THE ONE A CATCH-ALL FALLBACK GETS WRONG. Handing index.html to
        // every unknown path makes a typo'd link render an empty page that looks like a slow load,
        // and makes this whole test class unable to fail.
        Path assets = builtUi(root);

        withServer(root, assets, base -> {
            HttpResponse<String> answered = get(base + "/findings");
            assertEquals(404, answered.statusCode(), answered.body());
            assertTrue(answered.body().contains("no such endpoint"), answered.body());
        });
    }

    @Test
    void anImageBuiltWithoutTheInterfaceSaysSoInsteadOfServingAnEmptyPage(@TempDir Path root)
            throws Exception {
        // AN EMPTY 200 IS THE ONE FAILURE A READER CANNOT TELL FROM A SLOW NETWORK. If the UI stage
        // is dropped from the Dockerfile, the record must keep serving and the page must say why it
        // is not there.
        withServer(root, root.resolve("no-ui"), base -> {
            assertEquals(503, get(base + "/").statusCode());
            assertTrue(get(base + "/").body().contains("was not built into this image"),
                    get(base + "/").body());
            assertEquals(200, get(base + "/api/health").statusCode(),
                    "the API does not go dark because the interface is missing");
        });
    }

    @Test
    void anAssetPathCannotWalkOutOfTheAssetDirectory(@TempDir Path root) throws Exception {
        Path assets = builtUi(root);
        Files.writeString(root.resolve("secret.txt"), "not for you");

        withServer(root, assets, base -> {
            assertEquals(404, get(base + "/assets/../secret.txt").statusCode());
            assertEquals(404, get(base + "/assets/").statusCode());
            assertEquals(200, get(base + "/assets/index-test.js").statusCode(),
                    "and an ordinary asset is still served");
        });
    }

    /** A stand-in for what `vite build` produces: an index and one hashed asset. */
    private static Path builtUi(Path root) throws IOException {
        Path assets = root.resolve("static");
        Files.createDirectories(assets.resolve("assets"));
        Files.writeString(assets.resolve("index.html"),
                "<!doctype html><html><body><div id=\"root\"></div></body></html>",
                StandardCharsets.UTF_8);
        Files.writeString(assets.resolve("assets").resolve("index-test.js"), "export {}",
                StandardCharsets.UTF_8);
        return assets;
    }

    private interface Asking {
        void ask(String base) throws Exception;
    }

    private static void withServer(Path root, Path assets, Asking asking) throws Exception {
        Files.createDirectories(root.resolve("corpus"));
        Dash dash = new Dash(root.resolve("records"), root.resolve("readings"),
                root.resolve("wiki"), new Text(root.resolve("corpus")), assets);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", dash::handle);
        server.setExecutor(null);
        server.start();
        try {
            asking.ask("http://127.0.0.1:" + server.getAddress().getPort());
        } finally {
            server.stop(0);
        }
    }

    private static HttpResponse<String> get(String url) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
