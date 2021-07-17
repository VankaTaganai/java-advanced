package info.kgeorgiy.ja.panov.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveFileVisitor extends SimpleFileVisitor<Path> {
    private final BufferedWriter writer;

    public RecursiveFileVisitor(BufferedWriter writer) {
        this.writer = writer;
    }


    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        writer.write(String.format("%016x %s%n", hashFile(file), file.toString())); // todo copypaste
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        writer.write(String.format("%016x %s%n", 0, file.toString())); // todo copypaste
        return FileVisitResult.CONTINUE;
    }

    private static long hashFile(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] bytes = new byte[1024]; // todo почему?
            int size;
            long hash = 0;
            while ((size = inputStream.read(bytes)) >= 0) {
                for (int i = 0; i < size; i++) {
                    hash = (hash << 8) + (bytes[i] & 0xff);
                    final long high = hash & 0xff00_0000_0000_0000L;
                    if (high != 0) {
                        hash ^= high >> 48;
                        hash &= ~high;
                    }
                }
            }
            return hash;
        } catch (IOException e) {
            return 0;
        }
    }
}
