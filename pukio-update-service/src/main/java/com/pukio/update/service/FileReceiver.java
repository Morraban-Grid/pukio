package com.pukio.update.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP server that listens for incoming file transmissions from Send_Service.
 * Each connection is handled in a separate thread. (TASK-E1-37)
 * Sends ACK or NACK back to the sender. (TASK-E1-45)
 * Logs every received file with origin, timestamp, and record counts. (TASK-E1-46)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileReceiver {

    @Value("${pukio.server.port}")
    private int serverPort;

    @Value("${pukio.store.id:STORE-001}")
    private String storeId;

    private final SyncService syncService;

    private final ExecutorService threadPool =
            Executors.newFixedThreadPool(10);

    /**
     * Start the TCP server and listen for incoming connections indefinitely.
     */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            log.info("Update Service listening on port {} at {}",
                    serverPort, LocalDateTime.now());

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                log.info("Accepted connection from {}",
                        clientSocket.getInetAddress().getHostAddress());

                threadPool.submit(() -> handleConnection(clientSocket));
            }
        }
    }

    /**
     * Handle a single client connection:
     * receive files, sync to DB, send ACK or NACK.
     */
    private void handleConnection(Socket socket) {
        String clientAddr = socket.getInetAddress().getHostAddress();
        log.info("Handling connection from {} at {}", clientAddr, LocalDateTime.now());

        try (socket;
             DataInputStream dis = new DataInputStream(
                     new BufferedInputStream(socket.getInputStream()));
             DataOutputStream dos = new DataOutputStream(
                     new BufferedOutputStream(socket.getOutputStream()))) {

            // Receive all files into memory map
            Map<String, byte[]> fileMap = receiveFiles(dis);

            // Log received files (TASK-E1-46)
            fileMap.forEach((name, bytes) ->
                    log.info("Received file: name={}, size={} bytes, from={}, at={}",
                            name, bytes.length, clientAddr, LocalDateTime.now()));

            // Sync to database inside single transaction (TASK-E1-43, TASK-E1-44)
            Map<String, Integer> counts = syncService.syncAll(fileMap, storeId);
            int totalRecords = counts.values().stream().mapToInt(Integer::intValue).sum();

            // Send ACK (TASK-E1-45)
            String ack = String.format("ACK:%d records processed (products=%d, inventory=%d)",
                    totalRecords, counts.get("products"), counts.get("inventory"));
            dos.writeUTF(ack);
            dos.flush();

            log.info("ACK sent to {}: {}", clientAddr, ack);

        } catch (Exception e) {
            log.error("Error handling connection from {}: {}", clientAddr, e.getMessage(), e);

            // Try to send NACK
            try (DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(socket.getOutputStream()))) {
                dos.writeUTF("NACK:" + e.getMessage());
                dos.flush();
                log.info("NACK sent to {}", clientAddr);
            } catch (IOException ex) {
                log.error("Failed to send NACK to {}", clientAddr, ex);
            }
        }
    }

    /**
     * Read all files from the DataInputStream until END signal.
     * Protocol mirrors FileSender: UTF filename + long size + bytes.
     */
    private Map<String, byte[]> receiveFiles(DataInputStream dis) throws IOException {
        Map<String, byte[]> fileMap = new HashMap<>();
        String token;

        while (!(token = dis.readUTF()).equals("END")) {
            String filename = token;
            long size = dis.readLong();

            byte[] data = new byte[0];
            if (size > 0) {
                data = dis.readNBytes((int) size);
            }

            fileMap.put(filename, data);
            log.debug("Received chunk: filename={}, size={}", filename, size);
        }

        return fileMap;
    }
}
