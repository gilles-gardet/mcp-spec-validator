package org.agntcy.oasf.validator.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.agntcy.oasf.validator.error.OasfValidationException;
import org.springframework.stereotype.Component;

/** Loads and caches OASF JSON Schemas bundled in the application classpath. */
@Component
public class OasfSchemaCache {
  private final ObjectMapper objectMapper;
  private final Map<String, JsonSchema> cache = new ConcurrentHashMap<>();

  private static final String SCHEMA_RESOURCE_PATTERN = "/schemas/%s/record.json";

  /**
   * Constructor.
   *
   * @param objectMapper Jackson mapper used to parse the bundled schema JSON
   */
  public OasfSchemaCache(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Returns the compiled {@link JsonSchema} for the given OASF version.
   *
   * @param version the OASF schema version (e.g. {@code "1.0.0"})
   * @return the compiled schema ready for local validation
   * @throws OasfValidationException if no bundled schema exists for the requested version
   */
  public JsonSchema getSchema(final String version) {
    return cache.computeIfAbsent(version, this::loadSchema);
  }

  private JsonSchema loadSchema(final String version) {
    final String resourcePath = SCHEMA_RESOURCE_PATTERN.formatted(version);
    try (final InputStream schemaStream = OasfSchemaCache.class.getResourceAsStream(resourcePath)) {
      if (Objects.isNull(schemaStream)) {
        final var errorMessage =
            "No bundled schema found for version: %s (expected classpath resource: %s)"
                .formatted(version, resourcePath);
        throw new OasfValidationException(errorMessage);
      }
      final ObjectNode schemaNode = (ObjectNode) objectMapper.readTree(schemaStream);
      schemaNode.remove("$schema");
      final JsonSchemaFactory factory =
          JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
      return factory.getSchema(schemaNode);
    } catch (final IOException exception) {
      final var errorMessage = "Failed to load schema for version: %s".formatted(version);
      throw new OasfValidationException(errorMessage, exception);
    }
  }
}
