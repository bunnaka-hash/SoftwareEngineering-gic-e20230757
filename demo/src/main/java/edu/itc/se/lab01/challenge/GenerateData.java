
package edu.itc.se.lab01.challenge;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class GenerateData {

    private static final int FILE_COUNT = 200;
    private static final int WORDS_PER_FILE = 50_000;
    private static final int VOCABULARY_SIZE = 2_000;

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "data");
        Files.createDirectories(dir);

        String[] vocabulary = new String[VOCABULARY_SIZE];

        for (int i = 0; i < vocabulary.length; i++) {
            vocabulary[i] = "word" + letters(i);
        }

        Random random = new Random(42);

        for (int file = 0; file < FILE_COUNT; file++) {
            Path output = dir.resolve("file-" + file + ".txt");

            try (BufferedWriter writer =
                     Files.newBufferedWriter(output)) {

                for (int word = 1; word <= WORDS_PER_FILE; word++) {
                    writer.write(vocabulary[random.nextInt(
                        VOCABULARY_SIZE)]);

                    if (word % 100 == 0) {
                        writer.newLine();
                    } else {
                        writer.write(' ');
                    }
                }
            }

            if ((file + 1) % 50 == 0) {
                System.out.println(
                    "Generated " + (file + 1) + "/" + FILE_COUNT
                    + " files");
            }
        }

        System.out.println("Data directory: " + dir.toAbsolutePath());
        System.out.println("Total files: " + FILE_COUNT);
        System.out.println("Total words: "
            + (long) FILE_COUNT * WORDS_PER_FILE);
    }

    private static String letters(int number) {
        StringBuilder result = new StringBuilder();

        do {
            result.append((char) ('a' + number % 26));
            number /= 26;
        } while (number > 0);

        return result.reverse().toString();
    }
}
