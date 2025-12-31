package memory;

import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        // TODO: store vector data and its orientation
        this.vector = vector;
        this.orientation = orientation;
    }

    public double get(int index) {
        // TODO: return element at index (read-locked)
        this.readLock();    // Read lock ensures visibility of vector contents during concurrent writes
        try {
            return vector[index];
        } finally {
            this.readUnlock();
        }
    }

    public int length() {
        // TODO: return vector length
        this.readLock();   // Read lock prevents observing an inconsistent vector length during concurrent modification
        try {
            return vector.length;
        } finally {
            this.readUnlock();
        }
    }

    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        this.readLock();     // Read lock ensures consistent visibility of the vector orientation
        try {
            return orientation;
        } finally {
            this.readUnlock();
        }
    }

    public void writeLock() {
        // TODO: acquire write lock
        lock.writeLock().lock();     // Acquires exclusive access for modifying vector state

    }

    public void writeUnlock() {
        // TODO: release write lock
        lock.writeLock().unlock();
    }

    public void readLock() {
        // TODO: acquire read lock
        lock.readLock().lock();      // Allows concurrent readers while preventing concurrent writes

    }

    public void readUnlock() {
        // TODO: release read lock
        lock.readLock().unlock();
    }

    public void transpose() {
        // TODO: transpose vector
        this.writeLock();    // Write lock prevents concurrent reads from observing a partially updated orientation
        try {
            if (this.orientation == VectorOrientation.ROW_MAJOR) {
                this.orientation = VectorOrientation.COLUMN_MAJOR;
            } else {
                this.orientation = VectorOrientation.ROW_MAJOR;
            }
        } finally {
            this.writeUnlock();
        }
    }

    public void add(SharedVector other) {
        // TODO: add two vectors

        if (other == null) {
            throw new IllegalArgumentException("Can't add other vector is null.");
        }
        // A write lock is required since this vector's contents are modified.
        // No deadlock risk exists here because the linear algebra engine enforces
        // a fixed computation order, where additions are always applied to the left operand.
        this.writeLock();
        other.readLock();
        try {
            if (other.vector.length != this.vector.length) {
                throw new IllegalArgumentException("Vectors must be of the same length to add.");
            }
            if (other.orientation != this.orientation) {
                throw new IllegalArgumentException("Vectors must have the same orientation to add.");
            }
            for (int i = 0; i < this.vector.length; i++) {
                this.vector[i] += other.vector[i];
            }
        } finally {
            other.readUnlock();
            this.writeUnlock();
        }
    }

    public void negate() {
        // TODO: negate vector
        this.writeLock();     // Write lock prevents concurrent reads from observing partially negated values
        try {
            for (int i = 0; i < vector.length; i++) {
                this.vector[i] = -this.vector[i];
            }
        } finally {
            this.writeUnlock();
        }
    }


    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        if (other == null) {
            throw new IllegalArgumentException("Can't compute dot product, other vector is null.");
        }
        // Read locks on both vectors ensure consistent reads
        // while allowing concurrent read-only operations
        this.readLock();
        other.readLock();
        try {
            if (other.vector.length != this.vector.length) {
                throw new IllegalArgumentException("Vectors must be of the same length for doing dot product.");
            }
            if (this.orientation != VectorOrientation.ROW_MAJOR || other.orientation != VectorOrientation.COLUMN_MAJOR) {
                throw new IllegalArgumentException("Vectors orientations are not fits for dot product.");
            }
            double sum = 0;
            for (int i = 0; i < vector.length; i++) {
                sum += (this.vector[i] * other.vector[i]);
            }
            return sum;
        } finally {
            other.readUnlock();
            readUnlock();
        }
    }
    // Write lock is required because the operation replaces the vector contents
    // and must be atomic with respect to concurrent reads
    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        if (matrix == null) {
            throw new IllegalArgumentException("Can't do vector-matrix multiplication, matrix is null.");
        }
        if (matrix.length() == 0 || matrix.get(0).length() == 0) {
            throw new IllegalArgumentException("Matrix is empty.");
        }
         if (matrix.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
             throw new IllegalArgumentException("Matrix must be column-major for vector-matrix multiplication.");
         }

        this.writeLock();
        try{
            if (this.orientation != VectorOrientation.ROW_MAJOR) {
                throw new IllegalArgumentException("Vector must be row-major for vector-matrix multiplication.");
            }
            //matrix is column-major
            if (matrix.getOrientation() == VectorOrientation.COLUMN_MAJOR){
                if (this.vector.length != matrix.get(0).length()){
                    throw new IllegalArgumentException("Vector length must equal to matrix row count for vector-matrix multiplication.");
                }
                double[] result = new double[matrix.length()];
                for (int i = 0; i < result.length; i++){
                    result[i] = this.dot(matrix.get(i));
                }
                this.vector = result;
                this.orientation = VectorOrientation.ROW_MAJOR;
            }
            }
        finally {
            this.writeUnlock();
        }
    }
}