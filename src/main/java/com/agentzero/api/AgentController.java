package com.agentzero.api;

import com.agentzero.agent.AgentEngine;
import com.agentzero.agent.AgentSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgentController {

    private final AgentEngine agentEngine;
    private final AgentSessionManager sessionManager;

    // ── Start a new pentest session ──────────────────────────────
    @PostMapping("/sessions")
    public ResponseEntity<?> startSession(@RequestBody StartSessionRequest request) {
        log.info("New session request: target={}:{}", request.targetIp(), request.targetPort());

        String sessionId = UUID.randomUUID().toString();
        sessionManager.createSession(sessionId, request.targetIp(), request.targetPort());

        // Launch async pentest
        agentEngine.runPentest(sessionId, request.targetIp(), request.targetPort());

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "status", "STARTED",
                "message", "AgentZero is now running. Connect to WebSocket for live updates.",
                "wsEndpoint", "/topic/session/" + sessionId
        ));
    }

    // ── Get session status & findings ────────────────────────────
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
        return sessionManager.getSession(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Stop a running session ───────────────────────────────────
    @PostMapping("/sessions/{sessionId}/stop")
    public ResponseEntity<?> stopSession(@PathVariable String sessionId) {
        sessionManager.stopSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session stop requested"));
    }

    // ── List all sessions ────────────────────────────────────────
    @GetMapping("/sessions")
    public ResponseEntity<?> listSessions() {
        return ResponseEntity.ok(sessionManager.getAllSessions());
    }

    // ── Get available targets ────────────────────────────────────
    @GetMapping("/targets")
    public ResponseEntity<?> getTargets() {
        return ResponseEntity.ok(Map.of("targets", sessionManager.getAvailableTargets()));
    }

    // ── Health check ─────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "AgentZero",
                "version", "1.0.0"
        ));
    }

    record StartSessionRequest(String targetIp, int targetPort, String targetName) {}
}
