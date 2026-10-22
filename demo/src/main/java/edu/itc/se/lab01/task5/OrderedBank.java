
package edu.itc.se.lab01.task5;

public class OrderedBank {

    static class Account {
        final int id;
        int balance;

        Account(int id, int balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    private final Account[] accounts = {
        new Account(0, 1000),
        new Account(1, 1000),
        new Account(2, 1000),
        new Account(3, 1000)
    };

    public void transfer(int fromId, int toId, int amount) {
        if (fromId == toId) {
            return;
        }

        Account from = accounts[fromId];
        Account to = accounts[toId];

        // Always lock accounts in ascending ID order.
        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;

        synchronized (first) {
            synchronized (second) {
                if (from.balance >= amount) {
                    from.balance -= amount;
                    to.balance += amount;
                }
            }
        }
    }

    public int totalBalance() {
        // Lock all accounts in the same consistent order.
        synchronized (accounts[0]) {
            synchronized (accounts[1]) {
                synchronized (accounts[2]) {
                    synchronized (accounts[3]) {
                        return accounts[0].balance
                             + accounts[1].balance
                             + accounts[2].balance
                             + accounts[3].balance;
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        OrderedBank bank = new OrderedBank();
        int threadCount = 8;
        int transfersPerThread = 100_000;
        Thread[] workers = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int workerId = i;

            workers[i] = new Thread(() -> {
                java.util.Random random =
                    new java.util.Random(workerId);

                for (int j = 0; j < transfersPerThread; j++) {
                    int from = random.nextInt(4);
                    int to = random.nextInt(4);

                    if (from != to) {
                        bank.transfer(from, to, 1);
                    }
                }
            }, "transfer-" + i);

            workers[i].start();
        }

        for (Thread worker : workers) {
            worker.join();
        }

        System.out.println("All transfers completed.");
        System.out.println("Total balance: " + bank.totalBalance());
        System.out.println("Expected total: 4000");
    }
}
