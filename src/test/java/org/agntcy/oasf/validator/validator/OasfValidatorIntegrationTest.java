package org.agntcy.oasf.validator.validator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for {@link OasfValidator}.
 * These tests download the JSON Schema from the live OASF schema server on first run —
 * an active internet connection is required.
 *
 * <p>The OASF 1.0.0 record format for an MCP server:
 * <pre>
 * {
 *   "schema_version": "1.0.0",
 *   "version": "v0.1.0",
 *   "name":        "org/agent-name",
 *   "description": "...",
 *   "authors":     ["..."],
 *   "created_at":  "ISO-8601",
 *   "skills": [...],
 *   "modules": [
 *     {
 *       "name": "integration/mcp",
 *       "data": {
 *         "name": "server-name",
 *         "connections": [ { "type": "streamable-http", "url": "https://..." } ]
 *       }
 *     }
 *   ]
 * }
 * </pre>
 */
@SpringBootTest
class OasfValidatorIntegrationTest {

    /** Minimal valid OASF record with a well-formed MCP module. */
    private static final String VALID_MCP_RECORD = """
        {
          "schema_version": "1.0.0",
          "version": "v0.1.0",
          "name": "example.org/my-mcp-server",
          "description": "A test MCP server",
          "authors": ["Test Corp"],
          "created_at": "2025-01-01T00:00:00Z",
          "skills": [
            { "name": "natural_language_processing/natural_language_understanding", "id": 101 }
          ],
          "modules": [
            {
              "name": "integration/mcp",
              "data": {
                "name": "my-mcp-server",
                "connections": [
                  { "type": "streamable-http", "url": "https://www.example.com/mcp" }
                ]
              }
            }
          ]
        }
        """;

    /**
     * Invalid MCP record: the {@code connections} array is missing from the MCP module data,
     * which is a required field per the OASF MCP module schema.
     */
    private static final String INVALID_MCP_RECORD_MISSING_CONNECTIONS = """
        {
          "schema_version": "1.0.0",
          "version": "v0.1.0",
          "name": "example.org/shots-occasional-into",
          "description": "An MCP server record missing the required connections field",
          "authors": ["Test Corp"],
          "created_at": "2025-01-01T00:00:00Z",
          "skills": [
            { "name": "natural_language_processing/natural_language_understanding", "id": 101 }
          ],
          "modules": [
            {
              "name": "integration/mcp",
              "data": {
                "name": "shots occasional into"
              }
            }
          ]
        }
        """;

    /**
     * Invalid MCP record: a connection contains an unknown property.
     * The {@code mcp_server_connection} object uses {@code additionalProperties: false}.
     */
    private static final String INVALID_MCP_RECORD_UNKNOWN_FIELD = """
        {
          "schema_version": "1.0.0",
          "version": "v0.1.0",
          "name": "example.org/shots-occasional-into",
          "description": "An MCP server record with an unknown field in a connection",
          "authors": ["Test Corp"],
          "created_at": "2025-01-01T00:00:00Z",
          "skills": [
            { "name": "natural_language_processing/natural_language_understanding", "id": 101 }
          ],
          "modules": [
            {
              "name": "integration/mcp",
              "data": {
                "name": "shots occasional into",
                "connections": [
                  {
                    "type": "streamable-http",
                    "url": "https://www.above.coop",
                    "unknown_field": "not allowed"
                  }
                ]
              }
            }
          ]
        }
        """;

    @Autowired
    private OasfValidator validator;

    @Test
    @DisplayName("🎉 A well-formed MCP record should be valid")
    void validateRecordValidMcpSpec() {
        final var result = validator.validateRecord(VALID_MCP_RECORD);
        Assertions.assertThat(result.valid()).isTrue();
        Assertions.assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("💩 A record missing the required connections field should be invalid")
    void validateRecordMissingConnections() {
        final var result = validator.validateRecord(INVALID_MCP_RECORD_MISSING_CONNECTIONS);
        Assertions.assertThat(result.valid()).isFalse();
        Assertions.assertThat(result.errors())
            .isNotEmpty()
            .anyMatch(msg -> msg.contains("connections"));
        result.errors().forEach(error -> System.out.println("Validation error: " + error));
    }

    @Test
    @DisplayName("💩 A record with an unknown field in a connection should be invalid")
    void validateRecordUnknownFieldInConnection() {
        final var result = validator.validateRecord(INVALID_MCP_RECORD_UNKNOWN_FIELD);
        Assertions.assertThat(result.valid()).isFalse();
        Assertions.assertThat(result.errors())
            .isNotEmpty()
            .anyMatch(msg -> msg.contains("unknown_field"));
        result.errors().forEach(error -> System.out.println("Validation error: " + error));
    }

    @Test
    @DisplayName("🎉 Schema version passed explicitly should validate against the correct schema")
    void validateRecordWithExplicitSchemaVersion() {
        final var result = validator.validateRecord(VALID_MCP_RECORD, "1.0.0");
        Assertions.assertThat(result.valid()).isTrue();
        Assertions.assertThat(result.errors()).isEmpty();
    }
}
