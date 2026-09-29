package tacos.observability;

import java.util.UUID;

import reactor.util.context.ContextView;

public final class CorrelationIds {

  public static final String HEADER = "X-Correlation-Id";
  public static final String MDC_KEY = "correlationId";
  public static final String CONTEXT_KEY = CorrelationIds.class.getName();
  private CorrelationIds() {
  }

  public static String resolve(String value) {
    try {
      return value != null && value.length() <= 64 ? UUID.fromString(value).toString()
          : UUID.randomUUID().toString();
    } catch (IllegalArgumentException error) {
      return UUID.randomUUID().toString();
    }
  }

  public static String current(ContextView context) {
    return context.getOrDefault(CONTEXT_KEY, UUID.randomUUID().toString());
  }
}
