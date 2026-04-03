package org.agntcy.oasf.validator.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import java.util.List;
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

  /**
   * Creates a new validator from the given configuration properties.
   *
   * @param properties OASF validator configuration (default schema version)
   * @param schemaCache cache that loads and stores JSON Schemas by version
   * @param objectMapper Jackson mapper used to parse the MCP module JSON
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
   * Validates an MCP module JSON string against the OASF MCP module schema. Uses the default
   * schema version configured in the application properties.
   *
   * @param moduleJson the MCP module as a JSON string (e.g. {@code {"name":
   *     "integration/mcp","data":{...}}})
   * @return the validation outcome with error messages
   */
  public ValidationResult validateModule(final String moduleJson) {
    return validateModule(moduleJson, defaultSchemaVersion);
  }

  /**
   * Validates an MCP module JSON string against a specific OASF MCP schema version.
   *
   * @param moduleJson the MCP module as a JSON string
   * @param schemaVersion the schema version to validate against (e.g. {@code "1.0.0"})
   * @return the validation outcome with error messages
   */
  public ValidationResult validateModule(final String moduleJson, final String schemaVersion) {
    try {
      objectMapper.readTree(moduleJson);
    } catch (final JsonProcessingException exception) {
      throw new OasfValidationException("Failed to parse module JSON", exception);
    }
    final List<Error> violations =
        schemaCache.getSchema(schemaVersion).validate(moduleJson, InputFormat.JSON);
    final List<String> errors = violations.stream().map(Error::getMessage).toList();
    if (errors.isEmpty()) {
      return ValidationResult.valid(List.of());
    }
    return ValidationResult.invalid(errors, List.of());
  }
}
