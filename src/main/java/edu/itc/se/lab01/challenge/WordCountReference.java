package edu.itc.se.lab01.challenge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Simple single-threaded reference implementation used by tests. */
public final class WordCountReference {
    private static final Pattern SEPARATOR = Pattern.compile("[^\\p{L}]+");

    private WordCountReference() {}

    public static Map<String, Long> count(Path dir) throws IOException {
        Map<String, Long> result = new HashMap<>();
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                try (var lines = Files.lines(path)) {
                    lines.forEach(line -> {
                        for (String word : SEPARATOR.split(line.toLowerCase(Locale.ROOT))) {
                            if (!word.isEmpty()) result.merge(word, 1L, Long::sum);
                        }
                    });
                }
            }
        }
        return result;
    }
}
