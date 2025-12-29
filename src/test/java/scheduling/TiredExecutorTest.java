package scheduling;

import memory.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TiredExecutorTest {

    private TiredExecutor executor;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    @Test
    void constructorRejectsInvalidThreadCount() {
        assertThrows(IllegalArgumentException.class, () -> new TiredExecutor(0));
        assertThrows(IllegalArgumentException.class, () -> new TiredExecutor(-1));
    }

    @Test
    void constructorCreatesExecutor() {
        executor = new TiredExecutor(3);
        assertNotNull(executor);
    }

    @Test
    void submitSingleTask() {
        executor = new TiredExecutor(2);
        AtomicInteger counter = new AtomicInteger(0);

        List<Runnable> tasks = new ArrayList<>();
        tasks.add(counter::incrementAndGet);

        executor.submitAll(tasks);
        assertEquals(1, counter.get());
    }

    @Test
    void submitMultipleTasks() {
        executor = new TiredExecutor(4);
        AtomicInteger counter = new AtomicInteger(0);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            tasks.add(counter::incrementAndGet);
        }

        executor.submitAll(tasks);
        assertEquals(50, counter.get());
    }

    @Test
    void submitNullThrowsException() {
        executor = new TiredExecutor(2);
        assertThrows(IllegalArgumentException.class, () -> executor.submit(null));
    }

    @Test
    void submitEmptyTaskList() {
        executor = new TiredExecutor(2);
        executor.submitAll(new ArrayList<>());
    }

    @Test
    void multipleSubmitAllCalls() {
        executor = new TiredExecutor(3);
        AtomicInteger counter = new AtomicInteger(0);

        for (int round = 0; round < 3; round++) {
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                tasks.add(counter::incrementAndGet);
            }
            executor.submitAll(tasks);
        }

        assertEquals(30, counter.get());
    }

    @Test
    void shutdownWithoutTasks() throws InterruptedException {
        executor = new TiredExecutor(2);
        executor.shutdown();
        executor = null;
    }

    @Test
    void shutdownAfterTasks() throws InterruptedException {
        executor = new TiredExecutor(3);
        AtomicInteger counter = new AtomicInteger(0);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(counter::incrementAndGet);
        }

        executor.submitAll(tasks);
        executor.shutdown();

        assertEquals(20, counter.get());
        executor = null;
    }

    @Test
    void matrixRowNegateTasks() {
        executor = new TiredExecutor(4);

        double[][] data = new double[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                data[i][j] = i * 5 + j;
            }
        }

        SharedMatrix matrix = new SharedMatrix(data);
        List<Runnable> tasks = new ArrayList<>();

        for (int i = 0; i < matrix.length(); i++) {
            final int row = i;
            tasks.add(() -> matrix.get(row).negate());
        }

        executor.submitAll(tasks);

        double[][] result = matrix.readRowMajor();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                double expected = -(i * 5 + j);
                double actual = result[i][j];

                if (expected == 0.0) {
                    assertTrue(actual == 0.0);
                } else {
                    assertEquals(expected, actual);
                }
            }
        }
    }

    @Test
    void vectorOperationsInParallel() {
        executor = new TiredExecutor(4);

        SharedVector[] vectors = new SharedVector[10];
        for (int i = 0; i < 10; i++) {
            vectors[i] = new SharedVector(
                    new double[]{i, i + 1, i + 2},
                    VectorOrientation.ROW_MAJOR
            );
        }

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            tasks.add(() -> vectors[idx].negate());
        }

        executor.submitAll(tasks);

        for (int i = 0; i < 10; i++) {
            assertTrue(vectors[i].get(0) == -i);
            assertTrue(vectors[i].get(1) == -(i + 1));
            assertTrue(vectors[i].get(2) == -(i + 2));
        }
    }
}
