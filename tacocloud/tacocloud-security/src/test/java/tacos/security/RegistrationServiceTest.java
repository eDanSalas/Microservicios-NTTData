package tacos.security;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ResponseStatus;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.User;
import tacos.data.UserRepository;

public class RegistrationServiceTest {

  private UserRepository userRepo;
  private PasswordEncoder passwordEncoder;
  private RegistrationService service;

  @BeforeEach
  public void setUp() {
    userRepo = Mockito.mock(UserRepository.class);

    passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    service = new RegistrationService(userRepo, passwordEncoder);
  }

  @Test
  public void shouldPersistEncodedPasswordReactively() {
    RegistrationForm form = validForm();

    AtomicReference<User> savedUser = new AtomicReference<>();

    when(userRepo.existsByUsername("daniel")).thenReturn(Mono.just(false));

    when(userRepo.existsByEmail("daniel@example.com")).thenReturn(Mono.just(false));

    when(userRepo.save(any(User.class)))
        .thenAnswer(invocation -> {
          User user =
              invocation.getArgument(
                  0,
                  User.class);

          savedUser.set(user);

          return Mono.just(user);
        });

    Mono<Void> registration = service.register(form);

    verifyNoInteractions(userRepo);

    StepVerifier.create(registration).verifyComplete();

    User persisted = savedUser.get();

    assertNotNull(persisted);

    assertNotEquals("safe-password", persisted.getPassword());

    assertTrue(persisted.getPassword().startsWith("{bcrypt}"));

    assertTrue(passwordEncoder.matches("safe-password", persisted.getPassword()));

    verify(userRepo).existsByUsername("daniel");

    verify(userRepo).existsByEmail("daniel@example.com");

    verify(userRepo).save(any(User.class));
  }

  @Test
  public void shouldRejectExistingUsername() {
    RegistrationForm form = validForm();

    when(userRepo.existsByUsername("daniel")).thenReturn(Mono.just(true));

    when(userRepo.existsByEmail("daniel@example.com")).thenReturn(Mono.just(false));

    StepVerifier.create(service.register(form)).expectErrorSatisfies(this::assertConflict).verify();

    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  public void shouldRejectExistingEmail() {
    RegistrationForm form = validForm();

    when(userRepo.existsByUsername("daniel")).thenReturn(Mono.just(false));

    when(userRepo.existsByEmail("daniel@example.com")).thenReturn(Mono.just(true));

    StepVerifier.create(service.register(form)).expectErrorSatisfies(this::assertConflict).verify();

    verify(userRepo, never()).save(any(User.class));
  }

  @Test
  public void shouldMapDuplicateKeyRaceToConflict() {
    RegistrationForm form = validForm();

    when(userRepo.existsByUsername("daniel")).thenReturn(Mono.just(false));

    when(userRepo.existsByEmail("daniel@example.com")).thenReturn(Mono.just(false));

    when(userRepo.save(any(User.class))).thenReturn(Mono.error(new DuplicateKeyException("E11000 duplicate key")));

    StepVerifier.create(
            service.register(form))
        .expectErrorSatisfies(
            this::assertConflict)
        .verify();

    verify(userRepo).save(any(User.class));
  }

  private void assertConflict(Throwable error) {

    assertTrue(error instanceof RegistrationConflictException);

    ResponseStatus annotation = error.getClass().getAnnotation(ResponseStatus.class);

    assertNotNull(annotation);

    assertTrue(annotation.value() == HttpStatus.CONFLICT);
  }

  private RegistrationForm validForm() {
    RegistrationForm form = new RegistrationForm();

    form.setUsername("daniel");
    form.setPassword("safe-password");
    form.setConfirm("safe-password");
    form.setFullname("Daniel Test");
    form.setStreet("Test Street 123");
    form.setCity("Guadalajara");
    form.setState("Jalisco");
    form.setZip("44100");
    form.setPhone("3312345678");
    form.setEmail("daniel@example.com");

    return form;
  }
}