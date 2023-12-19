package wood.message;

import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.Getter;
import wood.commands.Chat;

import java.util.Optional;

/** A pair class between usernames and an associated ChatMessage */
public class DiscordMessage extends ChatMessage {

    @Getter private final Optional<String> username;

    public static final DiscordMessage EMPTY_MSG = new DiscordMessage(Optional.empty(),
            new ChatMessage("system", ""));

    public DiscordMessage(Optional<String> username, ChatMessage chatMessage) {
        super(chatMessage.getRole(), chatMessage.getContent());
        this.username = username;
    }

}
