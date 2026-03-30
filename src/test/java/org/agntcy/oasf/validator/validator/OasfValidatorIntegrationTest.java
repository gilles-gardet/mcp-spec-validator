package org.agntcy.oasf.validator.validator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for {@link OasfValidator}.
 *
 * <p>The expected input is an MCP module entry validated against the OASF MCP module schema:
 *
 * <pre>
 * {
 *   "name": "integration/mcp",
 *   "data": {
 *     "name": "server-name",
 *     "connections": [ { "url": "https://..." } ]
 *   }
 * }
 * </pre>
 */
@SpringBootTest
class OasfValidatorIntegrationTest {

    /** Minimal valid MCP module with a streamable-HTTP connection. */
    private static final String VALID_MCP_MODULE = """
        {
          "name": "integration/mcp",
          "data": {
            "name": "my-mcp-server",
            "connections": [
              { "type": "streamable-http", "url": "https://www.example.com/mcp" }
            ]
          }
        }
        """;

    /** Valid MCP module using a stdio (command-based) connection. */
    private static final String VALID_MCP_MODULE_STDIO = """
        {
          "name": "integration/mcp",
          "data": {
            "name": "my-stdio-server",
            "connections": [
              { "type": "stdio", "command": "npx", "args": ["-y", "@modelcontextprotocol/server-everything"] }
            ]
          }
        }
        """;

    /**
     * Invalid MCP module: the {@code connections} array is missing from {@code data},
     * which is a required field per the OASF MCP module schema.
     */
    private static final String INVALID_MCP_MODULE_MISSING_CONNECTIONS = """
        {
          "name": "integration/mcp",
          "data": {
            "name": "broken-server"
          }
        }
        """;

    /**
     * Invalid MCP module: a connection contains an unknown property.
     * The {@code mcp_server_connection} object uses {@code additionalProperties: false}.
     */
    private static final String INVALID_MCP_MODULE_UNKNOWN_FIELD = """
        {
          "name": "integration/mcp",
          "data": {
            "name": "broken-server",
            "connections": [
              {
                "type": "streamable-http",
                "url": "https://www.example.com/mcp",
                "unknown_field": "not allowed"
              }
            ]
          }
        }
        """;

    @Autowired
    private OasfValidator validator;

    @Test
    @DisplayName("🎉 A well-formed MCP module should be valid")
    void validateModuleValidHttpConnection() {
        final var result = validator.validateModule(VALID_MCP_MODULE);
        Assertions.assertThat(result.valid()).isTrue();
        Assertions.assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("🎉 A well-formed MCP module with a stdio connection should be valid")
    void validateModuleValidStdioConnection() {
        final var result = validator.validateModule(VALID_MCP_MODULE_STDIO);
        Assertions.assertThat(result.valid()).isTrue();
        Assertions.assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("💩 A module missing the required connections field should be invalid")
    void validateModuleMissingConnections() {
        final var result = validator.validateModule(INVALID_MCP_MODULE_MISSING_CONNECTIONS);
        Assertions.assertThat(result.valid()).isFalse();
        Assertions.assertThat(result.errors())
            .isNotEmpty()
            .anyMatch(msg -> msg.contains("connections"));
        result.errors().forEach(error -> System.out.println("Validation error: " + error));
    }

    @Test
    @DisplayName("💩 A module with an unknown field in a connection should be invalid")
    void validateModuleUnknownFieldInConnection() {
        final var result = validator.validateModule(INVALID_MCP_MODULE_UNKNOWN_FIELD);
        Assertions.assertThat(result.valid()).isFalse();
        Assertions.assertThat(result.errors())
            .isNotEmpty()
            .anyMatch(msg -> msg.contains("unknown_field"));
        result.errors().forEach(error -> System.out.println("Validation error: " + error));
    }

    @Test
    @DisplayName("🎉 Schema version passed explicitly should validate against the correct schema")
    void validateModuleWithExplicitSchemaVersion() {
        final var result = validator.validateModule(VALID_MCP_MODULE, "1.0.0");
        Assertions.assertThat(result.valid()).isTrue();
        Assertions.assertThat(result.errors()).isEmpty();
    }
}
