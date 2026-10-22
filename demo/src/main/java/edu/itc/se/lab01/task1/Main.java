
package edu.itc.se.lab01.task1;

/*
 * Concurrent mode: start() launches separate threads, so their
 * countdown messages can interleave.
 
 * Sequential mode: run() executes on the main thread, so each
 * countdown finishes before the next begins. The Thread objects
 * remain in NEW state because start() was never called.
 */

public class Main {

    public static void main(String[] args) throws InterruptedException {

        boolean sequential =
                args.length > 0 && args[0].equals("--sequential");

        Thread apollo = new Countdown("apollo");

        Thread gemini = new Thread(new CountdownTask(), "gemini");

        Thread mercury = new Thread(() -> {runCountdown("mercury");}, "mercury");

        Thread artemis =
                Thread.ofVirtual()
                      .name("artemis")
                      .unstarted(new CountdownTask());

        Thread[] threads = {apollo, gemini, mercury, artemis};

        // Start or run all four tasks
        for (Thread thread : threads) {
            if (sequential) {
                if (thread == apollo) {
                    thread.run();
                } else if (thread == gemini) {
                    new CountdownTaskWithName("gemini").run();
                } else if (thread == mercury) {
                    runCountdown("mercury");
                } else {
                    new CountdownTaskWithName("artemis").run();
                }
            } else {
                thread.start();
            }
        }

        System.out.println("Mission control: all launched");

        // Wait for all threads in concurrent mode
        if (!sequential) {
            for (Thread thread : threads) {
                thread.join();
            }
        }

        System.out.println("Mission control: all landed");

        // Display thread information
        for (Thread thread : threads) {
            System.out.printf(
                    "%-8s virtual=%-5s state=%s%n",
                    thread.getName(),
                    thread.isVirtual(),
                    thread.getState()
            );
        }
    }

    private static void runCountdown(String name) {
        try {
            for (int i = 3; i >= 1; i--) {
                System.out.println(name + ": " + i);
                Thread.sleep(200);
            }
            System.out.println(name + ": liftoff!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class CountdownTaskWithName implements Runnable {
        private final String name;

        CountdownTaskWithName(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            runCountdown(name);
        }
    }
}
