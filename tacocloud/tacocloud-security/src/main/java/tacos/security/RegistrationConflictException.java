package tacos.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RegistrationConflictException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public RegistrationConflictException() {
    super("Username or email is already registered");
  }
}