package info.kgeorgiy.ja.panov.concurrent;


import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;

    public IterativeParallelism() {
        this(null);
    }

    public IterativeParallelism(final ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), monoid);
    }

    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid) throws InterruptedException {
        final Function<? super Stream<R>, R> reducer = stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator(), monoid.getOperator());
        return parallelOperation(
                threads,
                values,
                stream -> reducer.apply(stream.map(lift)),
                reducer
        );
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return parallelOperation(
                threads,
                values,
                stream -> stream.map(Objects::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining())
        );
    }

    private <T, U> List<U> applyWithFlat(
            final int threads,
            final List<? extends T> values,
            final Function<? super Stream<? extends T>, Stream<? extends U>> f
    ) throws InterruptedException {
        return parallelOperation(
                threads,
                values,
                stream -> f.apply(stream).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList())
        );
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return applyWithFlat(threads, values, stream -> stream.filter(predicate));
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return applyWithFlat(threads, values, stream -> stream.map(f));
    }

    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        final Function<? super Stream<? extends T>, T> threadFunc = stream -> stream.max(comparator).orElseThrow();
        return parallelOperation(threads, values, threadFunc, threadFunc);
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return parallelOperation(
                threads,
                values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue)
        );
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    private static <T, U> List<U> distributeThreads(
            final List<Stream<? extends T>> values,
            final Function<? super Stream<? extends T>, ? extends U> threadFunc
    ) throws InterruptedException {
        final int valuesSize = values.size();

        final List<U> result = new ArrayList<>(Collections.nCopies(valuesSize, null));
        final List<Thread> threadsList = IntStream.range(0, valuesSize)
                .mapToObj(i -> new Thread(() -> result.set(i, threadFunc.apply(values.get(i)))))
                .peek(Thread::start)
                .collect(Collectors.toList());

        for (int i = 0; i < valuesSize; i++) {
            final Thread thread = threadsList.get(i);
            try {
                thread.join();
            } catch (final InterruptedException e) {
                for (int j = i; j < threadsList.size(); j++) {
                    threadsList.get(j).interrupt();
                }
                for (int j = i; j < threadsList.size(); j++) {
                    try {
                        threadsList.get(j).join();
                    } catch (final InterruptedException interruptedException) {
                        e.addSuppressed(interruptedException);
                        j--;
                    }

                }
                throw e;
            }
        }

        return result;
    }

    private <T, U> U parallelOperation(
            final int threads,
            final List<? extends T> values,
            final Function<? super Stream<? extends T>, ? extends U> threadFunc,
            final Function<? super Stream<U>, U> joiner
    ) throws InterruptedException {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads should be at least one");
        }

        final List<Stream<? extends T>> splitValues = split(values, threads);

        final List<U> threadsProduct = parallelMapper == null
                ? distributeThreads(splitValues, threadFunc)
                : parallelMapper.map(threadFunc, splitValues);

        return joiner.apply(threadsProduct.stream());
    }

    private static <T> List<Stream<? extends T>> split(final List<? extends T> values, final int threadsInUse) {
        final int itemsNumber = values.size() / threadsInUse;
        int itemsLeft = values.size() % threadsInUse;

        final List<Stream<? extends T>> splitValues = new ArrayList<>();

        for (int i = 0, j = 0; i < threadsInUse && j < values.size(); i++) {
            final int from = j;
            final int to = from + itemsNumber + (itemsLeft-- > 0 ? 1 : 0);
            splitValues.add(values.subList(from, to).stream());
            j = to;
        }
        return splitValues;
    }
}