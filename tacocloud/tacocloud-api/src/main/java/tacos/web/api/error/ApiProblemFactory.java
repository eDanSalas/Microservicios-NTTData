package tacos.web.api.error;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import tacos.web.api.ApiVersioningFilter;
import tacos.web.api.dto.ApiProblem;
import tacos.web.api.dto.ApiViolation;

@Component
public class ApiProblemFactory {

  public static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

  public ApiProblem create(HttpStatus status, ApiErrorCode code, String detail,
      HttpServletRequest request, List<ApiViolation> violations) {
    return ApiProblem.builder().type(URI.create("urn:tacocloud:problem:" + code.value()))
        .title(code.getTitle()).status(status.value()).detail(detail)
        .instance(URI.create(requestPath(request))).code(code.name()).violations(violations).build();
  }

  public ResponseEntity<ApiProblem> response(HttpStatus status, ApiErrorCode code, String detail,
      HttpServletRequest request, List<ApiViolation> violations) {
    return ResponseEntity.status(status).contentType(PROBLEM_JSON)
        .body(create(status, code, detail, request, violations));
  }

  public ApiErrorCode codeForStatus(HttpStatus status) {
    switch (status) {
      case BAD_REQUEST:
        return ApiErrorCode.MALFORMED_REQUEST;
      case UNAUTHORIZED:
        return ApiErrorCode.AUTHENTICATION_REQUIRED;
      case FORBIDDEN:
        return ApiErrorCode.ACCESS_DENIED;
      case NOT_FOUND:
        return ApiErrorCode.RESOURCE_NOT_FOUND;
      case CONFLICT:
        return ApiErrorCode.RESOURCE_CONFLICT;
      case UNPROCESSABLE_ENTITY:
        return ApiErrorCode.BUSINESS_RULE_VIOLATION;
      default:
        return status.is5xxServerError() ? ApiErrorCode.INTERNAL_ERROR : ApiErrorCode.REQUEST_REJECTED;
    }
  }

  public String requestPath(HttpServletRequest request) {
    if (request == null) return "/api";
    Object original = request.getAttribute(ApiVersioningFilter.ORIGINAL_REQUEST_URI);
    if (original != null) return original.toString();
    Object errorPath = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    return errorPath != null ? errorPath.toString() : request.getRequestURI();
  }

  public Map<String, Object> body(ApiProblem problem) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", problem.getType());
    body.put("title", problem.getTitle());
    body.put("status", problem.getStatus());
    body.put("detail", problem.getDetail());
    body.put("instance", problem.getInstance());
    body.put("code", problem.getCode());
    body.put("violations", problem.getViolations());
    return body;
  }
}
