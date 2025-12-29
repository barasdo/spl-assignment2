package scheduling;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TiredThreadTest {
    @Test
    //Initialization test and getters
    void testInitializationAndGetters() {
        TiredThread worker = new TiredThread(5, 1.5);

        assertEquals(5, worker.getWorkerId());
        assertEquals(0, worker.getTimeUsed());
        assertEquals(0, worker.getTimeIdle());
        assertFalse(worker.isBusy());
        assertEquals(0.0, worker.getFatigue());
    }

    @Test
        // Test executes submitted task and terminates on poison pill
    void executesSubmittedTaskAndTerminatesOnPoisonPill() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean(false);

        TiredThread worker = new TiredThread(0, 1.0);
        worker.start();

        worker.newTask(() -> ran.set(true));
        worker.shutdown();
        worker.join(1000);

        assertTrue(ran.get(), "The task should have been executed.");
        assertFalse(worker.isAlive(), "The worker should have terminated after poison pill.");
    }

    @Test
        // Test worker is not busy after completing task
    void workerIsNotBusyAfterCompletingTask() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean(false);
        TiredThread worker = new TiredThread(1, 1.0);
        worker.start();
        worker.newTask(() -> {
            ran.set(true);
        });
        while (!ran.get()) {
            Thread.sleep(1);
        }
        assertFalse(worker.isBusy(), "The worker should not be busy after completing the task.");
        worker.shutdown();
        worker.join(1000);

    }

    @Test
    // Test null task submission throws exception
    void testNullTaskSubmissionThrowsException() {
        TiredThread worker = new TiredThread(2, 1.0);
        worker.start();
        assertThrows(IllegalArgumentException.class, () -> {
            worker.newTask(null);
        });
        worker.shutdown();
    }

    @Test
    //Test new task submission when worker is not alive
    void testNewTaskSubmissionWhenWorkerIsNotAlive() {
        TiredThread worker = new TiredThread(3, 1.0);
        worker.start();
        worker.shutdown();
        assertThrows(IllegalStateException.class, () -> {
            worker.newTask(() -> {
            });
        });
    }

    @Test
    //Test worker excecutes multiple tasks sequentially
    void testWorkerExecutesMultipleTasksSequentially() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        TiredThread worker = new TiredThread(4, 1.0);
        worker.start();

        worker.newTask(() -> counter.incrementAndGet());
        worker.newTask(() -> counter.incrementAndGet());
        worker.shutdown();
        worker.join(1000);

        assertEquals(2, counter.get(), "The worker should have executed both tasks sequentially.");
    }

}
