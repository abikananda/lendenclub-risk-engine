package com.abikananda.lendenclub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_event")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "loan_id", length = 64)
    private String loanId;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
