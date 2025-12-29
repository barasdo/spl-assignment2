package memory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import memory.SharedMatrix;
import memory.VectorOrientation;

public class SharedMatrixTest {
    @Test
        // Test initialization and row-major loading
    void testInitializationAndRowMajorLoading() {
        double[][] data = {
                {1.0, 2.0, 3.0},
                {4.0, 5.0, 6.0}
        };
        SharedMatrix matrix = new SharedMatrix(data);

        double[][] readData = matrix.readRowMajor();
        assertArrayEquals(data, readData);

        double[][] newData = {
                {7.0, 8.0, 9.0},
                {10.0, 11.0, 12.0}
        };
        matrix.loadRowMajor(newData);
        readData = matrix.readRowMajor();
        assertArrayEquals(newData, readData);
    }

    @Test
        // Test column-major loading
    void testColumnMajorLoading() {
        double[][] data = {
                {1.0, 4.0},
                {2.0, 5.0},
                {3.0, 6.0}
        };

        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(data);

        SharedVector col0 = matrix.get(0);
        SharedVector col1 = matrix.get(1);

        assertEquals(VectorOrientation.COLUMN_MAJOR, col0.getOrientation());
        assertEquals(VectorOrientation.COLUMN_MAJOR, col1.getOrientation());

        assertEquals(1.0, col0.get(0));
        assertEquals(2.0, col0.get(1));
        assertEquals(3.0, col0.get(2));

        assertEquals(4.0, col1.get(0));
        assertEquals(5.0, col1.get(1));
        assertEquals(6.0, col1.get(2));
    }

    @Test
        // Test readRowMajor on empty matrix
    void testReadRowMajorEmptyMatrix() {
        SharedMatrix matrix = new SharedMatrix();
        double[][] readData = matrix.readRowMajor();
        assertArrayEquals(new double[0][0], readData);
    }

    @Test
        // Test getOrientation
    void testGetOrientation() {
        double[][] data = {
                {1.0, 2.0},
                {3.0, 4.0}
        };
        SharedMatrix matrix = new SharedMatrix(data);
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());
        SharedMatrix matrixCol = new SharedMatrix();
        matrixCol.loadColumnMajor(data);
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrixCol.getOrientation());
        SharedMatrix emptyMatrix = new SharedMatrix();
        assertThrows(IllegalStateException.class, () -> {
            emptyMatrix.getOrientation();
        });
    }

    @Test
        // Test length method
    void testLength() {
        double[][] data = {
                {1.0, 2.0, 3.0},
                {4.0, 5.0, 6.0}
        };
        SharedMatrix matrix = new SharedMatrix(data);
        assertEquals(2, matrix.length());
    }
}
