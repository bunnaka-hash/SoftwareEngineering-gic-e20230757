
package edu.itc.se.lab01.task4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

    static final long LIMIT = 4_000_000L;
    static final int CHUNKS = 16;
    static final int DOWNLOADS = 500;

    // Counts prime numbers in [from, to).
    static long countPrimes(long from, long to) {
        long count = 0;

        for (long n = Math.max(from, 2); n < to; n++) {
            boolean prime = true;

            for (long d = 2; d * d <= n; d++) {
                if (n % d == 0) {
                    prime = false;
                    break;
                }
            }

            if (prime) {
                count++;
            }
        }

        return count;
    }

    // Callable lets us submit a task and retrieve its result.
    static class PrimeCounter implements Callable<Long> {
        private final long from;
        private final long to;

        PrimeCounter(long from, long to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public Long call() {
            return countPrimes(from, to);
        }
    }

    // Simulates a network download.
    static class FakeDownload implements Callable<String> {
        private final String url;
        private final int id;

        FakeDownload(String url, int id) {
            this.url = url;
            this.id = id;
        }

        @Override
        public String call() throws Exception {
            // One download takes 5 seconds to test timeout handling.
            if (id == 1) {
                Thread.sleep(5000);
            } else {
                Thread.sleep(200);
            }

            // Every 50th download fails.
            if (id % 50 == 0) {
                throw new IOException("boom");
            }

            return url + " -> " + url.length();
        }
    }

    static void benchmarkDownloads(
            String label, ExecutorService executor)
            throws InterruptedException {

        long start = System.nanoTime();

        List<Future<String>> futures = new ArrayList<>();

        for (int i = 1; i <= DOWNLOADS; i++) {
            String url = "https://example.com/file/" + i;

            futures.add(executor.submit(
                new FakeDownload(url, i)
            ));
        }

        int ok = 0;
        int failed = 0;
        int timedOut = 0;

        for (int i = 0; i < futures.size(); i++) {
            Future<String> future = futures.get(i);

            try {
                // Download 1 has a 1-second timeout.
                if (i == 0) {
                    future.get(1, TimeUnit.SECONDS);
                } else {
                    future.get();
                }

                ok++;

            } catch (TimeoutException e) {
                timedOut++;
                future.cancel(true);

            } catch (ExecutionException e) {
                failed++;

                // The failure is expected; do not crash.
                if (!(e.getCause() instanceof IOException)) {
                    System.err.println(
                        "Unexpected task error: " + e.getCause()
                    );
                }

            } catch (CancellationException e) {
                // A cancelled task is not counted as successful.
                timedOut++;
            }
        }

        long ms = (System.nanoTime() - start) / 1_000_000;

        System.out.printf(
            "%-40s ok=%d failed=%d timedOut=%d in %d ms%n",
            label, ok, failed, timedOut, ms
        );
    }

    public static void main(String[] args) throws Exception {

        // PART 1: Single-threaded prime count.
        long start = System.nanoTime();
        long single = countPrimes(0, LIMIT);

        System.out.printf(
            "single-threaded: %d primes in %d ms%n",
            single, (System.nanoTime() - start) / 1_000_000
        );

        // PART 2: Parallel prime count.
        start = System.nanoTime();

        int threads = Runtime.getRuntime().availableProcessors();
        long total = 0;

        try (ExecutorService pool =
                Executors.newFixedThreadPool(threads)) {

            List<Future<Long>> futures = new ArrayList<>();

            for (int i = 0; i < CHUNKS; i++) {
                long from = LIMIT * i / CHUNKS;
                long to = LIMIT * (i + 1) / CHUNKS;

                futures.add(pool.submit(
                    new PrimeCounter(from, to)
                ));
            }

            for (Future<Long> future : futures) {
                total += future.get();
            }

            System.out.printf(
                "pool (%d threads): %d primes in %d ms%n",
                threads, total,
                (System.nanoTime() - start) / 1_000_000
            );
        }

        if (single != total) {
            throw new AssertionError("Prime counts do not match!");
        }

        // PART 3: I/O-bound downloads using platform threads.
        try (ExecutorService pool =
                Executors.newFixedThreadPool(20)) {

            benchmarkDownloads(
                "downloads on fixed(20) platform threads",
                pool
            );
        }

        // PART 4: I/O-bound downloads using virtual threads.
        try (ExecutorService pool =
                Executors.newVirtualThreadPerTaskExecutor()) {

            benchmarkDownloads(
                "downloads on virtual threads",
                pool
            );
        }
    }
}
