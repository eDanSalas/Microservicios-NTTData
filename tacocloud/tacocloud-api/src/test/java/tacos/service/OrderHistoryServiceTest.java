package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.OrderStatus;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;

public class OrderHistoryServiceTest {
  private OrderRepository repo;
  private OrderHistoryService service;
  private User user;

  @BeforeEach
  public void setUp() {
    repo = Mockito.mock(OrderRepository.class);
    service = new OrderHistoryService(repo, 50);
    user = Mockito.mock(User.class);
    when(user.getId()).thenReturn("user-a");
  }

  @Test
  public void shouldReturnOnlyAuthenticatedUsersOrders() {
    when(repo.findByUserId(Mockito.eq("user-a"), Mockito.any(Pageable.class)))
        .thenReturn(Flux.just(order("a")));
    when(repo.countByUserId("user-a")).thenReturn(Mono.just(1L));

    OrderHistoryPage result = service.findMine(user, 0, 10).block();

    assertEquals("a", result.getContent().get(0).getId());
    verify(repo).findByUserId(Mockito.eq("user-a"), Mockito.any(Pageable.class));
    verify(repo, never()).findAll();
  }

  @Test
  public void shouldUseStableDescendingPaginationAndReturnEmptyOutsideRange() {
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    when(repo.findByUserId(Mockito.eq("user-a"), pageable.capture())).thenReturn(Flux.empty());
    when(repo.countByUserId("user-a")).thenReturn(Mono.just(11L));

    OrderHistoryPage result = service.findMine(user, 3, 5).block();

    assertTrue(result.getContent().isEmpty());
    assertEquals(3, result.getPage());
    assertEquals(3, result.getTotalPages());
    assertEquals("placedAt: DESC,id: DESC", pageable.getValue().getSort().toString());
  }

  @Test
  public void shouldHideMissingAndForeignOrdersWithNotFound() {
    when(repo.findByIdAndUserId("foreign", "user-a")).thenReturn(Mono.empty());

    StepVerifier.create(service.findMine(user, "foreign"))
        .expectErrorMatches(error -> error instanceof ResponseStatusException
            && ((ResponseStatusException) error).getStatus().value() == 404)
        .verify();
    verify(repo, never()).findById("foreign");
  }

  @Test
  public void shouldApplyExplicitAdminFilters() {
    when(repo.findByUserIdAndStatus(Mockito.eq("user-b"), Mockito.eq(OrderStatus.READY),
        Mockito.any(Pageable.class))).thenReturn(Flux.just(order("b")));
    when(repo.countByUserIdAndStatus("user-b", OrderStatus.READY)).thenReturn(Mono.just(1L));

    OrderHistoryPage result = service.findAll("user-b", OrderStatus.READY, 0, 20).block();

    assertEquals("b", result.getContent().get(0).getId());
    verify(repo, never()).findAllBy(Mockito.any());
  }

  @Test
  public void shouldRejectInvalidPagination() {
    org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
        () -> service.findMine(user, -1, 20));
    org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
        () -> service.findMine(user, 0, 51));
  }

  private TacoOrder order(String id) {
    TacoOrder order = new TacoOrder();
    order.setId(id);
    order.setPlacedAt(new Date());
    return order;
  }
}
