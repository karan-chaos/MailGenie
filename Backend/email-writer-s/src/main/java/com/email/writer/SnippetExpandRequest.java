package com.email.writer;

import lombok.Data;
import java.util.Map;

/**
 * Request DTO for expanding a snippet with variable values.
 * Supports both direct expansion and AI-enhanced expansion.
 */
@Data
public class SnippetExpandRequest {

    /**
     * ID of the snippet to expand. Mutually exclusive with shortcut.
     */
    private Long snippetId;

    /**
     * Keyboard shortcut of the snippet to expand. Mutually exclusive with snippetId.
     */
    private String shortcut;

    /**
     * Variable values to substitute into the snippet body.
     * Key = variable name, Value = replacement text.
     */
    private Map<String, String> variables;

    /**
     * If true, the expanded content is further refined by an LLM using the snippet's tone.
     */
    private boolean aiEnhanced;

    /**
     * Provider to use for AI enhancement (groq, openai, gemini, claude).
     */
    private String provider;

    /**
     * Model override for AI enhancement.
     */
    private String model;

    /**
     * Optional API key override.
     */
    private String apiKey;

    /**
     * How this expansion was triggered: shortcut, api, dropdown, search.
     */
    private String triggerSource;
}
