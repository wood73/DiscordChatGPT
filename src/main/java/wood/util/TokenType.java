package wood.util;

/** The type of a LLM token
    PROMPT: tokens in a prompt (sent to a LLM)
    COMPLETION: tokens in a completion (generated by a LLM)
    UNDEFINED: token type not specified
 */
public enum TokenType {
    PROMPT, COMPLETION, UNDEFINED
}
