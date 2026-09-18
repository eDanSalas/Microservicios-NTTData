package tacos.security;

import java.util.Objects;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.Getter;
import lombok.Setter;
import tacos.User;

@Getter
@Setter
public class RegistrationForm {
  @NotBlank
  @Size(min = 3, max = 50)
  private String username;

  @NotBlank
  @Size(
      min = 8,
      max = 72,
      message =
          "must contain between 8 and 72 characters")
  private String password;

  @NotBlank
  private String confirm;

  @NotBlank
  @Size(max = 100)
  private String fullname;

  @NotBlank
  @Size(max = 120)
  private String street;

  @NotBlank
  @Size(max = 80)
  private String city;

  @NotBlank
  @Size(max = 80)
  private String state;

  @NotBlank
  @Pattern(
      regexp = "\\d{5}",
      message = "must contain exactly 5 digits")
  private String zip;

  @NotBlank
  @Size(max = 30)
  private String phone;

  @NotBlank
  @Email
  @Size(max = 254)
  private String email;

  @AssertTrue(
      message = "password confirmation does not match")
  public boolean isPasswordConfirmed() {
    return Objects.equals(
        password,
        confirm);
  }

  public User toUser(
      PasswordEncoder passwordEncoder) {

    return new User(
        username.trim(),
        passwordEncoder.encode(password),
        fullname.trim(),
        street.trim(),
        city.trim(),
        state.trim(),
        zip.trim(),
        phone.trim(),
        email.trim().toLowerCase());
  }
}