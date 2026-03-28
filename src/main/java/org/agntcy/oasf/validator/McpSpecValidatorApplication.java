package org.agntcy.oasf.validator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point to boostrap the MCP Spec Validator application. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class McpSpecValidatorApplication {

  /**
   * Starts the Spring Boot application.
   *
   * @param args command-line arguments forwarded to the Spring context
   */
  public static void main(final String[] args) {
    SpringApplication.run(McpSpecValidatorApplication.class, args);
  }
}
