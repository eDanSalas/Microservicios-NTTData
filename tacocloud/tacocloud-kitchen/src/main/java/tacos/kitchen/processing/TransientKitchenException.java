package tacos.kitchen.processing;

public class TransientKitchenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TransientKitchenException(String message) {
    super(message);
  }
}
