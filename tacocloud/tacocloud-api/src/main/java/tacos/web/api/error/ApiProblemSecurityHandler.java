package tacos.web.api.error;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import tacos.web.api.dto.ApiProblem;

@Component("apiProblemSecurityHandler")
public class ApiProblemSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ApiProblemFactory problems;
  private final ObjectMapper objectMapper;

  public ApiProblemSecurityHandler(ApiProblemFactory problems, ObjectMapper objectMapper) {
    this.problems = problems;
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException {
    write(response, problems.create(HttpStatus.UNAUTHORIZED,
        ApiErrorCode.AUTHENTICATION_REQUIRED, "Authentication is required", request, List.of()));
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response,
      AccessDeniedException exception) throws IOException, ServletException {
    write(response, problems.create(HttpStatus.FORBIDDEN, ApiErrorCode.ACCESS_DENIED,
        "Access is denied", request, List.of()));
  }

  private void write(HttpServletResponse response, ApiProblem problem) throws IOException {
    response.setStatus(problem.getStatus());
    response.setContentType(ApiProblemFactory.PROBLEM_JSON.toString());
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
