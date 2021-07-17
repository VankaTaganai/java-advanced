package info.kgeorgiy.ja.panov.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;

public class HelloUDPNonblockingServer extends AbstractHelloServer {
    private static int QUEUE_LENGTH_SCALE = 300;

    private Selector selector;
    private ExecutorService service;
    private ExecutorService workers;
    private DatagramChannel datagramChannel;
    private Queue<ResponseInfo> responseQueue;

    @Override
    public void start(final int port, final int threads) {
        final InetSocketAddress address = new InetSocketAddress(port);
        try {
            selector = Selector.open();
            datagramChannel = Utils.createDatagramChannel(
                    selector,
                    SelectionKey.OP_READ,
                    null,
                    channel -> channel.bind(address)
            );
        } catch (IOException e) {
            try {
                selector.close();
            } catch (IOException ignore) {
                // ignore
            }
            System.err.println("I/O error occurs=[" + e.getMessage() + "]");
            return;
        }
        responseQueue = new ConcurrentLinkedDeque<>();
        service = Executors.newSingleThreadExecutor();
        workers = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(threads * QUEUE_LENGTH_SCALE), new ThreadPoolExecutor.DiscardPolicy());

        service.submit(() -> {
            while (!Thread.interrupted() && datagramChannel.isOpen()) {
                try {
                    selector.select();
                } catch (IOException e) {
                    System.err.println("Selector I/O error occurs=[" + e.getMessage() + "]");
                }

                Utils.iterateSelectedKeys(selector, key -> {
                    if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                });
            }
        });
    }

    private void read(final SelectionKey key) {
        try {
            final ByteBuffer byteBuffer = ByteBuffer.allocate(datagramChannel.socket().getReceiveBufferSize());
            final SocketAddress socketAddress = datagramChannel.receive(byteBuffer);
            workers.submit(() -> {
                final String receive = Utils.byteBufferToString(byteBuffer);
                final String responseString = Utils.RESPONSE_PREFIX + receive;
                responseQueue.add(new ResponseInfo(responseString, socketAddress));
                key.interestOps(SelectionKey.OP_WRITE);
                selector.wakeup();
            });
        } catch (IOException e) {
            System.err.println("I/O error occurs while reading=[" + e.getMessage() + "]");
        }
    }

    private void write(final SelectionKey key) {
        if (!responseQueue.isEmpty()) {
            final ResponseInfo responseInfo = responseQueue.remove();
            final ByteBuffer byteBuffer = ByteBuffer.wrap(responseInfo.getResponseString().getBytes(StandardCharsets.UTF_8));
            try {
                datagramChannel.send(byteBuffer, responseInfo.getAddress());
            } catch (IOException e) {
                System.err.println("I/O error occurs while sending=[" + e.getMessage() + "]");
            }
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } else {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    @Override
    public void close() {
        try {
            datagramChannel.close();
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.shutDownExecutorService(workers, Utils.AWAIT_TIME_MILLISECONDS);
        Utils.shutDownExecutorService(service, Utils.AWAIT_TIME_MILLISECONDS);
    }

    private static class ResponseInfo {
        private final String responseString;
        private final SocketAddress address;

        public ResponseInfo(final String responseString, final SocketAddress address) {
            this.responseString = responseString;
            this.address = address;
        }

        public String getResponseString() {
            return responseString;
        }

        public SocketAddress getAddress() {
            return address;
        }
    }

    public static void main(String[] args) {
        abstractMain(args, HelloUDPNonblockingServer::new);
    }
}
