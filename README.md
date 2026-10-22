# Lab01 – Multithreading in Java

This repository contains my Java Multithreading lab exercises for the Software Engineering course at the Institute of Technology of Cambodia (ITC).

### 1. Java Threads

Learn how to create and execute threads, understand thread states, and observe concurrent program execution.

### 2. Platform Threads and Virtual Threads

Compare traditional platform threads with Java virtual threads and learn how to create and inspect different thread types.

### 3. Synchronization and Race Conditions

Explore how concurrent access to shared resources can cause race conditions and how synchronization helps protect shared data.

### 4. Deadlocks

Study how deadlocks occur when threads wait indefinitely for resources held by one another. Examine thread states and use diagnostic tools such as `jstack` to investigate deadlocks.

### 5. Parallel Word Count Challenge

Implement a parallel word-frequency analyzer that processes multiple text files using worker threads.

Features include:

- Generate 200 text files containing 10 million words in total.
- Scan files and distribute file paths through a bounded `BlockingQueue`.
- Count word frequencies using a `ConcurrentHashMap` and `LongAdder`.
- Display progress, processing speed, and the most frequent words.
- Support platform threads, virtual threads, and timeout-based cancellation.
- Export word-frequency results to `result.csv`.

## Author

**Bunnaka**  
Software Engineering / GIC  
Institute of Technology of Cambodia (ITC)