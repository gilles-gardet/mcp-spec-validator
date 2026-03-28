package org.agntcy.oasf.validator.model;

import java.util.List;

/** The outcome of a record validation. */
public record ValidationResult(boolean valid, List<String> errors, List<String> warnings) {

  /** Factory for a fully-valid result. */
  public static ValidationResult valid(final List<String> warnings) {
    return new ValidationResult(true, List.of(), warnings);
  }

  /** Factory for an invalid result. */
  public static ValidationResult invalid(final List<String> errors, final List<String> warnings) {
    return new ValidationResult(false, errors, warnings);
  }
}
