package com.pukio.update.service;

import com.pukio.common.model.InventoryRecord;
import com.pukio.common.model.ProductRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses binary indexed file content received over TCP.
 * Deserializes ProductRecord and InventoryRecord objects
 * from the raw bytes sent by FileSender. (TASK-E1-38)
 */
@Slf4j
@Component
public class IndexedFileParser {

    /**
     * Deserialize all ProductRecord objects from a .dat file byte array.
     */
    public List<ProductRecord> parseProducts(byte[] datFileBytes) throws IOException {
        List<ProductRecord> records = new ArrayList<>();

        if (datFileBytes == null || datFileBytes.length == 0) {
            log.warn("Empty products data file received.");
            return records;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new ByteArrayInputStream(datFileBytes)))) {

            while (true) {
                try {
                    Object obj = ois.readObject();
                    if (obj instanceof ProductRecord product) {
                        if (!product.isDeleted()) {
                            records.add(product);
                        }
                    }
                } catch (EOFException e) {
                    break;
                } catch (ClassNotFoundException e) {
                    throw new IOException("Unknown class in products data file", e);
                }
            }
        }

        log.info("Parsed {} product records from data file.", records.size());
        return records;
    }

    /**
     * Deserialize all InventoryRecord objects from a .dat file byte array.
     */
    public List<InventoryRecord> parseInventory(byte[] datFileBytes) throws IOException {
        List<InventoryRecord> records = new ArrayList<>();

        if (datFileBytes == null || datFileBytes.length == 0) {
            log.warn("Empty inventory data file received.");
            return records;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new ByteArrayInputStream(datFileBytes)))) {

            while (true) {
                try {
                    Object obj = ois.readObject();
                    if (obj instanceof InventoryRecord inventory) {
                        records.add(inventory);
                    }
                } catch (EOFException e) {
                    break;
                } catch (ClassNotFoundException e) {
                    throw new IOException("Unknown class in inventory data file", e);
                }
            }
        }

        log.info("Parsed {} inventory records from data file.", records.size());
        return records;
    }
}
