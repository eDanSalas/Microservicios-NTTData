package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.reactive.result.method.annotation
    .AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;
import tacos.messaging.OrderMessagingService;
import tacos.service.OrderService;
import org.springframework.core.ReactiveAdapterRegistry;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

import org.springframework.security.core.Authentication;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import tacos.service.EmailOrderSubmissionService;

import tacos.service.OrderCreationService;
import tacos.service.TacoClassificationService;
import tacos.web.api.dto.OrderResponse;
import tacos.web.api.error.GlobalApiExceptionHandler;
import tacos.web.api.mapper.IngredientMapper;
import tacos.web.api.mapper.OrderMapper;

public class OrderApiControllerTest {

    private OrderRepository orderRepo;
    private OrderMessagingService orderMessages;
    private EmailOrderSubmissionService emailOrderSubmissionService;
    private OrderService orderService;
    private User authenticatedUser;
    private WebTestClient testClient;
    private OrderCreationService orderCreationService;

    @BeforeEach
    public void setUp() {
        orderRepo = Mockito.mock(OrderRepository.class);
        orderMessages = Mockito.mock(OrderMessagingService.class);
        emailOrderSubmissionService = Mockito.mock(EmailOrderSubmissionService.class);
        orderService = Mockito.mock(OrderService.class);

        authenticatedUser = Mockito.mock(User.class);

        Mockito.when(authenticatedUser.getId())
            .thenReturn("owner-id");

        Mockito.doReturn(
                List.of(
                    new SimpleGrantedAuthority("ROLE_USER")))
            .when(authenticatedUser)
            .getAuthorities();

        Authentication authentication =
            new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                authenticatedUser.getAuthorities());

        orderCreationService = Mockito.mock(OrderCreationService.class);

        OrderMapper orderMapper =
            new OrderMapper(
                new IngredientMapper(),
                new TacoClassificationService());

        OrderApiController controller =
            new OrderApiController(
                orderRepo,
                emailOrderSubmissionService,
                orderService,
                orderCreationService,
                orderMapper);

        testClient = WebTestClient
                    .bindToController(controller)
                    .controllerAdvice(new GlobalApiExceptionHandler())
                    .apply(springSecurity())
                    .argumentResolvers(configurer ->
                        configurer.addCustomResolver(
                            new AuthenticationPrincipalArgumentResolver(
                                ReactiveAdapterRegistry
                                    .getSharedInstance())))
                    .build()
                    .mutateWith(
                        mockAuthentication(authentication));
    }

    @Test
    public void shouldPatchZip() {
        TacoOrder updatedOrder = testOrder();
        updatedOrder.setDeliveryZip("54321");

        Mockito.when(orderService.patchOrder(
                Mockito.eq("1L"),
                Mockito.any(OrderPatchRequest.class),
                Mockito.same(authenticatedUser)))
            .thenReturn(Mono.just(updatedOrder));

        testClient.patch()
            .uri("/api/orders/1L")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                "{\"deliveryZip\":\"54321\"}")
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderResponse.class)
            .value(order -> {
                assertEquals(
                    "54321",
                    order.getDeliveryZip());

                assertEquals(
                    "Jalisco",
                    order.getDeliveryState());
            });

        Mockito.verify(orderService)
            .patchOrder(
                Mockito.eq("1L"),
                Mockito.any(OrderPatchRequest.class),
                Mockito.same(authenticatedUser));

        Mockito.verifyNoInteractions(orderRepo);
    }

    @Test
    public void shouldRejectEmptyPatch() {
        testClient.patch()
            .uri("/api/orders/1L")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isBadRequest();

        Mockito.verifyNoInteractions(
            orderService,
            orderRepo);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   "
    })
    public void shouldRejectBlankPatchValue(
            String invalidZip) {

        String body =
            "{\"deliveryZip\":\""
                + invalidZip
                + "\"}";

        testClient.patch()
            .uri("/api/orders/1L")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isBadRequest();

        Mockito.verifyNoInteractions(
            orderService,
            orderRepo);
    }

    @Test
    public void shouldRejectExplicitNull() {
        testClient.patch()
            .uri("/api/orders/1L")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                "{\"deliveryZip\":null}")
            .exchange()
            .expectStatus().isBadRequest();

        Mockito.verifyNoInteractions(
            orderService,
            orderRepo);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"deliveryZip\":\"54321\","
            + "\"ccNumber\":\"NO-ACEPTAR\"}",

        "{\"deliveryZip\":\"54321\","
            + "\"id\":\"other\"}"
    })
    public void shouldRejectProhibitedPatchFields(
            String body) {

        testClient.patch()
            .uri("/api/orders/1L")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()
            .expectStatus().isBadRequest();

        Mockito.verifyNoInteractions(
            orderService,
            orderRepo);
    }

    @Test
    public void shouldReturnNotFoundForMissingOrder() {
        Mockito.when(orderService.patchOrder(
                Mockito.eq("1L"),
                Mockito.any(OrderPatchRequest.class),
                Mockito.same(authenticatedUser)))
            .thenReturn(Mono.error(
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Order not found")));

        testClient.patch()
            .uri("/api/orders/1L")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                "{\"deliveryZip\":\"54321\"}")
            .exchange()
            .expectStatus().isNotFound();

        Mockito.verify(orderService)
            .patchOrder(
                Mockito.eq("1L"),
                Mockito.any(OrderPatchRequest.class),
                Mockito.same(authenticatedUser));

        Mockito.verifyNoInteractions(orderRepo);
    }

    @Test
    public void shouldReplaceOrderUsingPathId() {
        OrderReplaceRequest request =
            validReplacementRequest();

        TacoOrder updated = testOrder();
        updated.setDeliveryName("Nuevo nombre");
        updated.setDeliveryZip("99999");

        Mockito.when(orderService.replaceOrder(
                Mockito.eq("1L"),
                Mockito.any(OrderReplaceRequest.class),
                Mockito.same(authenticatedUser)))
            .thenReturn(Mono.just(updated));

        testClient.put()
            .uri("/api/orders/1L")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody(OrderResponse.class)
            .value(order -> {
                assertEquals(
                    "1L",
                    order.getId());
                assertEquals(
                    "99999",
                    order.getDeliveryZip());
            });

        Mockito.verify(orderService)
            .replaceOrder(
                Mockito.eq("1L"),
                Mockito.any(OrderReplaceRequest.class),
                Mockito.same(authenticatedUser));

        Mockito.verifyNoInteractions(orderRepo);
    }

    @Test
    public void shouldRejectPutThatContainsBodyId() {
        testClient.put()
            .uri("/api/orders/1L")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                "{\"id\":\"different-id\","
                    + "\"deliveryName\":\"Nombre\","
                    + "\"deliveryStreet\":\"Calle\","
                    + "\"deliveryCity\":\"Ciudad\","
                    + "\"deliveryState\":\"Jalisco\","
                    + "\"deliveryZip\":\"12345\","
                    + "\"tacos\":[]}")
            .exchange()
            .expectStatus().isBadRequest();

        Mockito.verifyNoInteractions(
            orderService,
            orderRepo);
    }

    @Test
    public void shouldDeleteExistingOrder() {
        Mockito.when(orderService.deleteOrder(
                Mockito.eq("1L"),
                Mockito.same(authenticatedUser)))
            .thenReturn(Mono.empty());

        testClient.delete()
            .uri("/api/orders/1L")
            .exchange()
            .expectStatus().isNoContent()
            .expectBody()
            .isEmpty();

        Mockito.verify(orderService)
            .deleteOrder(
                Mockito.eq("1L"),
                Mockito.same(authenticatedUser));

        Mockito.verifyNoInteractions(orderRepo);
    }

    private OrderReplaceRequest validReplacementRequest() {
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

    @Test
    public void shouldCreateOrderFromEmail() {
        TacoOrder savedOrder = testOrder();
        savedOrder.setId("email-order-1");
        savedOrder.setDeliveryName("Daniel");

        Mockito.when(
                emailOrderSubmissionService.submit(
                    Mockito.<Mono<EmailOrder>>any()))
            .thenReturn(Mono.just(savedOrder));

        testClient.post()
            .uri("/api/orders/fromEmail")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                "{"
                    + "\"email\":\"daniel@example.com\","
                    + "\"tacos\":["
                    + "{"
                    + "\"name\":\"Beef taco\","
                    + "\"ingredients\":[\"FLTO\",\"GRBF\"]"
                    + "}"
                    + "]"
                    + "}")
            .exchange()
            .expectStatus().isCreated()
            .expectBody(OrderResponse.class)
            .value(order -> {
                assertEquals(
                    "email-order-1",
                    order.getId());

                assertEquals(
                    "Daniel",
                    order.getDeliveryName());
            });

        Mockito.verify(
                emailOrderSubmissionService,
                Mockito.times(1))
            .submit(
                Mockito.<Mono<EmailOrder>>any());

        Mockito.verifyNoInteractions(
            orderRepo,
            orderMessages,
            orderService);
    }

    private TacoOrder testOrder() {
        TacoOrder order = new TacoOrder();
        order.setId("1L");
        order.setDeliveryZip("12345");
        order.setDeliveryState("Jalisco");
        order.setDeliveryCity("Guadalajara");
        return order;
    }
}
