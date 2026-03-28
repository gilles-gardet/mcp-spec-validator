package org.agntcy.oasf.validator.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.agntcy.oasf.validator.error.OasfValidationException;
import org.agntcy.oasf.validator.model.OasfValidatorProperties;
import org.agntcy.oasf.validator.model.ValidationResult;
import org.agntcy.oasf.validator.schema.OasfSchemaCache;
import org.springframework.stereotype.Component;

/** Java port of the OASF SDK Validator (Go). */
@Component
public class OasfValidator {
  private final String defaultSchemaVersion;
  private final OasfSchemaCache schemaCache;
  private final ObjectMapper objectMapper;

  private static final String SCHEMA_VERSION_FIELD = "schema_version";

  /**
   * Creates a new validator from the given configuration properties.
   *
   * @param properties OASF validator configuration (default schema version)
   * @param schemaCache cache that fetches and stores JSON Schemas by version
   * @param objectMapper Jackson mapper used to parse record JSON
   */
  public OasfValidator(
      final OasfValidatorProperties properties,
      final OasfSchemaCache schemaCache,
      final ObjectMapper objectMapper) {
    this.defaultSchemaVersion = properties.defaultSchemaVersion();
    this.schemaCache = schemaCache;
    this.objectMapper = objectMapper;
  }

  /**
   * Validates a record JSON string against the OASF schema. The schema version is extracted from
   * the {@code schema_version} field in the record. Falls back to the configured default version
   * when the field is absent.
   *
   * @param recordJson the full OASF record as a JSON string
   * @return the validation outcome with error messages
   */
  public ValidationResult validateRecord(final String recordJson) {
    final String schemaVersion = extractSchemaVersion(recordJson);
    return validateRecord(recordJson, schemaVersion);
  }

  /**
   * Validates a record JSON string against a specific schema version.
   *
   * @param recordJson the full OASF record as a JSON string
   * @param schemaVersion the schema version to validate against (e.g. {@code "1.0.0"})
   * @return the validation outcome with error messages
   */
  public ValidationResult validateRecord(final String recordJson, final String schemaVersion) {
    final JsonNode recordNode;
    try {
      recordNode = objectMapper.readTree(recordJson);
    } catch (final JsonProcessingException exception) {
      throw new OasfValidationException("Failed to parse record JSON", exception);
    }
    final Set<ValidationMessage> violations =
        schemaCache.getSchema(schemaVersion).validate(recordNode);
    final List<String> errors = violations.stream().map(ValidationMessage::getMessage).toList();
    if (errors.isEmpty()) {
      return ValidationResult.valid(List.of());
    }
    return ValidationResult.invalid(errors, List.of());
  }

  private String extractSchemaVersion(final String recordJson) {
    try {
      final JsonNode root = objectMapper.readTree(recordJson);
      final JsonNode versionNode = root.get(SCHEMA_VERSION_FIELD);
      if (Objects.nonNull(versionNode) && versionNode.isTextual()) {
        return versionNode.asText();
      }
    } catch (final JsonProcessingException exception) {
      throw new OasfValidationException("Failed to parse record JSON", exception);
    }
    return defaultSchemaVersion;
  }
}
