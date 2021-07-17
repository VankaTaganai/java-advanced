package info.kgeorgiy.ja.panov.hello;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class HelloUDPNonblockingClient extends AbstractHelloClient {
    private int threadsNeed;

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final InetSocketAddress address = new InetSocketAddress(host, port);
        Selector selector = null;
        try {
            selector = Selector.open();
            for (int threadNumber = 0; threadNumber < threads; threadNumber++) {
                try {
                    Utils.createDatagramChannel(
                            selector,
                            SelectionKey.OP_WRITE,
                            new HelloAttr(threadNumber, requests),
                            channel -> channel.connect(address)
                    );
                } catch (IOException e) {
                    System.err.println("I/O error occurs while creating datagram channel=[" + e.getMessage() + "]");
                    Utils.closeIterableWithIgnore(channelsFromSelector(selector));
                    return;
                }
            }

            threadsNeed = threads;
            while (threadsNeed > 0) {
                selector.select(Utils.TIMEOUT_MILLISECONDS);
                if (selector.selectedKeys().isEmpty()) {
                    selector.keys().forEach(i -> i.interestOps(SelectionKey.OP_WRITE));
                }
                Utils.iterateSelectedKeys(selector, key -> {
                    final HelloAttr attr = (HelloAttr) key.attachment();

                    final DatagramChannel datagramChannel = (DatagramChannel) key.channel();
                    if (key.isReadable()) {
                        read(datagramChannel, attr, key);
                    } else if (key.isWritable()) {
                        write(prefix, datagramChannel, attr, key);
                    }
                });
            }
        } catch (IOException | UncheckedIOException e) {
            System.err.println("I/O error occurs=[" + e.getMessage() + "]");
        } finally {
            if (!Objects.isNull(selector)) {
                Utils.closeIterableWithIgnore(channelsFromSelector(selector));
                Utils.closeWithIgnore(selector);
            }
        }
    }

    private void read(final DatagramChannel datagramChannel, final HelloAttr attr, final SelectionKey key) throws IOException {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(datagramChannel.socket().getReceiveBufferSize());
        datagramChannel.read(byteBuffer);
        final String receive = Utils.byteBufferToString(byteBuffer);
        if (Utils.validateReceive(receive, attr.getThreadNumber(), attr.getRequestNumber())) {
            attr.nextRequest();
            if (attr.isCompleted()) {
                threadsNeed--;
                datagramChannel.close();
            } else {
                key.interestOps(SelectionKey.OP_WRITE);
            }
        }
    }

    private void write(
            final String prefix,
            final DatagramChannel datagramChannel,
            final HelloAttr attr,
            final SelectionKey key
    ) throws IOException {
        final String request = Utils.requestString(prefix, attr.getThreadNumber(), attr.getRequestNumber());
        final ByteBuffer byteBuffer = ByteBuffer.wrap(request.getBytes(StandardCharsets.UTF_8));
        datagramChannel.write(byteBuffer);
        System.out.println("Request=[" + request + "]");
        key.interestOps(SelectionKey.OP_READ);
    }

    private List<Channel> channelsFromSelector(final Selector selector) {
        return selector.keys().stream().map(SelectionKey::channel).collect(Collectors.toList());
    }

    private static class HelloAttr {
        private final int threadNumber;
        private final int allRequests;
        private int requests;

        public HelloAttr(final int threadNumber, final int requests) {
            this.threadNumber = threadNumber;
            this.allRequests = requests;
            this.requests = requests;
        }

        public int getThreadNumber() {
            return threadNumber;
        }

        public int getRequestNumber() {
            return allRequests - requests;
        }

        public void nextRequest() {
            requests--;
        }

        public boolean isCompleted() {
            return requests == 0;
        }
    }

    public static void main(String[] args) {
        new HelloUDPNonblockingClient().clientMain(args);
    }
}
