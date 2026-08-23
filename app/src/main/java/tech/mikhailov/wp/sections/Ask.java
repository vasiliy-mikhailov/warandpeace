package tech.mikhailov.wp.sections;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;

/** One question, one answer. The retry, the stall guard and the record are ratchet's, not ours. */
public final class Ask {

    private Ask() {
    }

    public static String once(ChatModel model, String question) {
        return model.chat(ChatRequest.builder().messages(UserMessage.from(question)).build())
                .aiMessage().text().trim();
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
