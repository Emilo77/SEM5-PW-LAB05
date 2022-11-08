package przyklady05;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class Ones {

    private static final int SIZE = 1000000;
    private static final int LIMIT = 10;

    private static class Action extends RecursiveAction {

        private static final long serialVersionUID = 1L;

        private final int[] array;
        private final int first;
        private final int afterLast;
        private final int limit;

        public Action(int[] array, int first, int afterLast, int limit) {
            this.array = array;
            this.first = first;
            this.afterLast = afterLast;
            this.limit = limit;
        }

        @Override
        protected void compute() {
            if (afterLast - first < limit) {
                for (int i = first; i < afterLast; ++i) {
                    array[i] = 1;
                }
            } else {
                int middle = (afterLast + first) / 2;
                Action right = new Action(array, middle, afterLast, limit);
                right.fork();
                Action left = new Action(array, first, middle, limit);
                left.compute();
                right.join();
            }
        }

    }

    public static void main(String[] args) {
        int[] array = new int[SIZE];
        ForkJoinPool pool = new ForkJoinPool();
        try {
            Action ones = new Action(array, 0, SIZE, LIMIT);
            pool.invoke(ones);
            int sum = 0;
            for (int x : array) {
                sum += x;
            }
            System.out.println(sum);
        } finally {
            pool.shutdown();
        }
    }

}
