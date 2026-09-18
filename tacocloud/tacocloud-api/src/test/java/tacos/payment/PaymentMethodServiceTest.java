package tacos.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.PaymentMethod;
import tacos.User;
import tacos.data.PaymentMethodRepository;
import tacos.web.api.dto.PaymentTokenizationRequest;

public class PaymentMethodServiceTest {
    private PaymentMethodRepository repository;
    private PaymentMethodService service;

    @BeforeEach
    public void setUp() {
        repository =
            Mockito.mock(
                PaymentMethodRepository.class);

        service =
            new PaymentMethodService(
                new FakePaymentGateway(),
                repository);
    }

    @Test
    public void tokenizesAndPersistsOnlySafeData() {
        when(repository.save(
                any(PaymentMethod.class)))
            .thenAnswer(invocation -> {
            PaymentMethod paymentMethod =
                invocation.getArgument(
                    0,
                    PaymentMethod.class);

            paymentMethod.setId("pm-1");

            return Mono.just(paymentMethod);
            });

        User authenticatedUser =
            Mockito.mock(User.class);

        when(authenticatedUser.getId())
            .thenReturn("user-1");

        PaymentTokenizationRequest request =
            validRequest();

        StepVerifier.create(
                service.tokenize(
                    request,
                    authenticatedUser))
            .assertNext(response -> {
            assertEquals(
                "pm-1",
                response.getPaymentMethodId());

            assertEquals(
                "VISA",
                response.getBrand());

            assertEquals(
                "1111",
                response.getLast4());
            })
            .verifyComplete();

        ArgumentCaptor<PaymentMethod> captor =
            ArgumentCaptor.forClass(
                PaymentMethod.class);

        verify(repository)
            .save(captor.capture());

        PaymentMethod persisted =
            captor.getValue();

        assertEquals(
            "user-1",
            persisted.getUserId());

        assertEquals(
            "VISA",
            persisted.getBrand());

        assertEquals(
            "1111",
            persisted.getLast4());

        assertNotNull(
            persisted.getPaymentToken());

        assertTrue(
            persisted.getPaymentToken()
                .startsWith("tok_fake_"));

        assertNotEquals(
            request.getPan(),
            persisted.getPaymentToken());

        assertNotEquals(
            request.getSecurityCode(),
            persisted.getPaymentToken());

        assertTrue(
            !persisted.toString()
                .contains(
                    persisted.getPaymentToken()));
    }

    @Test
    public void rejectsUnauthenticatedRequest() {
        StepVerifier.create(
                service.tokenize(
                    validRequest(),
                    null))
            .expectErrorMatches(error ->
                error instanceof
                    org.springframework.web.server
                        .ResponseStatusException
                && ((org.springframework.web.server
                        .ResponseStatusException) error)
                    .getStatus()
                    == org.springframework.http
                        .HttpStatus.UNAUTHORIZED)
            .verify();

        Mockito.verifyNoInteractions(repository);
    }

    private PaymentTokenizationRequest validRequest() {
        PaymentTokenizationRequest request =
            new PaymentTokenizationRequest();

        request.setPan(
            "4111111111111111");
        request.setExpirationMonth(12);
        request.setExpirationYear(2099);
        request.setSecurityCode("123");

        return request;
    }
}