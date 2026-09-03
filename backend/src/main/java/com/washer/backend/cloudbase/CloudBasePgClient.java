package com.washer.backend.cloudbase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CloudBasePgClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudBasePgClient.class);
    private static final Pattern RESOURCE_NAME = Pattern.compile("[a-z][a-z0-9_]{0,62}");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final CloudBasePgProperties properties;

    public CloudBasePgClient(
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        CloudBasePgProperties properties
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public JsonNode select(String table, Map<String, List<String>> query) {
        return request(HttpMethod.GET, resourceUri(table, query), null, false);
    }

    public PageResult selectPage(String table, Map<String, List<String>> query) {
        URI uri = resourceUri(table, query);
        try {
            ResponseEntity<String> response = restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header("Prefer", "count=exact")
                .retrieve()
                .toEntity(String.class);
            JsonNode rows = parseResponse(response.getBody());
            return new PageResult(rows, parseTotal(response.getHeaders().getFirst("Content-Range"), rows.size()));
        } catch (RestClientResponseException exception) {
            throw requestException(HttpMethod.GET, uri, exception);
        } catch (JsonProcessingException exception) {
            throw new CloudBasePgException("CloudBase PostgreSQL returned invalid JSON", exception);
        } catch (Exception exception) {
            throw new CloudBasePgException("CloudBase PostgreSQL request failed", exception);
        }
    }

    public JsonNode insert(String table, Object body) {
        return request(HttpMethod.POST, resourceUri(table, Map.of()), body, true);
    }

    public JsonNode update(String table, Map<String, List<String>> filters, Object body) {
        return request(HttpMethod.PATCH, resourceUri(table, filters), body, true);
    }

    public JsonNode delete(String table, Map<String, List<String>> filters) {
        return request(HttpMethod.DELETE, resourceUri(table, filters), null, false);
    }

    public JsonNode rpc(String function, Object body) {
        validateResourceName(function, "RPC function");
        return request(HttpMethod.POST, endpointUri("/v1/rdb/rest/rpc/" + function, Map.of()), body, true);
    }

    private JsonNode request(HttpMethod method, URI uri, Object body, boolean hasBody) {
        try {
            RestClient.RequestBodySpec request = restClient.method(method)
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            if (hasBody) {
                request.contentType(MediaType.APPLICATION_JSON)
                    .header("Prefer", "return=representation")
                    .body(body);
            }
            String response = request.retrieve().body(String.class);
            return parseResponse(response);
        } catch (RestClientResponseException exception) {
            throw requestException(method, uri, exception);
        } catch (JsonProcessingException exception) {
            throw new CloudBasePgException("CloudBase PostgreSQL returned invalid JSON", exception);
        } catch (Exception exception) {
            throw new CloudBasePgException("CloudBase PostgreSQL request failed", exception);
        }
    }

    private JsonNode parseResponse(String response) throws JsonProcessingException {
        return response == null || response.isBlank() ? objectMapper.nullNode() : objectMapper.readTree(response);
    }

    private CloudBasePgException requestException(HttpMethod method, URI uri, RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        LOGGER.warn(
            "CloudBase PostgreSQL request failed: method={}, resource={}, status={}, response={}",
            method,
            uri.getPath(),
            exception.getStatusCode().value(),
            compact(responseBody)
        );
        return new CloudBasePgException(extractMessage(responseBody, exception.getStatusCode().value()), exception);
    }

    private long parseTotal(String contentRange, int fallback) {
        if (contentRange == null || contentRange.isBlank()) {
            return fallback;
        }
        int separator = contentRange.lastIndexOf('/');
        if (separator < 0 || separator == contentRange.length() - 1) {
            return fallback;
        }
        try {
            return Long.parseLong(contentRange.substring(separator + 1));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private URI resourceUri(String table, Map<String, List<String>> query) {
        validateResourceName(table, "table");
        return endpointUri("/v1/rdb/rest/" + table, query);
    }

    private URI endpointUri(String path, Map<String, List<String>> query) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getGatewayBaseUrl()).path(path);
        query.forEach((key, values) -> values.forEach(value -> builder.queryParam(key, value)));
        return builder.build(true).toUri();
    }

    private void validateResourceName(String value, String description) {
        if (value == null || !RESOURCE_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsupported CloudBase PostgreSQL " + description + " name");
        }
    }

    private String extractMessage(String body, int status) {
        try {
            JsonNode response = objectMapper.readTree(body);
            String message = firstNonBlank(
                response.path("message").asText(""),
                response.path("error").asText(""),
                response.path("error").path("message").asText(""),
                response.path("details").asText(""),
                response.path("detail").asText("")
            );
            if (!message.isBlank()) {
                return "CloudBase PostgreSQL request failed (HTTP " + status + "): " + message;
            }
        } catch (JsonProcessingException ignored) {
            // A status code without a JSON message is still safe to report.
        }
        return "CloudBase PostgreSQL request failed (HTTP " + status + ")";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String compact(String value) {
        if (value == null || value.isBlank()) {
            return "<empty>";
        }
        String compacted = value.replaceAll("\\s+", " ").trim();
        return compacted.length() <= 1_000 ? compacted : compacted.substring(0, 1_000) + "...";
    }

    public record PageResult(JsonNode rows, long total) {
    }
}
