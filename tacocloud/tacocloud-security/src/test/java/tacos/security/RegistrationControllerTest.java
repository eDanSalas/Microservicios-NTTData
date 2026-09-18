package tacos.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class RegistrationControllerTest {

  @Test
  public void shouldRegisterJsonRequest() {
    RegistrationService service = Mockito.mock(RegistrationService.class);
    RegistrationForm form = new RegistrationForm();
    when(service.register(form)).thenReturn(Mono.empty());

    StepVerifier.create(new RegistrationController(service).processJsonRegistration(form))
        .assertNext(response -> assertEquals(HttpStatus.CREATED, response.getStatusCode()))
        .verifyComplete();
    verify(service).register(form);
  }

  @Test
  public void shouldReturnConflictForDuplicateRegistration() {
    RegistrationService service = Mockito.mock(RegistrationService.class);
    RegistrationForm form = new RegistrationForm();
    when(service.register(form)).thenReturn(Mono.error(new RegistrationConflictException()));

    StepVerifier.create(new RegistrationController(service).processJsonRegistration(form))
        .assertNext(response -> assertEquals(HttpStatus.CONFLICT, response.getStatusCode()))
        .verifyComplete();
  }
}
