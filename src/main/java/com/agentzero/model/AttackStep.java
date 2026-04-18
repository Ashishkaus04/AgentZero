package com.agentzero.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attack_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttackStep {

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
