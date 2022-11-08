#Laboratoria 5

## Obliczenia

Tematem zajęć jest modelowanie obliczeń współbieżnych w Javie, abstrahujące od podziału pracy na wątki.

Do scenariusza dołączony jest zestaw programów przykładowych.

## Problem z dotychczasowym podejściem

Do tej pory w celu modelowania obliczeń współbieżnych posługiwaliśmy się wątkami. Wątki pozwalają na opisywanie sekwencji obliczeń, które mogą wykonywać się niezależnie od siebie. Korzystając z wątków, trzeba pamiętać o ich właściwej synchronizacji. Tworzenie wątków jest dość kosztowne, choć mniej kosztowne niż tworzenie nowych procesów.

Często wygodniejszym i wydajniejszym rozwiązaniem od ręcznego zarządzania wątkami przez programistę jest utworzenie pewnej puli wątków, które dzielą się pracą. Jest tak przede wszystkim wtedy, gdy mamy do wykonania wiele małych, niezależnych od siebie obliczeń, które nie wymagają skomplikowanej synchronizacji.

Narzut wynikający z tworzenia dla każdego obliczenia nowego wątku może zniwelować korzyści wynikające z równoległego wykonywania się obliczeń. Poza tym w danym momencie wykonywać może się równolegle tylko tyle wątków, ile procesor ma rdzeni (uruchomionych wątków może być dużo więcej). Dlatego wydajniejszy będzie kod, w którym wątki z puli wykonują po kolei zlecane obliczenia, niż kod, w którym na każde obliczenia przypada krótko żyjący wątek, utworzony jedynie na potrzeby wykonania danego obliczenia.

## Pula wątków

Pakiet [java.util.concurrent](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/package-summary.html) oferuje narzędzia do zarządzania pulami wątków, umożliwiające oddzielenie wątku od wykonywanej pracy.

Akcję, czyli pracę, która ma tylko efekty uboczne, reprezentuje obiekt klasy implementującej interfejs [Runnable ](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runnable.html) z metodą `run()`.

Obliczenie, czyli praca z wynikiem typu `V`, jest reprezentowana przez obiekt klasy implementującej interfejs [Callable<V>](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Callable.html) z metodą `call()`, dającą wynik typu `V`. Metoda ta ma deklarację `throws Exception`.

Statyczna metoda `newFixedThreadPool(int nThreads)` klasy [Executors](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Executors.html) daje obiekt klasy implementującej interfejs [ExecutorService](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ExecutorService.html). Obiekt ten jest wykonywaczem (*executor*), zarządzającym pulą `nThreads` wątków.

Wykonywacz ma metodę `submit(task)`, która zleca wykonanie pracy `task`. Typem `task` jest `Runnable` w przypadku akcji, lub `Callable<V>`, dla obliczenia wartości typu `V`.

## Przekazywanie wyników

Jeśli chcemy poczekać, aż wątek skończy pracę, możemy na wątku wywołać metodę `join()`. Co zrobić, jeśli pracę zleciliśmy jakiemuś wątkowi z puli (nie wiemy któremu) i chcemy poczekać, aż ta praca się skończy? Co, jeśli praca zwraca jakiś wynik? W jaki sposób "wyciągnąć" z wątku ten wynik?

Pula wątków po przyjęciu zgłoszenia przekazuje w wyniku obiekt, który będzie stanowił obietnicę, że **kiedyś w przyszłości** w tym miejscu zostanie zwrócona obliczona wartość. Obiekt taki (w zależności od języka programowania) nazywany jest `Promise` lub `Future`. W Javie będzie to obiekt implementujący interfejs [Future](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Future.html). Interfejs ten pozwala zaczekać na zakończenie obliczenia i w zasadzie nic więcej (można jeszcze obliczenie anulować i sprawdzić jego stan).

Wynik metody `submit()`, typu `Future<V>`, nazywany przyszłością, reprezentuje wartość typu V, która w przyszłości zostanie obliczona.

Przyszłość ma metodę `get()`, która czeka na wykonanie obiecanej pracy i daje wynik typu v.

Metoda `get()` zgłasza wyjątek `InterruptedException` w przypadku, gdy oczekujący wątek został przerwany. Jeśli przerwana została praca, na wykonanie której wątek czekał, zgłaszany jest wyjątek `ExecutionException`.

Pracę wykonywacza kończy metoda `shutdown()`.

Program `Squares.java` demonstruje zastosowanie wykonywacza.

## Wyniki zbiorcze

Gdy chcemy zlecić na raz wykonanie wielu obliczeń i zaczekać na zakończenie wszystkich, korzystamy z metody wykonywacza `invokeAll(tasks)`.

Argument `tasks` tej metody jest kolekcją obliczeń typu `Callable<T>` a wynik jest typu `List<Future<T>>`.

Program `Pyramid.java` demonstruje zastosowanie metody `invokeAll()`.

## Model fork/join

Obliczenia, w których większa praca jest rekurencyjnie dzielona na części, można opisać za pomocą uproszczonej wersji modelu wykonywaczy, nazywanej modelem fork/join.

Praca w tym modelu jest wartością typu [ForkJoinTask](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ForkJoinTask.html)`<T>`.

Praca będąca akcją jest reprezentowana przez obiekt podklasy klasy RecursiveAction. W klasie tej definiujemy metodę compute(), wykonującą akcję.

Praca będąca obliczeniem wartości typu V jest reprezentowana przez obiekt podklasy klasy RecursiveTask<V>. Metoda compute() tej klasy ma dać wynik typu V.

Podczas obliczenia, za pomocą metody fork() możemy uruchomić nowe obliczenie, wykonujące się współbieżnie z obliczeniem aktualnym. Jako wynik dostajemy wartość typu ForkJoinTask. Metodą join(), z wynikiem typu T, czekamy na zakończenie obliczenia.

Wykonywacz obliczenia fork/join jest obiektem klasy ForkJoinPool. Ma metodę invoke(task) z argumentem task typu ForkJoinTask<T>. Uruchamia ona obliczenie task i czeka na jego zakończenie. Daje wynik typu T.

Program Ones.java demonstruje współbieżne rekurencjne wykonanie akcji.

Program Fibonacci.java demonstruje współbieżne rekurencyjne obliczenie.

Ćwiczenie punktowane (MatrixRowSumsExecutors)

W rozwiązaniach zadań z poprzednich laboratoriów mieliśmy po jednym wątku liczącym elementy macierzy dla każdej jej kolumny.

Dla macierzy o dużej liczbie kolumn, koszt tworzenia i zarządzania wątkami w takim programie byłby znaczny.
Polecenie

Napisz nową wersję programu, w której elementy macierzy są liczone przez czteroelementową pulę wątków. Obliczenie każdego elementu macierzy powinno być osobnym zleceniem dla puli. Sumowanie może się odbywać w wątku głównym.
Materiał dodatkowy (nieobowiązkowy)

Porównanie

W pliku HomemadeFuture.java zaimplementowana została pula wątków. Korzysta ona z kolejek blokujących do rozdzielania pracy pomiędzy wątki i powiadamiania o zakończeniu obliczeń.

Porównanie wydajności programu wykonującego wiele razy prostą czynność w wersji z wątkami i z pulą wątków zostało zaimplementowane w pliku ThreadsVsFutures.java

Obliczenia zależne

Często jest tak, że chcemy wykonać pewną sekwencję obliczeń w sposób asynchroniczny. Najczęściej chodzi o sekwencje kroków korzystających z I/O, czyli pisania lub czytania do plików, łączenia się z bazą danych, wysyłania zapytań przez internet itd.

Wyobraźmy to sobie na przykładzie serwera portalu społecznościowego. Serwer taki musi przyjmować połączenia od wielu użytkowników jednocześnie. Po przyjęciu połączenia i sprawdzeniu czego użytkownik żąda (np. wyświetlenia istniejącego wpisu lub dodania nowego wpisu), serwer wysyła zapytanie do bazy danych. Jeśli wyświetlamy istniejący wpis, to wystarczy odczytać jego treść i odesłać użytkownikowi. Jeśli natomiast dodajemy nowy wpis, to może on, dajmy na to, zawierać obrazek, który trzeba gdzieś zapisać.

Przesyłanie plików przez sieć trwa znacznie dłużej niż przesyłanie krótkiego tekstu. Po zapisaniu tekstu i obrazka trzeba jeszcze wysłać użytkownikowi potwierdzenie.

W skrócie schemat działania wyglądałby tak:

    Przyjęcie zapytania od użytkownika
    Wysłanie zapytania do bazy danych
    (Jeśli jest obrazek) Zapisanie obrazka
    Odesłanie użytkownikowi potwierdzenia

Zauważmy, że każdy z tych punktów może trwać dość długo. Wątek ma tak naprawdę niewiele rzeczywistej "pracy" do wykonania - większość czasu spędza na czekaniu, aż coś się wyśle/odbierze/zapisze. Gdyby do obsługi każdego zapytania wykorzystywany był jeden wątek, to wątek ten blokowałby się/usypiał do czasu wykonania każdego z punktów. Ponieważ przełączanie aktualnie wykonującego się wątku nie jest darmowe, wydajniejsze będzie rozdzielenie pracy pomiędzy wątki z puli w taki sposób, aby jednostkę pracy stanowił jeden punkt z powyższego schematu. Problemem jest jednak to, że każdy punkt zależy od wyniku poprzedniego. Trudno taką sytuację zamodelować przy użyciu jedynie interfejsu Future.
CompletableFuture

Rozwiązaniem powyższego problemu jest wprowadzony w Javie 8 interfejs CompletableFuture. Pozwala on wygodnie (korzystając z paradygmatu programowania funkcyjnego) tworzyć obliczenia asynchroniczne, które mogą korzystać z wyników wcześniejszych obliczeń. Aby wytworzyć CompletableFuture możemy użyć metody supplyAsync, która zleca uruchomienie przekazanego w argumencie obliczenia synchronicznego w wątku ze wspólnej puli zarządzanej przez maszynę wirtualną Javy.

Na utworzonym w ten sposób obiekcie możemy zarejestrować tzw. callback, czyli funkcję, która wykona się po zakończeniu danego obliczenia. Służy do tego metoda thenApplyAsync (w językach funkcyjnych często nazywana map). Bierze ona jako argument funkcję z wyniku obecnego obliczenia (które jest, dajmy na to, typu T) w coś typu, powiedzmy, U. thenApplyAsync zwraca w takim przypadku obiekt typu CompletableFuture<U>.

Co jeśli jednak chcemy zlecić kolejne obliczenie asynchroniczne (tzn. zwracające CompletableFuture<T>)? Wówczas metoda thenApplyAsync zwróciłaby obiekt typu CompletableFuture<CompletableFuture<U>>;. Aby uniknąć takiego zagnieżdżenia i "spłaszczyć" typ zwracanego obiektu należy wykorzystać metodę thenComposeAsync (w językach funkcyjnych: flatMap lub bind).

Często jest tak, że chcemy zaczekać na zakończenie kilku obliczeń, zanim przejdziemy dalej. W Javie interfejs CompletableFuture ma metody allOf(CompletableFuture... cfs) oraz anyOf(CompletableFuture... cfs), które pozwalają zaczekać na – odpowiednio – wszystkie obliczenia przekazane jako argumenty lub tylko to z nich, które zakończy się jako pierwsze. W praktyce ze względu na tzw. type erasure w Javie dość niewygodnie się z nich korzysta, dlatego w klasie Utils zdefiniowaliśmy dla Państwa ich wygodniejsze odpowiedniki.

W pliku RabbitAndTurtle.java zademonstrowane zostało, jak korzystać z metody awaitAny.

Plik BigFile.java pokazuje natomiast zastosowanie CompletableFuture do asynchronicznych operacji I/O.
Materiały dodatkowe

Polecamy Państwa uwadze następujące artykuły:

    wprowadzenie do programowania asynchronicznego (w Javascripcie, ale język nie ma tu wielkiego znaczenia) – Mozilla Developer Network, Asynchronous JavaScript

    Dokumentacja dot. programowania asynchronicznego w różnych językach
        Scala
        Rust
        C#
        Swift

Ćwiczenie dodatkowe (BankDatabase)

W pliku BankDatabase.java mamy klasę symulującą łączenie się z bazą danych. Baza danych zawiera informacje o stanach kont pewnych klientów. Łączenie się z bazą danych jest przykładem czynności ograniczonej przez I/O, co jest zasymulowane w kodzie przez sleep. Zdefiniowana jest metoda robinHood, która:

    Pobiera listę klientów banku
    Dla każdego klienta pobiera stan jego konta
    Znajduje klienta o najwyższym stanie konta
    Rozdziela majątek tego tego klienta pomiędzy pozostałych klientów
    Zamyka jego konto

Polecenie

Napisz asynchroniczną wersję metody robinHood, korzystającą z CompletableFuture. Nie możesz zmieniać istniejącego kodu. Możesz korzystać z funkcji zdefiniowanych w pliku Utils.java. 
