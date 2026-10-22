
package edu.itc.se.lab01.task3;

import java.util.concurrent.atomic.AtomicInteger;

public class Consumer implements Runnable {
    private final OrderQueue queue;
    private final AtomicInteger processed;

    public Consumer(OrderQueue queue, AtomicInteger processed) {
        this.queue = queue;
        this.processed = processed;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Order order = queue.take();

                if (order.id() == -1) {
                    System.out.println(
                        Thread.currentThread().getName()
                        + " received poison pill, stopping"
                    );
                    break;
                }

                Thread.sleep(
                    (long) (Math.random() * 26) + 5
                );

                processed.incrementAndGet();

                System.out.println(
                    Thread.currentThread().getName()
                    + " processed " + order
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
