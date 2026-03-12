package com.agentzero.agent;

import com.agentzero.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages in-memory session state + would persist to PostgreSQL in full implementation.
 * For now uses ConcurrentHashMap for thread-safe session tracking.
 */
@Slf4j
@Service
public class AgentSessionManager {

    // In-memory store — replace with JPA repositories for full persistence
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();
    private final Set<String> stoppedSessions = ConcurrentHashMap.newKeySet();

    public void createSession(String sessionId, String targetIp, int targetPort) {
        SessionData session = new SessionData(
                sessionId, targetIp, targetPort,
                "PENDING", LocalDateTime.now(), null,
                new ArrayList<>(), new ArrayList<>(), null
        );
        sessions.put(sessionId, session);
        log.info("Session created: {}", sessionId);
    }

    public void updateStatus(String sessionId, String status) {
        SessionData s = sessions.get(sessionId);
        if (s != null) {
            sessions.put(sessionId, new SessionData(
                    s.id(), s.targetIp(), s.targetPort(), status,
                    s.startedAt(), s.completedAt(), s.steps(), s.vulnerabilities(), s.summary()
            ));
        }
    }

    public void saveStep(String sessionId, int stepNum, String type, String content, String toolName) {
        SessionData s = sessions.get(sessionId);
        if (s != null) {
            s.steps().add(Map.of(
                    "step", stepNum,
                    "type", type,
                    "content", content != null ? content : "",
                    "tool", toolName != null ? toolName : "",
                    "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    public void extractAndSaveVulnerabilities(String sessionId, String toolName, ToolResult result) {
        if (result.getOutput() == null) return;
        String output = result.getOutput().toLowerCase();

        List<Map<String, String>> findings = new ArrayList<>();

        if (output.contains("vulnerable") || output.contains("sqli") || output.contains("sql syntax")) {
            findings.add(Map.of("name", "SQL Injection", "severity", "HIGH",
                    "tool", toolName, "evidence", result.getOutput().substring(0, Math.min(500, result.getOutput().length()))));
        }
        if (output.contains("valid credentials") || output.contains("✅")) {
            findings.add(Map.of("name", "Weak/Default Credentials", "severity", "CRITICAL",
                    "tool", toolName, "evidence", result.getOutput().substring(0, Math.min(500, result.getOutput().length()))));
        }
        if (output.contains("/admin") || output.contains("/config") || output.contains("/.env")) {
            findings.add(Map.of("name", "Sensitive Directory Exposed", "severity", "MEDIUM",
                    "tool", toolName, "evidence", result.getOutput().substring(0, Math.min(500, result.getOutput().length()))));
        }

        SessionData s = sessions.get(sessionId);
        if (s != null) s.vulnerabilities().addAll(findings);
    }

    public void complete(String sessionId, String summary) {
        SessionData s = sessions.get(sessionId);
        if (s != null) {
            sessions.put(sessionId, new SessionData(
                    s.id(), s.targetIp(), s.targetPort(), "COMPLETED",
                    s.startedAt(), LocalDateTime.now(), s.steps(), s.vulnerabilities(), summary
            ));
        }
    }

    public void stopSession(String sessionId) {
        stoppedSessions.add(sessionId);
        updateStatus(sessionId, "STOPPED");
    }

    public boolean isStopped(String sessionId) {
        return stoppedSessions.contains(sessionId);
    }

    public Optional<SessionData> getSession(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Collection<SessionData> getAllSessions() {
        return sessions.values();
    }

    public List<Map<String, String>> getAvailableTargets() {
        return List.of(
                Map.of("id", "dvwa", "name", "DVWA", "description",
                        "Damn Vulnerable Web Application", "image", "vulnerables/web-dvwa", "port", "80"),
                Map.of("id", "webgoat", "name", "WebGoat", "description",
                        "OWASP WebGoat", "image", "webgoat/goat-and-wolf", "port", "8080"),
                Map.of("id", "juiceshop", "name", "OWASP Juice Shop", "description",
                        "Modern vulnerable web app", "image", "bkimminich/juice-shop", "port", "3000")
        );
    }

    public record SessionData(
            String id,
            String targetIp,
            int targetPort,
            String status,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            List<Map<String, Object>> steps,
            List<Map<String, String>> vulnerabilities,
            String summary
    ) {}
}
