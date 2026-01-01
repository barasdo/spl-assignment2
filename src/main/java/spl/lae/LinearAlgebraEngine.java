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
        try {
            if (computationRoot == null) {
                throw new IllegalArgumentException("ComputationNode cannot be null");
            }
            computationRoot.associativeNesting();

            ComputationNode toCompute = computationRoot.findResolvable();
            while (toCompute != null) {
                loadAndCompute(toCompute);
                toCompute.resolve(leftMatrix.readRowMajor());
                toCompute = computationRoot.findResolvable();
            }
            return computationRoot;
        } finally {
            try {
                executor.shutdown();
            } catch (InterruptedException e) {
                // interruption during shutdown is ignored
            }
        }
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
        if (node == null) {
            throw new IllegalArgumentException("ComputationNode cannot be null");
        }
        List<ComputationNode> children = node.getChildren();
        List<Runnable> tasks = new ArrayList<>();
        if (node.getNodeType() == ComputationNodeType.ADD) {
            if (children.size() != 2) {
                throw new IllegalArgumentException("ADD node must have exactly 2 children");
            }
            leftMatrix.loadRowMajor(children.get(0).getMatrix());
            rightMatrix.loadRowMajor(children.get(1).getMatrix());
            tasks = createAddTasks();
        } else if (node.getNodeType() == ComputationNodeType.MULTIPLY) {
            if (children.size() != 2) {
                throw new IllegalArgumentException("MULTIPLY node must have exactly 2 children");
            }
            leftMatrix.loadRowMajor(children.get(0).getMatrix());
            rightMatrix.loadColumnMajor(children.get(1).getMatrix());
            tasks = createMultiplyTasks();
        } else if (node.getNodeType() == ComputationNodeType.NEGATE) {
            if (children.size() != 1) {
                throw new IllegalArgumentException("NEGATE node must have exactly 1 child");
            }
            leftMatrix.loadRowMajor(children.get(0).getMatrix());
            tasks = createNegateTasks();
        } else if (node.getNodeType() == ComputationNodeType.TRANSPOSE) {
            if (children.size() != 1) {
                throw new IllegalArgumentException("TRANSPOSE node must have exactly 1 child");
            }
            leftMatrix.loadRowMajor(children.get(0).getMatrix());
            tasks = createTransposeTasks();
        } else {
            throw new IllegalArgumentException("Unsupported operation: " + node.getNodeType());
        }
        executor.submitAll(tasks);
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        if (leftMatrix.length() == 0 || rightMatrix.length() == 0) {
            throw new IllegalArgumentException("Matrices must not be empty for addition");
        }
        if (leftMatrix.length() != rightMatrix.length()) {
            throw new IllegalArgumentException("Matrices must have the same number of rows for addition");
        }

        List<Runnable> addTasks = new ArrayList<>();
        for (int i = 0; i < leftMatrix.length(); i++) {
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
        if (leftMatrix.length() == 0 || rightMatrix.length() == 0) {
            throw new IllegalArgumentException("Matrices must not be empty for multiplication");
        }

        List<Runnable> multiplyTasks = new ArrayList<>();
        for (int i = 0; i < leftMatrix.length(); i++) {
            SharedVector leftVector = leftMatrix.get(i);
            Runnable task = () -> {
                leftVector.vecMatMul(rightMatrix);
            };
            multiplyTasks.add(task);
        }
        return multiplyTasks;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        List<Runnable> negateTasks = new ArrayList<>();
        for (int i = 0; i < leftMatrix.length(); i++) {
            SharedVector leftVector = leftMatrix.get(i);
            Runnable task = () -> {
                leftVector.negate();
            };
            negateTasks.add(task);
        }
        return negateTasks;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        List<Runnable> transposeTasks = new ArrayList<>();
        for (int i = 0; i < leftMatrix.length(); i++) {
            SharedVector leftVector = leftMatrix.get(i);
            Runnable task = () -> {
                leftVector.transpose();
            };
            transposeTasks.add(task);
        }
        return transposeTasks;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return executor.getWorkerReport();
    }

}
