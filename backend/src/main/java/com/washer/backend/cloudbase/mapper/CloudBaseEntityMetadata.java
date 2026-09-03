package com.washer.backend.cloudbase.mapper;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.cloudbase.CloudBasePgException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

final class CloudBaseEntityMetadata<T> {

    private final Class<T> entityType;
    private final String tableName;
    private final Field idField;
    private final ObjectMapper objectMapper;

    CloudBaseEntityMetadata(Class<T> entityType, ObjectMapper objectMapper) {
        this.entityType = entityType;
        this.objectMapper = objectMapper.copy().setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
        TableName tableNameAnnotation = entityType.getAnnotation(TableName.class);
        if (tableNameAnnotation == null || tableNameAnnotation.value().isBlank()) {
            throw new IllegalArgumentException("CloudBase entity must declare @TableName: " + entityType.getName());
        }
        this.tableName = tableNameAnnotation.value();
        this.idField = findIdField(entityType);
        this.idField.setAccessible(true);
        if (TableInfoHelper.getTableInfo(entityType) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new Configuration(), entityType.getName()), entityType);
        }
    }

    String tableName() {
        return tableName;
    }

    String idColumn() {
        return columnName(idField.getName());
    }

    Object idValue(T entity) {
        try {
            return idField.get(entity);
        } catch (IllegalAccessException exception) {
            throw new CloudBasePgException("Unable to read entity ID", exception);
        }
    }

    void assignId(T entity, JsonNode node) {
        if (node == null || node.isNull() || !node.has(idColumn())) {
            return;
        }
        try {
            Object id = objectMapper.treeToValue(node.get(idColumn()), idField.getType());
            idField.set(entity, id);
        } catch (Exception exception) {
            throw new CloudBasePgException("Unable to assign generated entity ID", exception);
        }
    }

    Map<String, Object> body(T entity, boolean includeId) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (Field field : entityType.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value != null && (includeId || field != idField)) {
                    body.put(columnName(field.getName()), postgrestValue(value));
                }
            } catch (IllegalAccessException exception) {
                throw new CloudBasePgException("Unable to serialize entity field", exception);
            }
        }
        return body;
    }

    T read(JsonNode node) {
        try {
            return objectMapper.treeToValue(node, entityType);
        } catch (Exception exception) {
            throw new CloudBasePgException("Unable to map CloudBase PostgreSQL response", exception);
        }
    }

    private Field findIdField(Class<T> type) {
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(TableId.class)) {
                return field;
            }
        }
        throw new IllegalArgumentException("CloudBase entity must declare @TableId: " + type.getName());
    }

    private String columnName(String fieldName) {
        return fieldName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }

    Object postgrestValue(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
        }
        if (value instanceof LocalDate date) {
            return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
        }
        if (value instanceof LocalTime time) {
            return DateTimeFormatter.ISO_LOCAL_TIME.format(time);
        }
        return value;
    }
}
