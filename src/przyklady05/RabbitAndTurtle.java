package przyklady05;

import static przyklady05.Utils.logWithThreadName;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class RabbitAndTurtle {

    private static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            // we ignore this exception on purpose
        }
    }

    private static CompletableFuture<String> run(String name, int seconds) {
        return CompletableFuture.supplyAsync(() -> {
            logWithThreadName(name + " started running");
            sleep(seconds);
            logWithThreadName(name + " finished running");
            return name;
        });
    }

    private static void announceWinner(String winner) {
        logWithThreadName("And the winner is..." + winner);
    }

    public static void main(String[] args) {
        CompletableFuture<String> rabbit = run("Rabbit", 1);
        CompletableFuture<String> turtle = run("Turtle", 10);
        CompletableFuture<Void> future = przyklady05.Utils
            .awaitAny(rabbit, turtle)
            .thenAccept(RabbitAndTurtle::announceWinner);

        // future.join(); // Uncomment this line to see the result
    }


}
