package wood.handler;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import wood.util.DiscordUtil;

public class ReactionHandler extends ListenerAdapter {

    public static final String trashEmoji = "\uD83D\uDDD1";

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if(event.getUser().isBot()) return;

        boolean isTrashEmoji = event.getReaction().getEmoji().getAsReactionCode().equals(trashEmoji);

        Message msgReactedTo = event.getChannel().retrieveMessageById(event.getMessageId()).complete();

        // If the reaction is trash emoji, and isn't the first message in the thread, delete the message.
        if (isTrashEmoji && !DiscordUtil.isFirstMessageInThread(event, msgReactedTo)) {
            event.getChannel().retrieveMessageById(event.getMessageId()).complete().delete().queue();
        }
    }
}
