package scheduling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TiredExecutorTest {

    // Helper: access private workers[] via reflection (מותר בטסטים)
    private static TiredThread[] getWorkers(TiredExecutor ex) {
        try {
            Field f = TiredExecutor.class.getDeclaredField("workers");
            f.setAccessible(true);
            return (TiredThread[]) f.get(ex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Timeout(5)
    void submitAllRunsAllTasksAndWaits() throws Exception {
        TiredExecutor ex = new TiredExecutor(3);

        int n = 50;
        CountDownLatch latch = new CountDownLatch(n);
        AtomicInteger counter = new AtomicInteger(0);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            tasks.add(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        ex.submitAll(tasks);

        assertTrue(latch.await(0, TimeUnit.MILLISECONDS), "submitAll returned before all tasks finished");
        assertEquals(n, counter.get(), "Not all tasks executed exactly once");

        ex.shutdown();
    }

    @Test
    @Timeout(5)
    void shutdownTerminatesAllWorkers() throws Exception {
        TiredExecutor ex = new TiredExecutor(4);

        // give workers a moment to start
        Thread.sleep(50);

        ex.shutdown();

        for (TiredThread w : getWorkers(ex)) {
            assertFalse(w.isAlive(), "Worker thread should be terminated after shutdown+join");
        }
    }

    @Test
    @Timeout(5)
    void submitExecutesTask() throws Exception {
        TiredExecutor ex = new TiredExecutor(1);

        CountDownLatch latch = new CountDownLatch(1);
        ex.submit(latch::countDown);

        assertTrue(latch.await(1, TimeUnit.SECONDS), "Task did not run");

        ex.shutdown();
    }
}
