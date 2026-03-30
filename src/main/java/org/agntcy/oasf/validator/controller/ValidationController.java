package org.agntcy.oasf.validator.controller;

import org.agntcy.oasf.validator.validator.OasfValidator;
import org.agntcy.oasf.validator.model.ValidationResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/** Controller exposing the OASF record validation as an HTTP endpoint. */
@RestController
@RequestMapping("/validate")
public class ValidationController {
  private final OasfValidator validator;

  /**
   * Constructor.
   *
   * @param validator the OASF validator to delegate validation calls to
   */
  public ValidationController(final OasfValidator validator) {
    this.validator = validator;
  }

  /**
   * Validates a raw MCP module JSON body against the OASF MCP module schema.
   *
   * @param moduleJson the MCP module to validate, as a raw JSON string (e.g. {@code
   *     {"name":"integration/mcp","data":{...}}})
   * @param schemaVersion optional schema version override (e.g. {@code "1.0.0"}); when omitted, the
   *     application's default schema version is used
   * @return {@code 200} with the {@link ValidationResult} when valid, {@code 422} with the {@link
   *     ValidationResult} (including errors) when invalid
   */
  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ValidationResult> validate(
      @RequestBody final String moduleJson,
      @RequestParam(required = false) final String schemaVersion) {
    final ValidationResult result =
        Objects.nonNull(schemaVersion)
            ? validator.validateModule(moduleJson, schemaVersion)
            : validator.validateModule(moduleJson);
    if (result.valid()) {
      return ResponseEntity.ok(result);
    }
    return ResponseEntity.unprocessableEntity().body(result);
  }
}
