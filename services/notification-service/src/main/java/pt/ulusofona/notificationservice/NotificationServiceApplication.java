package pt.ulusofona.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import pt.ulusofona.notificationservice.sqs.OrderCreatedSqsProperties;
import pt.ulusofona.notificationservice.ses.NotificationSesProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({OrderCreatedSqsProperties.class, NotificationSesProperties.class})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
