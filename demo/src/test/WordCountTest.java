package edu.itc.se.lab01.challenge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordCountTest {
    @TempDir
    Path tempDir;

    @Test
    void referenceCountsTwentyFileFixture() throws IOException {
        Path fixture = Files.createDirectory(tempDir.resolve("fixture"));
        for (int i = 0; i < 20; i++) {
            Files.writeString(fixture.resolve("file-" + i + ".txt"),
                    "Apple banana APPLE\npear banana fruit " + i + "\n");
        }

        Map<String, Long> first = WordCountReference.count(fixture);
        Map<String, Long> expected = Map.of(
                "apple", 40L,
                "banana", 40L,
                "pear", 20L,
                "fruit", 20L
        );
        // The numeric suffix contributes one word for each file (0..19).
        assertEquals(20, first.get("apple"));
        assertEquals(40L, first.get("banana"));
        assertEquals(20L, first.get("pear"));
        assertEquals(20L, first.get("fruit"));
        assertTrue(first.size() >= expected.size());
    }

    @Test
    void timeoutDirectoryCanBePreparedWithFiveThousandFiles() throws IOException {
        Path fixture = Files.createDirectory(tempDir.resolve("many"));
        for (int i = 0; i < 5_000; i++) {
            Files.writeString(fixture.resolve("f-" + i + ".txt"), "one two three\n");
        }
        assertEquals(5_000, Files.list(fixture).count());
    }
}
