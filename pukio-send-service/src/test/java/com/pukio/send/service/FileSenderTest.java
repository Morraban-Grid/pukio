package com.pukio.send.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class FileSenderTest {

    @TempDir
    Path tempDir;

    private FileSender fileSender;

    @BeforeEach
    void setUp() {
        fileSender = new FileSender();
    }

    /**
     * Test: successful transmission with a mock server that sends ACK.
     */
    @Test
    void sendFiles_mockServerACK_shouldSucceed() throws Exception {
        // Create dummy data files
        Path productsBase = tempDir.resolve("products");
        Path inventoryBase = tempDir.resolve("inventory");

        Files.write(productsBase.resolveSibling("products.dat"),
                "PRODUCTS_DATA".getBytes());
        Files.write(productsBase.resolveSibling("products.idx"),
                "PRODUCTS_IDX".getBytes());
        Files.write(inventoryBase.resolveSibling("inventory.dat"),
                "INVENTORY_DATA".getBytes());
        Files.write(inventoryBase.resolveSibling("inventory.idx"),
                "INVENTORY_IDX".getBytes());

        // Start mock TCP server on a random available port
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            // Inject test values via ReflectionTestUtils
            ReflectionTestUtils.setField(fileSender, "serverHost", "localhost");
            ReflectionTestUtils.setField(fileSender, "serverPort", port);
            ReflectionTestUtils.setField(fileSender, "productsFilePath",
                    tempDir.resolve("products").toString());
            ReflectionTestUtils.setField(fileSender, "inventoryFilePath",
                    tempDir.resolve("inventory").toString());

            // Mock server: accept connection, drain input, send ACK
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> serverTask = executor.submit(() -> {
                try (Socket client = serverSocket.accept();
                     DataInputStream dis = new DataInputStream(new BufferedInputStream(client.getInputStream()));
                     DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()))) {

                    // Drain all 4 files + END signal
                    String token;
                    while (!(token = dis.readUTF()).equals("END")) {
                        long size = dis.readLong();
                        if (size > 0) dis.skipNBytes(size);
                    }

                    // Send ACK
                    dos.writeUTF("ACK:4 files received");
                    dos.flush();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Run sender (should not throw)
            assertDoesNotThrow(() -> fileSender.sendFiles());

            serverTask.get(5, TimeUnit.SECONDS);
            executor.shutdown();
        }
    }

    /**
     * Test: server sends NACK — sender should log error but not throw.
     */
    @Test
    void sendFiles_mockServerNACK_shouldHandleGracefully() throws Exception {
        Path productsBase = tempDir.resolve("products");

        Files.write(tempDir.resolve("products.dat"), "DATA".getBytes());
        Files.write(tempDir.resolve("products.idx"), "IDX".getBytes());
        Files.write(tempDir.resolve("inventory.dat"), "DATA".getBytes());
        Files.write(tempDir.resolve("inventory.idx"), "IDX".getBytes());

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            ReflectionTestUtils.setField(fileSender, "serverHost", "localhost");
            ReflectionTestUtils.setField(fileSender, "serverPort", port);
            ReflectionTestUtils.setField(fileSender, "productsFilePath",
                    tempDir.resolve("products").toString());
            ReflectionTestUtils.setField(fileSender, "inventoryFilePath",
                    tempDir.resolve("inventory").toString());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try (Socket client = serverSocket.accept();
                     DataInputStream dis = new DataInputStream(new BufferedInputStream(client.getInputStream()));
                     DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()))) {

                    String token;
                    while (!(token = dis.readUTF()).equals("END")) {
                        long size = dis.readLong();
                        if (size > 0) dis.skipNBytes(size);
                    }

                    dos.writeUTF("NACK:checksum mismatch");
                    dos.flush();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            // Should not throw — NACK is handled internally with logging
            assertDoesNotThrow(() -> fileSender.sendFiles());

            executor.shutdown();
        }
    }

    /**
     * Test: server unreachable — retry x3 with exponential backoff,
     * then give up gracefully without throwing. (REQ 1.3)
     */
    @Test
    void sendFiles_serverUnreachable_shouldRetryAndGiveUp() {
        ReflectionTestUtils.setField(fileSender, "serverHost", "localhost");
        ReflectionTestUtils.setField(fileSender, "serverPort", 19999);
        ReflectionTestUtils.setField(fileSender, "productsFilePath",
                tempDir.resolve("products").toString());
        ReflectionTestUtils.setField(fileSender, "inventoryFilePath",
                tempDir.resolve("inventory").toString());

        // Should not throw — all retries exhausted gracefully
        assertDoesNotThrow(() -> fileSender.sendFiles());
    }
}
