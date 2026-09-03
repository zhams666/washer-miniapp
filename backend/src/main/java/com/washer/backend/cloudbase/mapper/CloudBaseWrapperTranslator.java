package com.washer.backend.cloudbase.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.washer.backend.cloudbase.CloudBasePgException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CloudBaseWrapperTranslator {

    private static final Pattern ORDER_BY = Pattern.compile("(?i)(?:^|\\s+)ORDER BY\\s+(.+)$");
    private static final Pattern LIMIT = Pattern.compile("(?i)\\s+LIMIT\\s+(\\d+)(?:\\s+OFFSET\\s+(\\d+))?$");
    private static final Pattern PARAMETER = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.([A-Za-z0-9_]+)}");
    private static final Pattern COMPARISON = Pattern.compile(
        "^([a-z][a-z0-9_]*?)\\s*(=|<>|>=|<=|>|<|LIKE|NOT LIKE)\\s*#\\{ew\\.paramNameValuePairs\\.([A-Za-z0-9_]+)}$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NULL_CHECK = Pattern.compile("^([a-z][a-z0-9_]*?)\\s+IS\\s+(NOT\\s+)?NULL$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IN = Pattern.compile("^([a-z][a-z0-9_]*?)\\s+(NOT\\s+)?IN\\s*\\((.+)\\)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SET = Pattern.compile("^([a-z][a-z0-9_]*?)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.([A-Za-z0-9_]+)}$", Pattern.CASE_INSENSITIVE);

    Map<String, List<String>> query(Wrapper<?> wrapper) {
        Map<String, List<String>> query = new LinkedHashMap<>();
        if (wrapper == null || wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank()) {
            return query;
        }
        if (!(wrapper instanceof AbstractWrapper<?, ?, ?> abstractWrapper)) {
            throw new CloudBasePgException("CloudBase HTTP profile requires a MyBatis Lambda wrapper");
        }
        String segment = abstractWrapper.getSqlSegment().trim();
        if (containsUnsupportedSyntax(segment)) {
            throw new CloudBasePgException("CloudBase HTTP profile does not support this MyBatis query expression");
        }

        Matcher limit = LIMIT.matcher(segment);
        if (limit.find()) {
            add(query, "limit", limit.group(1));
            if (limit.group(2) != null) {
                add(query, "offset", limit.group(2));
            }
            segment = segment.substring(0, limit.start()).trim();
        }

        Matcher orderBy = ORDER_BY.matcher(segment);
        if (orderBy.find()) {
            add(query, "order", translateOrder(orderBy.group(1)));
            segment = segment.substring(0, orderBy.start()).trim();
        }

        if (segment.isBlank()) {
            return query;
        }
        segment = stripOuterParentheses(segment);
        for (String condition : segment.split("(?i)\\s+AND\\s+")) {
            translateCondition(condition.trim(), abstractWrapper.getParamNameValuePairs(), query);
        }
        return query;
    }

    Map<String, Object> updateBody(Object entity, Wrapper<?> wrapper, CloudBaseEntityMetadata<?> metadata) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (entity != null) {
            @SuppressWarnings("unchecked")
            CloudBaseEntityMetadata<Object> typedMetadata = (CloudBaseEntityMetadata<Object>) metadata;
            body.putAll(typedMetadata.body(entity, false));
        }
        if (!(wrapper instanceof LambdaUpdateWrapper<?> updateWrapper)) {
            return body;
        }
        String sqlSet = updateWrapper.getSqlSet();
        if (sqlSet == null || sqlSet.isBlank()) {
            return body;
        }
        for (String assignment : sqlSet.split(",\\s*")) {
            Matcher matcher = SET.matcher(assignment.trim());
            if (!matcher.matches()) {
                throw new CloudBasePgException("CloudBase HTTP profile does not support this MyBatis update expression");
            }
            body.put(
                columnName(matcher.group(1)),
                metadata.postgrestValue(updateWrapper.getParamNameValuePairs().get(matcher.group(2)))
            );
        }
        return body;
    }

    private void translateCondition(String condition, Map<String, Object> parameters, Map<String, List<String>> query) {
        Matcher nullCheck = NULL_CHECK.matcher(condition);
        if (nullCheck.matches()) {
            add(query, columnName(nullCheck.group(1)), nullCheck.group(2) == null ? "is.null" : "not.is.null");
            return;
        }
        Matcher comparison = COMPARISON.matcher(condition);
        if (comparison.matches()) {
            add(query, columnName(comparison.group(1)), operator(comparison.group(2)) + "." + value(parameters.get(comparison.group(3))));
            return;
        }
        Matcher in = IN.matcher(condition);
        if (in.matches()) {
            List<String> values = new ArrayList<>();
            Matcher parameter = PARAMETER.matcher(in.group(3));
            while (parameter.find()) {
                values.add(value(parameters.get(parameter.group(1))));
            }
            if (values.isEmpty()) {
                throw new CloudBasePgException("CloudBase HTTP profile requires parameterized IN values");
            }
            add(query, columnName(in.group(1)), (in.group(2) == null ? "in" : "not.in") + ".(" + String.join(",", values) + ")");
            return;
        }
        throw new CloudBasePgException("CloudBase HTTP profile does not support this MyBatis condition");
    }

    private String translateOrder(String order) {
        List<String> translated = new ArrayList<>();
        for (String item : order.split(",\\s*")) {
            String[] parts = item.trim().split("\\s+");
            if (parts.length != 2 || !parts[0].matches("(?i)[a-z][a-z0-9_]*") || !(parts[1].equalsIgnoreCase("ASC") || parts[1].equalsIgnoreCase("DESC"))) {
                throw new CloudBasePgException("CloudBase HTTP profile does not support this MyBatis order expression");
            }
            translated.add(columnName(parts[0]) + "." + parts[1].toLowerCase(java.util.Locale.ROOT));
        }
        return String.join(",", translated);
    }

    private String operator(String operator) {
        return switch (operator.toUpperCase(java.util.Locale.ROOT)) {
            case "=" -> "eq";
            case "<>" -> "neq";
            case ">" -> "gt";
            case ">=" -> "gte";
            case "<" -> "lt";
            case "<=" -> "lte";
            case "LIKE" -> "like";
            case "NOT LIKE" -> "not.like";
            default -> throw new CloudBasePgException("Unsupported comparison operator");
        };
    }

    private String value(Object value) {
        if (value == null) {
            throw new CloudBasePgException("CloudBase HTTP profile does not allow null comparison values");
        }
        return String.valueOf(value).replace("\\", "\\\\").replace(",", "\\,").replace("(", "\\(").replace(")", "\\)");
    }

    private String stripOuterParentheses(String value) {
        String stripped = value;
        while (stripped.startsWith("(") && stripped.endsWith(")")) {
            stripped = stripped.substring(1, stripped.length() - 1).trim();
        }
        return stripped;
    }

    private boolean containsUnsupportedSyntax(String segment) {
        String upper = segment.toUpperCase(java.util.Locale.ROOT);
        return upper.contains(" OR ") || upper.contains(" EXISTS ") || upper.contains(" APPLY ")
            || upper.contains("SELECT ") || upper.contains(";" ) || upper.contains(" + ") || upper.contains(" - ");
    }

    private void add(Map<String, List<String>> query, String key, String value) {
        query.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
    }

    private String columnName(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }
}
