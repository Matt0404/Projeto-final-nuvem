package pt.ulusofona.productservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.ulusofona.productservice.model.Product;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductEventSqsPublisherTest {

    @Mock
    private SqsClient sqsClient;

    private ProductEventSqsPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ProductEventSqsPublisher(
                sqsClient,
                new ObjectMapper().findAndRegisterModules(),
                "https://sqs.example.com/123/product-events"
        );
    }

    @Test
    void publishProductCreated_sendsJsonWithEventType() throws Exception {
        Product product = new Product();
        product.setId(42L);
        product.setName("Widget");
        product.setDescription("D");
        product.setPrice(new BigDecimal("9.99"));
        product.setStockQuantity(3);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        publisher.publishProductCreated(product);

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        SendMessageRequest req = captor.getValue();
        assertEquals("https://sqs.example.com/123/product-events", req.queueUrl());
        assertTrue(req.messageBody().contains("\"eventType\":\"ProductCreated\""));
        assertTrue(req.messageBody().contains("\"productId\":42"));
        assertTrue(req.messageAttributes().containsKey("eventType"));
    }
}
