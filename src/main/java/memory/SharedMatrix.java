package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // TODO: initialize empty matrix
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        // TODO: construct matrix as row-major SharedVectors
        if (matrix == null){
            throw new IllegalArgumentException("Input matrix is null.");
        }
        vectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            vectors[i] = new SharedVector(matrix[i].clone(), VectorOrientation.ROW_MAJOR);
        }
    }

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
        if (matrix == null){
            throw new IllegalArgumentException("Input matrix is null.");
        }
        int len = matrix.length;
        SharedVector[] tmp = new SharedVector[len];
        for (int i = 0; i < len; i++){
            tmp [i] = new SharedVector(matrix[i].clone(),VectorOrientation.ROW_MAJOR);
        }
        vectors = tmp;
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
        if (matrix == null) {
            throw new IllegalArgumentException("Input matrix is null.");
        }
        if (matrix.length == 0) {
            vectors = new SharedVector[0];
            return;
        }
        int len = matrix[0].length;
        SharedVector[] tmp = new SharedVector[len];
        for (int i = 0; i < len; i++) {
            double[] col = new double[matrix.length];
            for (int j = 0; j < matrix.length; j++) {
                col[j] = matrix[j][i];
            }
            tmp[i] = new SharedVector(col, VectorOrientation.COLUMN_MAJOR);
        }
        vectors = tmp;
    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        if (vectors.length == 0){
            return new double[0][0];
        }
        int m;
        int n;
        SharedVector [] vecs = vectors;
        acquireAllVectorReadLocks(vecs);
        try {
            VectorOrientation orientation = vectors[0].getOrientation();
            if (orientation == VectorOrientation.ROW_MAJOR) {
                m = vectors.length;
                n = vectors[0].length();
                double[][] result = new double[m][n];
                for (int i = 0; i < m; i++) {
                    SharedVector vec = vectors[i];
                    for (int j = 0; j < n; j++) {
                        result[i][j] = vec.get(j);
                    }
                }
                return result;
            } else {
                n = vectors.length;
                m = vectors[0].length();
                double[][] result = new double[m][n];
                for (int i = 0; i < n; i++) {
                    SharedVector vec = vectors[i];
                    for (int j = 0; j < m; j++) {
                        result[j][i] = vec.get(j);
                    }
                }
                return result;
            }
        }
        finally {
            releaseAllVectorReadLocks(vectors);
            }

    }

    public SharedVector get(int index) {
        if (index < 0 || index >= vectors.length) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
        }
        return vectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        return vectors.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        if (vectors.length == 0){
            throw new IllegalStateException("Matrix is empty, no orientation defined.");
        }
        return vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        for (SharedVector vec : vecs){
            vec.readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        for (SharedVector vec : vecs){
            vec.readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        for (SharedVector vec : vecs){
            vec.writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        for (SharedVector vec : vecs){
            vec.writeUnlock();
        }
    }
}
