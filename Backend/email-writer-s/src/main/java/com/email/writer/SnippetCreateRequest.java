package com.email.writer;

import lombok.Data;

/**
 * Request DTO for creating or updating an email snippet.
 */
@Data
public class SnippetCreateRequest {

    private String title;
    private String body;
    private String category;
    private String shortcut;
    private String requiredVariables;
    private String defaultVariableValues;
    private String tone;
    private String language;
    private Boolean pinned;
    private Boolean shared;
}
