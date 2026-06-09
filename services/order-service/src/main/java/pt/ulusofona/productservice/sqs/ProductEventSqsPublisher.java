package pt.ulusofona.productservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.ulusofona.productservice.event.ProductCreatedSqsEvent;
import pt.ulusofona.productservice.model.Product;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

/**
 * Sends {@link ProductCreatedSqsEvent} JSON to the configured SQS queue.
 */
@Slf4j
@RequiredArgsConstructor
public class ProductEventSqsPublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public void publishProductCreated(Product product) throws Exception {
        ProductCreatedSqsEvent event = ProductCreatedSqsEvent.fromProduct(product);
        String body = objectMapper.writeValueAsString(event);

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .messageAttributes(Map.of(
                        "eventType",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(event.eventType())
                                .build()
                ))
                .build();

        sqsClient.sendMessage(request);
        log.debug("Published ProductCreatedSqsEvent for productId={}", event.productId());
    }
}
