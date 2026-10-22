
package edu.itc.se.lab01.task3;

import java.util.Random;

public class Producer implements Runnable {
    private final OrderQueue queue;
    private final int startId;
    private final int numberOfOrders;
    private final Random random = new Random();

    public Producer(
            OrderQueue queue, int startId, int numberOfOrders) {
        this.queue = queue;
        this.startId = startId;
        this.numberOfOrders = numberOfOrders;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < numberOfOrders; i++) {
                Order order = new Order(
                    startId + i,
                    "item-" + (startId + i),
                    random.nextInt(5) + 1
                );

                queue.put(order);
                Thread.sleep(random.nextInt(21));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
