package com.example.icsm.listener;

import com.example.icsm.service.AuditService;
import com.example.icsm.util.SpringContext;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class EntityAuditListener {

    @PostPersist
    public void onPostPersist(Object entity) {
        logAction(entity, "CREATE", null, null, null);
    }

    @PostRemove
    public void onPostRemove(Object entity) {
        logAction(entity, "DELETE", null, null, null);
    }

    @PreUpdate
    public void onPreUpdate(Object entity) {
        try {
            // Suppress audit logging during rollback to avoid duplicate logs
            if (com.example.icsm.service.AuditService.IS_ROLLING_BACK.get()) {
                return;
            }

            JdbcTemplate jdbcTemplate = SpringContext.getBean(JdbcTemplate.class);
            String tableName = getTableName(entity);
            Long id = (Long) getEntityId(entity);
            
            // Fetch current DB state directly via JDBC to avoid any JPA/Hibernate session issues
            String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
            Map<String, Object> oldState = jdbcTemplate.queryForMap(sql, id);
            
            if (oldState == null) return;

            // Compare fields using reflection
            for (Field field : entity.getClass().getDeclaredFields()) {
                String fieldName = field.getName();
                
                // Exclude metadata/audit fields from logging
                if (fieldName.equals("updatedAt") || 
                    fieldName.equals("lastLoginAt") || 
                    fieldName.equals("createdAt")) {
                    continue;
                }

                if (field.isAnnotationPresent(Transient.class) || 
                    field.isAnnotationPresent(Id.class) ||
                    java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                String columnName = getColumnName(field);
                if (!oldState.containsKey(columnName)) continue;
                
                // Skip sensitive fields
                if (columnName.toLowerCase().contains("password")) continue;

                field.setAccessible(true);
                Object newValue = field.get(entity);
                Object oldValue = oldState.get(columnName);

                if (!isEqual(oldValue, newValue)) {
                    logAction(entity, "UPDATE", field.getName(), 
                             String.valueOf(oldValue), String.valueOf(newValue));
                }
            }
        } catch (Exception e) {
            log.error("Error in EntityAuditListener during JDBC check", e);
        }
    }

    private boolean isEqual(Object o1, Object o2) {
        if (o1 == o2) return true;
        if (o1 == null || o2 == null) return false;
        
        // Handle numeric comparisons carefully (JDBC often returns different types than JPA entities)
        if (o1 instanceof Number && o2 instanceof Number) {
            return new java.math.BigDecimal(o1.toString()).compareTo(new java.math.BigDecimal(o2.toString())) == 0;
        }
        
        // Handle Date/Timestamp
        if (o1 instanceof java.sql.Timestamp && o2 instanceof java.time.LocalDateTime) {
            return ((java.sql.Timestamp) o1).toLocalDateTime().equals(o2);
        }
        if (o1 instanceof java.sql.Date && o2 instanceof java.time.LocalDate) {
            return ((java.sql.Date) o1).toLocalDate().equals(o2);
        }

        return Objects.equals(o1.toString(), o2.toString());
    }

    private String getTableName(Object entity) {
        Table table = entity.getClass().getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        // Fallback to pluralized class name (common convention)
        return entity.getClass().getSimpleName().toLowerCase() + "s";
    }

    private String getColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isEmpty()) {
            return column.name();
        }
        // Fallback to field name converted to snake_case
        return field.getName().replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    private void logAction(Object entity, String action, String field, String oldVal, String newVal) {
        try {
            AuditService auditService = SpringContext.getBean(AuditService.class);
            String entityName = getBaseClassName(entity);
            Long entityId = (Long) getEntityId(entity);
            
            auditService.logChange(entityName, entityId, action, field, oldVal, newVal);
        } catch (Exception e) {
            log.error("Failed to log audit change", e);
        }
    }

    private String getBaseClassName(Object entity) {
        String name = entity.getClass().getSimpleName();
        int proxyIndex = name.indexOf("$HibernateProxy");
        if (proxyIndex > -1) {
            return name.substring(0, proxyIndex);
        }
        return name;
    }

    private Object getEntityId(Object entity) throws NoSuchFieldException, IllegalAccessException {
        Field idField = null;
        Class<?> clazz = entity.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    idField = field;
                    break;
                }
            }
            if (idField != null) break;
            clazz = clazz.getSuperclass();
        }
        
        if (idField == null) throw new NoSuchFieldException("No @Id field found");
        idField.setAccessible(true);
        return idField.get(entity);
    }
}
