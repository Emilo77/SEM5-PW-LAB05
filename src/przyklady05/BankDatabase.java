package przyklady05;

import static przyklady05.Utils.logWithThreadName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import przyklady05.Utils.Stopwatch;

public class BankDatabase {

    private static class Database {

        private final ConcurrentHashMap<String, AtomicInteger> accounts;

        public Database() {
            accounts = new ConcurrentHashMap<>();
            accounts.putAll(Map.of("Jan Kowalski", new AtomicInteger(100),
                "Donald Trump", new AtomicInteger(314),
                "Britney Spears", new AtomicInteger(1234),
                "Clark Kent", new AtomicInteger(55),
                "Franz Kafka", new AtomicInteger(8789),
                "Queen Elizabeth II", new AtomicInteger(900000),
                "Adam Mickiewicz", new AtomicInteger(23987),
                "Mariusz Pudzianowski", new AtomicInteger(33332),
                "Magda Gessler", new AtomicInteger(1200),
                "Virginia Woolf", new AtomicInteger(400)
            ));
        }

        private void simulateDatabaseConnection(int secondsOfDelay) {
            try {
                TimeUnit.SECONDS.sleep(secondsOfDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public int getAccountBalance(String holder) {
            simulateDatabaseConnection(1);
            int balance = accounts.get(holder).get();
            logWithThreadName(holder + " has " + balance + "$");
            return balance;
        }

        public CompletableFuture<Integer> getAccountBalanceAsync(String holder) {
            return CompletableFuture.supplyAsync(() -> getAccountBalance(holder));
        }

        public List<String> getAccountHolders() {
            simulateDatabaseConnection(1);
            logWithThreadName("Fetched the list of account holders");
            return new ArrayList<>(accounts.keySet());
        }

        public CompletableFuture<List<String>> getAccountHoldersAsync() {
            return CompletableFuture.supplyAsync(this::getAccountHolders);
        }

        public void closeAccount(String holder) {
            simulateDatabaseConnection(3);
            logWithThreadName("Closed the account for " + holder);
            accounts.remove(holder);
        }

        public CompletableFuture<Void> closeAccountAsync(String holder) {
            return CompletableFuture.runAsync(() -> closeAccount(holder));
        }

        public void addMoney(String holder, int dollars) {
            simulateDatabaseConnection(1);
            accounts.get(holder).addAndGet(dollars);
            logWithThreadName(holder + " received " + dollars + "$");
        }

        public CompletableFuture<Void> addMoneyAsync(String holder, int dollars) {
            return CompletableFuture.runAsync(() -> addMoney(holder, dollars));
        }

        @Override
        public String toString() {
            return accounts.entrySet().stream()
                .map(entry -> entry.getKey() + " -> " + entry.getValue() + ",\n")
                .collect(Collectors.joining());
        }

        // Needed because AtomicIntegers can't be compared directly
        private Set<SimpleEntry<String, Integer>> getAccounts() {
            return accounts.entrySet().stream()
                .map(entry -> new SimpleEntry<>(entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toSet());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Database database = (Database) o;
            return getAccounts().equals(database.getAccounts());
        }

        @Override
        public int hashCode() {
            return Objects.hash(accounts);
        }
    }

    /*
      Some utility functions which might prove useful
     */

    private static Optional<Entry<String, Integer>> findTheRichestHolder(
        Map<String, Integer> accounts) {
        return accounts.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue));
    }

    /*
     Synchronous version written in an imperative style
     */

    public static void robinHood(Database db) {
        List<String> holders = db.getAccountHolders();
        Map<String, Integer> accounts = new HashMap<>();
        for (String holder : holders) {
            int balance = db.getAccountBalance(holder);
            accounts.put(holder, balance);
        }

        String richestHolder = null;
        int greatestAccountBalance = 0;
        for (String holder : holders) {
            int balance = accounts.get(holder);
            if (balance > greatestAccountBalance) {
                greatestAccountBalance = balance;
                richestHolder = holder;
            }
        }

        if (richestHolder != null && holders.size() > 1) {
            int moneyForEach = greatestAccountBalance / (holders.size() - 1);
            for (String holder : holders) {
                if (!holder.equals(richestHolder)) {
                    db.addMoney(holder, moneyForEach);
                }
            }
            db.closeAccount(richestHolder);
        }
    }

    /*
     Synchronous version written in a functional style.
     It has the same properties as the imperative version above.
     */

    public static void robinHoodFunctional(Database db) {
        List<String> holders = db.getAccountHolders();
        Map<String, Integer> accounts = holders
            .stream()
            .map(holder -> new AbstractMap.SimpleEntry<>(holder, db.getAccountBalance(holder)))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

        Optional<Entry<String, Integer>> maybeRichestHolder = findTheRichestHolder(accounts);

        if (maybeRichestHolder.isPresent() && holders.size() > 1) {
            String richestHolder = maybeRichestHolder.get().getKey();
            int money = maybeRichestHolder.get().getValue();
            int moneyForEach = money / (holders.size() - 1);

            holders.stream()
                   .filter(holder -> !holder.equals(richestHolder))
                   .forEach((holder) -> db.addMoney(holder, moneyForEach));

            db.closeAccount(richestHolder);
        }
    }

    public static CompletableFuture<Void> robinHoodAsync(Database db) {
        // Write your solution below
        // Requirements:
        // - fetching account balances should happen concurrently
        // - adding money should happen concurrently
        // - deleting the account should happen concurrently with adding money
        // - do not modify any other code!
        // - do not use get() or join() here!
        return CompletableFuture.completedFuture(null);
    }

    public static void main(String[] args) {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "32");
        Stopwatch stopwatch = new Stopwatch();

        System.out.println("Sync version:");
        Database syncDb = new Database();
        stopwatch.start();
        robinHood(syncDb);
        Duration syncDuration = stopwatch.stop();
        System.out.println(syncDb);
        System.out.println("It took " + syncDuration);

        System.out.println("\nAsync version:");
        Database asyncDb = new Database();
        stopwatch.start();
        robinHoodAsync(asyncDb).join();
        Duration asyncDuration = stopwatch.stop();
        System.out.println(asyncDb);
        System.out.println("It took " + asyncDuration);

        if (asyncDuration.compareTo(Duration.of(6, ChronoUnit.SECONDS)) > 0) {
            throw new AssertionError("The async version is too slow!");
        }
        if (!syncDb.equals(asyncDb)) {
            throw new AssertionError("The async version does not produce identical results!");
        }

        System.out.println("OK");
    }
}
