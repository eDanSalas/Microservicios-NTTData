package tacos.web.api.error;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import tacos.web.api.dto.ApiProblem;
import tacos.web.api.dto.ApiViolation;
import tacos.inventory.InsufficientStockException;
import tacos.physics.TacoDesignValidationException;

@RestControllerAdvice(basePackages = "tacos.web.api")
public class GlobalApiExceptionHandler {

  private static final Logger log =
      LoggerFactory.getLogger(
          GlobalApiExceptionHandler.class);

  private static final MediaType PROBLEM_JSON =
      MediaType.valueOf("application/problem+json");

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<ApiProblem>
      handleValidation(
          WebExchangeBindException exception,
          ServerWebExchange exchange) {

    List<ApiViolation> violations =
        exception.getBindingResult()
            .getAllErrors()
            .stream()
            .map(this::toViolation)
            .collect(Collectors.toList());

    return response(
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.VALIDATION_ERROR,
        "One or more fields are invalid",
        exchange,
        violations);
  }

  @ExceptionHandler(ServerWebInputException.class)
  public ResponseEntity<ApiProblem>
      handleMalformedRequest(
          ServerWebInputException exception,
          ServerWebExchange exchange) {

    return response(
        HttpStatus.BAD_REQUEST,
        ApiErrorCode.MALFORMED_REQUEST,
        "The request body is missing or malformed",
        exchange,
        List.of());
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiProblem>
      handleResponseStatus(
          ResponseStatusException exception,
          ServerWebExchange exchange) {

    HttpStatus status = exception.getStatus();

    if (status.is5xxServerError()) {
      log.error(
          "Controlled server error for {}",
          requestPath(exchange),
          exception);

      return internalError(exchange);
    }

    ApiErrorCode code = exception instanceof InsufficientStockException
        ? ApiErrorCode.INSUFFICIENT_STOCK
        : codeForStatus(status);

    String detail =
        exception.getReason() != null
            ? exception.getReason()
            : code.getTitle();

    return response(
        status,
        code,
        detail,
        exchange,
        List.of());
  }

  @ExceptionHandler(TacoDesignValidationException.class)
  public ResponseEntity<ApiProblem> handleTacoDesign(TacoDesignValidationException exception,
      ServerWebExchange exchange) {
    List<ApiViolation> violations = exception.getViolations().stream()
        .map(violation -> new ApiViolation(violation.getCode(), violation.getMessage()))
        .collect(Collectors.toList());
    return response(HttpStatus.UNPROCESSABLE_ENTITY, ApiErrorCode.BUSINESS_RULE_VIOLATION,
        exception.getMessage(), exchange, violations);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiProblem>
      handleUnexpectedException(
          Exception exception,
          ServerWebExchange exchange) {

    log.error(
        "Unexpected API error for {}",
        requestPath(exchange),
        exception);

    return internalError(exchange);
  }

  private ResponseEntity<ApiProblem>
      internalError(
          ServerWebExchange exchange) {

    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ApiErrorCode.INTERNAL_ERROR,
        "An unexpected error occurred",
        exchange,
        List.of());
  }

  private ResponseEntity<ApiProblem> response(
      HttpStatus status,
      ApiErrorCode code,
      String detail,
      ServerWebExchange exchange,
      List<ApiViolation> violations) {

    ApiProblem problem =
        ApiProblem.builder()
            .type(problemType(code))
            .title(code.getTitle())
            .status(status.value())
            .detail(detail)
            .instance(
                URI.create(requestPath(exchange)))
            .code(code.name())
            .violations(violations)
            .build();

    return ResponseEntity
        .status(status)
        .contentType(PROBLEM_JSON)
        .body(problem);
  }

  private ApiViolation toViolation(
      ObjectError error) {

    String field =
        error instanceof FieldError
            ? ((FieldError) error).getField()
            : error.getObjectName();

    String reason =
        error.getDefaultMessage() != null
            ? error.getDefaultMessage()
            : "invalid value";

    return new ApiViolation(
        field,
        reason);
  }

  private ApiErrorCode codeForStatus(
      HttpStatus status) {

    switch (status) {
      case BAD_REQUEST:
        return ApiErrorCode.MALFORMED_REQUEST;

      case UNAUTHORIZED:
        return ApiErrorCode
            .AUTHENTICATION_REQUIRED;

      case FORBIDDEN:
        return ApiErrorCode.ACCESS_DENIED;

      case NOT_FOUND:
        return ApiErrorCode.RESOURCE_NOT_FOUND;

      case CONFLICT:
        return ApiErrorCode.RESOURCE_CONFLICT;

      case UNPROCESSABLE_ENTITY:
        return ApiErrorCode
            .BUSINESS_RULE_VIOLATION;

      default:
        return ApiErrorCode.REQUEST_REJECTED;
    }
  }

  private URI problemType(
      ApiErrorCode code) {

    String value =
        code.name()
            .toLowerCase(Locale.ROOT)
            .replace('_', '-');

    return URI.create(
        "urn:tacocloud:problem:" + value);
  }

  private String requestPath(
      ServerWebExchange exchange) {

    return exchange
        .getRequest()
        .getPath()
        .value();
  }
}
