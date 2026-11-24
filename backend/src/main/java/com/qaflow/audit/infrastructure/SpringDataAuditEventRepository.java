package com.qaflow.audit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAuditEventRepository extends JpaRepository<AuditEventEntity, Long> {
}

