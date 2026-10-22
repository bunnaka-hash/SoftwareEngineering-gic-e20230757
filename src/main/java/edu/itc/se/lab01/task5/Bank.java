
package edu.itc.se.lab01.task5;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ThreadLocalRandom;

public class Bank {

    static final class Account {
        final int id;
        private long balance;

        Account(int id, long balance) {
            this.id = id;
            this.balance = balance;
        }

        long balance() {
            return balance;
        }

        void withdraw(long amount) {
            balance -= amount;
        }

        void deposit(long amount) {
            balance += amount;
        }
    }

    // INTENTIONALLY BROKEN: lock order depends on argument order.
    static void transfer(Account from, Account to, long amount) {
        synchronized (from) {
            synchronized (to) {
                if (from.balance() >= amount) {
                    from.withdraw(amount);
                    to.deposit(amount);
                }
            }
        }
    }

    public static void main(String[] args)
            throws InterruptedException {

        Account[] accounts = new Account[4];

        for (int i = 0; i < accounts.length; i++) {
            accounts[i] = new Account(i, 1_000);
        }

        // Watchdog detects deadlocked threads every second.
        Thread watchdog = new Thread(() -> {
            while (true) {
                long[] ids = ManagementFactory
                    .getThreadMXBean()
                    .findDeadlockedThreads();

                if (ids != null) {
                    var info = ManagementFactory
                        .getThreadMXBean()
                        .getThreadInfo(ids);

                    System.out.print(
                        "[watchdog] DEADLOCK detected between: "
                    );

                    for (var thread : info) {
                        System.out.print(thread.getThreadName() + " ");
                    }

                    System.out.println();
                    return;
                }

                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "deadlock-watchdog");

        watchdog.setDaemon(true);
        watchdog.start();

        Thread[] workers = new Thread[8];

        for (int w = 0; w < workers.length; w++) {
            workers[w] = new Thread(() -> {
                var rnd = ThreadLocalRandom.current();

                for (int i = 0; i < 100_000; i++) {
                    Account a = accounts[
                        rnd.nextInt(accounts.length)
                    ];
                    Account b = accounts[
                        rnd.nextInt(accounts.length)
                    ];

                    if (a != b) {
                        transfer(a, b, rnd.nextInt(1, 50));
                    }
                }
            }, "transfer-" + w);

            workers[w].start();
        }

        for (Thread worker : workers) {
            worker.join();
        }

        long total = 0;

        for (Account account : accounts) {
            total += account.balance();
        }

        System.out.println("total = " + total
            + " (expected 4000)");
    }
}
