package edu.itc.se.lab01.challenge;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class WordCount {
    private static final Path POISON = Path.of("__WORDCOUNT_POISON__");
    private static final Pattern SEPARATOR = Pattern.compile("[^\\p{L}]+");

    private final BlockingQueue<Path> queue = new ArrayBlockingQueue<>(16);
    private final ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
    private final AtomicInteger filesDiscovered = new AtomicInteger();
    private final AtomicInteger filesDone = new AtomicInteger();
    private final LongAdder wordsDone = new LongAdder();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final AtomicBoolean partialPrinted = new AtomicBoolean();

    private final Path dir;
    private final int workers;
    private final boolean virtual;
    private final int top;
    private final long timeoutSec;
    private final long startedNanos = System.nanoTime();

    private volatile Thread scanner;
    private volatile ExecutorService pool;
    private volatile Thread[] workerThreads = new Thread[0];

    WordCount(Path dir, int workers, boolean virtual, int top, long timeoutSec) {
        this.dir = dir;
        this.workers = Math.max(1, workers);
        this.virtual = virtual;
        this.top = Math.max(1, top);
        this.timeoutSec = Math.max(0, timeoutSec);
    }

    public void run() throws InterruptedException {
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Not a directory: " + dir.toAbsolutePath());
        }

        pool = virtual
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(workers);

        CountDownLatch workersFinished = new CountDownLatch(workers);
        workerThreads = new Thread[workers];
        for (int i = 0; i < workers; i++) {
            final int index = i;
            pool.submit(() -> {
                Thread.currentThread().setName("word-worker-" + index);
                try {
                    work();
                } finally {
                    workersFinished.countDown();
                }
            });
        }

        scanner = Thread.ofPlatform().name("word-scanner").unstarted(this::scan);
        scanner.start();

        Thread reporter = Thread.ofPlatform().name("word-reporter").start(() -> {
            while (!stopping.get() && !workersFinishedDone(workersFinished)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (!stopping.get()) report();
            }
        });

        Thread shutdownHook = new Thread(this::shutdown, "wordcount-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        boolean completed;
        try {
            if (timeoutSec > 0) {
                completed = workersFinished.await(timeoutSec, TimeUnit.SECONDS);
            } else {
                workersFinished.await();
                completed = true;
            }
            if (completed && !stopping.get()) {
                scanner.join(200);
                pool.shutdown();
                pool.awaitTermination(2, TimeUnit.SECONDS);
                reporter.interrupt();
                printResult(false);
            } else if (!stopping.get()) {
                System.out.println("deadline reached – stopping");
                shutdown();
            }
        } finally {
            reporter.interrupt();
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM shutdown is already in progress.
            }
        }
    }

    private boolean workersFinishedDone(CountDownLatch latch) {
        return latch.getCount() == 0;
    }

    private void scan() {
        try (Stream<Path> paths = Files.walk(dir)) {
            var iterator = paths.filter(Files::isRegularFile).iterator();
            while (iterator.hasNext() && !stopping.get()) {
                Path path = iterator.next();
                filesDiscovered.incrementAndGet();
                queue.put(path);
            }
        } catch (IOException e) {
            System.err.println("Scanner error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (!stopping.get()) {
                try {
                    for (int i = 0; i < workers; i++) queue.put(POISON);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void work() {
        try {
            while (!Thread.currentThread().isInterrupted() && !stopping.get()) {
                Path path = queue.take();
                if (path.equals(POISON)) return;
                if (Thread.currentThread().isInterrupted() || stopping.get()) return;
                countFile(path);
                filesDone.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void countFile(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted() || stopping.get()) return;
                String lower = line.toLowerCase(Locale.ROOT);
                for (String word : SEPARATOR.split(lower)) {
                    if (!word.isEmpty()) {
                        counts.computeIfAbsent(word, ignored -> new LongAdder()).increment();
                        wordsDone.increment();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read " + path + ": " + e.getMessage());
        }
    }

    private void report() {
        double elapsed = elapsedSeconds();
        double rate = elapsed <= 0 ? 0 : wordsDone.sum() / elapsed;
        System.out.printf(Locale.ROOT,
                "[%.1fs] files %d/%d  words %,d  %,d w/s  top: %s%n",
                elapsed, filesDone.get(), filesDiscovered.get(), wordsDone.sum(),
                Math.round(rate), topWords(3));
    }

    void shutdown() {
        if (!stopping.compareAndSet(false, true)) return;

        Thread scanThread = scanner;
        if (scanThread != null) scanThread.interrupt();

        ExecutorService executor = pool;
        if (executor != null) executor.shutdownNow();

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        if (scanThread != null && scanThread != Thread.currentThread()) {
            try {
                long remaining = deadline - System.nanoTime();
                if (remaining > 0) scanThread.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (executor != null) {
            try {
                long remaining = deadline - System.nanoTime();
                if (remaining > 0) executor.awaitTermination(remaining, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (partialPrinted.compareAndSet(false, true)) printResult(true);
    }

    private void printResult(boolean partial) {
        long totalWords = wordsDone.sum();
        long totalFiles = filesDone.get();
        String status = partial ? "[PARTIAL]" : "DONE";
        String mode = virtual ? "virtual(" + workers + ")" : "platform(" + workers + ")";
        System.out.printf(Locale.ROOT, "%s files=%d words=%,d elapsed=%.2fs mode=%s%n",
                status, totalFiles, totalWords, elapsedSeconds(), mode);

        List<Map.Entry<String, Long>> entries = sortedEntries();
        System.out.println(" rank  word                  count      share");
        int limit = Math.min(top, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Long> entry = entries.get(i);
            double share = totalWords == 0 ? 0 : entry.getValue() * 100.0 / totalWords;
            System.out.printf(Locale.ROOT, "%5d  %-20s %,10d  %6.2f%%%n",
                    i + 1, entry.getKey(), entry.getValue(), share);
        }

        try {
            Path csv = Path.of("result.csv");
            try (var writer = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
                writer.write("word,count,percentage");
                writer.newLine();
                for (Map.Entry<String, Long> entry : entries) {
                    double share = totalWords == 0 ? 0 : entry.getValue() * 100.0 / totalWords;
                    writer.write(csvEscape(entry.getKey()) + "," + entry.getValue()
                            + "," + String.format(Locale.ROOT, "%.6f", share));
                    writer.newLine();
                }
            }
            System.out.println("result.csv written");
        } catch (IOException e) {
            System.err.println("Could not write result.csv: " + e.getMessage());
        }
    }

    private String topWords(int n) {
        List<Map.Entry<String, Long>> entries = sortedEntries();
        List<String> words = new ArrayList<>();
        for (int i = 0; i < Math.min(n, entries.size()); i++) {
            words.add(entries.get(i).getKey() + "(" + String.format(Locale.ROOT, "%,d", entries.get(i).getValue()) + ")");
        }
        return String.join(" ", words);
    }

    private List<Map.Entry<String, Long>> sortedEntries() {
        List<Map.Entry<String, Long>> entries = new ArrayList<>();
        for (var entry : counts.entrySet()) {
            entries.add(Map.entry(entry.getKey(), entry.getValue().sum()));
        }
        entries.sort(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
                .reversed().thenComparing(Map.Entry::getKey));
        return entries;
    }

    private double elapsedSeconds() {
        return (System.nanoTime() - startedNanos) / 1_000_000_000.0;
    }

    private static String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: WordCount <dir> [--top N] [--timeout SECONDS] [--virtual]");
            System.exit(2);
        }

        Path dir = Path.of(args[0]);
        int top = 10;
        long timeout = 0;
        boolean virtual = false;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--top" -> {
                    if (++i >= args.length) throw new IllegalArgumentException("--top needs N");
                    top = Integer.parseInt(args[i]);
                }
                case "--timeout" -> {
                    if (++i >= args.length) throw new IllegalArgumentException("--timeout needs seconds");
                    timeout = Long.parseLong(args[i]);
                }
                case "--virtual" -> virtual = true;
                default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }

        int workers = Math.max(1, Runtime.getRuntime().availableProcessors());
        new WordCount(dir, workers, virtual, top, timeout).run();
    }
}
