package com.qaflow.audit.infrastructure;

import java.time.Clock;
import java.time.Instant;

import com.qaflow.audit.application.AuditEventCommand;
import com.qaflow.audit.application.AuditEventRecorder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaAuditEventRecorder implements AuditEventRecorder {

    private final SpringDataAuditEventRepository repository;
    private final Clock clock;

    JpaAuditEventRecorder(SpringDataAuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public long recordWithinBusinessTransaction(AuditEventCommand command) {
        return persist(command);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long recordInIsolatedTransaction(AuditEventCommand command) {
        return persist(command);
    }

    private long persist(AuditEventCommand command) {
        AuditEventEntity entity = AuditEventEntity.from(command, Instant.now(clock));
        return repository.saveAndFlush(entity).id();
    }
}

