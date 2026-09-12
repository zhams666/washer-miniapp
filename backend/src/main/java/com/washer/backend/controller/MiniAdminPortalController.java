package com.washer.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.dto.device.DeviceSimpleItem;
import com.washer.backend.dto.miniadmin.MiniAdminDashboardOverview;
import com.washer.backend.dto.miniadmin.MiniAdminDeviceConfigRequest;
import com.washer.backend.dto.miniadmin.MiniAdminMetricDetailItem;
import com.washer.backend.dto.miniadmin.MiniAdminOrderItem;
import com.washer.backend.dto.miniadmin.MiniAdminOperationOverview;
import com.washer.backend.dto.miniadmin.MiniAdminSessionContext;
import com.washer.backend.dto.miniadmin.MiniAdminStoreSettingsItem;
import com.washer.backend.dto.miniadmin.MiniAdminStoreSettingsRequest;
import com.washer.backend.service.MiniAdminAuthService;
import com.washer.backend.service.MiniAdminPortalService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/mini-admin")
public class MiniAdminPortalController {

    private static final long MAX_STORE_IMAGE_SIZE = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_STORE_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Logger LOGGER = LoggerFactory.getLogger(MiniAdminPortalController.class);

    private final MiniAdminAuthService miniAdminAuthService;
    private final MiniAdminPortalService miniAdminPortalService;

    public MiniAdminPortalController(
        MiniAdminAuthService miniAdminAuthService,
        MiniAdminPortalService miniAdminPortalService
    ) {
        this.miniAdminAuthService = miniAdminAuthService;
        this.miniAdminPortalService = miniAdminPortalService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<MiniAdminDashboardOverview> dashboard(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate bizDate,
        @RequestParam(required = false) Long storeId
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.getDashboard(context, bizDate, storeId));
    }

    @GetMapping("/operation/overview")
    public ApiResponse<MiniAdminOperationOverview> operationOverview(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestHeader(value = "X-Washer-Trace-Id", required = false) String traceId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate bizDate,
        @RequestParam(required = false) Long storeId
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        String safeTraceId = normalizeTraceId(traceId);
        Long staffId = context.getStaff() != null ? context.getStaff().getId() : null;
        LOGGER.info(
            "mini_admin_operation_overview_started traceId={}, staffId={}, storeId={}, bizDate={}",
            safeTraceId,
            staffId,
            storeId,
            bizDate
        );
        try {
            MiniAdminOperationOverview overview = miniAdminPortalService.getOperationOverview(context, bizDate, storeId);
            LOGGER.info(
                "mini_admin_operation_overview_completed traceId={}, staffId={}, storeId={}, bizDate={}",
                safeTraceId,
                staffId,
                storeId,
                bizDate
            );
            return ApiResponse.success(overview);
        } catch (RuntimeException exception) {
            LOGGER.error(
                "mini_admin_operation_overview_failed traceId={}, staffId={}, storeId={}, bizDate={}, reason={}",
                safeTraceId,
                staffId,
                storeId,
                bizDate,
                exception.getMessage(),
                exception
            );
            throw exception;
        }
    }

    @GetMapping("/finance/details")
    public ApiResponse<List<MiniAdminMetricDetailItem>> financeDetails(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate bizDate,
        @RequestParam(required = false) Long storeId,
        @RequestParam String metricKey
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(
            miniAdminPortalService.listMetricDetails(context, bizDate, storeId, metricKey)
        );
    }

    @GetMapping("/devices")
    public ApiResponse<List<DeviceSimpleItem>> devices(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String keyword
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.listDevices(context, storeId, keyword));
    }

    @PostMapping("/devices/{id}/start")
    public ApiResponse<DeviceSimpleItem> startDevice(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @PathVariable Long id
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.startDevice(context, id));
    }

    @PostMapping("/devices/{id}/stop")
    public ApiResponse<DeviceSimpleItem> stopDevice(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @PathVariable Long id
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.stopDevice(context, id));
    }

    @PostMapping("/devices/{id}/action")
    public ApiResponse<DeviceSimpleItem> operateDevice(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @PathVariable Long id,
        @RequestParam String action
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.operateDevice(context, id, action));
    }

    @PostMapping("/devices/{id}/config")
    public ApiResponse<DeviceSimpleItem> updateDeviceConfig(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @PathVariable Long id,
        @RequestBody MiniAdminDeviceConfigRequest request
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.updateDeviceConfig(context, id, request));
    }

    @GetMapping("/stores/{storeId}/settings")
    public ApiResponse<MiniAdminStoreSettingsItem> storeSettings(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @PathVariable Long storeId
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.getStoreSettings(context, storeId));
    }

    @PostMapping("/stores/{storeId}/settings")
    public ApiResponse<MiniAdminStoreSettingsItem> updateStoreSettings(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @PathVariable Long storeId,
        @RequestBody MiniAdminStoreSettingsRequest request
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(miniAdminPortalService.updateStoreSettings(context, storeId, request));
    }

    @PostMapping("/stores/{storeId}/cover-image")
    public ApiResponse<Map<String, Object>> uploadStoreCoverImage(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @PathVariable Long storeId,
        @RequestParam("file") MultipartFile file,
        HttpServletRequest servletRequest
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("store image file is required");
        }
        if (file.getSize() > MAX_STORE_IMAGE_SIZE) {
            throw new IllegalArgumentException("store image file too large");
        }

        String extension = resolveStoreImageExtension(file.getOriginalFilename(), file.getContentType());
        Path uploadDir = Paths.get("uploads", "stores").toAbsolutePath().normalize();
        String filename = "store-" + storeId + "-" + UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("invalid store image file path");
        }

        try {
            Files.createDirectories(uploadDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("save store image failed", ex);
        }

        String coverImage = buildPublicUrl(servletRequest, "/uploads/stores/" + filename);
        MiniAdminStoreSettingsItem settings = miniAdminPortalService.updateStoreCoverImage(context, storeId, coverImage);
        return ApiResponse.success(Map.of("coverImage", coverImage, "url", coverImage, "settings", settings));
    }

    @PostMapping("/stores/{storeId}/cover-image-url")
    public ApiResponse<MiniAdminStoreSettingsItem> updateStoreCoverImageUrl(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @PathVariable Long storeId,
        @RequestBody Map<String, Object> payload
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        Object value = payload != null && payload.containsKey("coverImage")
            ? payload.get("coverImage")
            : payload != null ? payload.get("url") : null;
        String coverImage = value != null ? String.valueOf(value) : "";
        return ApiResponse.success(miniAdminPortalService.updateStoreCoverImage(context, storeId, coverImage));
    }

    @GetMapping("/orders")
    public ApiResponse<Page<MiniAdminOrderItem>> orders(
        @RequestHeader(value = "X-Washer-Admin-Token", required = false) String token,
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long size,
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String orderStatus,
        @RequestParam(required = false) String paymentStatus,
        @RequestParam(required = false) String keyword
    ) {
        MiniAdminSessionContext context = miniAdminAuthService.requireContext(token);
        return ApiResponse.success(
            miniAdminPortalService.pageOrders(context, page, size, storeId, orderStatus, paymentStatus, keyword)
        );
    }

    private String resolveStoreImageExtension(String originalFilename, String contentType) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename.trim().toLowerCase() : "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            String extension = filename.substring(dotIndex);
            if (ALLOWED_STORE_IMAGE_EXTENSIONS.contains(extension)) {
                return extension;
            }
        }

        String normalizedContentType = StringUtils.hasText(contentType) ? contentType.trim().toLowerCase() : "";
        if ("image/png".equals(normalizedContentType)) {
            return ".png";
        }
        if ("image/webp".equals(normalizedContentType)) {
            return ".webp";
        }
        return ".jpg";
    }

    private String normalizeTraceId(String value) {
        if (!StringUtils.hasText(value)) {
            return "missing";
        }
        String normalized = value.replaceAll("[^A-Za-z0-9_-]", "");
        return StringUtils.hasText(normalized) ? normalized.substring(0, Math.min(normalized.length(), 80)) : "invalid";
    }

    private String buildPublicUrl(HttpServletRequest request, String path) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String scheme = StringUtils.hasText(forwardedProto) ? forwardedProto.trim() : request.getScheme();
        String host;

        if (StringUtils.hasText(forwardedHost)) {
            host = forwardedHost.trim();
        } else {
            int port = request.getServerPort();
            boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
            host = request.getServerName() + (defaultPort ? "" : ":" + port);
        }

        return scheme + "://" + host + path;
    }
}
