package tacos.web.api.error;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import tacos.web.api.dto.ApiProblem;

@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class ApiErrorController extends BasicErrorController {

  private final ApiProblemFactory problems;
  private final ObjectMapper objectMapper;

  public ApiErrorController(ErrorAttributes errorAttributes, ServerProperties serverProperties,
      ObjectProvider<ErrorViewResolver> errorViewResolvers, ApiProblemFactory problems,
      ObjectMapper objectMapper) {
    super(errorAttributes, serverProperties.getError(),
        errorViewResolvers.orderedStream().collect(Collectors.toList()));
    this.problems = problems;
    this.objectMapper = objectMapper;
  }

  @Override
  @RequestMapping
  public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
    String path = problems.requestPath(request);
    if (!path.startsWith("/api/")) return super.error(request);
    return apiError(request);
  }

  @Override
  @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
  public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
    if (!problems.requestPath(request).startsWith("/api/")) return super.errorHtml(request, response);
    ResponseEntity<Map<String, Object>> error = apiError(request);
    response.setStatus(error.getStatusCodeValue());
    response.setContentType(ApiProblemFactory.PROBLEM_JSON.toString());
    try {
      objectMapper.writeValue(response.getOutputStream(), error.getBody());
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
    return null;
  }

  private ResponseEntity<Map<String, Object>> apiError(HttpServletRequest request) {
    HttpStatus status = getStatus(request);
    ApiErrorCode code = problems.codeForStatus(status);
    String detail = status.is5xxServerError() ? "An unexpected error occurred" : code.getTitle();
    ApiProblem problem = problems.create(status, code, detail, request, List.of());
    return ResponseEntity.status(status).contentType(ApiProblemFactory.PROBLEM_JSON)
        .body(problems.body(problem));
  }
}
