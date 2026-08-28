package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.entity.AuditEvent;
import com.abikananda.lendenclub.repository.AuditEventRepository;
import com.abikananda.lendenclub.util.CorrelationIdUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void logEvent(String eventType, String sessionId, String loanId, String message) {
        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .correlationId(CorrelationIdUtil.getCorrelationId())
                .sessionId(sessionId)
                .loanId(loanId)
                .message(message)
                .build();
        auditEventRepository.save(event);
    }
}
