/**
 * HOW LONG SOMETHING TOOK, SPELLED THE SAME WAY IN BOTH DASHBOARDS.
 *
 * NO REACT REACHABLE FROM HERE, which is why this is a root module rather than a component one.
 * Both consumers format a duration in places that render nothing: a tally's string, a test's
 * expectation, a server's log line. Making them resolve React to spell "8m 45s" would be the same
 * mistake `style.ts` exists to avoid.
 *
 * WHAT IS DELIBERATELY NOT HERE IS "HOW LONG AGO". Both dashboards have one of those too and they
 * are not the same function: one crosses into minutes at sixty seconds and rounds, the other
 * crosses at ninety and floors, and one of the two is a component with a timer inside it that slows
 * down as the number does. That is two different products, not one written twice, and a shared
 * version would have to pick a rung ladder for a screen it cannot see. Each consumer keeps its own.
 */
/**
 * A DURATION, NOT A TIME AGO: "3h 4m", which is what a reader wants of a job still running, of a
 * run's elapsed time, and of an eta that has not happened.
 *
 * Seconds survive below an hour because that is the range where they carry information: "8m 45s"
 * against "9m" is the difference between a fast job and a rounded one. Above an hour they are noise
 * and the minutes are what matters.
 *
 * ROUNDED TO THE NEAREST SECOND, AND CLAMPED AT ZERO. The two sides of this move disagreed here:
 * one rounded and one truncated toward zero, following the Java integer division it was ported
 * from. Rounding is the version kept, because truncation reports 59.6 seconds as "59s" and the
 * reading a person compares it against is a clock that has already turned over. The clamp is the
 * same argument: a negative span means a clock that went backwards, not time running in reverse,
 * and "-1s" is a bug report rendered as a measurement.
 *
 * A UNIT WORTH NOTHING IS NOT PRINTED. Exactly ten minutes is "10m" rather than "10m 0s", and two
 * hours is "2h" rather than "2h 0m". This is the same argument one of the two repositories already
 * made out loud about its own human-minutes format, that a leading "0h" is a unit that is not
 * there; it holds at the other end of the number for the same reason.
 */
export function duration(ms) {
    const s = Math.max(0, Math.round(ms / 1000));
    if (s < 60)
        return `${s}s`;
    const m = Math.floor(s / 60);
    if (m < 60)
        return s % 60 === 0 ? `${m}m` : `${m}m ${s % 60}s`;
    const h = Math.floor(m / 60);
    return m % 60 === 0 ? `${h}h` : `${h}h ${m % 60}m`;
}
/**
 * The same ladder, entered in whole minutes, because that is the unit an estimate arrives in.
 *
 * It is one line over {@link duration} rather than a second ladder on purpose: the two were written
 * separately once in each of these dashboards, and in one of them the pair disagreed about whether
 * forty-five minutes reads "45m" or "0h 45m" on the same screen, in a tile that totals the column
 * beneath it.
 */
export function spellMinutes(minutes) {
    return duration(minutes * 60_000);
}
//# sourceMappingURL=time.js.map