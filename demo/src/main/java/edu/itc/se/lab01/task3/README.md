
## Task 3: Producer–Consumer

### Why use while instead of if?

A waiting thread must check the condition again after waking.
Another thread may have changed the buffer before it reacquires
the monitor. Spurious wakeups are also possible.

### Why use notifyAll()?

Multiple producers and consumers wait on the same monitor.
notifyAll() wakes all waiting threads so they can recheck
their conditions. notify() can wake a thread that cannot
make progress, potentially leaving the useful thread asleep.

### Custom buffer versus BlockingQueue

BoundedBuffer demonstrates how synchronized, wait(), and
notifyAll() coordinate threads manually. ArrayBlockingQueue
provides this functionality through a standard library class,
so less synchronization code is needed.
