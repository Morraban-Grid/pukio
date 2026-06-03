package com.pukio.appserver.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pukio.appserver.dataaccess.AuditLogRepository;
import com.pukio.appserver.domain.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String userId, String operation, String entity, String entityId, 
                   Object before, Object after) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(operation);
            auditLog.setEntityType(entity);
            auditLog.setEntityId(entityId);
            
            String details = "";
            if (before != null) {
                details += "Before: " + objectMapper.writeValueAsString(before);
            }
            if (after != null) {
                if (!details.isEmpty()) details += " | ";
                details += "After: " + objectMapper.writeValueAsString(after);
            }
            auditLog.setDetails(details);
            auditLog.setTimestamp(LocalDateTime.now());
            
            auditLogRepository.save(auditLog);
            
            log.debug("Audit log created: operation={}, entity={}, entityId={}", operation, entity, entityId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log data", e);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }
}
