package wood.util;

// import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import retrofit2.HttpException;
import wood.message.DiscordMessage;
import wood.message.MessageHistory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** A wrapper class for com.theokanning.openai. Requires GPTRequest.apiKey to be set. */
@Slf4j
public class GPTRequest {

    // ----------- static fields -----------

    /** The OpenAI API key to use for all requests. Can set using the testAndSetApiKey method. */
    public static String apiKey = "";

    /** Language models */
    public static final String gptTurbo = "gpt-3.5-turbo", gpt4 = "gpt-4";

    public static final String[] models = {gptTurbo, gpt4};

    /** counter for how many tokens have been used by each language model (irrespective of Base series vs Instruct) */
    private static int gptTurboTokenCounter = 0, gpt4PromptTokenCounter = 0, gpt4CompletionTokenCounter = 0;


    // ----------- instance fields -----------

    private final OpenAiService service;
    private final ChatCompletionRequest chatCompletionRequest;
    private final ChatCompletionRequest.ChatCompletionRequestBuilder chatCompletionRequestBuilder;
    //private final CompletionRequest completionRequest;
    //private final CompletionRequest.CompletionRequestBuilder completionRequestBuilder;

    /** List of all chat messages used in API request. */
    @Getter private final MessageHistory messageHistory = new MessageHistory();

    /** Language Model to use for this API request */
    @Getter private final String model;

    /** Maximum number of tokens that will be generated */
    @Getter private final int maxTokens;

    /** (default .7) a value 0-1 with 1 being very creative, 0 being very factual/deterministic */
    @Getter private final double temperature;

    /** (default 1) between 0-1 where 1.0 means "use all tokens in the vocabulary"
     *  while 0.5 means "use only the 50% most common tokens" */
    @Getter private final double topP;

    /** (default 0) 0-1, lowers the chances of a word being selected again the more times that word has already been used */
    @Getter private final double frequencyPenalty;

    /** (default 0) 0-1, lowers the chances of topic repetition */
    @Getter private final double presencePenalty;

    /** (default 1), queries GPT-3 this many times, then selects the 'best' generation to return */
    @Getter private final int bestOf;

    /** The Strings that GPT-3 will stop generating after (can have 4 stop sequences max) */
    @Getter private final List<String> stopSequences;

    /** The latest generated completion */
    @Getter private ChatMessage latestCompletion;

    public GPTRequest(GPTRequestBuilder builder) {
        for(DiscordMessage dm : builder.messageHistory.getDiscordMessages())
            this.messageHistory.add(dm);
        this.model = builder.model;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.presencePenalty = builder.presencePenalty;
        this.bestOf = builder.bestOf;
        this.stopSequences = builder.stopSequences;

        service = new OpenAiService(apiKey, Duration.ofSeconds(60));

        // Roles: user, assistant, system
        // system prompt: "You are..."
        chatCompletionRequestBuilder = ChatCompletionRequest.builder()
                .messages(messageHistory.getChatMessages())
                .model(model)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .topP(topP)
                .frequencyPenalty(frequencyPenalty)
                .presencePenalty(presencePenalty);

        if(stopSequences != null)
            chatCompletionRequestBuilder.stop(stopSequences);

        chatCompletionRequest = chatCompletionRequestBuilder.build();

        /*

        completionRequestBuilder = CompletionRequest.builder()
                .prompt(prompt)
                .model(model);

        completionRequestBuilder.maxTokens(maxTokens);
        completionRequestBuilder.temperature(temperature);
        completionRequestBuilder.topP(topP);
        completionRequestBuilder.frequencyPenalty(frequencyPenalty);
        completionRequestBuilder.presencePenalty(presencePenalty);
        completionRequestBuilder.echo(echoPrompt);
        if(stopSequences != null)
            completionRequestBuilder.stop(stopSequences);

        completionRequest = completionRequestBuilder.build();

        */
    }

    // TODO remove print statments & stuff
    /**
     * Tests the API key, and sets it if it's valid
     * API key validity is tested by a 1 token API request to the Ada model.
     * @param apiKey An OpenAI API key
     * @return Whether the API key is valid
     */
    public static boolean testAndSetApiKey(String apiKey) {
        String originalAPIKey = GPTRequest.apiKey;
        try {
            GPTRequest.apiKey = apiKey;
            new GPTRequestBuilder(gptTurbo, 1, DiscordMessage.EMPTY_MSG).build().request(
                    true, Optional.empty(), DiscordMessage.EMPTY_MSG);
            System.out.println("true");
            return true;
        }catch(Exception e) {
            e.printStackTrace();
            GPTRequest.apiKey = originalAPIKey;
            System.out.println("false");
            return false;
        }
    }

    //TODO update request(boolean endAtLastPunctuationMark), and complete javadoc vv
    /*
     * Makes an OpenAI API request.
     * @param message Message to append to {@link #messageHistory} before making the API request
     * @param username A username associated with the ChatMessage
     * @param endAtLastPunctuationMark Whether the completion should be cut off after the last punctuation mark
     * @return list of all messages from prompt and completion

    public MessageHistory request(ChatMessage message, Optional<String> username, boolean endAtLastPunctuationMark) {
        if(message != null && message.getContent().length() != 0)
            messageHistory.add(message, username);
        chatCompletionRequest.setMessages(messageHistory.getMessagesIncludingUsername());

        List<ChatCompletionChoice> outputList = null;
        try {
            outputList = service.createChatCompletion(chatCompletionRequest).getChoices();
            latestCompletion = outputList.get(0).getMessage();
            GPTUtil.removeNamePrefix(latestCompletion);
            String completion = latestCompletion.getContent();

            if(endAtLastPunctuationMark) {
                // get the index of the last punctuation mark inside the completion
                Optional<Integer> lastPunctuationIndex = StringUtil.lastIndexOf(completion, "[.!?]", 0);

                if(lastPunctuationIndex.isPresent()) {
                    latestCompletion.setContent(completion.substring(0, lastPunctuationIndex.get() + 1));
                }
            }

            chatCompletionRequest.getMessages().add(latestCompletion);
            messageHistory.add(latestCompletion, Optional.empty());
        } catch(HttpException e) {
            System.out.println("HTTP error message: " + e.getMessage());
            System.out.println("HTTP message: " + e.message());
        }

        return messageHistory;
    }*/

    //TODO remove
    public void requestTest() {
        List<ChatCompletionChoice> outputList = null;
        DiscordMessage dm = new DiscordMessage(Optional.of("rand"),
                new ChatMessage("system", "you are a helpful data science tutor meeting with a student."));
        List<ChatMessage> cms = new ArrayList<ChatMessage>();
        cms.add((ChatMessage)dm);
        chatCompletionRequest.setMessages(cms);
        try {
            outputList = service.createChatCompletion(chatCompletionRequest).getChoices();
            chatCompletionRequest.getMessages().add(outputList.get(0).getMessage());
        } catch(HttpException e) {
            System.out.println("HTTP error message: " + e.getMessage());
            System.out.println("HTTP message: " + e.message());
        }

        for(ChatCompletionChoice output : outputList)
            System.out.println(output.getMessage().getRole() + ": " + output.getMessage().getContent() +
                    "\n#############################");

        for(ChatMessage message : chatCompletionRequest.getMessages()) {
            System.out.println(message.getRole() + ": " + message.getContent() +
                    "\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        }

        System.out.println("*******************************");

        chatCompletionRequest.getMessages().add(new ChatMessage(GPTUtil.roleUser, "Thanks, next can you " +
                "help me understand how many hidden layers a given neural network should have?"));

        try {
            outputList = service.createChatCompletion(chatCompletionRequest).getChoices();
            chatCompletionRequest.getMessages().add(outputList.get(0).getMessage());
        } catch(HttpException e) {
            System.out.println("HTTP error message: " + e.getMessage());
            System.out.println("HTTP message: " + e.message());
        }

        for(ChatCompletionChoice output : outputList)
            System.out.println(output.getMessage().getRole() + ": " + output.getMessage().getContent() +
                    "\n#############################");

        for(ChatMessage message : chatCompletionRequest.getMessages()) {
            System.out.println(message.getRole() + ": " + message.getContent() +
                    "\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        }
    }

    /**
     * Makes an OpenAI API request.
     * @param endAtLastPunctuationMark Whether the completion should be cut off after the last punctuation mark
     * @param botsUsername The username to be associated with the LLM generated response
     * @param discordMessages Messages to append to {@link #messageHistory} before making the API request
     * @return list of all messages from prompt and completion
     */
    public MessageHistory request(boolean endAtLastPunctuationMark, Optional<String> botsUsername,
                                     DiscordMessage... discordMessages) {

        for(DiscordMessage message : discordMessages)
            messageHistory.add(message);

        chatCompletionRequest.setMessages(messageHistory.getChatMessages());

        List<ChatCompletionChoice> outputList = null;
        try {
            // TODO remove
            /*
            List<ChatMessage> cmList = chatCompletionRequest.getMessages();
            for(ChatMessage cm : cmList) {
                System.out.println("~ChatMessage:  Role- " + cm.getRole() + " | Content- " + cm.getContent() +
                        " | name- " + cm.getName() + " | function- " + cm.getFunctionCall());
            }
            chatCompletionRequest.setMessages(chatCompletionRequest.getMessages().subList(0, 1));
            */
            outputList = service.createChatCompletion(chatCompletionRequest).getChoices();
            latestCompletion = outputList.get(0).getMessage();
            GPTUtil.removeNamePrefix(latestCompletion);
            String completion = latestCompletion.getContent();

            if(endAtLastPunctuationMark) {
                // get the index of the last punctuation mark inside the completion
                Optional<Integer> lastPunctuationIndex = StringUtil.lastIndexOf(completion, "[.!?]", 0);

                if(lastPunctuationIndex.isPresent()) {
                    latestCompletion.setContent(completion.substring(0, lastPunctuationIndex.get() + 1));
                }
            }

            chatCompletionRequest.getMessages().add(latestCompletion);
            messageHistory.add(new DiscordMessage(botsUsername, latestCompletion));
        } catch(HttpException e) {
            System.out.println("HTTP error message: " + e.getMessage());
            System.out.println("HTTP message: " + e.message());
        }

        return messageHistory;
    }

    /** Updates {@link #latestCompletion} and {@link #messageHistory}
     * @param content
     */
    public void setLatestCompletion(String content) {
        latestCompletion.setContent(content);
        List<DiscordMessage> messages = messageHistory.getDiscordMessages();
        messages.get(messages.size() - 1).setContent(latestCompletion.getContent());

        chatCompletionRequest.setMessages(messageHistory.getChatMessages());
    }

    public static class GPTRequestBuilder {

        /** Language Model to use for this API request */
        @Getter
        private String model;

        /** List of all chat messages used in API request */
        @Getter private MessageHistory messageHistory;

        /** Maximum number of tokens that will be generated */
        @Getter private int maxTokens;

        /** (default .7) a value 0-1 with 1 being very creative, 0 being very factual/deterministic */
        @Getter private double temperature;

        /** (default 1) between 0-1 where 1.0 means "use all tokens in the vocabulary"
         *  while 0.5 means "use only the 50% most common tokens" */
        @Getter private double topP;

        /** (default 0) 0-1, lowers the chances of a word being selected again the more times that word has already been used */
        @Getter private double frequencyPenalty;

        /** (default 0) 0-1, lowers the chances of topic repetition */
        @Getter private double presencePenalty;

        /** (default 1), queries GPT-3 this many times, then selects the 'best' generation to return */
        @Getter private int bestOf;

        /** The Strings that GPT-3 will stop generating after (can have 4 stop sequences max) */
        @Getter private List<String> stopSequences;

        /** (default true) Whether messages generated by LLM should be appended to {@link GPTRequest#messageHistory} */
        @Getter private boolean appendGeneratedMessages;

        /**
         * Starts to build an API request for the given language model
         *
         * @param model Language model to use for this API request. Valid models: GPTRequest.gptTurbo, GPTRequest.gpt4
         * @param maxTokens Maximum number of tokens that will be generated
         * @param messages List of all chat messages used in API request
         */
        public GPTRequestBuilder(String model, int maxTokens, DiscordMessage... messages) {
            this.model = model;
            this.messageHistory = new MessageHistory();
            for(DiscordMessage discordMessage : messages)
                messageHistory.add(discordMessage);
            this.maxTokens = maxTokens;
            this.temperature = .7;
            this.topP = 1;
            this.frequencyPenalty = 0;
            this.presencePenalty = 0;
            this.bestOf = 1;
            this.appendGeneratedMessages = true;
        }

        public GPTRequest build() {
            return new GPTRequest(this);
        }

        /** @param messages List of all chat messages used in API request
         *  @return This GPTRequestBuilder, for chaining */
        public GPTRequestBuilder messages(DiscordMessage... messages) {
            this.messageHistory = new MessageHistory();
            for(DiscordMessage message : messages)
                this.messageHistory.add(message);
            return this;
        }

        /** @param maxTokens Maximum number of tokens that will be generated
         *  @param messages List of all chat messages used in API request
         *  @return This GPTRequestBuilder, for chaining */
        public GPTRequestBuilder promptAndTokens(int maxTokens, DiscordMessage... messages) {
            this.messageHistory = new MessageHistory();
            for(DiscordMessage message : messages)
                this.messageHistory.add(message);
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * @param model Language model to use for this API request. Valid Base Series models:
         *               UtilGPT.davinci, UtilGPT.curie, UtilGPT.babbage, UtilGPT.ada
         *               Valid Instruct Series models:
         *               UtilGPT.inDavinci, UtilGPT.inCurie, UtilGPT.inBabbage, UtilGPT.inAda
         * @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * @param maxTokens Maximum number of tokens that will be generated
         * @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /** @param temperature (default .7) a value 0-1 with 1 being very creative, 0 being very factual/deterministic
         *  @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        /** @param topP (default 1) between 0-1 where 1.0 means "use all tokens in the vocabulary"
         *               while 0.5 means "use only the 50% most common tokens"
         *  @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder topP(double topP) {
            this.topP = topP;
            return this;
        }

        /** @param frequencyPenalty (default 0) 0-1, lowers the chances of a word being selected again
         *                           the more times that word has already been used
         *  @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder frequencyPenalty(double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        /** @param presencePenalty (default 0) 0-1, lowers the chances of topic repetition
         *  @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder presencePenalty(double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        /** @param bestOf (default 1), queries GPT-3 this many times, then selects the 'best' generation to return
         *  @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder bestOf(int bestOf) {
            this.bestOf = bestOf;
            return this;
        }

        /**
         * set the stop sequence, the String that GPT-3 will stop generating after
         *     (can have 4 stop sequences max)
         * @param stopSequences The Strings that GPT-3 will stop generating after (can have 4 stop sequences max)
         * @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder stopSequences(List<String> stopSequences) {
            if(stopSequences.size() > 4)
                throw new IllegalArgumentException("Can only have 4 stop sequences max");
            else
                this.stopSequences = stopSequences;
            return this;
        }

        /** @param appendGeneratedMessages (default true) Whether messages generated by LLM should be appended to
         *                           {@link GPTRequest#messageHistory}
         *  @return This GPTRequestBuilder, for chaining
         */
        public GPTRequestBuilder appendGeneratedMessages(boolean appendGeneratedMessages) {
            this.appendGeneratedMessages = appendGeneratedMessages;
            return this;
        }

    }

    /**
     * For any model except GPT-4 (in which case, use logGPT4TokenUsage() )
     * Logs the token usage every time request() is called.
     * @param numTokens The number of tokens used in this API request.
     * @throws RuntimeException if GPT-4 is the current model when calling this method (logGPT4TokenUsage() should be used instead)
     */
    private void logTokenUsage(int numTokens) {
        switch(model) {
            case gptTurbo:
                gptTurboTokenCounter += numTokens;
                break;
            case gpt4:
                throw new RuntimeException("GPTRequest.logTokenUsage() should not be used with" +
                        " GPT-4, logGPT4TokenUsage() should be used instead.");
        }

        log.info(getFormattedTokenUsage());
    }

    /**
     * Exclusively for GPT-4 (since prompt and completion tokens need to be separately logged)
     * Logs the token usage every time request() is called.
     * @param numPromptTokens The number of prompt tokens used in this API request.
     * @param numCompletionTokens The number of completion tokens used in this API request.
     * @throws RuntimeException If this method is called using a model other than GPT-4 (in which case use logTokenUsage() instead)
     */
    private void logGPT4TokenUsage(int numPromptTokens, int numCompletionTokens) {
        if(!model.equals(GPTRequest.gpt4)) {
            throw new RuntimeException("GPTRequest.logGPT4TokenUsage() should only be used with GPT-4, use logTokenUsage instead.");
        }

        gpt4PromptTokenCounter += numPromptTokens;
        gpt4CompletionTokenCounter += numCompletionTokens;

        log.info(getFormattedTokenUsage());
    }

    /** @return a String containing all token usage data */
    private String getFormattedTokenUsage() {
        return String.format("Total tokens used:%n%s%s%s%s%s%s-----------------------------------------%n",
                gptTurboTokenCounter > 0 ? "GPT 3.5: " + gptTurboTokenCounter + " token" + (gptTurboTokenCounter > 1 ? "s\n" : "\n") : "",
                gpt4PromptTokenCounter > 0 ? "GPT-4 prompts: " + gpt4PromptTokenCounter + " token" + (gpt4PromptTokenCounter > 1 ? "s\n" : "\n") : "",
                gpt4CompletionTokenCounter > 0 ? "GPT-4 completions: " + gpt4CompletionTokenCounter + " token" + (gpt4CompletionTokenCounter > 1 ? "s\n" : "\n") : "");
    }


}
