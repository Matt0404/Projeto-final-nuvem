package pt.ulusofona.productservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

/**
 * Wires AWS SDK SQS client and {@link ProductEventSqsPublisher} when {@code cloud.sqs.product-events.enabled=true}.
 */
@Configuration
@ConditionalOnProperty(prefix = "cloud.sqs.product-events", name = "enabled", havingValue = "true")
public class ProductSqsConfiguration {

    @Bean(destroyMethod = "close")
    public SqsClient productEventsSqsClient(ProductSqsProperties properties) {
        if (properties.getQueueUrl() == null || properties.getQueueUrl().isBlank()) {
            throw new IllegalStateException(
                    "cloud.sqs.product-events.queue-url must be set when cloud.sqs.product-events.enabled=true"
            );
        }
        SqsClientBuilder builder = SqsClient.builder();
        if (properties.getRegion() != null && !properties.getRegion().isBlank()) {
            builder = builder.region(Region.of(properties.getRegion()));
        }
        return builder.build();
    }

    @Bean
    public ProductEventSqsPublisher productEventSqsPublisher(
            SqsClient productEventsSqsClient,
            ObjectMapper objectMapper,
            ProductSqsProperties properties
    ) {
        return new ProductEventSqsPublisher(productEventsSqsClient, objectMapper, properties.getQueueUrl());
    }
}
