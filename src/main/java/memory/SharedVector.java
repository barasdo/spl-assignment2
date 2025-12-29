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
        this.readLock();
        try {
            return vector[index];
        } finally {
            this.readUnlock();
        }
    }

    public int length() {
        // TODO: return vector length
        this.readLock();
        try {
            return vector.length;
        } finally {
            this.readUnlock();
        }
    }

    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        this.readLock();
        try {
            return orientation;
        } finally {
            this.readUnlock();
        }
    }

    public void writeLock() {
        // TODO: acquire write lock
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        // TODO: release write lock
        lock.writeLock().unlock();
    }

    public void readLock() {
        // TODO: acquire read lock
        lock.readLock().lock();
    }

    public void readUnlock() {
        // TODO: release read lock
        lock.readLock().unlock();
    }

    public void transpose() {
        // TODO: transpose vector
        this.writeLock();
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
        boolean thisIsFirst = System.identityHashCode(this) < System.identityHashCode(other);
        if (thisIsFirst) {
            this.writeLock();
            other.readLock();
        }
        else {
            other.readLock();
            this.writeLock();
        }
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
            if (thisIsFirst) {
                other.readUnlock();
                this.writeUnlock();
            }
            else {
                this.writeUnlock();
                other.readUnlock();
            }
        }
    }

    public void negate() {
        // TODO: negate vector
        this.writeLock();
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