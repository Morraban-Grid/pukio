package com.pukio.common.store;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Hash-indexed file store implementation.
 *
 * Uses HashMap<K, Long> for O(1) average-case key lookups.
 * Storage layout is identical to BTreeIndexedFileStore:
 * - Data file (.dat): append-only binary records.
 * - Index file (.idx): serialized HashMap<K, Long> mapping key -> byte offset.
 *
 * Concurrency:
 * - ReadWriteLock allows multiple concurrent readers, single writer.
 * - FileLock on the index file prevents concurrent access from other JVM processes.
 *
 * Soft-delete:
 * - delete() marks the record's deleted flag to true and rewrites the index.
 * - Physical records are never removed from the data file.
 */
@Slf4j
public class HashIndexedFileStore<K extends Serializable, V extends Serializable>
        implements IndexedFileStore<K, V> {

    private final Path dataFilePath;
    private final Path indexFilePath;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final HashMap<K, Long> index = new HashMap<>();
    private final java.util.function.Function<V, Boolean> deletedFlagAccessor;
    private final java.util.function.BiConsumer<V, Boolean> deletedFlagMutator;

    public HashIndexedFileStore(Path dataFilePath,
                                Path indexFilePath,
                                java.util.function.Function<V, Boolean> deletedFlagAccessor,
                                java.util.function.BiConsumer<V, Boolean> deletedFlagMutator)
            throws IOException {
        this.dataFilePath = dataFilePath;
        this.indexFilePath = indexFilePath;
        this.deletedFlagAccessor = deletedFlagAccessor;
        this.deletedFlagMutator = deletedFlagMutator;
        initFiles();
        loadIndex();
    }

    @Override
    public void insert(K key, V record) throws IOException {
        rwLock.writeLock().lock();
        try {
            if (index.containsKey(key)) {
                throw new IllegalArgumentException("Key already exists: " + key);
            }
            long offset = appendRecord(record);
            index.put(key, offset);
            persistIndex();
            log.debug("Inserted record with key={} at offset={}", key, offset);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void update(K key, V record) throws IOException {
        rwLock.writeLock().lock();
        try {
            if (!index.containsKey(key)) {
                throw new IllegalArgumentException("Key not found: " + key);
            }
            long offset = appendRecord(record);
            index.put(key, offset);
            persistIndex();
            log.debug("Updated record with key={} at new offset={}", key, offset);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void delete(K key) throws IOException {
        rwLock.writeLock().lock();
        try {
            Long offset = index.get(key);
            if (offset == null) {
                throw new IllegalArgumentException("Key not found: " + key);
            }
            V record = readRecordAt(offset);
            deletedFlagMutator.accept(record, true);
            long newOffset = appendRecord(record);
            index.put(key, newOffset);
            persistIndex();
            log.debug("Soft-deleted record with key={}", key);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public Optional<V> findByKey(K key) throws IOException {
        rwLock.readLock().lock();
        try {
            Long offset = index.get(key);
            if (offset == null) {
                return Optional.empty();
            }
            V record = readRecordAt(offset);
            if (Boolean.TRUE.equals(deletedFlagAccessor.apply(record))) {
                return Optional.empty();
            }
            return Optional.of(record);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<V> readAll() throws IOException {
        rwLock.readLock().lock();
        try {
            List<V> results = new ArrayList<>();
            for (Long offset : index.values()) {
                V record = readRecordAt(offset);
                if (!Boolean.TRUE.equals(deletedFlagAccessor.apply(record))) {
                    results.add(record);
                }
            }
            return Collections.unmodifiableList(results);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void close() throws IOException {
        rwLock.writeLock().lock();
        try {
            persistIndex();
            log.info("HashIndexedFileStore closed. dataFile={}", dataFilePath);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void initFiles() throws IOException {
        if (!Files.exists(dataFilePath)) {
            Files.createDirectories(dataFilePath.getParent());
            Files.createFile(dataFilePath);
        }
        if (!Files.exists(indexFilePath)) {
            Files.createDirectories(indexFilePath.getParent());
            Files.createFile(indexFilePath);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadIndex() throws IOException {
        if (Files.size(indexFilePath) == 0) return;

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(indexFilePath)))) {
            HashMap<K, Long> loaded = (HashMap<K, Long>) ois.readObject();
            index.putAll(loaded);
            log.info("Loaded hash index with {} entries from {}", index.size(), indexFilePath);
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize index: " + indexFilePath, e);
        }
    }

    private long appendRecord(V record) throws IOException {
        try (FileChannel channel = FileChannel.open(dataFilePath,
                StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            try (FileLock fileLock = channel.lock()) {
                long offset = channel.size();
                try (ObjectOutputStream oos = new ObjectOutputStream(
                        new BufferedOutputStream(new FileOutputStream(dataFilePath.toFile(), true)))) {
                    oos.writeObject(record);
                    oos.flush();
                }
                return offset;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private V readRecordAt(long offset) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(dataFilePath.toFile(), "r")) {
            raf.seek(offset);
            try (ObjectInputStream ois = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(raf.getFD())))) {
                return (V) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException("Failed to deserialize record at offset=" + offset, e);
            }
        }
    }

    private void persistIndex() throws IOException {
        try (FileChannel channel = FileChannel.open(indexFilePath,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            try (FileLock fileLock = channel.lock()) {
                try (ObjectOutputStream oos = new ObjectOutputStream(
                        new BufferedOutputStream(new FileOutputStream(indexFilePath.toFile())))) {
                    oos.writeObject(index);
                    oos.flush();
                }
            }
        }
    }
}
