package pt.ulusofona.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Order Service microservice.
 *
 * <p>Uses Spring Boot auto-configuration to set up the application context, including:
 * <ul>
 *   <li>Spring Data JPA for database access</li>
 *   <li>Spring Web for REST API endpoints</li>
 *   <li>AWS SQS for asynchronous messaging</li>
 *   <li>OpenFeign for synchronous inter-service communication (Product Service)</li>
 *   <li>Spring Boot Actuator for health checks and monitoring</li>
 * </ul>
 *
 * <p>The service runs on port 8083 by default (configured in application.yml).
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}