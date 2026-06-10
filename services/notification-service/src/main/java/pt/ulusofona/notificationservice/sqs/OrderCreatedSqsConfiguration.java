package pt.ulusofona.notificationservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.ulusofona.notificationservice.ses.NotificationSesProperties;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@Configuration
@ConditionalOnProperty(prefix = "cloud.sqs.order-created-consumer", name = "enabled", havingValue = "true")
public class OrderCreatedSqsConfiguration {

    @Bean(destroyMethod = "close")
    public SqsClient orderCreatedSqsClient(OrderCreatedSqsProperties properties) {
        if (properties.getQueueUrl() == null || properties.getQueueUrl().isBlank()) {
            throw new IllegalStateException(
                    "cloud.sqs.order-created-consumer.queue-url must be set when SQS consumer is enabled"
            );
        }
        SqsClientBuilder builder = SqsClient.builder();
        if (properties.getRegion() != null && !properties.getRegion().isBlank()) {
            builder = builder.region(Region.of(properties.getRegion()));
        }
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    public SesClient notificationSesClient(NotificationSesProperties sesProperties) {
        software.amazon.awssdk.services.ses.SesClientBuilder builder = SesClient.builder();
        if (sesProperties.getRegion() != null && !sesProperties.getRegion().isBlank()) {
            builder = builder.region(Region.of(sesProperties.getRegion()));
        }
        return builder.build();
    }

    @Bean
    public OrderCreatedSqsPollingConsumer orderCreatedSqsPollingConsumer(
            SqsClient orderCreatedSqsClient,
            SesClient notificationSesClient,
            ObjectMapper objectMapper,
            OrderCreatedSqsProperties sqsProperties,
            NotificationSesProperties sesProperties
    ) {
        return new OrderCreatedSqsPollingConsumer(
                orderCreatedSqsClient, notificationSesClient,
                objectMapper, sqsProperties, sesProperties
        );
    }
}
