package pt.ulusofona.notificationservice.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for consuming the {@code order-created} SQS queue.
 */
@ConfigurationProperties(prefix = "cloud.sqs.order-created-consumer")
public class OrderCreatedSqsProperties {

    private boolean enabled = false;
    private String queueUrl = "";
    private String region = "";
    private long pollIntervalMs = 5000L;
    private int maxNumberOfMessages = 10;
    private int waitTimeSeconds = 20;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getQueueUrl() { return queueUrl; }
    public void setQueueUrl(String queueUrl) { this.queueUrl = queueUrl; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public int getMaxNumberOfMessages() { return maxNumberOfMessages; }
    public void setMaxNumberOfMessages(int maxNumberOfMessages) { this.maxNumberOfMessages = maxNumberOfMessages; }

    public int getWaitTimeSeconds() { return waitTimeSeconds; }
    public void setWaitTimeSeconds(int waitTimeSeconds) { this.waitTimeSeconds = waitTimeSeconds; }
}
