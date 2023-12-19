package wood.handler;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import wood.commands.Chat;

@Slf4j
public class ModalHandler extends ListenerAdapter {

    private final Chat chatCmd;

    public ModalHandler(Chat chatCmd) {
        this.chatCmd = chatCmd;
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if(event.getModalId().equals(Chat.MODAL_ID)) {
            chatCmd.readModal(event);
        }
        else {
            log.error("Unknown modal ID: " + event.getModalId());
        }
    }

}
