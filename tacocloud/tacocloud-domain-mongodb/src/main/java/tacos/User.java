package tacos;
import java.util.Collection;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.
                                          SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.ToString;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Document
public class User implements UserDetails {

  private static final long serialVersionUID = 1L;

  @Id
  private String id;
  
  @Indexed(
    unique = true,
    name = "uk_user_username")
  private final String username;
  
  @JsonIgnore
  @ToString.Exclude
  private final String password;
  private final String fullname;
  private final String street;
  private final String city;
  private final String state;
  private final String zip;
  private final String phoneNumber;
  @Indexed(
    unique = true,
    name = "uk_user_email")
  private final String email;
  
  private Set<UserRole> roles = EnumSet.of(UserRole.USER);

  @PersistenceCreator
  public User(String username, String password, String fullname,
      String street, String city, String state, String zip,
      String phoneNumber, String email) {
    this.username = username;
    this.password = password;
    this.fullname = fullname;
    this.street = street;
    this.city = city;
    this.state = state;
    this.zip = zip;
    this.phoneNumber = phoneNumber;
    this.email = email;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Set<UserRole> effectiveRoles =
        roles == null || roles.isEmpty()
            ? EnumSet.of(UserRole.USER)
            : roles;

    return effectiveRoles.stream()
        .map(role ->
            new SimpleGrantedAuthority(
                role.authority()))
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

}
