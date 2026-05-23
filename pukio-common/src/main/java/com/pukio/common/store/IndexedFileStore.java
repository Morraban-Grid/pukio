package com.pukio.common.store;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Generic interface for indexed file-based persistence.
 * Supports O(log n) key-based access via B-tree index,
 * or O(1) via hash index depending on implementation.
 *
 * @param <K> the key type (must be Serializable)
 * @param <V> the value/record type (must be Serializable)
 */
public interface IndexedFileStore<K, V> {

    /**
     * Insert a new record. Throws exception if key already exists.
     */
    void insert(K key, V record) throws IOException;

    /**
     * Update an existing record. Throws exception if key does not exist.
     */
    void update(K key, V record) throws IOException;

    /**
     * Soft-delete a record by marking it as deleted.
     * Does NOT physically remove the record from the file.
     */
    void delete(K key) throws IOException;

    /**
     * Find a record by its key. Returns empty if not found or soft-deleted.
     */
    Optional<V> findByKey(K key) throws IOException;

    /**
     * Return all non-deleted records.
     */
    List<V> readAll() throws IOException;

    /**
     * Release file handles and flush any pending writes.
     */
    void close() throws IOException;
}
