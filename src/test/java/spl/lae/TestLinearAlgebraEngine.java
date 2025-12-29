package spl.lae;

import parser.*;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestLinearAlgebraEngine {

    private LinearAlgebraEngine lae;

    @BeforeEach
    void setUp() {
        lae = new LinearAlgebraEngine(4);
    }

    @Test
    void nullRootThrows() {
        assertThrows(IllegalArgumentException.class, () -> lae.run(null));
    }

    @Test
    void runSimpleMatrixNodeDoesNotCrash() {
        double[][] m = {{1, 2}, {3, 4}};
        ComputationNode node = new ComputationNode(m);

        assertDoesNotThrow(() -> lae.run(node));
    }



    @Test
    void runUnaryOperationSubmitsTasks() {
        double[][] m = {{1, 2}, {3, 4}};
        List<ComputationNode> children = List.of(new ComputationNode(m));
        ComputationNode root =
                new ComputationNode(ComputationNodeType.NEGATE, children);

        assertDoesNotThrow(() -> lae.run(root));
    }

    @Test
    void runBinaryOperationSubmitsTasks() {
        double[][] a = {{1, 2}, {3, 4}};
        double[][] b = {{5, 6}, {7, 8}};

        List<ComputationNode> children = List.of(
                new ComputationNode(a),
                new ComputationNode(b)
        );

        ComputationNode root =
                new ComputationNode(ComputationNodeType.ADD, children);

        assertDoesNotThrow(() -> lae.run(root));
    }



    @Test
    void nestedGraphsExecutedFully() {
        double[][] a = {{1, 0}, {0, 1}};
        double[][] b = {{1, 0}, {0, 1}};
        double[][] c = {{2, 3}, {4, 5}};

        ComputationNode add = new ComputationNode(
                ComputationNodeType.ADD,
                List.of(new ComputationNode(a), new ComputationNode(b))
        );

        ComputationNode root = new ComputationNode(
                ComputationNodeType.MULTIPLY,
                List.of(add, new ComputationNode(c))
        );

        assertDoesNotThrow(() -> lae.run(root));
    }

    @Test
    void wideGraphSubmitsManyTasks() {
        double[][] base = {{1, 1}, {1, 1}};
        List<ComputationNode> children = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            children.add(new ComputationNode(base));
        }

        ComputationNode root =
                new ComputationNode(ComputationNodeType.ADD, children);

        assertDoesNotThrow(() -> lae.run(root));
    }



    @Test
    void illegalMultiplicationDoesNotHangEngine() {
        double[][] a = {{1, 2, 3}};
        double[][] b = {{1, 2}};

        ComputationNode root = new ComputationNode(
                ComputationNodeType.MULTIPLY,
                List.of(new ComputationNode(a), new ComputationNode(b))
        );

        assertDoesNotThrow(() -> lae.run(root));
    }



    @Test
    void manyIndependentRunsDoNotBlock() {
        for (int i = 0; i < 10; i++) {
            double[][] m = {{i, i}, {i, i}};
            ComputationNode node = new ComputationNode(m);

            assertDoesNotThrow(() -> lae.run(node));
        }
    }

    @Test
    void shutdownAfterRunWorks() throws InterruptedException {
        double[][] m = {{1, 2}, {3, 4}};
        ComputationNode node = new ComputationNode(m);

        lae.run(node);

        lae = null;
    }
}
