package com.qaflow.audit.infrastructure;

import java.time.Instant;
import java.util.Map;

import com.qaflow.audit.application.AuditEventCommand;
import com.qaflow.audit.domain.AuditOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_events")
class AuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_username_snapshot", length = 100)
    private String actorUsernameSnapshot;

    @Column(name = "actor_role_snapshot", length = 32)
    private String actorRoleSnapshot;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType;

    @Column(name = "resource_id", length = 64)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 16)
    private AuditOutcome outcome;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary", columnDefinition = "json")
    private Map<String, Object> summary;

    @Column(name = "request_path", length = 255)
    private String requestPath;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant occurredAt;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    protected AuditEventEntity() {
    }

    private AuditEventEntity(AuditEventCommand command, Instant occurredAt) {
        this.actorUserId = command.actorUserId();
        this.actorUsernameSnapshot = command.actorUsernameSnapshot();
        this.actorRoleSnapshot = command.actorRoleSnapshot();
        this.action = command.action();
        this.resourceType = command.resourceType();
        this.resourceId = command.resourceId();
        this.outcome = command.outcome();
        this.summary = command.summary();
        this.requestPath = command.requestPath();
        this.occurredAt = occurredAt;
        this.correlationId = command.correlationId();
    }

    static AuditEventEntity from(AuditEventCommand command, Instant occurredAt) {
        return new AuditEventEntity(command, occurredAt);
    }

    Long id() {
        return id;
    }
}

