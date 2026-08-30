package com.washer.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.config.WechatMiniappProperties;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatMiniappAuthService {

    private static final Map<String, MockLoginIdentity> BUILTIN_MOCK_LOGIN_IDENTITIES = buildBuiltinMockLoginIdentities();

    private final WechatMiniappProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AtomicReference<MockLoginIdentity> nextMockLoginIdentity = new AtomicReference<>();
    private volatile String cachedAccessToken;
    private volatile LocalDateTime cachedAccessTokenExpireTime;

    public WechatMiniappAuthService(
        WechatMiniappProperties properties,
        ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String exchangeOpenId(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("code is required");
        }
        MockLoginIdentity mockIdentity = nextMockLoginIdentity.get();
        if (mockIdentity != null && properties.isMockLoginEnabled()) {
            return mockIdentity.openId();
        }
        if (shouldUseMockOpenId(code)) {
            return buildMockOpenId(code);
        }
        if (!StringUtils.hasText(properties.getAppId())) {
            throw new IllegalArgumentException("wechat miniapp appId is not configured");
        }
        if (!StringUtils.hasText(properties.getSecret())) {
            throw new IllegalArgumentException("wechat miniapp secret is not configured");
        }

        String requestUrl = buildSessionUrl(code.trim());
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(requestUrl))
            .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalArgumentException("wechat jscode2session request failed");
        }

        if (response.statusCode() != 200) {
            throw new IllegalArgumentException(
                "wechat jscode2session request failed: HTTP " + response.statusCode()
            );
        }

        return extractOpenId(response.body());
    }

    public String exchangePhoneNumber(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("phoneCode is required");
        }
        MockLoginIdentity mockIdentity = nextMockLoginIdentity.get();
        if (mockIdentity != null && properties.isMockLoginEnabled()) {
            nextMockLoginIdentity.compareAndSet(mockIdentity, null);
            return mockIdentity.mobile();
        }
        if (shouldUseMockPhoneNumber(code)) {
            return buildMockPhoneNumber(code);
        }

        String accessToken = getAccessToken();
        String requestUrl = normalizedBaseUrl()
            + "/wxa/business/getuserphonenumber?access_token=" + encode(accessToken);

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(Map.of("code", code.trim()));
        } catch (IOException ex) {
            throw new IllegalArgumentException("wechat phone request build failed");
        }

        HttpRequest request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .header("Content-Type", "application/json")
            .uri(URI.create(requestUrl))
            .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalArgumentException("wechat get phone number request failed");
        }

        if (response.statusCode() != 200) {
            throw new IllegalArgumentException(
                "wechat get phone number request failed: HTTP " + response.statusCode()
            );
        }

        return extractPhoneNumber(response.body());
    }

    public MockLoginIdentity prepareNextMockLogin(String openId, String mobile) {
        if (!properties.isMockLoginEnabled()) {
            throw new IllegalStateException("wechat mock login is disabled");
        }
        MockLoginIdentity identity = new MockLoginIdentity(
            normalizeMockOpenId(openId),
            normalizeMockMobile(mobile),
            ""
        );
        nextMockLoginIdentity.set(identity);
        return identity;
    }

    public MockLoginIdentity findBuiltinMockLoginIdentityByMobile(String mobile) {
        String normalizedMobile = normalizeMockMobile(mobile);
        return BUILTIN_MOCK_LOGIN_IDENTITIES.get(normalizedMobile);
    }

    public boolean isMockLoginEnabled() {
        return properties.isMockLoginEnabled();
    }

    public Map<String, MockLoginIdentity> getBuiltinMockLoginIdentities() {
        return BUILTIN_MOCK_LOGIN_IDENTITIES;
    }

    private boolean shouldUseMockOpenId(String code) {
        if (!properties.isMockLoginEnabled()) {
            return false;
        }
        String normalizedCode = code != null ? code.trim().toLowerCase() : "";
        return normalizedCode.startsWith("mock")
            || !StringUtils.hasText(properties.getAppId())
            || !StringUtils.hasText(properties.getSecret());
    }

    private boolean shouldUseMockPhoneNumber(String code) {
        if (!properties.isMockLoginEnabled()) {
            return false;
        }
        String normalizedCode = code != null ? code.trim().toLowerCase() : "";
        return normalizedCode.startsWith("mock")
            || !StringUtils.hasText(properties.getAppId())
            || !StringUtils.hasText(properties.getSecret());
    }

    private String buildMockPhoneNumber(String code) {
        String normalizedCode = code != null ? code.trim() : "";
        String digits = normalizedCode.replaceAll("\\D", "");
        if (digits.length() >= 11) {
            return digits.substring(digits.length() - 11);
        }
        return "13800138000";
    }

    private String normalizeMockOpenId(String openId) {
        String text = openId != null ? openId.trim() : "";
        if (!StringUtils.hasText(text)) {
            return "mock_openid_test_" + System.currentTimeMillis();
        }
        String normalized = text.replaceAll("[^A-Za-z0-9_-]", "_");
        if (normalized.length() > 64) {
            normalized = normalized.substring(0, 64);
        }
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return "mock_openid_test_" + System.currentTimeMillis();
    }

    private String normalizeMockMobile(String mobile) {
        String text = mobile != null ? mobile.trim().replaceAll("[\\s-]", "") : "";
        if (text.startsWith("+86")) {
            text = text.substring(3);
        } else if (text.startsWith("86") && text.length() == 13) {
            text = text.substring(2);
        }
        if (StringUtils.hasText(text) && text.matches("\\d{5,20}")) {
            return text;
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        String suffix = timestamp.substring(Math.max(0, timestamp.length() - 8));
        while (suffix.length() < 8) {
            suffix = "0" + suffix;
        }
        return "139" + suffix;
    }

    private String buildMockOpenId(String code) {
        String normalizedCode = code != null ? code.trim() : "";
        String lowerCode = normalizedCode.toLowerCase();
        if ("mock-platform".equals(lowerCode) || "mock_platform".equals(lowerCode)) {
            return "mock_openid_platform";
        }
        if ("mock-franchisee".equals(lowerCode) || "mock_franchisee".equals(lowerCode)) {
            return "mock_openid_franchisee";
        }
        if ("mock-store".equals(lowerCode) || "mock_store".equals(lowerCode)) {
            return "mock_openid_store";
        }
        if ("mock-staff".equals(lowerCode) || "mock_staff".equals(lowerCode)) {
            return "mock_openid_staff";
        }
        if (lowerCode.startsWith("mock")) {
            String suffix = normalizedCode.replaceAll("[^A-Za-z0-9_-]", "");
            if (suffix.length() > 40) {
                suffix = suffix.substring(0, 40);
            }
            if (StringUtils.hasText(suffix)) {
                return "mock_openid_" + suffix;
            }
        }
        return "mock_openid_local";
    }

    private String buildSessionUrl(String code) {
        return normalizedBaseUrl()
            + "/sns/jscode2session?appid=" + encode(properties.getAppId())
            + "&secret=" + encode(properties.getSecret())
            + "&js_code=" + encode(code)
            + "&grant_type=authorization_code";
    }

    private synchronized String getAccessToken() {
        if (
            StringUtils.hasText(cachedAccessToken)
                && cachedAccessTokenExpireTime != null
                && cachedAccessTokenExpireTime.isAfter(LocalDateTime.now().plusSeconds(60))
        ) {
            return cachedAccessToken;
        }

        if (!StringUtils.hasText(properties.getAppId())) {
            throw new IllegalArgumentException("wechat miniapp appId is not configured");
        }
        if (!StringUtils.hasText(properties.getSecret())) {
            throw new IllegalArgumentException("wechat miniapp secret is not configured");
        }

        String requestUrl = normalizedBaseUrl()
            + "/cgi-bin/token?grant_type=client_credential"
            + "&appid=" + encode(properties.getAppId())
            + "&secret=" + encode(properties.getSecret());

        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(requestUrl))
            .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalArgumentException("wechat access_token request failed");
        }
        if (response.statusCode() != 200) {
            throw new IllegalArgumentException("wechat access_token request failed: HTTP " + response.statusCode());
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            int errCode = root.path("errcode").asInt(0);
            if (errCode != 0) {
                String errMsg = root.path("errmsg").asText("wechat access_token failed");
                throw new IllegalArgumentException("wechat access_token failed: " + errCode + " " + errMsg);
            }
            String accessToken = root.path("access_token").asText("");
            if (!StringUtils.hasText(accessToken)) {
                throw new IllegalArgumentException("wechat access_token is missing");
            }
            int expiresIn = Math.max(300, root.path("expires_in").asInt(7200));
            cachedAccessToken = accessToken.trim();
            cachedAccessTokenExpireTime = LocalDateTime.now().plusSeconds(Math.max(60, expiresIn - 120));
            return cachedAccessToken;
        } catch (IOException ex) {
            throw new IllegalArgumentException("wechat access_token response parse failed");
        }
    }

    private String extractOpenId(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            int errCode = root.path("errcode").asInt(0);
            if (errCode != 0) {
                String errMsg = root.path("errmsg").asText("wechat jscode2session failed");
                throw new IllegalArgumentException(
                    "wechat jscode2session failed: " + errCode + " " + errMsg
                );
            }

            String openId = root.path("openid").asText("");
            if (!StringUtils.hasText(openId)) {
                throw new IllegalArgumentException("wechat openid is missing");
            }
            return openId.trim();
        } catch (IOException ex) {
            throw new IllegalArgumentException("wechat jscode2session response parse failed");
        }
    }

    private String extractPhoneNumber(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            int errCode = root.path("errcode").asInt(0);
            if (errCode != 0) {
                String errMsg = root.path("errmsg").asText("wechat get phone number failed");
                throw new IllegalArgumentException(
                    "wechat get phone number failed: " + errCode + " " + errMsg
                );
            }

            JsonNode phoneInfo = root.path("phone_info");
            String phoneNumber = phoneInfo.path("purePhoneNumber").asText("");
            if (!StringUtils.hasText(phoneNumber)) {
                phoneNumber = phoneInfo.path("phoneNumber").asText("");
            }
            if (!StringUtils.hasText(phoneNumber)) {
                throw new IllegalArgumentException("wechat phone number is missing");
            }
            return phoneNumber.trim();
        } catch (IOException ex) {
            throw new IllegalArgumentException("wechat get phone number response parse failed");
        }
    }

    private String normalizedBaseUrl() {
        String baseUrl = StringUtils.hasText(properties.getBaseUrl())
            ? properties.getBaseUrl().trim()
            : "https://api.weixin.qq.com";
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Map<String, MockLoginIdentity> buildBuiltinMockLoginIdentities() {
        Map<String, MockLoginIdentity> identities = new LinkedHashMap<>();
        identities.put("19552500939", new MockLoginIdentity("mock_openid_user_001", "19552500939", "测试用户1"));
        identities.put("19552500940", new MockLoginIdentity("mock_openid_user_002", "19552500940", "测试用户2"));
        identities.put("19552500941", new MockLoginIdentity("mock_openid_user_003", "19552500941", "测试用户3"));
        identities.put("19552500942", new MockLoginIdentity("mock_openid_user_004", "19552500942", "测试用户4"));
        identities.put("19552500943", new MockLoginIdentity("mock_openid_user_005", "19552500943", "测试用户5"));
        return Map.copyOf(identities);
    }

    public record MockLoginIdentity(String openId, String mobile, String nickname) {
    }
}
