
## Task 5: Diagnosing a Deadlock and Shutting Down Gracefully

### 1. Broken Bank

The original Bank can deadlock because each transfer acquires
the sender account first and the receiver account second.
Two threads may each hold one lock while waiting for the other.

The watchdog uses ThreadMXBean.findDeadlockedThreads()
to detect and report deadlocked thread names.

The actual jstack report from my computer is saved in
src/main/java/edu/itc/se/lab01/task5/deadlock.txt.

### 2. OrderedBank

OrderedBank acquires account monitors in ascending account ID
order. All transfers follow the same lock order, preventing
circular wait. Eight workers perform 800,000 transfer attempts.
The final total balance remains 4,000.

### 3. TryLockBank

TryLockBank uses ReentrantLock.tryLock() with a 10 ms timeout.
If both locks cannot be acquired, any acquired lock is released,
and the worker retries after a random back-off.

The retry counter records failed attempts to acquire both locks.
The final total balance remains 4,000.

### 4. Graceful shutdown

Workers check their interruption status and stop cooperatively.
When a blocking call throws InterruptedException, the worker
restores the interruption flag before exiting.

The main thread interrupts the workers after two seconds,
joins them with timeouts, and checks the final balance.

### 5. Coffman deadlock conditions

The broken Bank can satisfy all four Coffman conditions:
mutual exclusion, hold and wait, no preemption, and circular wait.

OrderedBank prevents circular wait by enforcing a consistent
account lock order. TryLockBank avoids indefinite hold-and-wait
by releasing acquired locks when the second lock cannot be obtained.
