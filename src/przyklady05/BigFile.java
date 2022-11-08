package przyklady05;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import przyklady05.Utils;
import przyklady05.Utils.Stopwatch;

public class BigFile {

    private static final int FILE_LENGTH = 10000000;
    private static final int FILES_COUNT = 30;

    // It returns an int, not a char, because BufferedWriter's write method expects an int
    private static int randomLetter() {
        return ThreadLocalRandom.current().nextInt(97, 97 + 25);
    }

    public static void writeSync(Path path) {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            for (int i = 0; i < FILE_LENGTH; i++) {
                if (i % 50 == 0) {
                    writer.write('\n');
                } else {
                    writer.write(randomLetter());
                }
            }
            przyklady05.Utils.logWithThreadName(path + " created!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static CompletableFuture<Path> writeAsync(Path path) {
        return CompletableFuture.runAsync(() -> writeSync(path))
            .thenApplyAsync(ignored -> path);
    }

    public static int countLetterOccurrences(Path path, char letter) {
        int counter = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            for (int i = 0; i < FILE_LENGTH; i++) {
                if (reader.read() == letter) {
                    counter++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        przyklady05.Utils.logWithThreadName(
            "Found " + counter + " occurences of letter " + letter + " in file " + path);
        return counter;
    }

    public static int countLetterOccurrencesSync(List<Path> paths, char letter) {
        return paths.stream()
            .map(path -> countLetterOccurrences(path, letter))
            .reduce(0, Integer::sum);

        // Same as:
        // int counter = 0;
        // for (Path path : paths) {
        //    counter += countLetterOccurrences(path, letter);
        // }
        // return counter;
    }

    public static CompletableFuture<Integer> countLetterOccurrencesAsync(List<Path> paths,
        char letter) {
        List<CompletableFuture<Integer>> futures = paths.stream()
            .map(path -> CompletableFuture.supplyAsync(() -> countLetterOccurrences(path, letter)))
            .collect(Collectors.toList());

        return Utils.awaitAll(futures)
            .thenApply(partialResults -> partialResults.stream().reduce(0, Integer::sum));
    }

    private static List<Path> createFilesSync(int count) {
        ArrayList<Path> paths = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Path path = Path.of(i + ".txt");
            writeSync(path);
            paths.add(path);

        }
        Utils.logWithThreadName("Files created!");
        return paths;
    }

    private static CompletableFuture<List<Path>> createFilesAsync(int count) {
        var futures = IntStream.rangeClosed(1, count)
            .mapToObj(i -> Path.of(i + ".txt"))
            .map(BigFile::writeAsync)
            .collect(Collectors.toList());

        return Utils.awaitAll(futures)
            .thenApplyAsync(list -> {
                Utils.logWithThreadName("Files created!");
                return list;
            });
    }

    public static void main(String[] args) {
        Stopwatch stopwatch = new Stopwatch();
        char letter = 'x';

        System.out.println("Test correctness");
        createFilesAsync(3)
            .thenCompose(files -> {
                CompletableFuture<Integer> async = countLetterOccurrencesAsync(files, letter);
                CompletableFuture<Integer> sync = CompletableFuture.supplyAsync(
                    () -> countLetterOccurrencesSync(files, letter));
                return Utils.awaitAll(async, sync).thenRun(() -> {
                    // We can use join here because the futures have already been awaited
                    boolean equal = async.join().intValue() == sync.join().intValue();
                    Utils.logWithThreadName("Async and sync counts are equal: " + equal);
                });
            }).join(); // We have to wait for the future to finish before we move on.
        // Otherwise the files might get overwritten by the next createFiles call.

        System.out.println("Sync version:");
        stopwatch.runWithStopwatch(() -> {
            List<Path> files = createFilesSync(FILES_COUNT);
            int count = countLetterOccurrencesSync(files, letter);
            System.out.println("Found " + count + " occurences of letter " + letter);
        });

        System.out.println("Async version:");
        stopwatch.runWithStopwatch(() -> {
            int count = createFilesAsync(FILES_COUNT)
                .thenCompose(files -> countLetterOccurrencesAsync(files, letter))
                .join();
            System.out.println("Found " + count + " occurences of letter " + letter);
        });
    }

}
