package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority
    .SimpleGrantedAuthority;
import org.springframework.web.server
    .ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.OrderStatus;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;
import tacos.inventory.InventoryService;
import tacos.web.api.OrderReplaceRequest;

public class OrderServiceTest {

    private OrderRepository repo;
    private OrderService service;
    private InventoryService inventoryService;

    @BeforeEach
    public void setUp() {
        repo = Mockito.mock(OrderRepository.class);
        inventoryService = Mockito.mock(InventoryService.class);
        when(inventoryService.release(Mockito.anyString())).thenReturn(Mono.empty());
        service = new OrderService(repo, inventoryService);
    }

    @Test
    public void ownerCanReplaceOrderWithoutChangingIdentity() {
        User owner = user("owner-id", "ROLE_USER");
        TacoOrder stored = order(owner, OrderStatus.PLACED);

        Date originalDate = stored.getPlacedAt();
        stored.setPaymentMethodId("pm-1");
        stored.setPaymentBrand("VISA");
        stored.setPaymentLast4("1111");

        OrderReplaceRequest request =
            replacementRequest();

        when(repo.findById("1L"))
            .thenReturn(Mono.just(stored));

        when(repo.save(any(TacoOrder.class)))
            .thenAnswer(invocation ->
                Mono.just(invocation.getArgument(
                    0,
                    TacoOrder.class)));

        StepVerifier.create(
                service.replaceOrder(
                    "1L",
                    request,
                    owner))
            .assertNext(updated -> {
                assertEquals("1L", updated.getId());
                assertSame(owner, updated.getUser());
                assertSame(
                    originalDate,
                    updated.getPlacedAt());

               assertEquals(
                    "pm-1",
                    updated.getPaymentMethodId());
                assertEquals(
                    "VISA",
                    updated.getPaymentBrand());
                assertEquals(
                    "1111",
                    updated.getPaymentLast4());

                assertEquals(
                    "Nueva ciudad",
                    updated.getDeliveryCity());
                assertEquals(
                    "99999",
                    updated.getDeliveryZip());
                assertEquals(
                    1,
                    updated.getTacos().size());
            })
            .verifyComplete();

        verify(repo).findById("1L");
        verify(repo, times(1))
            .save(any(TacoOrder.class));
    }

    @Test
    public void foreignUserCannotReplaceOrder() {
        User owner = user("owner-id", "ROLE_USER");
        User foreign = user("other-id", "ROLE_USER");

        TacoOrder stored =
            order(owner, OrderStatus.PLACED);

        when(repo.findById("1L"))
            .thenReturn(Mono.just(stored));

        StepVerifier.create(
                service.replaceOrder(
                    "1L",
                    replacementRequest(),
                    foreign))
            .expectErrorSatisfies(error ->
                assertStatus(
                    error,
                    HttpStatus.FORBIDDEN))
            .verify();

        verify(repo, never())
            .save(any(TacoOrder.class));
    }

    @Test
    public void missingOrderCannotBeReplaced() {
        User owner = user("owner-id", "ROLE_USER");

        when(repo.findById("missing"))
            .thenReturn(Mono.empty());

        StepVerifier.create(
                service.replaceOrder(
                    "missing",
                    replacementRequest(),
                    owner))
            .expectErrorSatisfies(error ->
                assertStatus(
                    error,
                    HttpStatus.NOT_FOUND))
            .verify();

        verify(repo, never())
            .save(any(TacoOrder.class));
    }

    @Test
    public void ownerCanDeletePlacedOrder() {
        User owner = user("owner-id", "ROLE_USER");
        TacoOrder stored =
            order(owner, OrderStatus.PLACED);

        when(repo.findById("1L"))
            .thenReturn(Mono.just(stored));

        when(repo.delete(stored))
            .thenReturn(Mono.empty());

        StepVerifier.create(
                service.deleteOrder("1L", owner))
            .verifyComplete();

        verify(repo).findById("1L");
        verify(repo, times(1)).delete(stored);
        verify(inventoryService, times(1)).release("1L");
    }

    @Test
    public void adminCanDeleteForeignOrder() {
        User owner = user("owner-id", "ROLE_USER");
        User admin = user("admin-id", "ROLE_ADMIN");

        TacoOrder stored =
            order(owner, OrderStatus.PLACED);

        when(repo.findById("1L"))
            .thenReturn(Mono.just(stored));

        when(repo.delete(stored))
            .thenReturn(Mono.empty());

        StepVerifier.create(
                service.deleteOrder("1L", admin))
            .verifyComplete();

        verify(repo, times(1)).delete(stored);
    }

    @Test
    public void missingOrderCannotBeDeleted() {
        User owner = user("owner-id", "ROLE_USER");

        when(repo.findById("missing"))
            .thenReturn(Mono.empty());

        StepVerifier.create(
                service.deleteOrder(
                    "missing",
                    owner))
            .expectErrorSatisfies(error ->
                assertStatus(
                    error,
                    HttpStatus.NOT_FOUND))
            .verify();

        verify(repo, never())
            .delete(any(TacoOrder.class));
    }

    @Test
    public void foreignUserCannotDeleteOrder() {
        User owner = user("owner-id", "ROLE_USER");
        User foreign = user("other-id", "ROLE_USER");

        TacoOrder stored =
            order(owner, OrderStatus.PLACED);

        when(repo.findById("1L"))
            .thenReturn(Mono.just(stored));

        StepVerifier.create(
                service.deleteOrder(
                    "1L",
                    foreign))
            .expectErrorSatisfies(error ->
                assertStatus(
                    error,
                    HttpStatus.FORBIDDEN))
            .verify();

        verify(repo, never())
            .delete(any(TacoOrder.class));
    }

    @Test
    public void preparingOrderCannotBeDeleted() {
        User owner = user("owner-id", "ROLE_USER");

        TacoOrder stored =
            order(owner, OrderStatus.PREPARING);

        when(repo.findById("1L"))
            .thenReturn(Mono.just(stored));

        StepVerifier.create(
                service.deleteOrder("1L", owner))
            .expectErrorSatisfies(error ->
                assertStatus(
                    error,
                    HttpStatus.CONFLICT))
            .verify();

        verify(repo, never())
            .delete(any(TacoOrder.class));
    }

    private User user(String id, String role) {
        User user = Mockito.mock(User.class);

        when(user.getId()).thenReturn(id);

        Mockito.doReturn(
                List.of(
                    new SimpleGrantedAuthority(role)))
            .when(user)
            .getAuthorities();

        return user;
    }

    private TacoOrder order(
            User owner,
            OrderStatus status) {

        TacoOrder order = new TacoOrder();
        order.setId("1L");
        order.setUser(owner);
        order.setStatus(status);
        order.setPlacedAt(new Date(1000L));
        order.setDeliveryName("Nombre original");
        order.setDeliveryStreet("Calle original");
        order.setDeliveryCity("Ciudad original");
        order.setDeliveryState("Jalisco");
        order.setDeliveryZip("12345");

        return order;
    }

    private OrderReplaceRequest replacementRequest() {
        Taco taco = new Taco();
        taco.setName("Test Taco");

        OrderReplaceRequest request =
            new OrderReplaceRequest();

        request.setDeliveryName("Nuevo nombre");
        request.setDeliveryStreet("Nueva calle");
        request.setDeliveryCity("Nueva ciudad");
        request.setDeliveryState("Nayarit");
        request.setDeliveryZip("99999");
        request.setTacos(List.of(taco));

        return request;
    }

    private void assertStatus(
            Throwable error,
            HttpStatus expectedStatus) {

        ResponseStatusException exception =
            assertInstanceOf(
                ResponseStatusException.class,
                error);

        assertEquals(
            expectedStatus,
            exception.getStatus());
    }
}
