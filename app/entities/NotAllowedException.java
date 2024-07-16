package entities;

public class NotAllowedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String message;

    public NotAllowedException() {
        this.message = null;
    }

    public NotAllowedException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
