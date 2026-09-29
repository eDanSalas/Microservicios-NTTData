package tacos.web.api.error;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import tacos.inventory.InsufficientStockException;
import tacos.physics.TacoDesignValidationException;
import tacos.web.api.dto.ApiProblem;
import tacos.web.api.dto.ApiViolation;

@RestControllerAdvice(basePackages = "tacos.web.api")
public class GlobalApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);
  private static final Comparator<ApiViolation> VIOLATION_ORDER = Comparator
      .comparing(ApiViolation::getField).thenComparing(ApiViolation::getReason);
  private final ApiProblemFactory problems;

  public GlobalApiExceptionHandler() {
    this(new ApiProblemFactory());
  }

  public GlobalApiExceptionHandler(ApiProblemFactory problems) {
    this.problems = problems;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiProblem> handleValidation(MethodArgumentNotValidException exception,
      HttpServletRequest request) {
    List<ApiViolation> violations = exception.getBindingResult().getAllErrors().stream()
        .map(this::toViolation).sorted(VIOLATION_ORDER).collect(Collectors.toList());
    return problems.response(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR,
        "One or more fields are invalid", request, violations);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiProblem> handleConstraintViolation(
      ConstraintViolationException exception, HttpServletRequest request) {
    List<ApiViolation> violations = exception.getConstraintViolations().stream()
        .map(violation -> new ApiViolation(violation.getPropertyPath().toString(),
            violation.getMessage()))
        .sorted(VIOLATION_ORDER).collect(Collectors.toList());
    return problems.response(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR,
        "One or more fields are invalid", request, violations);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiProblem> handleMalformedRequest(HttpMessageNotReadableException exception,
      HttpServletRequest request) {
    return problems.response(HttpStatus.BAD_REQUEST, ApiErrorCode.MALFORMED_REQUEST,
        "The request body is missing or malformed", request, List.of());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiProblem> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
      HttpServletRequest request) {
    return problems.response(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR,
        "One or more fields are invalid", request,
        List.of(new ApiViolation(exception.getName(), "invalid value")));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiProblem> handleMissingParameter(
      MissingServletRequestParameterException exception, HttpServletRequest request) {
    return problems.response(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR,
        "One or more fields are invalid", request,
        List.of(new ApiViolation(exception.getParameterName(), "is required")));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiProblem> handleResponseStatus(ResponseStatusException exception) {
    HttpServletRequest request = currentServletRequest();
    HttpStatus status = exception.getStatus();
    if (status.is5xxServerError()) {
      log.error("Controlled server error for {}", problems.requestPath(request), exception);
      return internalError(request);
    }
    ApiErrorCode code = exception instanceof InsufficientStockException
        ? ApiErrorCode.INSUFFICIENT_STOCK : problems.codeForStatus(status);
    String detail = exception.getReason() != null ? exception.getReason() : code.getTitle();
    return problems.response(status, code, detail, request, List.of());
  }

  @ExceptionHandler(TacoDesignValidationException.class)
  public ResponseEntity<ApiProblem> handleTacoDesign(TacoDesignValidationException exception,
      HttpServletRequest request) {
    List<ApiViolation> violations = exception.getViolations().stream()
        .map(violation -> new ApiViolation(violation.getCode(), violation.getMessage()))
        .sorted(VIOLATION_ORDER).collect(Collectors.toList());
    return problems.response(HttpStatus.UNPROCESSABLE_ENTITY,
        ApiErrorCode.BUSINESS_RULE_VIOLATION, exception.getMessage(), request, violations);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiProblem> handleUnexpectedException(Exception exception) {
    HttpServletRequest request = currentServletRequest();
    log.error("Unexpected API error for {}", problems.requestPath(request), exception);
    return internalError(request);
  }

  private HttpServletRequest currentServletRequest() {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (attrs instanceof ServletRequestAttributes) {
      return ((ServletRequestAttributes) attrs).getRequest();
    }
    return null;
  }

  private ResponseEntity<ApiProblem> internalError(HttpServletRequest request) {
    return problems.response(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR,
        "An unexpected error occurred", request, List.of());
  }

  private ApiViolation toViolation(ObjectError error) {
    String field = error instanceof FieldError ? ((FieldError) error).getField()
        : error.getObjectName();
    return new ApiViolation(field,
        error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid value");
  }
}
