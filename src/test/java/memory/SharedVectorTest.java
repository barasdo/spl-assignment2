package memory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import memory.SharedVector;
import memory.VectorOrientation;

class SharedVectorTest {
    @Test
    // Test initialization and getters
    void testInitializationAndGetters() {
        double[] data = { 1.0, 2.0, 3.0 };
        SharedVector vector = new SharedVector(data, VectorOrientation.ROW_MAJOR);

        assertEquals(3, vector.length());
        assertEquals(VectorOrientation.ROW_MAJOR, vector.getOrientation());
        assertEquals(1.0, vector.get(0));
        assertEquals(2.0, vector.get(1));
        assertEquals(3.0, vector.get(2));

        SharedVector vectorCol = new SharedVector(data, VectorOrientation.COLUMN_MAJOR);
        assertEquals(VectorOrientation.COLUMN_MAJOR, vectorCol.getOrientation());
    }

    @Test
    // Test transpose method
    void testTranspose() {
        double[] data = { 1.0, 2.0, 3.0 };
        SharedVector vector = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        vector.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, vector.getOrientation());
        vector.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, vector.getOrientation());
    }

    @Test
    // Test add
    void testAdd() {
        double[] data1 = { 1.0, 2.0, 3.0 };
        double[] data2 = { 4.0, 5.0, 6.0 };
        double[] data3 = { 7.0, 8.0 };
        SharedVector vector1 = new SharedVector(data1, VectorOrientation.ROW_MAJOR);
        SharedVector vector2 = new SharedVector(data2, VectorOrientation.ROW_MAJOR);
        vector1.add(vector2);
        assertEquals(5.0, vector1.get(0));
        assertEquals(7.0, vector1.get(1));
        assertEquals(9.0, vector1.get(2));
        // Test adding vectors of different lengths
        SharedVector vector3 = new SharedVector(data3, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> {
            vector1.add(vector3);
        });
        // Test adding vectors of different orientations
        SharedVector vector4 = new SharedVector(data2, VectorOrientation.COLUMN_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> {
            vector1.add(vector4);
        });
        // Test adding null vector
        SharedVector vector5 = null;
        assertThrows(IllegalArgumentException.class, () -> {
            vector1.add(vector5);
        });
    }

    @Test
    // Test negate method
    void testNegate() {
        double[] data = { 1.0, -2.0, 3.0 };
        SharedVector vector = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        vector.negate();
        assertEquals(-1.0, vector.get(0));
        assertEquals(2.0, vector.get(1));
        assertEquals(-3.0, vector.get(2));
    }

    @Test
    // Test dot product
    void testDotProduct() {
        double[] rowData = { 1.0, 2.0, 3.0 };
        double[] colData = { 4.0, 5.0, 6.0 };
        SharedVector rowVector = new SharedVector(rowData, VectorOrientation.ROW_MAJOR);
        SharedVector colVector = new SharedVector(colData, VectorOrientation.COLUMN_MAJOR);
        double result = rowVector.dot(colVector);
        assertEquals(32.0, result);

        // Test dot product with different lengths
        double[] shortData = { 1.0, 2.0 };
        SharedVector shortVector = new SharedVector(shortData, VectorOrientation.COLUMN_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> {
            rowVector.dot(shortVector);
        });
        // Test dot product with incorrect orientations
        SharedVector wrongOrientationVector = new SharedVector(colData, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> {
            rowVector.dot(wrongOrientationVector);
        });
        // Test dot product with null vector
        SharedVector nullVector = null;
        assertThrows(IllegalArgumentException.class, () -> {
            rowVector.dot(nullVector);
        });
    }

    @Test
    // vecMatMulTest
    void testVecMatMul() {
        double[] rowData = { 1.0, 2.0 };
        double[][] matrixData = { { 3.0, 4.0 }, { 5.0, 6.0 } };
        SharedVector rowVector = new SharedVector(rowData, VectorOrientation.ROW_MAJOR);
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(matrixData);
        rowVector.vecMatMul(matrix);
        assertEquals(13.0, rowVector.get(0));
        assertEquals(16.0, rowVector.get(1));

        // Test vecMatMul with incorrect orientation
        SharedVector colVector = new SharedVector(rowData, VectorOrientation.COLUMN_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> {
            colVector.vecMatMul(matrix);
        });
        // Test vecMatMul with incompatible dimensions
        double[] incompatibleData = { 1.0, 2.0, 3.0 };
        SharedVector incompatibleVector = new SharedVector(incompatibleData, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> {
            incompatibleVector.vecMatMul(matrix);
        });
        // Test vecMatMul with null matrix
        SharedMatrix nullMatrix = null;
        assertThrows(IllegalArgumentException.class, () -> {
            rowVector.vecMatMul(nullMatrix);
        });

        // Test vecMatMul with empty matrix
        SharedMatrix emptyMatrix = new SharedMatrix();
        assertThrows(IllegalArgumentException.class, () -> {
            rowVector.vecMatMul(emptyMatrix);
        });

        // Test vecMatMul with empty vector and empty matrix
        double[] emptyData = {};
        SharedVector emptyVector = new SharedVector(emptyData, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> {
            emptyVector.vecMatMul(emptyMatrix);
        });
    }
}