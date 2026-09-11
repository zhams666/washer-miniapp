package com.washer.backend.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class DeviceManagementRemark {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private DeviceManagementRemark() {
    }

    public static Map<String, Object> parse(String remark) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(remark)) {
            return result;
        }
        String text = remark.trim();
        if (text.startsWith("{") && text.endsWith("}")) {
            try {
                return new LinkedHashMap<>(OBJECT_MAPPER.readValue(text, MAP_TYPE));
            } catch (Exception ignored) {
                result.put("legacyRemark", text);
                return result;
            }
        }
        result.put("legacyRemark", text);
        return result;
    }

    public static String serialize(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return null;
        }
        Map<String, Object> compact = new LinkedHashMap<>();
        config.forEach((key, value) -> {
            if (!StringUtils.hasText(key) || value == null) {
                return;
            }
            if (value instanceof String text) {
                String trimmed = text.trim();
                if (StringUtils.hasText(trimmed)) {
                    compact.put(key, trimmed);
                }
                return;
            }
            compact.put(key, value);
        });
        if (compact.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(compact);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String text(Map<String, Object> config, String key) {
        if (config == null || !config.containsKey(key)) {
            return "";
        }
        return String.valueOf(config.get(key)).trim();
    }

    public static Integer integer(Map<String, Object> config, String key) {
        String text = text(config, key);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static BigDecimal decimal(Map<String, Object> config, String key) {
        String text = text(config, key);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static Boolean bool(Map<String, Object> config, String key) {
        String text = text(config, key);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }
}
