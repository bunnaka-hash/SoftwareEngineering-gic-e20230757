
package edu.itc.se.lab01.task5;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class TryLockBank {

    static final class Account {
        final int id;
        private long balance;
        final ReentrantLock lock = new ReentrantLock();

        Account(int id, long balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    static final Account[] accounts = new Account[4];
    static final AtomicLong transfers = new AtomicLong();
    static final AtomicLong retries = new AtomicLong();

    static {
        for (int i = 0; i < accounts.length; i++) {
            accounts[i] = new Account(i, 1_000);
        }
    }

    static boolean transfer(Account from, Account to, long amount)
            throws InterruptedException {

        while (!Thread.currentThread().isInterrupted()) {
            boolean firstLocked = false;
            boolean secondLocked = false;

            try {
                firstLocked = from.lock.tryLock(
                    10, TimeUnit.MILLISECONDS
                );

                if (firstLocked) {
                    secondLocked = to.lock.tryLock(
                        10, TimeUnit.MILLISECONDS
                    );
                }

                if (firstLocked && secondLocked) {
                    if (from.balance >= amount) {
                        from.balance -= amount;
                        to.balance += amount;
                    }

                    transfers.incrementAndGet();
                    return true;
                }

                retries.incrementAndGet();

            } finally {
                if (secondLocked) {
                    to.lock.unlock();
                }

                if (firstLocked) {
                    from.lock.unlock();
                }
            }

            // Back off after releasing any acquired lock.
            Thread.sleep(
                ThreadLocalRandom.current().nextLong(1, 6)
            );
        }

        return false;
    }

    static long totalBalance() {
        long total = 0;

        for (Account account : accounts) {
            account.lock.lock();
        }

        try {
            for (Account account : accounts) {
                total += account.balance;
            }
        } finally {
            for (int i = accounts.length - 1; i >= 0; i--) {
                accounts[i].lock.unlock();
            }
        }

        return total;
    }

    static void runBenchmark() throws InterruptedException {
        int workerCount = 8;
        int iterations = 100_000;
        Thread[] workers = new Thread[workerCount];

        transfers.set(0);
        retries.set(0);

        long start = System.nanoTime();

        for (int w = 0; w < workerCount; w++) {
            workers[w] = new Thread(() -> {
                var random = ThreadLocalRandom.current();

                try {
                    for (int i = 0; i < iterations; i++) {
                        Account from = accounts[
                            random.nextInt(accounts.length)
                        ];
                        Account to = accounts[
                            random.nextInt(accounts.length)
                        ];

                        if (from != to) {
                            transfer(from, to, random.nextInt(1, 50));
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "trylock-transfer-" + w);

            workers[w].start();
        }

        for (Thread worker : workers) {
            worker.join();
        }

        long elapsed = (System.nanoTime() - start) / 1_000_000;
        long total = totalBalance();

        System.out.printf(
            "transfers=%d retries=%d total=%d "
                + "(expected 4000) in %d ms%n",
            transfers.get(), retries.get(), total, elapsed
        );

        if (total != 4_000) {
            throw new AssertionError("Balance changed!");
        }
    }

    static void runGracefulShutdown() throws InterruptedException {
        int workerCount = 8;
        Thread[] workers = new Thread[workerCount];

        transfers.set(0);
        retries.set(0);

        for (int w = 0; w < workerCount; w++) {
            workers[w] = new Thread(() -> {
                var random = ThreadLocalRandom.current();

                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Account from = accounts[
                            random.nextInt(accounts.length)
                        ];
                        Account to = accounts[
                            random.nextInt(accounts.length)
                        ];

                        if (from != to) {
                            transfer(from, to, random.nextInt(1, 50));
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                System.out.println(
                    Thread.currentThread().getName()
                        + " stopped"
                );
            }, "transfer-" + w);

            workers[w].start();
        }

        Thread.sleep(2_000);

        System.out.println(
            "[main] interrupting " + workerCount
                + " workers after 2000 ms"
        );

        for (Thread worker : workers) {
            worker.interrupt();
        }

        for (Thread worker : workers) {
            worker.join(1_000);
        }

        boolean allStopped = true;

        for (Thread worker : workers) {
            if (worker.isAlive()) {
                allStopped = false;
                worker.interrupt();
            }
        }

        // Allow a final short wait after the first timeout.
        for (Thread worker : workers) {
            if (worker.isAlive()) {
                worker.join(500);
            }
        }

        for (Thread worker : workers) {
            if (worker.isAlive()) {
                allStopped = false;
            }
        }

        long total = totalBalance();

        System.out.printf(
            "transfers=%d%nall workers stopped=%b total=%d "
                + "(expected 4000)%n",
            transfers.get(), allStopped, total
        );

        if (!allStopped || total != 4_000) {
            throw new AssertionError(
                "Shutdown or balance verification failed"
            );
        }
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length > 0 && args[0].equals("--shutdown")) {
            runGracefulShutdown();
        } else {
            runBenchmark();
        }
    }
}
