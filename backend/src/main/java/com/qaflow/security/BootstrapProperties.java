package com.qaflow.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qaflow.bootstrap")
public record BootstrapProperties(
        boolean enabled,
        String adminUsername,
        String adminDisplayName,
        String adminPassword,
        String operatorUsername,
        String operatorDisplayName,
        String operatorPassword,
        String inspectorUsername,
        String inspectorDisplayName,
        String inspectorPassword) {
}
