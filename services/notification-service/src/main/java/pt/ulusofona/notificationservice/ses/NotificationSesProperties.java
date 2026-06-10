package pt.ulusofona.notificationservice.ses;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for SES email sending.
 * Set {@code cloud.ses.enabled=false} to only log (no real email sent).
 */
@ConfigurationProperties(prefix = "cloud.ses")
public class NotificationSesProperties {

    private boolean enabled = false;
    private String region = "";
    private String fromEmail = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getFromEmail() { return fromEmail; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }
}
