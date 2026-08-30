package com.washer.backend.integration.points;

public record PointFulfillmentResult(String status, String reference, String message) {

    public static PointFulfillmentResult completed(String reference) {
        return new PointFulfillmentResult("completed", reference, "fulfilled");
    }

    public static PointFulfillmentResult pending(String message) {
        return new PointFulfillmentResult("pending", null, message);
    }
}
