
## Task 4: Thread Pools, Future and Virtual Threads

### CPU-bound work

The prime-counting task is divided into 16 chunks.
Each chunk is submitted to an ExecutorService, and
Future.get() retrieves its result. The parallel result
must equal the single-threaded result: 283146.

### I/O-bound work

The fake downloads simulate network waiting using
Thread.sleep(200). A fixed pool of 20 platform threads
limits concurrency. Virtual threads are lightweight,
so many waiting downloads can make progress without
requiring one platform thread per task.

### Failure handling

Every 50th download throws IOException("boom").
ExecutionException is caught when retrieving the result
using Future.get(). Failed tasks are counted rather than
crashing the program.

### Timeouts

Download 1 sleeps for five seconds. Its Future is retrieved
with get(1, TimeUnit.SECONDS). When TimeoutException
occurs, future.cancel(true) requests interruption.

### Shutdown

Try-with-resources closes the ExecutorService after
the work completes. No System.exit() is needed.
