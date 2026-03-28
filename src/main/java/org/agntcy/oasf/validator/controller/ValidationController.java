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
   * Validates a raw OASF record JSON body against the configured OASF schema server.
   *
   * @param recordJson the OASF record to validate, as a raw JSON string
   * @param schemaVersion optional schema version override (e.g. {@code "1.0.0"}); when omitted, the
   *     version is extracted from the record's {@code schema_version} field
   * @return {@code 200} with the {@link ValidationResult} when valid, {@code 422} with the {@link
   *     ValidationResult} (including errors) when invalid
   */
  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ValidationResult> validate(
      @RequestBody final String recordJson,
      @RequestParam(required = false) final String schemaVersion) {
    final ValidationResult result =
        Objects.nonNull(schemaVersion)
            ? validator.validateRecord(recordJson, schemaVersion)
            : validator.validateRecord(recordJson);
    if (result.valid()) {
      return ResponseEntity.ok(result);
    }
    return ResponseEntity.unprocessableEntity().body(result);
  }
}
