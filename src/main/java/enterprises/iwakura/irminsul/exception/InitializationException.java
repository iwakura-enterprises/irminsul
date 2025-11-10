package enterprises.iwakura.irminsul.exception;

public class InitializationException extends RuntimeException {

    public InitializationException(String message, Exception cause) {
        super(message, cause);
    }
}
