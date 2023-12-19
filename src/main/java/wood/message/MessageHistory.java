package wood.message;

import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.Getter;
import wood.commands.Chat;
import wood.discord_threads.ChatThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** A collection of {@link ChatMessage} objects, and associated usernames */
public class MessageHistory {

    /** A collection of messages, optionally including usernames */
    @Getter private final List<DiscordMessage> discordMessages = new ArrayList<>();

    /**
     *
     * @param message to record in history
     */
    public void add(DiscordMessage message) {
        discordMessages.add(message);
    }

    /** @return Deep copy List of ChatMessage objects */
    public List<ChatMessage> getChatMessages() {
        // untested; convert history from DiscordMessage list to ChatMessage list
        //List<ChatMessage> chatMsgHistory =
        //        messageHistory.getMessages().stream().map(s -> (ChatMessage)s).collect(Collectors.toList());

        List<ChatMessage> chatMessages = new ArrayList<>();
        for(ChatMessage cm : discordMessages)
            chatMessages.add(new ChatMessage(cm.getRole(), cm.getContent()));

        return chatMessages;
    }

    public DiscordMessage getLatestDiscordMessage() {
        if(discordMessages.size() > 0)
            return discordMessages.get(discordMessages.size() - 1);
        else
            return null;
    }

}
