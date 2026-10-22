
## Task 2: Fixing a Race Condition

### 1. Why is UnsafeCounter incorrect?

The expression count++ is not atomic. Multiple threads
can read the same value and overwrite one another's updates.
This causes lost updates and an incorrect final result.

### 2. Comparison

- UnsafeCounter: not thread-safe; updates can be lost.
- SynchronizedCounter: uses synchronized methods to protect
  shared state.
- LockCounter: uses ReentrantLock and releases it in finally.
- AtomicCounter: uses AtomicLong for atomic increments.
- LongAdderCounter: spreads updates across internal cells,
  which can reduce contention when many threads update it.

### 3. Why must get() also be synchronized?

If only increment() is synchronized, get() can read the
shared count without acquiring the same monitor. It can
therefore observe a stale or intermediate value when called
concurrently with updates. Synchronizing both methods
provides consistent visibility and access through the same
monitor.

### 4. Results

Record your three UnsafeCounter results and the measured
execution times for all implementations here.
