package info.kgeorgiy.java.advanced.concurrent;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
public interface OldAdvancedIP extends ListIP {
    /**
     * Reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values values to reduce.
     * @param monoid monoid to use.
     *
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    <T> T reduce(final int threads, List<T> values, final Monoid<T> monoid) throws InterruptedException;

    /**
     * Maps and reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values values to reduce.
     * @param lift mapping function.
     * @param monoid monoid to use.
     *
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     *
     * @throws InterruptedException if executing thread was interrupted.
     */
    <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid) throws InterruptedException;

    class Monoid<T> {
        private final T identity;
        private final BinaryOperator<T> operator;

        public Monoid(final T identity, final BinaryOperator<T> operator) {
            this.identity = identity;
            this.operator = operator;
        }

        public T getIdentity() {
            return identity;
        }

        public BinaryOperator<T> getOperator() {
            return operator;
        }
    }
}