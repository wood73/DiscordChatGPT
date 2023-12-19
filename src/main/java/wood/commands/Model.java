package wood.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import wood.Settings;
import wood.util.GPTRequest;
import wood.util.GPTUtil;

import java.util.Arrays;
import java.util.Locale;

// TODO make buttons for user to select model
public class Model extends Commands {

    /** The name of the required argument inside the command (must be lowercase, and without whitespace). */
    private final String commandOptionName = "model";
    /** The description of the required argument inside the command. Must not exceed 100 characters. */
    private final String commandOptionDescription = "e.g. " + GPTRequest.gptTurbo + ", or " + GPTRequest.gpt4;

    public Model() {
        super.name = "model";
        super.description = "The default language model to use for OpenAI API calls";
    }

    @Override
    public void runCommand(long userId, SlashCommandInteractionEvent event) {
        String modelArg = event.getOption(commandOptionName).getAsString();
        boolean validModel = Arrays.stream(GPTRequest.models).anyMatch(s -> s.equalsIgnoreCase(modelArg));

        if(validModel) {
            Settings.model = modelArg.toLowerCase(Locale.ROOT);
            event.reply("Model set to " + Settings.model).queue();
        }
        else {
            event.reply("'" + modelArg + "' is an Invalid model.\nValid models are: " + GPTUtil.listModels())
                    .setEphemeral(true).queue();
        }
    }

    @Override
    public void addCommand(JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.STRING, commandOptionName, commandOptionDescription, true)
                .complete();
    }

    @Override
    public String getDescription() {
        return description;
    }

}