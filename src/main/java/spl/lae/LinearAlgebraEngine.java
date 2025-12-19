package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        // TODO: create executor with given thread count
        executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        return null;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
        if (node == null) {
            throw new IllegalArgumentException("ComputationNode cannot be null");
        }
        List<Runnable> tasks = new ArrayList<>();
        if (node.getNodeType() == ComputationNodeType.ADD) {
            tasks = createAddTasks();
        } else if (node.getNodeType() == ComputationNodeType.MULTIPLY) {
            tasks = createMultiplyTasks();
        } else if (node.getNodeType() == ComputationNodeType.NEGATE) {
            tasks = createNegateTasks();
        } else if (node.getNodeType() == ComputationNodeType.TRANSPOSE) {
            tasks = createTransposeTasks();
        } else {
            throw new IllegalArgumentException("Unsupported operation: " + node.getNodeType());
        }
        executor.submitAll(tasks);
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        List<Runnable> addTasks = new ArrayList<>();
        for (int i = 0; i < leftMatrix.length(); i++){
            SharedVector leftVector = leftMatrix.get(i);
            SharedVector rightVector = rightMatrix.get(i);
            Runnable task = () -> {
                leftVector.add(rightVector);
            };
            addTasks.add(task);
        }
        return addTasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        return null;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        return null;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        List <Runnable> toDo = new ArrayList<>();

    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return null;
    }
}
