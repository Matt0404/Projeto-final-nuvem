package pt.ulusofona.productservice.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional AWS SQS integration for product-created notifications.
 * <p>
 * Students enable this from configuration / environment after creating the queue in AWS.
 */
@ConfigurationProperties(prefix = "cloud.sqs.product-events")
public class ProductSqsProperties {

    /**
     * When {@code true}, the application builds an AWS SDK {@code SqsClient}
     * and publishes after each successful product create.
     */
    private boolean enabled = false;

    /**
     * Full queue URL (e.g. {@code https://sqs.eu-west-1.amazonaws.com/123456789012/product-events}).
     */
    private String queueUrl = "";

    /**
     * Optional AWS region override (e.g. {@code eu-west-1}). When blank, the SDK default chain is used.
     */
    private String region = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public void setQueueUrl(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
