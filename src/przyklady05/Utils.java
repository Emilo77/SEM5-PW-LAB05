package przyklady05;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Various small utils.
 *
 * awaitAll/awaitAny provide nicer versions of CompletableFuture.allOf/anyOf for handling multiple
 * futures of the same type (working around type erasure in Java arrays of generics).
 */
public class Utils {

    // Turns a list of futures into a future of a list.
    public static <T> CompletableFuture<List<T>> awaitAll(List<CompletableFuture<T>> futures) {
        return CompletableFuture
            .allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(ignored ->
                futures.stream()
                       .map(CompletableFuture::join)
                       .collect(Collectors.toList())
            );
    }

    // Turns any number of given futures (of the same type) into a future of a list.
    @SafeVarargs
    public static <T> CompletableFuture<List<T>> awaitAll(CompletableFuture<T>... futures) {
        return CompletableFuture
            .allOf(futures)
            .thenApply(ignored ->
                Arrays.stream(futures)
                      .map(CompletableFuture::join)
                      .collect(Collectors.toList()));
    }

    // Turn a list of futures into a future with the first completed result.
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> awaitAny(List<CompletableFuture<T>> futures) {
        return CompletableFuture
            .anyOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(o -> (T) o);
    }

    // Turn any number of given futures (of the same type) into a future with the first completed result.
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> awaitAny(CompletableFuture<T>... futures) {
        return CompletableFuture
            .anyOf(futures)
            .thenApply(o -> (T) o);
    }

    public static <K, V> Map<K, V> createMapFromEntries(List<? extends Entry<K, V>> entries) {
        return entries.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public static void logWithThreadName(String message) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + message);
    }

    public static class Stopwatch {

        private Instant start;

        public void start() {
            start = Instant.now();
        }

        public Duration stop() {
            Duration duration = Duration.between(start, Instant.now());
            start = null;
            return duration;
        }

        public void runWithStopwatch(Runnable runnable) {
            start();
            runnable.run();
            Duration duration = stop();
            System.out.println("It took " + duration);
        }
    }

}
