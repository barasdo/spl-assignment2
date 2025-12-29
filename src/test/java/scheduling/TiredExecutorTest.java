package scheduling;

import memory.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

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
    void submitSingleTask_executesTask() {
        executor = new TiredExecutor(2);
        AtomicInteger counter = new AtomicInteger(0);

        executor.submitAll(List.of(counter::incrementAndGet));

        assertEquals(1, counter.get());
    }

    @Test
    void submitMultipleTasks_allExecuted() {
        executor = new TiredExecutor(4);
        AtomicInteger counter = new AtomicInteger(0);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(counter::incrementAndGet);
        }

        executor.submitAll(tasks);
        assertEquals(100, counter.get());
    }

    @Test
    void submitNullTask_throwsException() {
        executor = new TiredExecutor(2);
        assertThrows(IllegalArgumentException.class, () -> executor.submit(null));
    }

    @Test
    void submitEmptyTaskList_doesNothing() {
        executor = new TiredExecutor(2);
        executor.submitAll(new ArrayList<>());
    }

    @Test
    void multipleSubmitAllCalls_workCorrectly() {
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
    void submitAll_waitsUntilAllTasksFinish() {
        executor = new TiredExecutor(2);
        AtomicBoolean finished = new AtomicBoolean(false);

        List<Runnable> tasks = new ArrayList<>();
        tasks.add(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
        });
        tasks.add(() -> finished.set(true));

        executor.submitAll(tasks);

        assertTrue(finished.get(), "submitAll should block until all tasks complete");
    }


    @Test
    void shutdownAfterTasks_tasksCompleteFirst() throws InterruptedException {
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
    void fairness_allWorkersParticipate() {
        executor = new TiredExecutor(3);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            tasks.add(() -> {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ignored) {}
            });
        }

        executor.submitAll(tasks);

        String report = executor.getWorkerReport();

        assertTrue(report.contains("Worker #0"));
        assertTrue(report.contains("Worker #1"));
        assertTrue(report.contains("Worker #2"));
    }


    private static final double DELTA = 1e-6;

    @Test
    void matrixRowNegateTasks_executeCorrectly() {
        executor = new TiredExecutor(4);

        double[][] data = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };

        SharedMatrix matrix = new SharedMatrix(data);
        List<Runnable> tasks = new ArrayList<>();

        for (int i = 0; i < matrix.length(); i++) {
            final int row = i;
            tasks.add(() -> matrix.get(row).negate());
        }

        executor.submitAll(tasks);

        double[][] result = matrix.readRowMajor();
        assertEquals(-1, result[0][0], DELTA);
        assertEquals(-5, result[1][1], DELTA);
        assertEquals(-9, result[2][2], DELTA);
    }

    @Test
    void vectorOperationsInParallel_workCorrectly() {
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
            assertEquals(-i, vectors[i].get(0), DELTA);
            assertEquals(-(i + 1), vectors[i].get(1), DELTA);
            assertEquals(-(i + 2), vectors[i].get(2), DELTA);
        }
    }
}
