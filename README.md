# Lab01 – Multithreading in Java

This repository contains my Java Multithreading lab exercises for the Software Engineering course at the Institute of Technology of Cambodia (ITC).

The lab explores Java threads, concurrent execution, synchronization, deadlocks, and performance considerations when working with multiple threads.

## Technologies Used

- **Java:** JDK 25
- **Build Tool:** Apache Maven
- **IDE:** Visual Studio Code
- **Operating System:** macOS
- **Testing:** JUnit (for tests, when configured)

## Project Structure

```text
```text
lab01-multithreading/
├── pom.xml
├── src/
│   ├── main/
│   │   └── java/
│   │       └── edu/itc/se/lab01/
│   │           ├── task1/
│   │           ├── task2/
│   │           ├── task3/
│   │           ├── task4/
│   │           ├── task5/
│   │           └── challenge/
│   └── test/
├── README.md
└── .gitignore
```

*Note: The package and folder names above are illustrative. Update them to match the actual project structure.*

## Topics Covered

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

## Requirements

Install the following tools before running the project:

- JDK 25 or another compatible JDK configured for the project.
- Apache Maven.
- Git (optional, for version control).

Verify the installations:

```bash
java -version
mvn -version
```

## Getting Started

Clone the repository and enter the Maven project directory:

```bash
git clone <your-repository-url>
cd lab01-multithreading/demo
```

Compile the project:

```bash
mvn clean compile
```

## Running the Exercises

Run each exercise using its fully qualified main class.

For example, to run the first task:

```bash
mvn exec:java -Dexec.mainClass="edu.itc.se.lab01.task1.Main"
```

Replace the main class with the appropriate class for each exercise.

## Running the Word Count Challenge

### Step 1: Generate the Dataset

```bash
mvn exec:java \
-Dexec.mainClass="edu.itc.se.lab01.challenge.GenerateData" \
-Dexec.args="data"
```

This generates the text files in the `data/` directory.

### Step 2: Run the Analyzer

```bash
mvn exec:java \
-Dexec.mainClass="edu.itc.se.lab01.challenge.WordCount" \
-Dexec.args="data --top 5 --timeout 30"
```

The program processes the dataset and displays the top five most frequent words.

Example output:

```text
DONE files=200 words=10,000,000 elapsed=1.13s mode=platform(8)
 rank  word                  count      share
    1  wordcfw                   5253    0.05%
    2  wordctf                   5249    0.05%
    3  wordcfh                   5244    0.05%
    4  wordbie                   5242    0.05%
    5  wordta                    5213    0.05%
result.csv written
```

*Example results from one run; processing time and word frequencies may vary between runs.*

### Step 3: Inspect the Results

```bash
head -n 6 result.csv
```

The CSV file contains the word, its frequency, and its percentage of the total word count.

## Useful Maven Commands

| Command | Purpose |
|---|---|
| `mvn clean` | Remove generated build files |
| `mvn compile` | Compile the source code |
| `mvn clean compile` | Clean and recompile the project |
| `mvn test` | Run configured automated tests |
| `mvn package` | Build the packaged application |

## Git Ignore

Generated files and large datasets should not be committed to the repository.

The `.gitignore` file excludes:

```gitignore
target/
data/
result.csv
*.class
.DS_Store
.idea/
.vscode/
```

Source code, configuration files, and documentation should remain under version control.

## Learning Objectives

By completing this lab, I aim to:

- Understand Java thread creation and execution.
- Distinguish platform threads from virtual threads.
- Recognize and prevent race conditions.
- Understand deadlocks and thread synchronization.
- Apply concurrent collections and thread-safe counters.
- Measure the performance of parallel processing.
- Practice building, running, and debugging Java applications with Maven.

## Author

**Bunnaka**  
Software Engineering / GIC  
Institute of Technology of Cambodia (ITC)