package przyklady05;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class Pyramid {

    private static final int SUMMEDUP = 100;
    private static final int WORKERS = 8;

    private static class Square implements Callable<Integer> {

        private final int value;

        private Square(int value) {
            this.value = value;
        }

        @Override
        public Integer call() {
            return value * value;
        }

    }

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(WORKERS);
        List<Callable<Integer>> calculations = new ArrayList<>();
        for (int i = 1; i <= SUMMEDUP; ++i) {
            Callable<Integer> work = new Square(i);
            calculations.add(work);
        }
        try {
            List<Future<Integer>> promises = pool.invokeAll(calculations);
            int sum = 0;
            for (Future<Integer> next : promises) {
                sum += next.get();
            }
            System.out.println(sum + " " + SUMMEDUP * (SUMMEDUP + 1) * (2 * SUMMEDUP + 1) / 6);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            System.err.println("Calculations interrupted");
        } finally {
            pool.shutdown();
        }
    }

}
