import memory.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class Memory_tests {

    private static final double DELTA = 1e-4;

    // ==========================================
    //              VECTOR TESTS
    // ==========================================

    @Test
    void testVectorInit() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);

        assertEquals(3, v.length(), "Vector length incorrect");
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation(), "Orientation incorrect");
        assertEquals(1.0, v.get(0), DELTA);
        assertEquals(3.0, v.get(2), DELTA);
    }

    @Test
    void testVectorTranspose() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());

        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    void testVectorAdd() {
        SharedVector v1 = new SharedVector(new double[]{10, 20}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        v1.add(v2);

        assertEquals(11.0, v1.get(0), DELTA);
        assertEquals(22.0, v1.get(1), DELTA);

        // Ensure source vector is not modified
        assertEquals(1.0, v2.get(0), DELTA);
        assertEquals(2.0, v2.get(1), DELTA);
    }

    @Test
    void testVectorNegate() {
        SharedVector v = new SharedVector(new double[]{1, -2}, VectorOrientation.ROW_MAJOR);
        v.negate();

        assertEquals(-1.0, v.get(0), DELTA);
        assertEquals(2.0, v.get(1), DELTA);
    }

    @Test
    void testVectorDotProduct() {
        SharedVector row = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{3, 4}, VectorOrientation.COLUMN_MAJOR);

        double result = row.dot(col);
        assertEquals(11.0, result, DELTA);
    }

    @Test
    void testVectorErrors() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector v3 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> v1.add(v2),
                "Expected exception on length mismatch");

        assertThrows(IllegalArgumentException.class, () -> v1.dot(v3),
                "Expected exception on invalid dot product orientation");
    }

    // ==========================================
    //              MATRIX TESTS
    // ==========================================

    @Test
    void testMatrixInitAndRead() {
        double[][] data = {{1, 2}, {3, 4}};
        SharedMatrix m = new SharedMatrix(data);

        double[][] read = m.readRowMajor();

        assertEquals(1.0, read[0][0], DELTA);
        assertEquals(4.0, read[1][1], DELTA);
    }

    @Test
    void testMatrixLoadColumnMajor() {
        double[][] data = {{1, 2}, {3, 4}};
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(data);

        double[][] read = m.readRowMajor();

        assertEquals(1.0, read[0][0], DELTA);
        assertEquals(2.0, read[0][1], DELTA);
        assertEquals(3.0, read[1][0], DELTA);
        assertEquals(4.0, read[1][1], DELTA);
    }

    @Test
    void testVectorMatrixMultiplication() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        double[][] matData = {{3, 4}, {5, 6}};
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(matData);

        v.vecMatMul(m);

        assertEquals(13.0, v.get(0), DELTA);
        assertEquals(16.0, v.get(1), DELTA);
    }

    @Test
    void testVecMatMulDoesNotModifyMatrix() {
        double[][] matData = {{1, 1}, {1, 1}};
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(matData);

        double[][] before = m.readRowMajor();

        SharedVector v = new SharedVector(new double[]{2, 2}, VectorOrientation.ROW_MAJOR);
        v.vecMatMul(m);

        double[][] after = m.readRowMajor();

        assertArrayEquals(before[0], after[0], DELTA);
        assertArrayEquals(before[1], after[1], DELTA);
    }

    // ==========================================
    //          CONCURRENCY TESTS
    // ==========================================

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testConcurrentVectorAdd() throws InterruptedException {
        int threadCount = 1000;

        SharedVector target = new SharedVector(new double[]{0}, VectorOrientation.ROW_MAJOR);
        SharedVector adder = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    target.add(adder);
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Threads did not finish");
        executor.shutdown();

        assertEquals(0, failures.get(), "Exceptions occurred during concurrent add");
        assertEquals(1000.0, target.get(0), DELTA,
                "Incorrect result – possible race condition");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testConcurrentVecMatMul() throws InterruptedException {
        int threadCount = 20;

        double[][] matData = {{1, 1}, {1, 1}};
        SharedMatrix sharedMatrix = new SharedMatrix();
        sharedMatrix.loadColumnMajor(matData);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    SharedVector v = new SharedVector(new double[]{2, 2}, VectorOrientation.ROW_MAJOR);
                    v.vecMatMul(sharedMatrix);

                    if (Math.abs(v.get(0) - 4.0) > DELTA ||
                            Math.abs(v.get(1) - 4.0) > DELTA) {
                        failures.incrementAndGet();
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Threads did not finish");
        executor.shutdown();

        assertEquals(0, failures.get(),
                "Concurrent vector-matrix multiplication failed");
    }
}
