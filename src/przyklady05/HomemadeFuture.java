package przyklady05;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class HomemadeFuture {

    public static class Future<T> {

        // The worker will notify us using this queue, once the computation is done.
        private final LinkedBlockingQueue<T> channel;
        private T result = null;

        public Future(LinkedBlockingQueue<T> channel) {
            this.channel = channel;
        }

        public T get() throws InterruptedException {
            if (result == null) {
                result = channel.take();
            }
            return result;
        }


    }

    // Stores the task to be done and the channel used to notify when it's done.
    private static class Handle<T> {

        private final Callable<T> computation;
        private final LinkedBlockingQueue<T> channel;

        public Handle(Callable<T> task, LinkedBlockingQueue<T> channel) {
            this.computation = task;
            this.channel = channel;
        }

        public Callable<T> getComputation() {
            return computation;
        }

        public LinkedBlockingQueue<T> getChannel() {
            return channel;
        }
    }

    public static class ThreadPool {

        private final List<Thread> threads; // list of worker threads
        // workers will take tasks from this queue
        private final BlockingQueue<Handle<Object>> handleQueue;

        public ThreadPool(int size) {
            this.handleQueue = new LinkedBlockingQueue<>();
            this.threads = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                threads.add(new Thread(new Worker(handleQueue)));
            }

            for (Thread thread : threads) {
                thread.start();
            }
        }

        // Submits a computation to be computed and returns a future value.
        public <T> Future<T> submit(Callable<T> computation) {
            LinkedBlockingQueue<T> notificationChannel = new LinkedBlockingQueue<>();

            Handle<T> handle = new Handle<>(computation, notificationChannel);
            Future<T> future = new Future<>(notificationChannel);
            try {
                handleQueue.put((Handle<Object>) handle);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return future;
        }

        public void shutdown() {
            for (Thread thread : threads) {
                thread.interrupt();
            }
        }
    }

    private static class Worker implements Runnable {

        private final BlockingQueue<Handle<Object>> handleQueue;

        public Worker(BlockingQueue<Handle<Object>> handleQueue) {
            this.handleQueue = handleQueue;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Handle<Object> handle = handleQueue.take();
                    Object result = handle.getComputation().call();
                    // The types of the channel and the computation
                    // will always match because when creating a Task<T> from
                    // a computation Callable<T>
                    // we create a LinkedBlockingQueue<T>.
                    handle.getChannel().put(result);
                }
            } catch (InterruptedException e) {
                // We simply finish.
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ThreadPool threadPool = new ThreadPool(16);
        long sum = 0;
        List<Future<Long>> futures = new ArrayList<>();
        Instant start = Instant.now();
        for (int i = 0; i < 100; i++) {
            Future<Long> future = threadPool.submit(() -> {
                Instant thisFutureStart = Instant.now();
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    System.err.println(Thread.currentThread().getName() + " interrupted");
                }
                Duration thisFutureDuration = Duration.between(thisFutureStart, Instant.now());
                return thisFutureDuration.toMillis();

            });
            futures.add(future);
        }

        for (Future<Long> future : futures) {
            try {
                sum += future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Duration realDuration = Duration.between(start, Instant.now());

        System.out.println("Total work time: " + sum + "ms");
        System.out.println(
            "But in the real world only " + realDuration.toMillis() + "ms have passed");

        threadPool.shutdown();
    }
}
