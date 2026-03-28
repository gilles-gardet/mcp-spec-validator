package org.agntcy.oasf.validator.error;

import org.agntcy.oasf.validator.model.ValidationResult;

/**
 * Thrown when the validation process itself fails (schema download error, bad JSON, etc.). Distinct
 * from a record being invalid — that is expressed via {@link ValidationResult}.
 */
public class OasfValidationException extends RuntimeException {

  /**
   * Creates an exception with a descriptive message about the validation failure.
   *
   * @param message human-readable description of what went wrong
   */
  public OasfValidationException(final String message) {
    super(message);
  }

  /**
   * Creates an exception wrapping a lower-level cause, preserving the original stack trace.
   *
   * @param message human-readable description of what went wrong
   * @param cause the underlying exception that triggered this failure
   */
  public OasfValidationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
