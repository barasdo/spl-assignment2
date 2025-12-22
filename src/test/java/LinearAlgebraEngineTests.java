import org.junit.jupiter.api.Test;
import spl.lae.LinearAlgebraEngine;
import parser.ComputationNode;

import static org.junit.jupiter.api.Assertions.*;

class LinearAlgebraEngineTests {

    @Test
    void testSimpleAdd() {
        ComputationNode node = new ComputationNode("+", java.util.List.of(
                new ComputationNode(new double[][]{{1, 2}, {3, 4}}),
                new ComputationNode(new double[][]{{5, 6}, {7, 8}})
        ));

        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);
        ComputationNode result = lae.run(node);

        double[][] out = result.getMatrix();
        assertEquals(6.0, out[0][0], 1e-6);
        assertEquals(12.0, out[1][1], 1e-6);
    }

    @Test
    void testNegate() {
        ComputationNode node = new ComputationNode("-", java.util.List.of(
                new ComputationNode(new double[][]{{1, -2}})
        ));

        LinearAlgebraEngine lae = new LinearAlgebraEngine(1);
        double[][] out = lae.run(node).getMatrix();

        assertEquals(-1.0, out[0][0], 1e-6);
        assertEquals(2.0, out[0][1], 1e-6);
    }
}
