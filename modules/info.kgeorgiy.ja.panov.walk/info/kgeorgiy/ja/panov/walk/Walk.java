package info.kgeorgiy.ja.panov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class Walk {
    public static void main(String[] args) {
        try {
            if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
                throw new IOException("Wrong number of program arguments");
            }

            final String inputFile = args[0];
            final String outputFile = args[1];

            Path inputPath;
            try {
                inputPath = Path.of(inputFile);
                Path parent = inputPath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectory(parent);
                }
            } catch (InvalidPathException e) {
                throw new IOException("Input path string cannot bbe converted to a Path: " + e.getMessage());
            }
            Path outputPath;
            try {
                outputPath = Path.of(outputFile);
            } catch (InvalidPathException e) {
                throw new IOException("Output path string cannot be converted to a Path: " + e.getMessage());
            }

            try (final BufferedReader inputReader = Files.newBufferedReader(inputPath)) {
                try (final BufferedWriter outputWriter = Files.newBufferedWriter(outputPath)) {
                    String line;
                    while ((line = inputReader.readLine()) != null) {
                        recursiveVisit(line, outputWriter);
                    }
                } catch (IOException e) {
                    throw new IOException("Output error occurred: " + e.getMessage());
                }
            } catch (IOException e) {
                throw new IOException("Input error occurred: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void recursiveVisit(String stringPath, BufferedWriter writer) throws IOException {
        Path path;
        try {
            path = Path.of(stringPath);
        } catch (InvalidPathException e) {
            writer.write(String.format("%016x %s%n", 0, stringPath));
            return;
        }

        if (Files.isDirectory(path)) {
            for (Path p : Files.walk(path).filter(p -> !Files.isDirectory(p)).toArray(Path[]::new)) {
                writer.write(String.format("%016x %s%n", hashFile(p), p.toString()));
            }
        } else {
            writer.write(String.format("%016x %s%n", hashFile(path), stringPath)); // todo copypaste
        }
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
