package pt.ulusofona.notificationservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import pt.ulusofona.notificationservice.ses.NotificationSesProperties;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

/**
 * Long-polls the {@code order-created} SQS queue.
 * For each event it either sends a confirmation email via SES
 * or simply logs it (when {@code cloud.ses.enabled=false}).
 */
@Slf4j
@RequiredArgsConstructor
public class OrderCreatedSqsPollingConsumer {

    private final SqsClient sqsClient;
    private final SesClient sesClient;
    private final ObjectMapper objectMapper;
    private final OrderCreatedSqsProperties sqsProperties;
    private final NotificationSesProperties sesProperties;

    @Scheduled(fixedDelayString = "${cloud.sqs.order-created-consumer.poll-interval-ms:5000}")
    public void pollQueue() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(sqsProperties.getQueueUrl())
                .maxNumberOfMessages(sqsProperties.getMaxNumberOfMessages())
                .waitTimeSeconds(sqsProperties.getWaitTimeSeconds())
                .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();

        for (Message message : messages) {
            try {
                OrderCreatedSqsPayload payload = objectMapper.readValue(
                        message.body(),
                        OrderCreatedSqsPayload.class
                );

                log.info(
                        "SQS order-created event received: eventType={} orderId={} customerEmail={} totalAmount={}",
                        payload.eventType(),
                        payload.orderId(),
                        payload.customerEmail(),
                        payload.totalAmount()
                );

                if (sesProperties.isEnabled()) {
                    sendEmail(payload);
                } else {
                    log.info("SES disabled — notification logged only for orderId={}", payload.orderId());
                }

                sqsClient.deleteMessage(
                        DeleteMessageRequest.builder()
                                .queueUrl(sqsProperties.getQueueUrl())
                                .receiptHandle(message.receiptHandle())
                                .build()
                );

            } catch (Exception ex) {
                log.error("Failed to process SQS message id={}", message.messageId(), ex);
            }
        }
    }

    private void sendEmail(OrderCreatedSqsPayload payload) {
        try {
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .source(sesProperties.getFromEmail())
                    .destination(Destination.builder()
                            .toAddresses(payload.customerEmail())
                            .build())
                    .message(software.amazon.awssdk.services.ses.model.Message.builder()
                            .subject(Content.builder()
                                    .data("Order Confirmation #" + payload.orderId())
                                    .charset("UTF-8")
                                    .build())
                            .body(Body.builder()
                                    .text(Content.builder()
                                            .data(String.format(
                                                    "Your order #%d has been placed successfully.%n" +
                                                    "Total: %.2f%n" +
                                                    "Thank you for your purchase!",
                                                    payload.orderId(),
                                                    payload.totalAmount()))
                                            .charset("UTF-8")
                                            .build())
                                    .build())
                            .build())
                    .build();

            sesClient.sendEmail(emailRequest);
            log.info("SES email sent for orderId={} to={}", payload.orderId(), payload.customerEmail());

        } catch (Exception ex) {
            log.error("Failed to send SES email for orderId={}", payload.orderId(), ex);
        }
    }
}
