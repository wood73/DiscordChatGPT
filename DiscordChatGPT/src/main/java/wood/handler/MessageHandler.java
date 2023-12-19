package wood.handler;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import wood.commands.Chat;
import wood.util.DiscordUtil;


public class MessageHandler extends ListenerAdapter {

    private final Chat chatCmd;


    public MessageHandler(Chat chatCmd) {
        this.chatCmd = chatCmd;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) {
            boolean isEphemeral = event.getMessage().isEphemeral();

            // if the bot that sent a message both isn't the first message and isn't ephemeral,
            // add emoji reaction allowing user to delete it (ReactionHandler).
            if(!isEphemeral && !DiscordUtil.isFirstMessageInThread(event, event.getMessage())) {
                event.getMessage().addReaction(Emoji.fromUnicode(ReactionHandler.trashEmoji)).queue();
            }
            return;
        }

        // if sent inside a /chat thread, handle it in the Chat class.
        if(event.getChannelType().isThread() && Chat.isChatThread(event.getThreadChannel().getIdLong())) {
            chatCmd.registerMessage(event.getThreadChannel().getIdLong(), event.getMessage().getContentDisplay(), event);
        }
    }

}
