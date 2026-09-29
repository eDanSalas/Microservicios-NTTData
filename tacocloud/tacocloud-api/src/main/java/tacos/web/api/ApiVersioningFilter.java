package tacos.web.api;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiVersioningFilter extends OncePerRequestFilter {

  public static final String ORIGINAL_REQUEST_URI = ApiVersioningFilter.class.getName() + ".originalUri";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String context = request.getContextPath();
    String path = request.getRequestURI().substring(context.length());
    if (path.equals("/api/v1") || path.startsWith("/api/v1/")) {
      request.setAttribute(ORIGINAL_REQUEST_URI, request.getRequestURI());
      String target = "/api" + path.substring(7);
      chain.doFilter(new VersionedRequest(request, context + target, target), response);
      return;
    }
    if (path.equals("/api") || path.startsWith("/api/")) {
      response.setHeader("Deprecation", "true");
      response.setHeader("Sunset", "Fri, 31 Dec 2027 23:59:59 GMT");
      response.setHeader("Link", "</api/v1" + path.substring(4)
          + ">; rel=\"successor-version\"");
    }
    chain.doFilter(request, response);
  }

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    return false;
  }

  private static class VersionedRequest extends HttpServletRequestWrapper {

    private final String requestUri;
    private final String servletPath;

    VersionedRequest(HttpServletRequest request, String requestUri, String servletPath) {
      super(request);
      this.requestUri = requestUri;
      this.servletPath = servletPath;
    }

    @Override
    public String getRequestURI() {
      return requestUri;
    }

    @Override
    public String getServletPath() {
      return servletPath;
    }
  }
}
