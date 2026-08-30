package com.washer.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.washer.backend.config.WechatPayProperties;
import com.washer.backend.dto.pay.WechatPayNotifyResult;
import com.washer.backend.dto.pay.WechatPayOrderQueryResult;
import com.washer.backend.dto.pay.WechatPayPrepayRequest;
import com.washer.backend.dto.pay.WechatPayPrepayResult;
import com.washer.backend.dto.pay.WxPayRequestPaymentParams;
import com.washer.backend.service.WechatPayService;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatPayServiceImpl implements WechatPayService {

    private static final String AUTH_SCHEMA = "WECHATPAY2-SHA256-RSA2048";

    private final WechatPayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WechatPayServiceImpl(
        WechatPayProperties properties,
        ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public WechatPayPrepayResult createJsapiPrepay(WechatPayPrepayRequest request) {
        assertEnabledAndConfigured();
        if (request == null) {
            throw new IllegalArgumentException("wechat pay prepay request is required");
        }
        if (!StringUtils.hasText(request.getOpenid())) {
            throw new IllegalArgumentException("wechat payer openid is required");
        }
        if (!StringUtils.hasText(request.getOutTradeNo())) {
            throw new IllegalArgumentException("outTradeNo is required");
        }

        String path = "/v3/pay/transactions/jsapi";
        String body = buildPrepayBody(request);
        String responseBody = sendWechatRequest("POST", path, body);
        String prepayId = parseText(responseBody, "prepay_id");
        if (!StringUtils.hasText(prepayId)) {
            throw new IllegalArgumentException("wechat prepay_id is missing");
        }

        return new WechatPayPrepayResult(prepayId, buildRequestPaymentParams(prepayId));
    }

    @Override
    public WechatPayOrderQueryResult queryOrderByOutTradeNo(String outTradeNo) {
        assertEnabledAndConfigured();
        if (!StringUtils.hasText(outTradeNo)) {
            throw new IllegalArgumentException("outTradeNo is required");
        }

        String encodedOutTradeNo = encode(outTradeNo.trim());
        String encodedMchId = encode(properties.getMchId().trim());
        String path = "/v3/pay/transactions/out-trade-no/" + encodedOutTradeNo + "?mchid=" + encodedMchId;
        String responseBody = sendWechatRequest("GET", path, "");
        return parseOrderQueryResult(responseBody);
    }

    @Override
    public WechatPayNotifyResult parseNotify(Map<String, String> headers, String body) {
        assertEnabledAndConfigured();
        if (!StringUtils.hasText(body)) {
            throw new IllegalArgumentException("wechat pay notify body is required");
        }
        verifyNotifySignature(headers, body);

        try {
            JsonNode root = objectMapper.readTree(body);
            String notifyId = root.path("id").asText("");
            String eventType = root.path("event_type").asText("");
            JsonNode resource = root.path("resource");
            String decrypted = decryptResource(
                resource.path("associated_data").asText(""),
                resource.path("nonce").asText(""),
                resource.path("ciphertext").asText("")
            );
            JsonNode transaction = objectMapper.readTree(decrypted);
            return new WechatPayNotifyResult(
                notifyId,
                eventType,
                transaction.path("out_trade_no").asText(""),
                transaction.path("transaction_id").asText(""),
                transaction.path("trade_state").asText(""),
                transaction.path("amount").path("payer_total").isMissingNode()
                    ? null
                    : transaction.path("amount").path("payer_total").asInt(),
                parseWechatTime(transaction.path("success_time").asText("")),
                body
            );
        } catch (IOException ex) {
            throw new IllegalArgumentException("wechat pay notify parse failed");
        }
    }

    private void assertEnabledAndConfigured() {
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("wechat pay is not enabled");
        }
        requireText(properties.getAppId(), "wechat pay appId is not configured");
        requireText(properties.getMchId(), "wechat pay mchId is not configured");
        requireText(properties.getMerchantSerialNo(), "wechat pay merchant serial no is not configured");
        requireText(properties.getPrivateKeyPath(), "wechat pay private key path is not configured");
        requireText(properties.getApiV3Key(), "wechat pay api v3 key is not configured");
        requireText(properties.getNotifyUrl(), "wechat pay notify url is not configured");
    }

    private void verifyNotifySignature(Map<String, String> headers, String body) {
        requireText(properties.getPlatformCertificatePath(), "wechat pay platform certificate path is not configured");
        String timestamp = getHeader(headers, "wechatpay-timestamp");
        String nonce = getHeader(headers, "wechatpay-nonce");
        String signatureText = getHeader(headers, "wechatpay-signature");
        requireText(timestamp, "wechat pay notify timestamp is missing");
        requireText(nonce, "wechat pay notify nonce is missing");
        requireText(signatureText, "wechat pay notify signature is missing");

        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(loadPlatformPublicKey());
            verifier.update((timestamp + "\n" + nonce + "\n" + body + "\n").getBytes(StandardCharsets.UTF_8));
            boolean verified = verifier.verify(Base64.getDecoder().decode(signatureText));
            if (!verified) {
                throw new IllegalArgumentException("wechat pay notify signature verify failed");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("wechat pay notify signature verify failed");
        }
    }

    private String getHeader(Map<String, String> headers, String name) {
        if (headers == null || !StringUtils.hasText(name)) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String buildPrepayBody(WechatPayPrepayRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appid", properties.getAppId());
        body.put("mchid", properties.getMchId());
        body.put("description", StringUtils.hasText(request.getDescription()) ? request.getDescription() : "wallet recharge");
        body.put("out_trade_no", request.getOutTradeNo());
        body.put("notify_url", properties.getNotifyUrl());
        if (StringUtils.hasText(request.getAttach())) {
            body.put("attach", request.getAttach());
        }

        Map<String, Object> amount = new LinkedHashMap<>();
        amount.put("total", toFen(request.getAmount()));
        amount.put("currency", "CNY");
        body.put("amount", amount);

        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("openid", request.getOpenid());
        body.put("payer", payer);

        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException ex) {
            throw new IllegalArgumentException("wechat pay prepay body build failed");
        }
    }

    private String sendWechatRequest(String method, String pathWithQuery, String body) {
        String normalizedBaseUrl = normalizeBaseUrl();
        String authorization = buildAuthorization(method, pathWithQuery, body);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(normalizedBaseUrl + pathWithQuery))
            .header("Accept", "application/json")
            .header("Authorization", authorization);

        if ("POST".equalsIgnoreCase(method)) {
            requestBuilder.header("Content-Type", "application/json");
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        } else {
            requestBuilder.GET();
        }

        try {
            HttpResponse<String> response = httpClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException(
                    "wechat pay request failed: HTTP " + response.statusCode() + " " + response.body()
                );
            }
            return response.body();
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalArgumentException("wechat pay request failed");
        }
    }

    private String buildAuthorization(String method, String pathWithQuery, String body) {
        String nonce = randomNonce();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String message = method.toUpperCase() + "\n" + pathWithQuery + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
        String signature = sign(message);
        return AUTH_SCHEMA
            + " mchid=\"" + properties.getMchId() + "\","
            + "nonce_str=\"" + nonce + "\","
            + "signature=\"" + signature + "\","
            + "timestamp=\"" + timestamp + "\","
            + "serial_no=\"" + properties.getMerchantSerialNo() + "\"";
    }

    private WxPayRequestPaymentParams buildRequestPaymentParams(String prepayId) {
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = randomNonce();
        String packageValue = "prepay_id=" + prepayId;
        String paySign = sign(properties.getAppId() + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageValue + "\n");
        return new WxPayRequestPaymentParams(timeStamp, nonceStr, packageValue, "RSA", paySign);
    }

    private String sign(String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(loadPrivateKey());
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new IllegalArgumentException("wechat pay sign failed");
        }
    }

    private PrivateKey loadPrivateKey() throws Exception {
        String pem = Files.readString(Path.of(properties.getPrivateKeyPath()), StandardCharsets.UTF_8);
        String normalized = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private PublicKey loadPlatformPublicKey() throws Exception {
        try (java.io.InputStream inputStream = Files.newInputStream(Path.of(properties.getPlatformCertificatePath()))) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
            return certificate.getPublicKey();
        }
    }

    private String decryptResource(String associatedData, String nonce, String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec key = new SecretKeySpec(properties.getApiV3Key().getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            if (StringUtils.hasText(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("wechat pay notify decrypt failed");
        }
    }

    private WechatPayOrderQueryResult parseOrderQueryResult(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return new WechatPayOrderQueryResult(
                root.path("out_trade_no").asText(""),
                root.path("transaction_id").asText(""),
                root.path("trade_state").asText(""),
                root.path("amount").path("payer_total").isMissingNode()
                    ? null
                    : root.path("amount").path("payer_total").asInt(),
                parseWechatTime(root.path("success_time").asText(""))
            );
        } catch (IOException ex) {
            throw new IllegalArgumentException("wechat pay order query parse failed");
        }
    }

    private String parseText(String body, String fieldName) {
        try {
            return objectMapper.readTree(body).path(fieldName).asText("");
        } catch (IOException ex) {
            throw new IllegalArgumentException("wechat pay response parse failed");
        }
    }

    private LocalDateTime parseWechatTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception ex) {
            return null;
        }
    }

    private int toFen(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("wechat pay amount must be > 0");
        }
        return amount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private String normalizeBaseUrl() {
        String baseUrl = StringUtils.hasText(properties.getBaseUrl())
            ? properties.getBaseUrl().trim()
            : "https://api.mch.weixin.qq.com";
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String randomNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
