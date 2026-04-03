package org.agntcy.oasf.validator.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.agntcy.oasf.validator.error.OasfValidationException;
import org.springframework.stereotype.Component;

/** Loads and caches OASF JSON Schemas bundled in the application classpath. */
@Component
public class OasfSchemaCache {
  private final ObjectMapper objectMapper;
  private final Map<String, Schema> cache = new ConcurrentHashMap<>();

  private static final String SCHEMA_RESOURCE_PATTERN = "/schemas/%s/mcp.json";

  /**
   * Constructor.
   *
   * @param objectMapper Jackson mapper used to parse the bundled schema JSON
   */
  public OasfSchemaCache(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Returns the compiled {@link Schema} for the given OASF version.
   *
   * @param version the OASF schema version (e.g. {@code "1.0.0"})
   * @return the compiled schema ready for local validation
   * @throws OasfValidationException if no bundled schema exists for the requested version
   */
  public Schema getSchema(final String version) {
    return cache.computeIfAbsent(version, this::loadSchema);
  }

  private Schema loadSchema(final String version) {
    final String resourcePath = SCHEMA_RESOURCE_PATTERN.formatted(version);
    try (final InputStream schemaStream = OasfSchemaCache.class.getResourceAsStream(resourcePath)) {
      if (Objects.isNull(schemaStream)) {
        throw new OasfValidationException(
            "No bundled schema found for version: %s (expected classpath resource: %s)"
                .formatted(version, resourcePath));
      }
      final byte[] schemaBytes = schemaStream.readAllBytes();
      final String schemaString = new String(schemaBytes, StandardCharsets.UTF_8);
      final JsonNode schemaNode = objectMapper.readTree(schemaBytes);
      final String schemaId = schemaNode.path("$id").asText();
      final SchemaRegistry schemaRegistry =
          SchemaRegistry.withDialect(
              Dialects.getDraft7(), builder -> builder.schemas(Map.of(schemaId, schemaString)));
      return schemaRegistry.getSchema(SchemaLocation.of(schemaId));
    } catch (final IOException exception) {
      throw new OasfValidationException(
          "Failed to load schema for version: %s".formatted(version), exception);
    }
  }
}
