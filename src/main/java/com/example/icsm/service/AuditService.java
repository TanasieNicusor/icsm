package com.example.icsm.service;

import com.example.icsm.model.ChangeLog;
import com.example.icsm.repository.ChangeLogRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import com.example.icsm.util.SpringContext;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final ChangeLogRepository changeLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Flag to suppress audit listener during rollback to avoid duplicate logs
    public static final ThreadLocal<Boolean> IS_ROLLING_BACK = ThreadLocal.withInitial(() -> false);

    public String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "System";
    }

    @Transactional
    public void logChange(String entityName, Long entityId, String actionType, String fieldName, String oldValue, String newValue) {
        ChangeLog logEntry = ChangeLog.builder()
                .entityName(entityName)
                .entityId(entityId)
                .actionType(actionType)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(getCurrentUser())
                .build();
        changeLogRepository.save(logEntry);
    }

    public List<ChangeLog> getLogs(String entityName, String changedBy, LocalDateTime startDate, LocalDateTime endDate) {
        return changeLogRepository.findByFilters(entityName, changedBy, startDate, endDate);
    }

    @Transactional
    public void rollbackChange(Long logId) {
        IS_ROLLING_BACK.set(true);
        try {
            ChangeLog logEntry = changeLogRepository.findById(logId)
                    .orElseThrow(() -> new IllegalArgumentException("Change log not found"));

            if (logEntry.getActionType().endsWith("_ROLLED_BACK")) {
                throw new IllegalStateException("This change has already been rolled back.");
            }

            Class<?> entityClass = Class.forName("com.example.icsm.model." + logEntry.getEntityName());
            Object entity = entityManager.find(entityClass, logEntry.getEntityId());

            if (entity == null) {
                if ("DELETE".equals(logEntry.getActionType())) {
                    throw new IllegalStateException("Cannot rollback a DELETE action - record is already gone.");
                }
                throw new IllegalStateException("Entity not found for rollback");
            }

            if (logEntry.getActionType().startsWith("UPDATE")) {
                Field field = getField(entityClass, logEntry.getFieldName());
                field.setAccessible(true);
                
                Object valueToRestore = convertStringToType(logEntry.getOldValue(), field.getType());
                field.set(entity, valueToRestore);
                entityManager.merge(entity);
            } else if (logEntry.getActionType().startsWith("CREATE")) {
                entityManager.remove(entity);
            }
            
            // Mark original log as rolled back by appending suffix
            logEntry.setActionType(logEntry.getActionType() + "_ROLLED_BACK");
            changeLogRepository.save(logEntry);

            // Force flush while IS_ROLLING_BACK is still true
            entityManager.flush();
            
            // Log the rollback itself
            String fieldName = logEntry.getActionType().contains("CREATE") ? "ALL" : logEntry.getFieldName();
            String oldVal = logEntry.getActionType().contains("CREATE") ? "Record Deleted" : logEntry.getNewValue();
            String newVal = logEntry.getActionType().contains("CREATE") ? "Reverted Creation" : logEntry.getOldValue();
            
            logChange(logEntry.getEntityName(), logEntry.getEntityId(), "ROLLBACK", 
                    fieldName, oldVal, newVal);
            
        } catch (Exception e) {
            log.error("Failed to rollback change", e);
            throw new RuntimeException("Rollback failed: " + e.getMessage());
        } finally {
            IS_ROLLING_BACK.set(false);
        }
    }

    private Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return getField(clazz.getSuperclass(), fieldName);
            }
            throw e;
        }
    }

    private Object convertStringToType(String value, Class<?> type) {
        if (value == null || "null".equals(value)) return null;
        if (type == String.class) return value;
        if (type == Integer.class || type == int.class) return Integer.parseInt(value);
        if (type == Long.class || type == long.class) return Long.parseLong(value);
        if (type == Double.class || type == double.class) return Double.parseDouble(value);
        if (type == Boolean.class || type == boolean.class) return Boolean.parseBoolean(value);
        if (type == java.math.BigDecimal.class) return new java.math.BigDecimal(value);
        if (type == java.time.LocalDate.class) return java.time.LocalDate.parse(value);
        if (type == java.time.LocalDateTime.class) return java.time.LocalDateTime.parse(value);
        if (type.isEnum()) {
            return Enum.valueOf((Class<Enum>) type, value);
        }
        // For simple relationships, we might need more logic, but for REQ-67 basic fields are usually target.
        return value; 
    }
}
