package pt.ulusofona.orderservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@Configuration
@ConditionalOnProperty(prefix = "cloud.sqs.product-events-consumer", name = "enabled", havingValue = "true")
public class OrderProductEventsSqsConfiguration {

    @Bean(destroyMethod = "close")
    public SqsClient orderProductEventsSqsClient(OrderProductEventsSqsProperties properties) {
        if (properties.getQueueUrl() == null || properties.getQueueUrl().isBlank()) {
            throw new IllegalStateException(
                    "cloud.sqs.product-events-consumer.queue-url must be set when SQS consumer is enabled"
            );
        }
        SqsClientBuilder builder = SqsClient.builder();
        if (properties.getRegion() != null && !properties.getRegion().isBlank()) {
            builder = builder.region(Region.of(properties.getRegion()));
        }
        return builder.build();
    }

    @Bean
    public ProductEventSqsPollingConsumer productEventSqsPollingConsumer(
            SqsClient orderProductEventsSqsClient,
            ObjectMapper objectMapper,
            OrderProductEventsSqsProperties properties
    ) {
        return new ProductEventSqsPollingConsumer(orderProductEventsSqsClient, objectMapper, properties);
    }
}
