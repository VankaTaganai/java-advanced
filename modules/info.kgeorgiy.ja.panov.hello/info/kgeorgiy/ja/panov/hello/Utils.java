package info.kgeorgiy.ja.panov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static int TIMEOUT_MILLISECONDS = 300;
    public static final int AWAIT_TIME_MILLISECONDS = 100;
    public static final String RESPONSE_PREFIX = "Hello, ";
    private static final Pattern VALIDATE_PATTERN = Pattern.compile("(\\D*)(\\d+)(\\D+)(\\d+)(\\D*)");

    public static String packetToString(final DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), CHARSET);
    }

    public static String receive(final DatagramSocket datagramSocket,
                                 final DatagramPacket responsePacket,
                                 final String prefix) throws IOException {
        datagramSocket.receive(responsePacket);
        return prefix + Utils.packetToString(responsePacket);
    }

    public static void shutDownExecutorService(final ExecutorService executorService, final long awaitTimeMilliseconds) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(awaitTimeMilliseconds, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(awaitTimeMilliseconds, TimeUnit.MILLISECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    public static boolean validateReceive(final String receive, final int threadNum, final int requestNum) {
        Matcher matcher = VALIDATE_PATTERN.matcher(receive);
        return matcher.matches() &&
                matcher.group(2).equals(Integer.toString(threadNum)) &&
                matcher.group(4).equals(Integer.toString(requestNum));
    }

    public static String requestString(final String prefix, final int threadNumber, final int requestNumber) {
        return prefix + threadNumber + "_" + requestNumber;
    }

    public static String byteBufferToString(final ByteBuffer byteBuffer) {
        byteBuffer.flip();
        return StandardCharsets.UTF_8.decode(byteBuffer).toString();
    }

    public static void closeWithIgnore(final Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignore) {
            // ignore
        }
    }

    public static void closeIterableWithIgnore(final Iterable<? extends Closeable> iterable) {
        iterable.forEach(Utils::closeWithIgnore);
    }

    public static void iterateSelectedKeys(final Selector selector, IOConsumer<SelectionKey> consumer) {
        for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); ) {
            try {
                final SelectionKey key = iterator.next();
                if (!key.isValid()) {
                    continue;
                }
                consumer.accept(key);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                iterator.remove();
            }
        }
    }

    @FunctionalInterface
    public interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }

    public static DatagramChannel createDatagramChannel(
            final Selector selector,
            final int ops,
            final Object attr,
            final IOConsumer<DatagramChannel> addressBind
            ) throws IOException {
        DatagramChannel datagramChannel;
        datagramChannel = DatagramChannel.open();
        try {
            addressBind.accept(datagramChannel);
            datagramChannel.configureBlocking(false);
            datagramChannel.register(selector, ops, attr);
        } catch (IOException e) {
            datagramChannel.close();
            throw e;
        }
        return datagramChannel;
    }
}
