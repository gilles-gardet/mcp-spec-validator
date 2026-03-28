package org.agntcy.oasf.validator.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for the OASF validator. */
@ConfigurationProperties(prefix = "oasf.validator")
public record OasfValidatorProperties(String defaultSchemaVersion) {}
