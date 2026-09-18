package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;
import tacos.inventory.InventoryService;
import tacos.service.OrderService;

public class OrderServiceTest {
    @Test
    public void shouldRejectPatchWhenUserIsNotOwner() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        User owner = Mockito.mock(User.class);
        User authenticatedUser = Mockito.mock(User.class);

        when(owner.getId()).thenReturn("owner-id");
        when(authenticatedUser.getId()).thenReturn("other-id");
        Mockito.doReturn(
                List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .when(authenticatedUser)
            .getAuthorities();

        TacoOrder order = new TacoOrder();
        order.setId("1L");
        order.setUser(owner);
        order.setDeliveryZip("12345");

        OrderPatchRequest patch = new OrderPatchRequest();
        patch.setDeliveryZip("54321");

        when(orderRepo.findById("1L"))
            .thenReturn(Mono.just(order));

        OrderService service = service(orderRepo);

        StepVerifier.create(
                service.patchOrder("1L", patch, authenticatedUser))
            .expectErrorSatisfies(error -> {
                ResponseStatusException exception =
                    assertInstanceOf(
                        ResponseStatusException.class,
                        error);

                assertEquals(
                    HttpStatus.FORBIDDEN,
                    exception.getStatus());
            })
            .verify();

        verify(orderRepo).findById("1L");
        verify(orderRepo, never())
            .save(any(TacoOrder.class));
    }

    @Test 
    public void shouldAllowPatchWhenUserIsOwner() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        User owner = Mockito.mock(User.class);
        User authenticatedUser = Mockito.mock(User.class);

        when(owner.getId()).thenReturn("owner-id");
        when(authenticatedUser.getId()).thenReturn("owner-id");
        Mockito.doReturn(
                List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .when(authenticatedUser)
            .getAuthorities();

        TacoOrder order = new TacoOrder();
        order.setId("1L");
        order.setUser(owner);
        order.setDeliveryZip("12345");
        order.setDeliveryState("Jalisco");

        when(orderRepo.save(any(TacoOrder.class)))
        .thenAnswer(invocation ->
            Mono.just(
                invocation.getArgument(0, TacoOrder.class)));

        OrderPatchRequest patch = new OrderPatchRequest();
        patch.setDeliveryZip("54321");

        when(orderRepo.findById("1L"))
            .thenReturn(Mono.just(order));

        OrderService service = service(orderRepo);

        StepVerifier.create(
                service.patchOrder(
                    "1L",
                    patch,
                    authenticatedUser))
            .expectNextMatches(updatedOrder ->
                "54321".equals(
                    updatedOrder.getDeliveryZip())
                && "Jalisco".equals(
                    updatedOrder.getDeliveryState()))
            .verifyComplete();

        verify(orderRepo).findById("1L");
        verify(orderRepo).save(any(TacoOrder.class));
    }

    @Test 
    public void shouldAllowPatchWhenUserIsAdmin() {
        OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
        User owner = Mockito.mock(User.class);
        User authenticatedUser = Mockito.mock(User.class);

        when(owner.getId()).thenReturn("owner-id");
        when(authenticatedUser.getId()).thenReturn("admin-id");
        Mockito.doReturn(
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
            .when(authenticatedUser)
            .getAuthorities();

        TacoOrder order = new TacoOrder();
        order.setId("1L");
        order.setUser(owner);
        order.setDeliveryZip("12345");
        order.setDeliveryState("Jalisco");

        when(orderRepo.save(any(TacoOrder.class)))
        .thenAnswer(invocation ->
            Mono.just(
                invocation.getArgument(0, TacoOrder.class)));

        OrderPatchRequest patch = new OrderPatchRequest();
        patch.setDeliveryZip("54321");

        when(orderRepo.findById("1L"))
            .thenReturn(Mono.just(order));

        OrderService service = service(orderRepo);

        StepVerifier.create(
                service.patchOrder(
                    "1L",
                    patch,
                    authenticatedUser))
            .expectNextMatches(updatedOrder ->
                "54321".equals(
                    updatedOrder.getDeliveryZip())
                && "Jalisco".equals(
                    updatedOrder.getDeliveryState()))
            .verifyComplete();

        verify(orderRepo).findById("1L");
        verify(orderRepo).save(any(TacoOrder.class));
    }

    @Test
    public void shouldReturnNotFoundWhenOrderDoesNotExist() {
        OrderRepository orderRepo =
            Mockito.mock(OrderRepository.class);

        User authenticatedUser =
            Mockito.mock(User.class);

        OrderPatchRequest patch =
            new OrderPatchRequest();
        patch.setDeliveryZip("54321");

        when(orderRepo.findById("missing"))
            .thenReturn(Mono.empty());

        OrderService service =
            service(orderRepo);

        StepVerifier.create(
                service.patchOrder(
                    "missing",
                    patch,
                    authenticatedUser))
            .expectErrorSatisfies(error -> {
                ResponseStatusException exception =
                    assertInstanceOf(
                        ResponseStatusException.class,
                        error);

                assertEquals(
                    HttpStatus.NOT_FOUND,
                    exception.getStatus());
            })
            .verify();

        verify(orderRepo).findById("missing");
        verify(orderRepo, never())
            .save(any(TacoOrder.class));
    }

    @Test
    public void shouldPatchStateWithoutChangingZip() {
        OrderRepository orderRepo =
            Mockito.mock(OrderRepository.class);

        User owner = Mockito.mock(User.class);
        User authenticatedUser = Mockito.mock(User.class);

        when(owner.getId()).thenReturn("owner-id");
        when(authenticatedUser.getId())
            .thenReturn("owner-id");

        Mockito.doReturn(
                List.of(
                    new SimpleGrantedAuthority("ROLE_USER")))
            .when(authenticatedUser)
            .getAuthorities();

        TacoOrder order = new TacoOrder();
        order.setId("1L");
        order.setUser(owner);
        order.setDeliveryState("Jalisco");
        order.setDeliveryZip("12345");

        OrderPatchRequest patch =
            new OrderPatchRequest();
        patch.setDeliveryState("Nayarit");

        when(orderRepo.findById("1L"))
            .thenReturn(Mono.just(order));

        when(orderRepo.save(any(TacoOrder.class)))
            .thenAnswer(invocation ->
                Mono.just(
                    invocation.getArgument(
                        0,
                        TacoOrder.class)));

        OrderService service =
            service(orderRepo);

        StepVerifier.create(
                service.patchOrder(
                    "1L",
                    patch,
                    authenticatedUser))
            .expectNextMatches(updatedOrder ->
                "Nayarit".equals(
                    updatedOrder.getDeliveryState())
                && "12345".equals(
                    updatedOrder.getDeliveryZip()))
            .verifyComplete();

        verify(orderRepo).findById("1L");
        verify(orderRepo).save(any(TacoOrder.class));
    }

    @Test
    public void userShouldOnlyListOwnedOrders() {
        OrderRepository repo = Mockito.mock(OrderRepository.class);

        User authenticatedUser = Mockito.mock(User.class);

        TacoOrder ownedOrder = new TacoOrder();

        ownedOrder.setId("owned-order");

        Mockito.when(authenticatedUser.getId()).thenReturn("user-a");

        Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(authenticatedUser).getAuthorities();

        Mockito.when(repo.findByUser_IdOrderByPlacedAtDesc("user-a")).thenReturn(Flux.just(ownedOrder));

        OrderService service = service(repo);

        StepVerifier.create(service.findVisibleOrders(authenticatedUser)).expectNext(ownedOrder).verifyComplete();

        Mockito.verify(repo).findByUser_IdOrderByPlacedAtDesc("user-a");

        Mockito.verify(repo, Mockito.never()).findAll();
    }

    @Test
    public void adminShouldListAllOrders() {
        OrderRepository repo = Mockito.mock(OrderRepository.class);

        User admin = Mockito.mock(User.class);

        TacoOrder firstOrder = new TacoOrder();

        TacoOrder secondOrder = new TacoOrder();

        Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(admin).getAuthorities();

        Mockito.when(repo.findAll()).thenReturn(Flux.just(firstOrder,secondOrder));

        OrderService service = service(repo);

        StepVerifier.create(service.findVisibleOrders(admin)).expectNext(firstOrder,secondOrder).verifyComplete();

        Mockito.verify(repo).findAll();

        Mockito.verify(repo,Mockito.never()).findByUser_IdOrderByPlacedAtDesc(Mockito.anyString());
    }

    private OrderService service(OrderRepository repo) {
        return new OrderService(repo, Mockito.mock(InventoryService.class));
    }
}
