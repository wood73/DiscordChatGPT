package wood;

import wood.util.GPTRequest;

// TODO use a JSON file to store settings
public class Settings {

    /** The default model to use for all API calls. */
    public static String model = GPTRequest.gptTurbo;

    /** The maximum cost USD of a prompt's tokens */
    public static double maxPromptCostPerAPIRequest = .025;

    /** In the /chat command, the number of tokens used in the API request's completion  */
    public static int chatCompletionTokens = 130;

}