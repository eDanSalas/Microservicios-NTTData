package tacos.web.api.error;

public enum ApiErrorCode {

  VALIDATION_ERROR("Request validation failed"),

  MALFORMED_REQUEST("Malformed request"),

  AUTHENTICATION_REQUIRED("Authentication required"),

  ACCESS_DENIED("Access denied"),

  RESOURCE_NOT_FOUND("Resource not found"),

  RESOURCE_CONFLICT("Resource conflict"),

  INSUFFICIENT_STOCK("Insufficient stock"),

  BUSINESS_RULE_VIOLATION("Business rule violation"),

  REQUEST_REJECTED("Request rejected"),

  INTERNAL_ERROR("Internal server error");

  private final String title;

  ApiErrorCode(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }
}
