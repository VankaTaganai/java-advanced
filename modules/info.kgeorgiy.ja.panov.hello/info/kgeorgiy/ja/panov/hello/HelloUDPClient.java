package info.kgeorgiy.ja.panov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.Externalizable;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class HelloUDPClient extends AbstractHelloClient {
    private static final int SO_TIMEOUT_MILLISECONDS = 300;
    private static final int AWAIT_TIME_SCALE_MILLISECONDS = 10_000;
    private static final Pattern VALIDATE_PATTERN = Pattern.compile("(\\D*)(\\d+)(\\D+)(\\d+)(\\D*)");


    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        final InetSocketAddress address = new InetSocketAddress(host, port);
        final ExecutorService executorsPool = Executors.newFixedThreadPool(threads);

        IntStream.range(0, threads)
                .forEach(threadNumber -> executorsPool.submit(() -> createTask(prefix, requests, address, threadNumber)));
        Utils.shutDownExecutorService(executorsPool, AWAIT_TIME_SCALE_MILLISECONDS * threads * requests);
    }

    private void createTask(final String prefix, final int requests, final InetSocketAddress address, final int threadNumber) {
        try (final DatagramSocket datagramSocket = new DatagramSocket()) {
            datagramSocket.setSoTimeout(SO_TIMEOUT_MILLISECONDS);

            final int receiveBufferSize = datagramSocket.getReceiveBufferSize();

            final DatagramPacket requestPacket = new DatagramPacket(new byte[0], 0, address);
            final DatagramPacket responsePacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);

            for (int requestNumber = 0; requestNumber < requests; requestNumber++) {
                final String requestString = prefix + threadNumber + "_" + requestNumber;
                System.out.println("Request=[" + requestString + "]");
                requestPacket.setData(requestString.getBytes());
                String responseString = "";
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        datagramSocket.send(requestPacket);
                        responseString = Utils.receive(datagramSocket, responsePacket, "");
                        Matcher matcher = VALIDATE_PATTERN.matcher(responseString);
                        if (matcher.matches() &&
                                matcher.group(2).equals(Integer.toString(threadNumber)) &&
                                matcher.group(4).equals(Integer.toString(requestNumber))) {
                            break;
                        }
                    } catch (IOException ignore) {
                        // ignore
                    }
                }
                System.out.println("Response=[" + responseString + "]");
            }
        } catch (final SocketException e) {
            System.err.println("Error occurred while using datagram socket=[" + e.getMessage() + "]");
        }
    }

    public static void main(final String[] args) {
        (new HelloUDPClient()).clientMain(args);
    }
}
