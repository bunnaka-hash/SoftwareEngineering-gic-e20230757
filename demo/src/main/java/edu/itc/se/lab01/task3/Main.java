
package edu.itc.se.lab01.task3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args)
            throws InterruptedException {

        OrderQueue queue = new BoundedBuffer(5);
        AtomicInteger processed = new AtomicInteger(0);

        List<Thread> consumers = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            Thread consumer = new Thread(
                new Consumer(queue, processed),
                "consumer-" + i
            );
            consumers.add(consumer);
            consumer.start();
        }

        Thread producer1 = new Thread(
            new Producer(queue, 1, 50), "producer-1"
        );

        Thread producer2 = new Thread(
            new Producer(queue, 51, 50), "producer-2"
        );

        long start = System.nanoTime();

        producer1.start();
        producer2.start();

        producer1.join();
        producer2.join();

        // All orders have been produced. Stop all consumers.
        for (int i = 0; i < 3; i++) {
            queue.put(new Order(-1, "poison", 0));
        }

        for (Thread consumer : consumers) {
            consumer.join();
        }

        long ms = (System.nanoTime() - start) / 1_000_000;

        System.out.printf(
            "Total processed: %d (expected 100) in %d ms%n",
            processed.get(), ms
        );

        if (processed.get() != 100) {
            throw new AssertionError("Incorrect order count!");
        }
    }
}
