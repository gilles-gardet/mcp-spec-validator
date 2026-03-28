package org.agntcy.oasf.validator.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Integration tests for {@link ValidationController}.
 * These tests download the JSON Schema from the live OASF schema server on first run —
 * an active internet connection is required.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ValidationControllerIntegrationTest {

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

    private static final String INVALID_MCP_RECORD = """
        {
          "schema_version": "1.0.0",
          "version": "v0.1.0",
          "name": "example.org/invalid-mcp-server",
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
                "name": "invalid-mcp-server"
              }
            }
          ]
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("🎉 POST /validate with a valid record should return 200 with valid=true")
    void postValidateValidRecord() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_MCP_RECORD))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.valid").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("💩 POST /validate with an invalid record should return 422 with errors")
    void postValidateInvalidRecord() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_MCP_RECORD))
            .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
            .andExpect(MockMvcResultMatchers.jsonPath("$.valid").value(false))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("🎉 POST /validate?schemaVersion=1.0.0 should use the explicit version")
    void postValidateWithExplicitSchemaVersion() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .param("schemaVersion", "1.0.0")
                .content(VALID_MCP_RECORD))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.valid").value(true));
    }
}
