package tacos.security;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.User;
import tacos.data.UserRepository;

@Service
public class RegistrationService {

  private final UserRepository userRepo;
  private final PasswordEncoder passwordEncoder;

  public RegistrationService(UserRepository userRepo, PasswordEncoder passwordEncoder) {

    this.userRepo = userRepo;
    this.passwordEncoder = passwordEncoder;
  }

  public Mono<Void> register(RegistrationForm form) {
    return Mono.defer(() ->
            Mono.zip(
                userRepo.existsByUsername(
                    form.getUsername().trim()),
                userRepo.existsByEmail(
                    form.getEmail()
                        .trim()
                        .toLowerCase())))
        .flatMap(exists -> {
          boolean usernameExists =
              exists.getT1();

          boolean emailExists =
              exists.getT2();

          if (usernameExists || emailExists) {
            return Mono.<User>error(
                new RegistrationConflictException());
          }

          User newUser =
              form.toUser(passwordEncoder);

          return userRepo.save(newUser);
        })
        .onErrorMap(
            DuplicateKeyException.class,
            exception ->
                new RegistrationConflictException())
        .then();
  }
}