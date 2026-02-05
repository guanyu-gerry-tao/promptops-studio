package com.promptops.platformapi.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom exception for business logic errors.
 * Carries an HTTP status code so the controller layer knows
 * what status to return to the client (e.g. 404 NOT_FOUND, 409 CONFLICT).
 *
 * <p>Usage: {@code throw new BusinessException(HttpStatus.NOT_FOUND, "Project not found");}
 */
public class BusinessException extends RuntimeException {

  private final HttpStatus status;

  public BusinessException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
