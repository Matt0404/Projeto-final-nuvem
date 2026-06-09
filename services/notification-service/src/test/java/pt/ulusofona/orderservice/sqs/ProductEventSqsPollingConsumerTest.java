package pt.ulusofona.orderservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductEventSqsPollingConsumerTest {

    @Mock
    private SqsClient sqsClient;

    private ProductEventSqsPollingConsumer consumer;
    private OrderProductEventsSqsProperties properties;

    @BeforeEach
    void setUp() {
        properties = new OrderProductEventsSqsProperties();
        properties.setQueueUrl("https://sqs.example.com/123/product-events");
        properties.setMaxNumberOfMessages(10);
        properties.setWaitTimeSeconds(20);
        consumer = new ProductEventSqsPollingConsumer(
                sqsClient,
                new ObjectMapper().findAndRegisterModules(),
                properties
        );
    }

    @Test
    void pollQueue_deletesAfterSuccessfulParse() throws Exception {
        ProductCreatedSqsPayload payload = new ProductCreatedSqsPayload(
                "ProductCreated",
                7L,
                "Book",
                new BigDecimal("12.50"),
                Instant.parse("2026-01-01T12:00:00Z")
        );
        String body = new ObjectMapper().findAndRegisterModules().writeValueAsString(payload);

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(
                ReceiveMessageResponse.builder()
                        .messages(
                                Message.builder()
                                        .messageId("m1")
                                        .receiptHandle("rh1")
                                        .body(body)
                                        .build()
                        )
                        .build()
        );

        consumer.pollQueue();

        ArgumentCaptor<DeleteMessageRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient).deleteMessage(deleteCaptor.capture());
        assertEquals("https://sqs.example.com/123/product-events", deleteCaptor.getValue().queueUrl());
        assertEquals("rh1", deleteCaptor.getValue().receiptHandle());
    }
}
