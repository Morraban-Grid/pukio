package com.pukio.send.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Service responsible for sending indexed files to the Data_Server via TCP. (REQ 1.3)
 * Implements retry logic with exponential backoff (x3: 1s, 2s, 4s).
 */
@Slf4j
@Service
public class FileSender {

    @Value("${pukio.server.host}")
    private String serverHost;

    @Value("${pukio.server.port}")
    private int serverPort;

    @Value("${pukio.files.products}")
    private String productsFilePath;

    @Value("${pukio.files.inventory}")
    private String inventoryFilePath;

    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 1000;

    /**
     * Read and send both indexed files (products + inventory) to the server.
     * Implements retry x3 with exponential backoff. (REQ 1.3)
     */
    public void sendFiles() {
        log.info("Starting file transmission to {}:{} at {}",
                serverHost, serverPort, LocalDateTime.now());

        Path productsData = Paths.get(productsFilePath + ".dat");
        Path productsIndex = Paths.get(productsFilePath + ".idx");
        Path inventoryData = Paths.get(inventoryFilePath + ".dat");
        Path inventoryIndex = Paths.get(inventoryFilePath + ".idx");

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("Attempt {}/{} — connecting to {}:{}",
                        attempt, MAX_RETRIES, serverHost, serverPort);

                sendWithConnection(productsData, productsIndex,
                        inventoryData, inventoryIndex);

                log.info("Transmission successful on attempt {}", attempt);
                return;

            } catch (IOException e) {
                log.error("Attempt {}/{} failed: {}", attempt, MAX_RETRIES, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    long waitMs = BASE_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
                    log.info("Retrying in {} ms...", waitMs);
                    try {
                        Thread.sleep(waitMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry sleep interrupted", ie);
                        return;
                    }
                } else {
                    log.error("All {} attempts failed. Transmission aborted.", MAX_RETRIES);
                }
            }
        }
    }

    /**
     * Open a single TCP connection and transmit all files,
     * then wait for ACK/NACK from the server. (REQ 1.3)
     */
    private void sendWithConnection(Path productsData, Path productsIndex,
                                     Path inventoryData, Path inventoryIndex)
            throws IOException {

        try (Socket socket = new Socket(serverHost, serverPort);
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            // Send products .dat file
            long productRecords = sendFile(dos, productsData, "products.dat");

            // Send products .idx file
            sendFile(dos, productsIndex, "products.idx");

            // Send inventory .dat file
            long inventoryRecords = sendFile(dos, inventoryData, "inventory.dat");

            // Send inventory .idx file
            sendFile(dos, inventoryIndex, "inventory.idx");

            // Signal end of transmission
            dos.writeUTF("END");
            dos.flush();

            log.info("Files sent — products: {} bytes, inventory: {} bytes. Waiting for ACK...",
                    productRecords, inventoryRecords);

            // Receive ACK or NACK from Update_Service (REQ 1.3)
            String response = dis.readUTF();
            if (response.startsWith("ACK")) {
                log.info("ACK received from server: {}", response);
            } else if (response.startsWith("NACK")) {
                throw new IOException("NACK received from server: " + response);
            } else {
                throw new IOException("Unknown response from server: " + response);
            }
        }
    }

    /**
     * Send a single file over the DataOutputStream.
     * Protocol: filename (UTF), file size (long), file bytes.
     * Returns the size in bytes of the file sent.
     */
    private long sendFile(DataOutputStream dos, Path filePath, String logicalName)
            throws IOException {

        if (!Files.exists(filePath)) {
            log.warn("File not found, sending empty placeholder: {}", filePath);
            dos.writeUTF(logicalName);
            dos.writeLong(0L);
            dos.flush();
            return 0L;
        }

        byte[] fileBytes = Files.readAllBytes(filePath);
        dos.writeUTF(logicalName);
        dos.writeLong(fileBytes.length);
        dos.write(fileBytes);
        dos.flush();

        log.debug("Sent file: {} ({} bytes)", logicalName, fileBytes.length);
        return fileBytes.length;
    }
}
