import memory.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Memory_tests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== Starting SharedMemory Tests ===\n");

        run("Vector init/get", Memory_tests::testVectorInit);
        run("Vector transpose", Memory_tests::testVectorTranspose);
        run("Vector add", Memory_tests::testVectorAdd);
        run("Vector negate", Memory_tests::testVectorNegate);
        run("Vector dot", Memory_tests::testVectorDot);
        run("Vector errors", Memory_tests::testVectorErrors);

        run("Matrix row-major read", Memory_tests::testMatrixRowMajor);
        run("Matrix column-major read", Memory_tests::testMatrixColumnMajor);

        run("Vector × Matrix", Memory_tests::testVecMatMul);

        run("Concurrency: vector add", Memory_tests::testConcurrentVectorAdd);
        run("Concurrency: vecMatMul", Memory_tests::testConcurrentVecMatMul);

        System.out.println("\n=== Summary ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);

        if (failed > 0) {
            System.exit(1);
        }
    }

    // -------------------------------------------------

    private static void run(String name, Runnable test) {
        try {
            test.run();
            System.out.println("✔ " + name);
            passed++;
        } catch (Throwable t) {
            System.out.println("✘ " + name + " -> " + t.getMessage());
            failed++;
        }
    }

    private static void assertEq(double exp, double act) {
        if (Math.abs(exp - act) > 1e-6)
            throw new RuntimeException("expected " + exp + ", got " + act);
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new RuntimeException(msg);
    }

    private static void assertThrows(Runnable r) {
        try {
            r.run();
            throw new RuntimeException("expected exception");
        } catch (Exception ignored) {}
    }

    // -------------------------------------------------
    // SharedVector tests
    // -------------------------------------------------

    private static void testVectorInit() {
        SharedVector v = new SharedVector(new double[]{1,2,3}, VectorOrientation.ROW_MAJOR);
        assertEq(1, v.get(0));
        assertEq(3, v.get(2));
        assertTrue(v.length() == 3, "length");
    }

    private static void testVectorTranspose() {
        SharedVector v = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        v.transpose();
        assertTrue(v.getOrientation() == VectorOrientation.COLUMN_MAJOR, "transpose");
    }

    private static void testVectorAdd() {
        SharedVector a = new SharedVector(new double[]{1,2}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{3,4}, VectorOrientation.ROW_MAJOR);
        a.add(b);
        assertEq(4, a.get(0));
        assertEq(6, a.get(1));
    }

    private static void testVectorNegate() {
        SharedVector v = new SharedVector(new double[]{1,-2}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEq(-1, v.get(0));
        assertEq(2, v.get(1));
    }

    private static void testVectorDot() {
        SharedVector r = new SharedVector(new double[]{1,2}, VectorOrientation.ROW_MAJOR);
        SharedVector c = new SharedVector(new double[]{3,4}, VectorOrientation.COLUMN_MAJOR);
        assertEq(11, r.dot(c));
    }

    private static void testVectorErrors() {
        SharedVector a = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{1,2}, VectorOrientation.ROW_MAJOR);
        assertThrows(() -> a.add(b));
    }

    // -------------------------------------------------
    // SharedMatrix tests
    // -------------------------------------------------

    private static void testMatrixRowMajor() {
        double[][] m = {{1,2},{3,4}};
        SharedMatrix sm = new SharedMatrix(m);
        double[][] r = sm.readRowMajor();
        assertEq(1, r[0][0]);
        assertEq(4, r[1][1]);
    }

    private static void testMatrixColumnMajor() {
        double[][] m = {{1,2},{3,4}};
        SharedMatrix sm = new SharedMatrix();
        sm.loadColumnMajor(m);

        double[][] r = sm.readRowMajor();
        // חייב להיות זהה למטריצה המקורית
        assertEq(1, r[0][0]);
        assertEq(2, r[0][1]);
        assertEq(3, r[1][0]);
        assertEq(4, r[1][1]);
    }

    // -------------------------------------------------
    // vecMatMul
    // -------------------------------------------------

    private static void testVecMatMul() {
        SharedVector v = new SharedVector(new double[]{1,2}, VectorOrientation.ROW_MAJOR);

        double[][] m = {{3,5},{4,6}};
        SharedMatrix sm = new SharedMatrix();
        sm.loadColumnMajor(m);

        v.vecMatMul(sm);

        assertEq(11, v.get(0)); // 1*3 + 2*4
        assertEq(17, v.get(1)); // 1*5 + 2*6
    }

    // -------------------------------------------------
    // Concurrency
    // -------------------------------------------------

    private static void testConcurrentVectorAdd() {
        SharedVector base = new SharedVector(new double[]{0}, VectorOrientation.ROW_MAJOR);
        SharedVector one  = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);

        int N = 500;
        ExecutorService ex = Executors.newFixedThreadPool(8);
        CountDownLatch l = new CountDownLatch(N);

        for (int i = 0; i < N; i++) {
            ex.execute(() -> {
                base.add(one);
                l.countDown();
            });
        }

        try { l.await(); } catch (InterruptedException ignored) {}
        ex.shutdown();

        assertEq(N, base.get(0));
    }

    private static void testConcurrentVecMatMul() {
        double[][] m = {{1,1},{1,1}};
        SharedMatrix sm = new SharedMatrix();
        sm.loadColumnMajor(m);

        AtomicInteger errors = new AtomicInteger();
        ExecutorService ex = Executors.newFixedThreadPool(8);

        for (int i = 0; i < 20; i++) {
            ex.execute(() -> {
                SharedVector v = new SharedVector(new double[]{2,2}, VectorOrientation.ROW_MAJOR);
                v.vecMatMul(sm);
                if (v.get(0) != 4 || v.get(1) != 4)
                    errors.incrementAndGet();
            });
        }

        ex.shutdown();
        try { ex.awaitTermination(2, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

        assertTrue(errors.get() == 0, "concurrency error");
    }
}
