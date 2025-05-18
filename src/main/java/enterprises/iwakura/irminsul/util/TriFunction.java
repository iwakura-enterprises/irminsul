package enterprises.iwakura.irminsul.util;

/**
 * A functional interface that takes three arguments and produces a result.
 *
 * @param <T> Argument type 1
 * @param <U> Argument type 2
 * @param <V> Argument type 3
 * @param <R> Result type
 */
public interface TriFunction<T, U, V, R> {

    /**
     * Applies the function to the given arguments.
     *
     * @param t Argument 1
     * @param u Argument 2
     * @param v Argument 3
     *
     * @return The result of the function
     */
    R apply(T t, U u, V v);
}
