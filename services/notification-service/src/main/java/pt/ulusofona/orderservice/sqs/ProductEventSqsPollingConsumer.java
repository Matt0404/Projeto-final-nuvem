package pt.ulusofona.orderservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

/**
 * Long-polls the product-events queue and logs {@link ProductCreatedSqsPayload} messages.
 * <p>
 * Students only need to provision the queue and set {@code cloud.sqs.product-events-consumer.*}.
 */
@Slf4j
@RequiredArgsConstructor
public class ProductEventSqsPollingConsumer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final OrderProductEventsSqsProperties properties;

    @Scheduled(fixedDelayString = "${cloud.sqs.product-events-consumer.poll-interval-ms:5000}")
    public void pollQueue() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(properties.getQueueUrl())
                .maxNumberOfMessages(properties.getMaxNumberOfMessages())
                .waitTimeSeconds(properties.getWaitTimeSeconds())
                .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();
        for (Message message : messages) {
            try {
                ProductCreatedSqsPayload payload = objectMapper.readValue(
                        message.body(),
                        ProductCreatedSqsPayload.class
                );
                log.info(
                        "SQS product event: type={} productId={} name={} price={}",
                        payload.eventType(),
                        payload.productId(),
                        payload.name(),
                        payload.price()
                );
                sqsClient.deleteMessage(
                        DeleteMessageRequest.builder()
                                .queueUrl(properties.getQueueUrl())
                                .receiptHandle(message.receiptHandle())
                                .build()
                );
            } catch (Exception ex) {
                log.error("Failed to process SQS message id={}", message.messageId(), ex);
            }
        }
    }
}
