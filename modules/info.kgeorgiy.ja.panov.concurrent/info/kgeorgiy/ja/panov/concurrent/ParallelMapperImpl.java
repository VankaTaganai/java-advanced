package info.kgeorgiy.ja.panov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threads;
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final Map<Integer, ConcurrentResult<?>> results = new HashMap<>();
    private volatile boolean isClosed = false;
    private int resultId = 0;

    public ParallelMapperImpl(final int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads should be at least one");
        }

        final Runnable runnable = () -> {
            try {
                while (!Thread.interrupted()) {
                    pollTask().run();
                }
            } catch (final InterruptedException ignore) {
            }
        };

        this.threads = Stream.generate(() -> new Thread(runnable))
                .limit(threads)
                .peek(Thread::start)
                .collect(Collectors.toList());
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final int id;
        synchronized (this) {
            if (isClosed) {
                throw new IllegalStateException("ParallelMapper is closed");
            }
            id = resultId++;
        }

        final int argsSize = args.size();
        final ConcurrentResult<R> result = new ConcurrentResult<>(argsSize);
        synchronized (results) {
            results.put(id, result);
        }
        final RuntimeException exception = new RuntimeException("Execution was interrupted by an exception");
        IntStream.range(0, argsSize).forEach(ind ->
            addTask(() -> {
                try {
                    result.set(ind, f.apply(args.get(ind)));
                } catch (final RuntimeException e) {
                    exception.addSuppressed(e);
                } finally {
                    result.countDown();
                }
            })
        );
        if (exception.getSuppressed().length > 0) {
            throw exception;
        }
        List<R> mapResult = result.get();
        synchronized (results) {
            results.remove(id);
        }
        return mapResult;
    }

    @Override
    synchronized public void close() {
        if (isClosed) {
            return;
        }
        isClosed = true;
        threads.forEach(Thread::interrupt);
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
            } catch (final InterruptedException interruptedException) {
                i--;
            }
        }
        results.values().forEach(ConcurrentResult::invalidate);
    }

    private void addTask(final Runnable task) {
        synchronized (tasks) {
            tasks.add(task);
            tasks.notify();
        }
    }

    private Runnable pollTask() throws InterruptedException {
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            return tasks.poll();
        }
    }

    private static class ConcurrentResult<R> {
        private final List<R> result;
        private int latch;

        public ConcurrentResult(final int resultSize) {
            result = new ArrayList<>(Collections.nCopies(resultSize, null));
            latch = resultSize;
        }

        synchronized public void set(final int ind, final R value) {
            result.set(ind, value);
        }

        synchronized public List<R> get() throws InterruptedException {
            while (latch > 0) {
                wait();
            }
            return result;
        }

        synchronized public void countDown() {
            latch--;
            if (latch == 0) {
                notify();
            }
        }

        synchronized public void invalidate() {
            latch = 0;
            notify();
        }
    }
}
