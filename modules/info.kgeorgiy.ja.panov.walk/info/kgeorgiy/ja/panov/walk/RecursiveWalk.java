package info.kgeorgiy.ja.panov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class RecursiveWalk {
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
            FileVisitor<Path> visitor = new RecursiveFileVisitor(writer);
            Files.walkFileTree(path, visitor);
        } catch (InvalidPathException e) {
            writer.write(String.format("%016x %s%n", 0, stringPath));
        }
    }
}
