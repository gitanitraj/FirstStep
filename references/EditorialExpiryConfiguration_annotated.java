/*
 * IM-01 annotated reference — full mirror of backend/src/main/java/org/firststep/backend/shared/lifecycle/EditorialExpiryConfiguration.java
 *
 * WHAT: Shared configurable, Clock-driven editorial expiry.
 * WHY: PC-01 requires one editorial lifecycle interpretation; repositories retain history.
 * INTERACTION: current service queries apply EditorialLifecycle before projection/counting;
 * classification, Community selection and article approval retain their existing owners.
 * ALTERNATIVE: filtering separately in every destination would duplicate date/status policy.
 * Event occurrence is deliberately separate (PC-02 / IM-10). See the IM-01 execution report.
 */
package org.firststep.backend.shared.lifecycle;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Community date interpretation and the single configurable PC-01 threshold. */
@Configuration
@ConfigurationProperties(prefix = "app.editorial-expiry")
public class EditorialExpiryConfiguration {
    private int approachingDays = 30;
    private String defaultZone = "America/New_York";
    private Map<String, String> communityZones = new LinkedHashMap<>();

    public int getApproachingDays() { return approachingDays; }
    public void setApproachingDays(int approachingDays) { this.approachingDays = approachingDays; }
    public String getDefaultZone() { return defaultZone; }
    public void setDefaultZone(String defaultZone) { this.defaultZone = defaultZone; }
    public Map<String, String> getCommunityZones() { return communityZones; }
    public void setCommunityZones(Map<String, String> communityZones) { this.communityZones = communityZones; }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock editorialClock() {
        return Clock.systemUTC();
    }
}
