package pt.ulusofona.notificationservice.sqs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JSON shape produced by order-service when an order is created.
 * Field names must match exactly what order-service publishes to SQS.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedSqsPayload(
        String eventType,
        Long orderId,
        String customerEmail,
        BigDecimal totalAmount,
        Instant occurredAt
) {
}
