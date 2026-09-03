package com.washer.backend.cloudbase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudBasePgClientTest {

    private HttpServer server;
    private String authorization;
    private String requestPath;
    private String prefer;
    private CloudBasePgClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/rdb/rest/store", this::handleStore);
        server.start();

        CloudBasePgProperties properties = new CloudBasePgProperties();
        properties.setEnvId("washer-test");
        properties.setApiKey("server-key");
        properties.setGatewayBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        client = new CloudBasePgClient(RestClient.builder(), new ObjectMapper(), properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void selectUsesPostgrestUrlAndServerAuthorization() {
        assertEquals(1, client.select("store", Map.of("id", List.of("eq.1"))).get(0).path("id").asInt());
        assertEquals("Bearer server-key", authorization);
        assertEquals("/v1/rdb/rest/store?id=eq.1", requestPath);
    }

    @Test
    void selectPageRequestsExactCountAndParsesContentRange() {
        CloudBasePgClient.PageResult result = client.selectPage("store", Map.of("limit", List.of("10"), "offset", List.of("0")));

        assertEquals(1, result.rows().get(0).path("id").asInt());
        assertEquals(42L, result.total());
        assertEquals("count=exact", prefer);
    }

    @Test
    void unsuccessfulResponseBecomesCloudBasePgException() {
        CloudBasePgException exception = assertThrows(
            CloudBasePgException.class,
            () -> client.select("store", Map.of("id", List.of("eq.403")))
        );
        assertEquals("CloudBase PostgreSQL request failed (HTTP 403): permission denied", exception.getMessage());
    }

    @Test
    void unsuccessfulResponseReadsNestedCloudBaseErrorMessage() {
        CloudBasePgException exception = assertThrows(
            CloudBasePgException.class,
            () -> client.select("store", Map.of("id", List.of("eq.400")))
        );
        assertEquals("CloudBase PostgreSQL request failed (HTTP 400): column points does not exist", exception.getMessage());
    }

    @Test
    void rejectsUnsafeResourceNamesBeforeNetworkCall() {
        assertThrows(IllegalArgumentException.class, () -> client.select("store;drop_table", Map.of()));
    }

    private void handleStore(HttpExchange exchange) throws IOException {
        authorization = exchange.getRequestHeaders().getFirst("Authorization");
        prefer = exchange.getRequestHeaders().getFirst("Prefer");
        requestPath = exchange.getRequestURI().toString();
        if (requestPath.contains("eq.403")) {
            writeJson(exchange, 403, "{\"message\":\"permission denied\"}");
            return;
        }
        if (requestPath.contains("eq.400")) {
            writeJson(exchange, 400, "{\"error\":{\"message\":\"column points does not exist\"}}");
            return;
        }
        if ("count=exact".equals(prefer)) {
            exchange.getResponseHeaders().set("Content-Range", "0-0/42");
        }
        writeJson(exchange, 200, "[{\"id\":1}]");
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
