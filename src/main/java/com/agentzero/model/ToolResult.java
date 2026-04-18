package com.agentzero.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolResult {
    private String toolName;
    private boolean success;
    private String output;
    private String error;
    private long executionTimeMs;
}
