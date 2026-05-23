# Pukio Send Service

Independent service that transmits local indexed files to the Data_Server via TCP socket connection.

## Overview

The Send Service is a standalone Spring Boot application that:
- Reads local indexed files (products and inventory)
- Establishes TCP connection to the central Data_Server
- Transmits file contents using a custom binary protocol
- Implements retry logic with exponential backoff (3 attempts: 1s, 2s, 4s)
- Receives ACK/NACK confirmation from the Update_Service

## Requirements Implemented

- **REQ 1.3**: Transfer indexed files to central server
- **TASK-E1-28**: Standalone executable JAR with main class
- **TASK-E1-29**: Read complete indexed files (products + inventory)
- **TASK-E1-30**: TCP socket connection with configurable host/port
- **TASK-E1-31**: File transmission via OutputStream
- **TASK-E1-32**: ACK/NACK reception via InputStream
- **TASK-E1-33**: Retry x3 with exponential backoff
- **TASK-E1-34**: Comprehensive logging (timestamp, host, status, records)
- **TASK-E1-35**: Unit tests with mocked TCP server

## Configuration

### application.properties
Contains non-sensitive configuration with placeholders:
```properties
pukio.server.host=${SERVER_HOST}
pukio.server.port=${SERVER_PORT}
pukio.files.products=${PUKIO_FILES_PRODUCTS}
pukio.files.inventory=${PUKIO_FILES_INVENTORY}
```

### application-secrets.properties (NOT committed)
Create this file locally from the template:
```bash
cp src/main/resources/application-secrets.properties.template \
   src/main/resources/application-secrets.properties
```

Then fill in the actual values:
```properties
SERVER_HOST=192.168.1.100
SERVER_PORT=9090
PUKIO_FILES_PRODUCTS=/var/pukio/data/products
PUKIO_FILES_INVENTORY=/var/pukio/data/inventory
```

## File Protocol

The service sends 4 files in sequence:
1. `products.dat` - Product data file
2. `products.idx` - Product index file
3. `inventory.dat` - Inventory data file
4. `inventory.idx` - Inventory index file

Each file is transmitted as:
- Filename (UTF-8 string)
- File size (long, 8 bytes)
- File content (byte array)

After all files, sends "END" signal and waits for server response.

## Running the Service

### Build
```bash
mvn clean package
```

### Run
```bash
java -jar target/pukio-send-service-1.0.0-SNAPSHOT.jar
```

The service will:
1. Start up
2. Attempt to send files
3. Log all transmission attempts
4. Exit after completion or failure

## Testing

Run unit tests:
```bash
mvn test
```

Tests include:
- Successful transmission with ACK
- Server NACK handling
- Connection failure with retry logic
- Exponential backoff verification

## Security

⚠️ **NEVER commit `application-secrets.properties` to Git**

This file contains:
- Server IP addresses
- Port numbers
- File system paths

All sensitive values are injected via environment variables or the secrets file.

## Logging

The service logs:
- Connection attempts with retry count
- File transmission progress
- Server responses (ACK/NACK)
- Error details with stack traces
- Transmission timestamps

Log level can be configured in `application.properties`:
```properties
logging.level.com.pukio.send=DEBUG
```

## Architecture

```
SendServiceApplication (main)
    └── FileSender (service)
        ├── sendFiles() - Main orchestration
        ├── sendWithConnection() - TCP transmission
        └── sendFile() - Individual file sender
```

## Dependencies

- Spring Boot 3.3.5
- Java 21
- Lombok (for logging)
- JUnit 5 (testing)

No dependency on `pukio-common` - this module operates independently.
