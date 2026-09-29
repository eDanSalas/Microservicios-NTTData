package tacos.kitchen.processing;

public class PermanentEventException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PermanentEventException(String message) {
    super(message);
  }
}
