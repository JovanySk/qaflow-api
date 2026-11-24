package com.qaflow.audit.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class DeferredAuditEventRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeferredAuditEventRecorder.class);

    private final AuditEventRecorder auditEventRecorder;

    public DeferredAuditEventRecorder(AuditEventRecorder auditEventRecorder) {
        this.auditEventRecorder = auditEventRecorder;
    }

    public void recordAfterCurrentTransaction(AuditEventCommand command) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    recordSafely(command);
                }
            });
            return;
        }
        recordSafely(command);
    }

    private void recordSafely(AuditEventCommand command) {
        try {
            auditEventRecorder.recordInIsolatedTransaction(command);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Deferred audit persistence failed action={} correlationId={} exceptionType={}",
                    command.action(),
                    command.correlationId(),
                    exception.getClass().getName());
        }
    }
}
