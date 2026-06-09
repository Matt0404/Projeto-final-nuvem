package pt.ulusofona.productservice.event;

import pt.ulusofona.productservice.model.Product;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payload published to SQS when a new product is created (Week 10 / cloud messaging lab).
 * <p>
 * JSON field names are stable so the order-service consumer can deserialize the same shape.
 */
public record ProductCreatedSqsEvent(
        String eventType,
        Long productId,
        String name,
        BigDecimal price,
        Instant occurredAt
) {

    public static ProductCreatedSqsEvent fromProduct(Product product) {
        return new ProductCreatedSqsEvent(
                "ProductCreated",
                product.getId(),
                product.getName(),
                product.getPrice(),
                Instant.now()
        );
    }
}
