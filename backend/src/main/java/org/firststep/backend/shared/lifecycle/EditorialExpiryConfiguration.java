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
