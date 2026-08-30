package com.washer.backend.cloudbase.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.cloudbase.CloudBasePgClient;
import com.washer.backend.cloudbase.CloudBasePgException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CloudBaseMapperInvocationHandler implements InvocationHandler {

    private final CloudBasePgClient client;
    private final CloudBaseEntityMetadata<Object> metadata;
    private final CloudBaseWrapperTranslator wrapperTranslator = new CloudBaseWrapperTranslator();

    @SuppressWarnings("unchecked")
    CloudBaseMapperInvocationHandler(Class<?> mapperType, CloudBasePgClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.metadata = new CloudBaseEntityMetadata<>((Class<Object>) resolveEntityType(mapperType), objectMapper);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() == Object.class) {
            return invokeObjectMethod(proxy, method, args);
        }
        return switch (method.getName()) {
            case "selectById" -> selectById(args[0]);
            case "selectBatchIds" -> selectBatchIds((Collection<?>) args[0]);
            case "selectOne" -> selectOne(wrapper(args, 0));
            case "selectList" -> selectList(wrapper(args, 0));
            case "selectCount" -> (long) selectList(wrapper(args, 0)).size();
            case "selectPage" -> selectPage((IPage<Object>) args[0], wrapper(args, 1));
            case "insert" -> insert(args[0]);
            case "updateById" -> updateById(args[0]);
            case "update" -> update(args[0], wrapper(args, 1));
            case "deleteById" -> deleteById(args[0]);
            case "delete" -> delete(wrapper(args, 0));
            default -> throw new CloudBasePgException("CloudBase HTTP profile does not support mapper method " + method.getName());
        };
    }

    private Object selectById(Object id) {
        List<Object> rows = select(metadata.idColumn(), "eq." + filterValue(id));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Object> selectBatchIds(Collection<?> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> values = ids.stream().map(this::filterValue).toList();
        return select(metadata.idColumn(), "in.(" + String.join(",", values) + ")");
    }

    private Object selectOne(Wrapper<?> wrapper) {
        Map<String, List<String>> query = wrapperTranslator.query(wrapper);
        query.put("limit", List.of("2"));
        List<Object> rows = select(query);
        if (rows.size() > 1) {
            throw new CloudBasePgException("Expected one row but CloudBase PostgreSQL returned more than one");
        }
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Object> selectList(Wrapper<?> wrapper) {
        return select(wrapperTranslator.query(wrapper));
    }

    private IPage<Object> selectPage(IPage<Object> page, Wrapper<?> wrapper) {
        Map<String, List<String>> baseQuery = wrapperTranslator.query(wrapper);
        long total = select(baseQuery).size();
        Map<String, List<String>> pageQuery = new LinkedHashMap<>(baseQuery);
        pageQuery.put("limit", List.of(String.valueOf(page.getSize())));
        pageQuery.put("offset", List.of(String.valueOf((page.getCurrent() - 1) * page.getSize())));
        page.setTotal(total);
        page.setRecords(select(pageQuery));
        return page;
    }

    private int insert(Object entity) {
        JsonNode response = client.insert(metadata.tableName(), metadata.body(entity, false));
        JsonNode created = first(response);
        metadata.assignId(entity, created);
        return rowsAffected(response);
    }

    private int updateById(Object entity) {
        Object id = metadata.idValue(entity);
        if (id == null) {
            throw new CloudBasePgException("Cannot update CloudBase entity without an ID");
        }
        JsonNode response = client.update(
            metadata.tableName(),
            Map.of(metadata.idColumn(), List.of("eq." + filterValue(id))),
            metadata.body(entity, false)
        );
        return rowsAffected(response);
    }

    private int update(Object entity, Wrapper<?> wrapper) {
        Map<String, Object> body = wrapperTranslator.updateBody(entity, wrapper, metadata);
        if (body.isEmpty()) {
            return 0;
        }
        return rowsAffected(client.update(metadata.tableName(), wrapperTranslator.query(wrapper), body));
    }

    private int deleteById(Object id) {
        return rowsAffected(client.delete(metadata.tableName(), Map.of(metadata.idColumn(), List.of("eq." + filterValue(id)))));
    }

    private int delete(Wrapper<?> wrapper) {
        return rowsAffected(client.delete(metadata.tableName(), wrapperTranslator.query(wrapper)));
    }

    private List<Object> select(String column, String value) {
        return select(Map.of(column, List.of(value)));
    }

    private List<Object> select(Map<String, List<String>> query) {
        JsonNode response = client.select(metadata.tableName(), query);
        if (!response.isArray()) {
            throw new CloudBasePgException("CloudBase PostgreSQL select response was not an array");
        }
        List<Object> rows = new ArrayList<>();
        response.forEach(node -> rows.add(metadata.read(node)));
        return rows;
    }

    private JsonNode first(JsonNode response) {
        return response != null && response.isArray() && !response.isEmpty() ? response.get(0) : response;
    }

    private int rowsAffected(JsonNode response) {
        if (response == null || response.isNull()) {
            return 1;
        }
        return response.isArray() ? response.size() : 1;
    }

    private Wrapper<?> wrapper(Object[] args, int index) {
        return args != null && args.length > index && args[index] != null ? (Wrapper<?>) args[index] : null;
    }

    private String filterValue(Object value) {
        if (value == null) {
            throw new CloudBasePgException("CloudBase HTTP profile does not allow null ID values");
        }
        return String.valueOf(value).replace("\\", "\\\\").replace(",", "\\,").replace("(", "\\(").replace(")", "\\)");
    }

    private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "CloudBasePostgrestMapper(" + metadata.tableName() + ")";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException(method.getName());
        };
    }

    private static Class<?> resolveEntityType(Class<?> mapperType) {
        for (Type candidate : mapperType.getGenericInterfaces()) {
            if (candidate instanceof ParameterizedType parameterized && parameterized.getRawType() == BaseMapper.class) {
                Type entity = parameterized.getActualTypeArguments()[0];
                if (entity instanceof Class<?> entityClass) {
                    return entityClass;
                }
            }
        }
        throw new IllegalArgumentException("CloudBase mapper must directly extend BaseMapper<T>: " + mapperType.getName());
    }
}
