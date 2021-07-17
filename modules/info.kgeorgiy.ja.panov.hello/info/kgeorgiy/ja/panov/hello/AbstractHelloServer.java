package info.kgeorgiy.ja.panov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class AbstractHelloServer implements HelloServer {
    protected static void abstractMain(final String[] args, final Supplier<HelloServer> serverSupplier) {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Wrong arguments format=[port threads]");
            return;
        }

        try {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);

            try (final HelloServer helloServer = serverSupplier.get()) {
                helloServer.start(port, threads);
            }
        } catch (final NumberFormatException e) {
            System.err.println("Wrong number format=[" + e.getMessage() + "]");
        }
    }
}
