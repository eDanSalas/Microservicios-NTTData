package tacos.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class CorrelationIdWebFilterTest {

  private final CorrelationIdWebFilter filter = new CorrelationIdWebFilter();

  @AfterEach
  public void cleanMdc() {
    MDC.clear();
  }

  @Test
  public void shouldGenerateUuidWhenHeaderIsMissing() throws Exception {
    MockHttpServletRequest request = request(null);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> requestId = new AtomicReference<>();

    filter.doFilter(request, response,
        (currentRequest, currentResponse) -> requestId.set(
            (String) currentRequest.getAttribute(CorrelationIds.CONTEXT_KEY)));

    String responseId = response.getHeader(CorrelationIds.HEADER);
    assertEquals(requestId.get(), responseId);
    assertEquals(responseId, UUID.fromString(responseId).toString());
    assertNull(MDC.get(CorrelationIds.MDC_KEY));
  }

  @Test
  public void shouldPreserveValidHeader() throws Exception {
    String correlationId = UUID.randomUUID().toString();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request(correlationId), response, (request, ignored) ->
        assertEquals(correlationId, MDC.get(CorrelationIds.MDC_KEY)));

    assertEquals(correlationId, response.getHeader(CorrelationIds.HEADER));
    assertNull(MDC.get(CorrelationIds.MDC_KEY));
  }

  @Test
  public void shouldReplaceMaliciousOrLongHeaders() {
    String malicious = "bad\nvalue";
    String oversized = "a".repeat(65);
    String safeMalicious = CorrelationIds.resolve(malicious);
    String safeOversized = CorrelationIds.resolve(oversized);

    assertNotEquals(malicious, safeMalicious);
    assertNotEquals(oversized, safeOversized);
    assertEquals(safeMalicious, UUID.fromString(safeMalicious).toString());
    assertEquals(safeOversized, UUID.fromString(safeOversized).toString());
  }

  private MockHttpServletRequest request(String correlationId) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
    if (correlationId != null) request.addHeader(CorrelationIds.HEADER, correlationId);
    return request;
  }
}
