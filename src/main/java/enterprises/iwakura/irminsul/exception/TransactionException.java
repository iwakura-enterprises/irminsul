package enterprises.iwakura.irminsul.exception;

import lombok.Getter;

/**
 * Exception thrown when a transaction fails.
 * This exception is a runtime exception and is used to indicate that a transaction with a specific ID has failed.
 */
@Getter
public class TransactionException extends RuntimeException {

    private final long id;

    public TransactionException(long id, Throwable cause) {
        super("Transaction with ID " + id + " failed in exception", cause);
        this.id = id;
    }
}
