package przyklady05;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Fibonacci {

    private static final int COUNTED = 20;

    private static class Computation extends RecursiveTask<Integer> {

        private static final long serialVersionUID = 1L;

        private final int argument;

        public Computation(int argument) {
            this.argument = argument;
        }

        @Override
        protected Integer compute() {
            if (argument <= 1) {
                return argument;
            } else {
                Computation right = new Computation(argument - 2);
                right.fork();
                Computation left = new Computation(argument - 1);
                return left.compute() + right.join();
            }
        }

    }

    public static void main(String[] args) {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            for (int i = 0; i < COUNTED; ++i) {
                RecursiveTask<Integer> Computation = new Computation(i);
                int result = pool.invoke(Computation);
                System.out.println(i + " " + result);
            }
        } finally {
            pool.shutdown();
        }
    }

}
