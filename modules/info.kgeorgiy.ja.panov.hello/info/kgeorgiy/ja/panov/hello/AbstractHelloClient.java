package info.kgeorgiy.ja.panov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.util.Arrays;
import java.util.Objects;

public abstract class AbstractHelloClient implements HelloClient {
    public void clientMain(final String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Wrong arguments format=[host port prefix threads requests]");
            return;
        }

        try {
            final int port = Integer.parseInt(args[1]);
            final int thread = Integer.parseInt(args[3]);
            final int requests = Integer.parseInt(args[4]);

            run(args[0], port, args[1], thread, requests);
        } catch (final NumberFormatException e) {
            System.err.println("Wrong number format=[" + e.getMessage() + "]");
        }
    }
}
