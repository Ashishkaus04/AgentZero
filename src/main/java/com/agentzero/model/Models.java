package com.agentzero.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ─────────────────────────────────────────────
// PentestSession — one full pentest run
// ─────────────────────────────────────────────
@Entity
@Table(name = "pentest_sessions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class PentestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String targetId;
    private String targetIp;
    private Integer targetPort;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String summary;

    @OneToMany(mappedBy = "sessionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AttackStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "sessionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Vulnerability> vulnerabilities = new ArrayList<>();

    public enum SessionStatus {
        PENDING, RUNNING, COMPLETED, FAILED, STOPPED
    }
}

// ─────────────────────────────────────────────
// AttackStep — every think/act/observe cycle
// ─────────────────────────────────────────────
@Entity
@Table(name = "attack_steps")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class AttackStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String sessionId;
    private Integer stepNumber;

    @Enumerated(EnumType.STRING)
    private StepType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String toolName;
    private LocalDateTime timestamp;

    public enum StepType {
        THINK, ACT, OBSERVE
    }
}

// ─────────────────────────────────────────────
// Vulnerability — found security issue
// ─────────────────────────────────────────────
@Entity
@Table(name = "vulnerabilities")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class Vulnerability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String sessionId;
    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(columnDefinition = "TEXT")
    private String remediation;

    private LocalDateTime discoveredAt;

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW, INFO
    }
}

// ─────────────────────────────────────────────
// Target — a Docker vulnerable environment
// ─────────────────────────────────────────────
@Entity
@Table(name = "targets")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class Target {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;
    private String description;
    private String dockerImage;
    private Integer exposedPort;
    private String containerId;

    @Enumerated(EnumType.STRING)
    private TargetStatus status;

    public enum TargetStatus {
        AVAILABLE, RUNNING, STOPPED, ERROR
    }
}

// ─────────────────────────────────────────────
// ToolResult — output from any pentest tool
// ─────────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class ToolResult {
    private String toolName;
    private boolean success;
    private String output;
    private String error;
    private long executionTimeMs;
}

// ─────────────────────────────────────────────
// ToolCall — LLM's decision to use a tool
// ─────────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class ToolCall {
    private String toolName;
    private String parameters;   // JSON string of params
    private String reasoning;    // why LLM chose this tool
}
