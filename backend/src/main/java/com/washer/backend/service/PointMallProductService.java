package com.washer.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.entity.PointMallProduct;
import com.washer.backend.mapper.PointMallProductMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointMallProductService {

    private static final Set<String> PRODUCT_TYPES = Set.of("wash_service", "coupon", "physical");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PointMallProductMapper productMapper;

    public PointMallProductService(PointMallProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public Page<PointMallProduct> pageProducts(long page, long size, String keyword, Integer status, String productType) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(100, Math.max(1, size));
        return productMapper.selectPage(
            new Page<>(safePage, safeSize),
            new LambdaQueryWrapper<PointMallProduct>()
                .like(StringUtils.hasText(keyword), PointMallProduct::getTitle, normalizeText(keyword))
                .eq(status != null, PointMallProduct::getStatus, normalizeStatus(status))
                .eq(StringUtils.hasText(productType), PointMallProduct::getProductType, normalizeProductType(productType))
                .orderByAsc(PointMallProduct::getSortOrder)
                .orderByDesc(PointMallProduct::getId)
        );
    }

    public List<PointMallProduct> listPublishedProducts() {
        LocalDateTime now = LocalDateTime.now();
        return productMapper.selectList(
            new LambdaQueryWrapper<PointMallProduct>()
                .eq(PointMallProduct::getStatus, 1)
                .gt(PointMallProduct::getStockTotal, 0)
                .and(wrapper -> wrapper.isNull(PointMallProduct::getEffectiveTime)
                    .or()
                    .le(PointMallProduct::getEffectiveTime, now))
                .and(wrapper -> wrapper.isNull(PointMallProduct::getExpireTime)
                    .or()
                    .gt(PointMallProduct::getExpireTime, now))
                .orderByAsc(PointMallProduct::getSortOrder)
                .orderByDesc(PointMallProduct::getId)
        );
    }

    public PointMallProduct saveProduct(Long id, Map<String, Object> payload) {
        PointMallProduct product = id == null ? new PointMallProduct() : productMapper.selectById(id);
        if (id != null && product == null) {
            throw new IllegalArgumentException("积分商品不存在");
        }

        String title = requireText(payload, "title", "商品名称不能为空");
        String productType = normalizeProductType(text(payload, "productType"));
        int pointsPrice = requirePositiveInteger(payload, "pointsPrice", "兑换积分必须大于 0");
        int stockTotal = requireNonNegativeInteger(payload, "stockTotal", "库存不能小于 0");
        int limitPerUser = optionalNonNegativeInteger(payload, "limitPerUser", 0, "每人限兑次数不能小于 0");
        int sortOrder = optionalNonNegativeInteger(payload, "sortOrder", 0, "排序不能小于 0");
        int status = optionalStatus(payload, product.getStatus());
        LocalDateTime effectiveTime = dateTime(payload, "effectiveTime");
        LocalDateTime expireTime = dateTime(payload, "expireTime");
        if (effectiveTime != null && expireTime != null && !expireTime.isAfter(effectiveTime)) {
            throw new IllegalArgumentException("失效时间必须晚于生效时间");
        }
        if (status == 1 && stockTotal == 0) {
            throw new IllegalArgumentException("库存为 0 的商品不能上架");
        }

        product.setTitle(limitLength(title, 100, "商品名称不能超过 100 个字符"));
        product.setDescription(limitLength(text(payload, "description"), 500, "商品说明不能超过 500 个字符"));
        product.setCoverImage(limitLength(text(payload, "coverImage"), 500, "封面地址不能超过 500 个字符"));
        product.setProductType(productType);
        product.setPointsPrice(pointsPrice);
        product.setStockTotal(stockTotal);
        product.setLimitPerUser(limitPerUser);
        product.setEffectiveTime(effectiveTime);
        product.setExpireTime(expireTime);
        product.setStatus(status);
        product.setSortOrder(sortOrder);

        if (id == null) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }
        return product;
    }

    public PointMallProduct changeStatus(Long id, Integer status) {
        PointMallProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new IllegalArgumentException("积分商品不存在");
        }
        int targetStatus = normalizeStatus(status);
        if (targetStatus == 1 && (product.getStockTotal() == null || product.getStockTotal() <= 0)) {
            throw new IllegalArgumentException("库存为 0 的商品不能上架");
        }
        product.setStatus(targetStatus);
        productMapper.updateById(product);
        return product;
    }

    private String normalizeProductType(String value) {
        String productType = StringUtils.hasText(value) ? value.trim().toLowerCase() : "wash_service";
        if (!PRODUCT_TYPES.contains(productType)) {
            throw new IllegalArgumentException("商品类型不合法");
        }
        return productType;
    }

    private int optionalStatus(Map<String, Object> payload, Integer fallback) {
        Object value = payload != null ? payload.get("status") : null;
        return value == null ? normalizeStatus(fallback == null ? 0 : fallback) : normalizeStatus(value);
    }

    private int normalizeStatus(Object value) {
        int status = parseInteger(value, 0, "商品状态不合法");
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("商品状态不合法");
        }
        return status;
    }

    private int requirePositiveInteger(Map<String, Object> payload, String key, String message) {
        int value = parseInteger(payload != null ? payload.get(key) : null, -1, message);
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private int requireNonNegativeInteger(Map<String, Object> payload, String key, String message) {
        int value = parseInteger(payload != null ? payload.get(key) : null, -1, message);
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private int optionalNonNegativeInteger(Map<String, Object> payload, String key, int fallback, String message) {
        Object value = payload != null ? payload.get(key) : null;
        int result = value == null || String.valueOf(value).isBlank() ? fallback : parseInteger(value, fallback, message);
        if (result < 0) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    private int parseInteger(Object value, int fallback, String message) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private LocalDateTime dateTime(Map<String, Object> payload, String key) {
        Object value = payload != null ? payload.get(key) : null;
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        String text = String.valueOf(value).trim();
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException(key + " 时间格式不合法");
            }
        }
    }

    private String requireText(Map<String, Object> payload, String key, String message) {
        String value = text(payload, key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String text(Map<String, Object> payload, String key) {
        if (payload == null || payload.get(key) == null) {
            return null;
        }
        String value = String.valueOf(payload.get(key)).trim();
        return StringUtils.hasText(value) ? value : null;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String limitLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
