package scheduling;

import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        // TODO
        if (numThreads <= 0) {
            throw new IllegalArgumentException("Number of threads must be positive");
        }
        workers = new TiredThread[numThreads];
        Random rand = new Random();
        for (int i=0; i < numThreads; i++){
            double fatigueFactor = rand.nextDouble(0.5,1.5);
            workers[i] = new TiredThread(i, fatigueFactor);
            workers[i].start();
            idleMinHeap.add(workers[i]);
        }
    }

    public void submit(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        TiredThread worker;

        try {
            //block until a worker is available
            worker = idleMinHeap.take();
        }
        catch (InterruptedException e) {
            //if interrupted while waiting, set interrupt flag and return
            Thread.currentThread().interrupt();
            return;
        }

        inFlight.incrementAndGet();

        try {
            //wrap the task to return the worker to the idle heap after completion
            Runnable wrapped = () -> {
                try {
                    //run the actual task
                    task.run();
                } finally {
                    //return the worker to the idle heap (that's the reason we wrap the task)
                    synchronized (TiredExecutor.this) {
                        idleMinHeap.add(worker);
                    //decrement in-flight task count and notify if zero (for shutdown and submitAll)
                        if (inFlight.decrementAndGet() == 0) {
                            TiredExecutor.this.notifyAll();
                        }
                    }
                }
            };
            //assign the wrapped task to the worker
            worker.newTask(wrapped);

        } catch (IllegalStateException e) {
            //if the worker rejected the task, decrement in-flight count and return the worker to idle heap
            synchronized (this) {
                inFlight.decrementAndGet();
                idleMinHeap.add(worker);
                notifyAll();
            }
            throw e;
        }
    }

    public void submitAll(Iterable<Runnable> tasks) {
        // TODO: submit tasks one by one and wait until all finish
        for (Runnable task : tasks) {
            submit(task);
        }
        synchronized (this){
            //wait until all tasks are done
            while (inFlight.get() > 0){
                //we wait that all tasks are finished, and then we notify
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }




    public void shutdown() throws InterruptedException {
        // TODO
        synchronized (this){
            while (inFlight.get() > 0){
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        for (TiredThread worker : workers){
            worker.shutdown();
            worker.join();
        }
    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        StringBuilder sb = new StringBuilder();

        sb.append("============== WORKER REPORT ==============\n");

        for (TiredThread w : workers) {
            sb.append("Worker #").append(w.getWorkerId())
                    .append(" | Busy: ").append(w.isBusy())
                    .append(" | Fatigue: ").append(w.getFatigue())
                    .append(" | Work Time: ").append(w.getTimeUsed() / 1_000_000.0).append(" ms")
                    .append(" | Idle Time: ").append(w.getTimeIdle() / 1_000_000.0).append(" ms")
                    .append("\n");
        }

        sb.append("==========================================\n");
        return sb.toString();
    }
}

