package tacos;

public enum UserRole {
  USER,
  ADMIN,
  KITCHEN;

  public String authority() {
    return "ROLE_" + name();
  }
}