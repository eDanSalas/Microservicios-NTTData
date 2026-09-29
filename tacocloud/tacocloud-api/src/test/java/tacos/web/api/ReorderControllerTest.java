package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.TacoOrder;
import tacos.User;
import tacos.service.ReorderDifference;
import tacos.service.ReorderResult;
import tacos.service.ReorderService;
import tacos.web.api.dto.ReorderRequest;
import tacos.web.api.mapper.OrderMapper;

public class ReorderControllerTest {
  @Test
  public void shouldReturnQuoteWithCurrentDifferences() {
    ReorderService service = mock(ReorderService.class);
    User user = mock(User.class);
    ReorderRequest request = request(false);
    ReorderResult result = new ReorderResult("QUOTE", "original", null,
        new BigDecimal("20.00"), new BigDecimal("24.00"), "MXN", true,
        List.of(new ReorderDifference("TOTAL", null, "20.00", "24.00")));
    when(service.reorder(user, "original", request, "retry-1")).thenReturn(Mono.just(result));
    ReorderController controller = new ReorderController(service, mock(OrderMapper.class));

    StepVerifier.create(controller.reorder("original", request, "retry-1", user))
        .assertNext(response -> {
          assertEquals(HttpStatus.OK, response.getStatusCode());
          assertEquals("QUOTE", response.getBody().getStatus());
          assertEquals(1, response.getBody().getDifferences().size());
        }).verifyComplete();
  }

  @Test
  public void shouldReturnCreatedOrder() {
    ReorderService service = mock(ReorderService.class);
    OrderMapper mapper = mock(OrderMapper.class);
    User user = mock(User.class);
    ReorderRequest request = request(true);
    TacoOrder order = new TacoOrder();
    order.setId("new-order");
    ReorderResult result = new ReorderResult("CREATED", "original", order,
        new BigDecimal("20.00"), new BigDecimal("20.00"), "MXN", false, List.of());
    when(service.reorder(user, "original", request, "retry-1")).thenReturn(Mono.just(result));
    ReorderController controller = new ReorderController(service, mapper);

    StepVerifier.create(controller.reorder("original", request, "retry-1", user))
        .assertNext(response -> assertEquals(HttpStatus.CREATED, response.getStatusCode()))
        .verifyComplete();
  }

  private ReorderRequest request(boolean confirm) {
    ReorderRequest request = new ReorderRequest();
    request.setPaymentMethodId("payment-2");
    request.setConfirmPriceChange(confirm);
    return request;
  }
}
