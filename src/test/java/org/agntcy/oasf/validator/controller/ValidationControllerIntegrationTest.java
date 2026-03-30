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

/** Integration tests for {@link ValidationController}. */
@SpringBootTest
@AutoConfigureMockMvc
class ValidationControllerIntegrationTest {

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

    private static final String INVALID_MCP_MODULE = """
        {
          "name": "integration/mcp",
          "data": {
            "name": "broken-server"
          }
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("🎉 POST /validate with a valid MCP module should return 200 with valid=true")
    void postValidateValidModule() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_MCP_MODULE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.valid").value(true))
            .andExpect(MockMvcResultMatchers.jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("💩 POST /validate with an invalid MCP module should return 422 with errors")
    void postValidateInvalidModule() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_MCP_MODULE))
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
                .content(VALID_MCP_MODULE))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.valid").value(true));
    }
}
