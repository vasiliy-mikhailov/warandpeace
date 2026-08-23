package tech.mikhailov.wp.sections;

import java.util.List;

import tech.mikhailov.ratchet.llm.Chat;
import tech.mikhailov.ratchet.llm.Said;

/**
 * One question, one answer. The retry, the stall guard and the record are ratchet's, not ours.
 *
 * <p>THE FULLY QUALIFIED NAME BELOW IS NOT AN OVERSIGHT. ratchet's request type is also called
 * {@code Ask}, which is the right name for it and the right name for this too — a helper called
 * anything else would be a worse name chosen to dodge a collision. One qualified reference in one
 * file is cheaper than renaming a class eight sections import.
 */
public final class Ask {

    private Ask() {
    }

    public static String once(Chat model, String question) {
        return model.answer(tech.mikhailov.ratchet.llm.Ask.of(List.of(Said.user(question))))
                .said().trim();
    }

    /** A JSON string literal. Small enough to write, and the alternative is a dependency. */
    public static String json(String value) {
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
