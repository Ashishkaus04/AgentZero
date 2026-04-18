package com.agentzero.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "targets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Target {

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
