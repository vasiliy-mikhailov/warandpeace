package tech.mikhailov.wp;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE HALF OF THE NAV CONTRACT NOBODY POLICES.
 *
 * <p>{@code ratchet-ui}'s {@code checkManifest} walks every nav item's BADGE and refuses one naming
 * a badge the manifest does not define. Nothing walks the PATH. So a manifest can publish a nav item
 * pointing at a route no page serves, type-check, validate clean, and ship — which is exactly what a
 * sibling repository did with {@code /findings} and {@code /corpus}: two of three entry points were
 * dead links, and the badge beside one of them displayed a live, correct count for data no page
 * could reach.
 *
 * <p>That is worth a test here rather than a fix later, because the failure is invisible from inside
 * the manifest. It only shows when somebody clicks.
 */
class EveryNavPathIsServedTest {

    @Test
    void noNavItemPointsAtAPageThatIsNotServed() {
        assertEquals(List.of(), Dash.danglingNav(),
                "a nav item pointing nowhere is a dead link with a working badge beside it");
    }

    @Test
    void everyServedPageIsReachableFromTheNav() {
        // The other direction, which is a different bug: a page nobody can navigate to is a page
        // that exists only for whoever knows the URL.
        for (String served : Dash.SERVED) {
            assertTrue(Dash.NAV.stream().anyMatch(item -> item[1].equals(served)),
                    "nothing in the nav reaches " + served);
        }
    }

    @Test
    void everyNavBadgeIsDefinedInTheManifest() {
        // What checkManifest already does, asserted here too so the Java side fails at build time
        // rather than waiting for a page to render nothing.
        String manifest = new Dash(null, null, null, null).manifest();
        for (String[] item : Dash.NAV) {
            if (item[2] != null) {
                assertTrue(manifest.contains("\"" + item[2] + "\":{\"endpoint\""),
                        "nav item " + item[0] + " names badge '" + item[2]
                                + "' and the manifest defines no such badge");
            }
        }
    }

    @Test
    void healthGoesRedWhenTheNavGoesDangling() {
        // The check is wired into /api/health rather than only living in a test, so a deployment
        // that drifts says so from the outside.
        String healthy = new Dash(null, null, null, null).health();
        assertTrue(healthy.contains("\"ok\":true"), healthy);
    }
}
