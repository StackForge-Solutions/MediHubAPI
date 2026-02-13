package com.MediHubAPI.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.idempotency")
public class IdempotencyProperties {

    /**
     * Time-to-live for idempotency records (in hours).
     */
    private long ttlHours = 48;
}
