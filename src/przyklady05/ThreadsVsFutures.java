package przyklady05;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import przyklady05.HomemadeFuture.Future;
import przyklady05.HomemadeFuture.ThreadPool;
import przyklady05.Utils.Stopwatch;

public class ThreadsVsFutures {

    private static final int WORK_SIZE = 100000;
    private static final int THREADS_COUNT = 10;

    public static void main(String[] args) {
        ThreadPool pool = new ThreadPool(THREADS_COUNT);
        List<Future<Object>> futures = new ArrayList<>();
        LongAdder counterForFutures = new LongAdder();

        List<Thread> threads = new ArrayList<>();
        LongAdder counterForThreads = new LongAdder();

        Stopwatch stopwatch = new Stopwatch();

        System.out.println("Threads:");
        stopwatch.runWithStopwatch(() -> {
            for (int i = 0; i < WORK_SIZE; i++) {
                Thread thread = new Thread(() -> {
                    counterForThreads.increment();
                });
                threads.add(thread);
                thread.start();
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        System.out.println("Futures:");
        stopwatch.runWithStopwatch(() -> {
            for (int i = 0; i < WORK_SIZE; i++) {
                Future<Object> future = pool.submit(() -> {
                    counterForFutures.increment();
                    return new Object();
                    // This is needed because our version of Future *must* return something.
                });
                futures.add(future);
            }

            for (Future<Object> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            pool.shutdown();
        });

        if (counterForFutures.sum() != counterForThreads.sum()) {
            throw new AssertionError("Sums are not equal");
        }

        System.out.println("OK");

    }

}
