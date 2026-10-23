package com.guardianai.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties (prefix = "guardian.rate-limit")
public record RateLimitProperties(
    //if yaml properties are missing then the default values are used
    @DefaultValue ("true") boolean enabled,
    @DefaultValue ("5") int loginPerMinute,
    @DefaultValue ("5") int registerPerMinute,
    @DefaultValue ("30") int scanPerMinute,
    @DefaultValue ("120") int defaultPerMinute
) {
    
}
