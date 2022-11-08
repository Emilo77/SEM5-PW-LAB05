package przyklady05;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Squares {

    private static final int MAX_NAP = 10;
    private static final Random RANDOM = new Random();

    private static final int COUNTED = 10;
    private static final int WORKERS = 4;

    private static void getSomeSleep() throws InterruptedException {
        int nap = RANDOM.nextInt(MAX_NAP);
        Thread.sleep(nap);
    }

    private static class Counting implements Callable<Integer> {

        private final int value;

        private Counting(int value) {
            this.value = value;
        }

        @Override
        public Integer call() throws InterruptedException {
            getSomeSleep();
            return value * value;
        }

    }

    private static class Writing implements Runnable {

        private final Future<Integer> promise;

        public Writing(Future<Integer> promise) {
            this.promise = promise;
        }

        @Override
        public void run() {
            try {
                getSomeSleep();
                System.out.println(promise.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Writing interrupted");
            } catch (ExecutionException e) {
                Thread.currentThread().interrupt();
                System.err.println("Counting interrupted");
            }
        }

    }

    public static void main(String[] args) {
        ExecutorService CountingPool = Executors.newFixedThreadPool(WORKERS);
        ExecutorService WritingPool = Executors.newFixedThreadPool(WORKERS);
        try {
            for (int i = 1; i <= COUNTED; ++i) {
                Callable<Integer> work = new Counting(i);
                Future<Integer> futureResult = CountingPool.submit(work);
                WritingPool.submit(new Writing(futureResult));
            }
        } finally {
            CountingPool.shutdown();
            WritingPool.shutdown();
        }
    }

}
