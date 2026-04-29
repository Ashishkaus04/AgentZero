package com.agentzero.agent;

import com.agentzero.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AgentSessionManager {

    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();
    private final Set<String> stoppedSessions = ConcurrentHashMap.newKeySet();

    public void createSession(String sessionId, String targetIp, int targetPort) {
        sessions.put(sessionId, new SessionData(
                sessionId, targetIp, targetPort, "PENDING",
                LocalDateTime.now(), null, new ArrayList<>(), new ArrayList<>(), null));
        log.info("Session created: {}", sessionId);
    }

    public void updateStatus(String sessionId, String status) {
        SessionData s = sessions.get(sessionId);
        if (s != null) {
            sessions.put(sessionId, new SessionData(
                    s.id(), s.targetIp(), s.targetPort(), status,
                    s.startedAt(), s.completedAt(), s.steps(), s.vulnerabilities(), s.summary()));
        }
    }

    public void saveStep(String sessionId, int stepNum, String type, String content, String toolName) {
        SessionData s = sessions.get(sessionId);
        if (s != null) {
            s.steps().add(Map.of(
                    "step", stepNum, "type", type,
                    "content", content != null ? content : "",
                    "tool", toolName != null ? toolName : "",
                    "timestamp", LocalDateTime.now().toString()));
        }
    }

    public void extractAndSaveVulnerabilities(String sessionId, String toolName, ToolResult result) {
        if (result.getOutput() == null) return;
        String output = result.getOutput().toLowerCase();
        String evidence = result.getOutput().substring(0, Math.min(300, result.getOutput().length()));
        SessionData s = sessions.get(sessionId);
        if (s == null) return;

        // SQL Injection — only on actual SQL error signatures or confirmed sqli_test hit
        if (output.contains("sql syntax") || output.contains("mysql_fetch")
                || output.contains("ora-") || output.contains("sqlstate")
                || (output.contains("vulnerable with payload") && toolName.equals("sqli_test"))) {
            addVulnIfNotDuplicate(s, "SQL Injection", "HIGH", toolName, evidence);
        }

        // Weak credentials — ONLY when brute_force actually finds valid ones (✅ marker)
        if (output.contains("valid credentials found") || output.contains("✅")
                || (output.contains("valid:") && !output.contains("no valid"))) {
            addVulnIfNotDuplicate(s, "Weak/Default Credentials", "CRITICAL", toolName, evidence);
        }

        // Sensitive files — only when dir_fuzz finds accessible sensitive paths
        if (toolName.equals("dir_fuzz") && output.contains("found") && output.contains("paths:")
                && (output.contains("/.env") || output.contains("/.git")
                || output.contains("/config") || output.contains("/backup"))) {
            addVulnIfNotDuplicate(s, "Sensitive File Exposed", "MEDIUM", toolName, evidence);
        }

        // Directory listing enabled
        if (output.contains("index of /") || output.contains("directory listing")) {
            addVulnIfNotDuplicate(s, "Directory Listing Enabled", "MEDIUM", toolName, evidence);
        }

        // Sensitive admin endpoints exposed
        if (toolName.equals("dir_fuzz") && output.contains("[200]")
                && (output.contains("/admin") || output.contains("/phpmyadmin")
                || output.contains("/actuator/env") || output.contains("/console"))) {
            addVulnIfNotDuplicate(s, "Sensitive Admin Endpoint Exposed", "HIGH", toolName, evidence);
        }
    }

    // Prevents duplicate findings for the same vulnerability name
    private void addVulnIfNotDuplicate(SessionData s, String name, String severity,
                                       String toolName, String evidence) {
        boolean exists = s.vulnerabilities().stream()
                .anyMatch(v -> name.equals(v.get("name")));
        if (!exists) {
            s.vulnerabilities().add(Map.of(
                    "name", name, "severity", severity,
                    "tool", toolName, "evidence", evidence));
            log.info("Vulnerability found: {} [{}] via {}", name, severity, toolName);
        }
    }

    public void complete(String sessionId, String summary) {
        SessionData s = sessions.get(sessionId);
        if (s != null) {
            sessions.put(sessionId, new SessionData(
                    s.id(), s.targetIp(), s.targetPort(), "COMPLETED",
                    s.startedAt(), LocalDateTime.now(), s.steps(), s.vulnerabilities(), summary));
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
                Map.of("id", "dvwa", "name", "DVWA",
                        "description", "Damn Vulnerable Web Application",
                        "image", "vulnerables/web-dvwa", "port", "80"),
                Map.of("id", "webgoat", "name", "WebGoat",
                        "description", "OWASP WebGoat",
                        "image", "webgoat/goat-and-wolf", "port", "8080"),
                Map.of("id", "juiceshop", "name", "OWASP Juice Shop",
                        "description", "Modern vulnerable web app",
                        "image", "bkimminich/juice-shop", "port", "3000")
        );
    }

    public record SessionData(
            String id, String targetIp, int targetPort, String status,
            LocalDateTime startedAt, LocalDateTime completedAt,
            List<Map<String, Object>> steps,
            List<Map<String, String>> vulnerabilities,
            String summary) {}
}