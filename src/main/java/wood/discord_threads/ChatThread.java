package wood.discord_threads;

import lombok.Getter;
import net.dv8tion.jda.api.entities.ThreadChannel;
import wood.Settings;
import wood.message.DiscordMessage;
import wood.util.GPTRequest;
import wood.util.GPTUtil;
import wood.util.TokenType;

import java.util.ArrayList;
import java.util.List;

public class ChatThread {

    private final ThreadChannel threadChannel;
    @Getter private final long threadID;
    @Getter private final String model;

    /** The name of this chatbot */
    public final String chatbotName;

    /** The description of this chatbot */
    private final String chatbotDescription;

    /** Raw string content of the discord messages */
    private final List<String> messages = new ArrayList<>();

    /** API interface */
    public final GPTRequest gptRequest;

    /** list of DiscordMessages */
    List<DiscordMessage> discordMessages;

    /** list of DiscordMessages that fit within token limit per request of {@link Settings#maxPromptCostPerAPIRequest}.
     *  While the messages exceed the limit, the 2nd message will repeatedly be removed */
    List<DiscordMessage> tokenLimitedDiscordMessages;

    /** The number of tokens inside {@link #tokenLimitedDiscordMessages} */
    private int tokensInTokenLimitedDiscordMessages = 0;

    /** What precedes and follows names in the chat */
    public final static String handleNamePrefix = "[", handleNameSuffix = "]> ";

    /** The chatbot's name formatted with handleNamePrefix and handleNameSuffix */
    @Getter public final String chatbotDisplayName;

    /** Whether the users have been notified that the prompt is being shortened to fit within Settings.maxCostPerAPIRequest limit */
    private boolean hasNotifiedUserOfPromptShortening = false;

    public ChatThread(ThreadChannel threadChannel, String model, String chatBotName, String chatBotDescription) {
        this.threadChannel = threadChannel;
        this.threadID = threadChannel.getIdLong();
        this.model = model;
        this.chatbotName = chatBotName;
        this.chatbotDescription = chatBotDescription;
        this.chatbotDisplayName = handleNamePrefix + chatBotName + handleNameSuffix;
        this.gptRequest = new GPTRequest.GPTRequestBuilder(model, Settings.chatCompletionTokens).build();
        this.discordMessages = new ArrayList<>();
        this.tokenLimitedDiscordMessages = new ArrayList<>();
    }

    public void registerMessage(DiscordMessage message) {
        discordMessages.add(message);
        tokenLimitedDiscordMessages.add(message);
        tokensInTokenLimitedDiscordMessages += GPTUtil.countTokens(message.getContent());

        // keep removing the 2nd message from tokenLimitedDiscordMessages until it costs under Settings.maxPromptCostPerAPIRequest
        while(Settings.maxPromptCostPerAPIRequest < GPTUtil.tokensToUSD(tokensInTokenLimitedDiscordMessages, model, TokenType.PROMPT)) {

            tokensInTokenLimitedDiscordMessages -= GPTUtil.countTokens(
                    tokenLimitedDiscordMessages.remove(1).getContent());

            // send only one notification per chat thread
            if(!hasNotifiedUserOfPromptShortening) {
                threadChannel.sendMessage("`To prevent the chatbot's memory from exceeding its limit,"
                        + " the oldest messages will be forgotten as needed.`").queue();
                hasNotifiedUserOfPromptShortening = true;
            }
        }
    }

    /**
     * @return The chat history of this thread that doesn't exceed Settings.maxCostPerAPIRequest -
     *         if the chat history is too long, the oldest messages will have been removed (excluding the first)
     */
    public List<DiscordMessage> getChatHistoryWithinTokenLimit() {
        return tokenLimitedDiscordMessages;
    }

}
