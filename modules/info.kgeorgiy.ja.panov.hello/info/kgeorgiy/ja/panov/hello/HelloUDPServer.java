package info.kgeorgiy.ja.panov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPServer extends AbstractHelloServer {
    private DatagramSocket datagramSocket;
    private ExecutorService executorsPool;

    @Override
    public void start(int port, int threads) {
        final int receiveBufferSize;
        try {
            datagramSocket = new DatagramSocket(port);
            receiveBufferSize = datagramSocket.getReceiveBufferSize();
        } catch (SocketException e) {
            System.err.println("Error occurred while using datagram socket=[" + e.getMessage() + "]");
            return;
        }

        executorsPool = Executors.newFixedThreadPool(threads);
        IntStream.range(0, threads).forEach(threadNum -> executorsPool.submit(() -> createTask(receiveBufferSize)));
    }

    private void createTask(final int receiveBufferSize) {
        final DatagramPacket responsePacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
        final DatagramPacket requestPacket = new DatagramPacket(new byte[0], 0);
        while (!Thread.interrupted() && !datagramSocket.isClosed()) {
            try {
                final String receiveString = Utils.receive(datagramSocket, responsePacket, Utils.RESPONSE_PREFIX);
                requestPacket.setSocketAddress(responsePacket.getSocketAddress());
                responsePacket.setData(receiveString.getBytes(Utils.CHARSET));
                datagramSocket.send(responsePacket);
            } catch (IOException ignore) {
                // ignore
            }
        }
    }

    @Override
    public void close() {
        datagramSocket.close();
        Utils.shutDownExecutorService(executorsPool, Utils.AWAIT_TIME_MILLISECONDS);
    }

    public static void main(final String[] args) {
        abstractMain(args, HelloUDPServer::new);
    }
}
