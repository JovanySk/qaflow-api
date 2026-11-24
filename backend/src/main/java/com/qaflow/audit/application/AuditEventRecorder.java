package com.qaflow.audit.application;

public interface AuditEventRecorder {

    long recordWithinBusinessTransaction(AuditEventCommand command);

    long recordInIsolatedTransaction(AuditEventCommand command);
}

