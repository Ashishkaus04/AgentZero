package com.agentzero.websocket;

import com.agentzero.model.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TOPIC = "/topic/session/";

    public void publishSessionStart(String sessionId, String targetIp, int targetPort) {
        publish(sessionId, AgentEvent.builder()
                .type("SESSION_START")
                .sessionId(sessionId)
                .message("🚀 AgentZero starting pentest on " + targetIp + ":" + targetPort)
                .timestamp(LocalDateTime.now().toString())
                .build());
    }

    public void publishThink(String sessionId, int step, String reasoning) {
        publish(sessionId, AgentEvent.builder()
                .type("THINK")
                .sessionId(sessionId)
                .step(step)
                .message("🧠 " + reasoning)
                .timestamp(LocalDateTime.now().toString())
                .build());
    }

    public void publishAct(String sessionId, int step, String toolName, String params) {
        publish(sessionId, AgentEvent.builder()
                .type("ACT")
                .sessionId(sessionId)
                .step(step)
                .toolName(toolName)
                .message("⚡ Executing tool: " + toolName)
                .data(params)
                .timestamp(LocalDateTime.now().toString())
                .build());
    }

    public void publishObserve(String sessionId, int step, String toolName, ToolResult result) {
        publish(sessionId, AgentEvent.builder()
                .type("OBSERVE")
                .sessionId(sessionId)
                .step(step)
                .toolName(toolName)
                .message("👁️  Result from: " + toolName)
                .data(result.isSuccess() ? result.getOutput() : "ERROR: " + result.getError())
                .success(result.isSuccess())
                .timestamp(LocalDateTime.now().toString())
                .build());
    }

    public void publishVulnerabilityFound(String sessionId, String vulnName, String severity) {
        publish(sessionId, AgentEvent.builder()
                .type("VULNERABILITY_FOUND")
                .sessionId(sessionId)
                .message("🚨 Vulnerability found: " + vulnName + " [" + severity + "]")
                .data(severity)
                .timestamp(LocalDateTime.now().toString())
                .build());
    }

    public void publishDone(String sessionId, String summary) {
        publish(sessionId, AgentEvent.builder()
                .type("DONE")
                .sessionId(sessionId)
                .message("✅ Assessment complete")
                .data(summary)
                .timestamp(LocalDateTime.now().toString())
                .build());
    }

    public void publishStopped(String sessionId) {
        publish(sessionId, AgentEvent.builder()
                .type("STOPPED")
                .sessionId(sessionId)
                .message("⏹️  Session stopped by user")
                .timestamp(LocalDateTime.now().toString())
                .build());
    }

    public void publishError(String sessionId, String error) {
        publish(sessionId, AgentEvent.builder()
                .type("ERROR")
                .sessionId(sessionId)
                .message("❌ Error: " + error)
                .timestamp(LocalDateTime.now().toString())
                .build());
    }

    private void publish(String sessionId, AgentEvent event) {
        try {
            messagingTemplate.convertAndSend(TOPIC + sessionId, event);
            log.debug("Published event type={} session={}", event.getType(), sessionId);
        } catch (Exception e) {
            log.error("Failed to publish event: {}", e.getMessage());
        }
    }

    // ─── Event DTO ────────────────────────────────────────────────
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AgentEvent {
        private String type;
        private String sessionId;
        private Integer step;
        private String toolName;
        private String message;
        private String data;
        private Boolean success;
        private String timestamp;
    }
}
