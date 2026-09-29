package tacos.observability;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CorrelationIdWebFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(CorrelationIdWebFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    Object current = request.getAttribute(CorrelationIds.CONTEXT_KEY);
    String correlationId = current != null ? current.toString()
        : CorrelationIds.resolve(request.getHeader(CorrelationIds.HEADER));
    request.setAttribute(CorrelationIds.CONTEXT_KEY, correlationId);
    response.setHeader(CorrelationIds.HEADER, correlationId);
    try (MDC.MDCCloseable ignored = MDC.putCloseable(CorrelationIds.MDC_KEY, correlationId)) {
      log.info("HTTP request started: method={}, path={}", request.getMethod(), request.getRequestURI());
      chain.doFilter(request, response);
      log.info("HTTP request completed: method={}, path={}, status={}", request.getMethod(),
          request.getRequestURI(), response.getStatus());
    } finally {
      MDC.remove(CorrelationIds.MDC_KEY);
    }
  }

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }
}
