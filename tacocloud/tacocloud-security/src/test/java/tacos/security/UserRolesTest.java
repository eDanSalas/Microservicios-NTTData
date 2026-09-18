package tacos.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import tacos.User;
import tacos.UserRole;

public class UserRolesTest {

  @Test
  public void newUserShouldHaveUserRole() {
    User user = testUser();

    assertTrue(
        user.getAuthorities()
            .stream()
            .anyMatch(authority ->
                "ROLE_USER".equals(
                    authority.getAuthority())));
  }

  @Test
  public void userCanHaveAdminRole() {
    User user = testUser();

    user.setRoles(
        EnumSet.of(
            UserRole.USER,
            UserRole.ADMIN));

    assertTrue(
        user.getAuthorities()
            .stream()
            .anyMatch(authority ->
                "ROLE_ADMIN".equals(
                    authority.getAuthority())));
  }

  @Test
  public void userCanHaveKitchenRole() {
    User user = testUser();

    user.setRoles(
        EnumSet.of(
            UserRole.KITCHEN));

    assertTrue(
        user.getAuthorities()
            .stream()
            .anyMatch(authority ->
                "ROLE_KITCHEN".equals(
                    authority.getAuthority())));
  }

  private User testUser() {
    return new User(
        "test-user",
        "{noop}password",
        "Test User",
        "Test Street",
        "Guadalajara",
        "Jalisco",
        "44100",
        "3312345678",
        "test@example.com");
  }
}