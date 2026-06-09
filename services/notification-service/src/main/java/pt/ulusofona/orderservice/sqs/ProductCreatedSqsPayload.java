package pt.ulusofona.orderservice.sqs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JSON shape produced by product-service {@code ProductCreatedSqsEvent} (same field names).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductCreatedSqsPayload(
        String eventType,
        Long productId,
        String name,
        BigDecimal price,
        Instant occurredAt
) {
}
