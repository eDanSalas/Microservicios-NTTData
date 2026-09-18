package tacos.security;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderTest {

  @Test
  public void shouldEncodePasswordUsingDelegatingEncoder() {
    PasswordEncoder encoder = new SecurityConfig().encoder();

    String rawPassword = "safe-password";

    String encodedPassword = encoder.encode(rawPassword);

    assertNotEquals(rawPassword,encodedPassword);
    assertTrue(encodedPassword.startsWith("{bcrypt}"));
    assertTrue(encoder.matches(rawPassword,encodedPassword));
  }
}