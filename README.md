# Java MQTT Broker (MQTT-Server)

A robust, custom MQTT broker implementation in Java featuring advanced security, message handling, and configurable properties.

## Features

### 1. Message Type Handling
The broker automatically detects and handles three types of messages:
- **JSON**: Validated and parsed using Jackson.
- **String**: UTF-8 encoded text.
- **Bytes**: Raw binary data.

Message types are detected automatically based on content analysis.

### 2. High Security
- **AES-256 Encryption**: User passwords in the configuration file are encrypted using AES-256-GCM.
- **Authentication**: Username/Password-based authentication.
- **Authorization**: Support for **Private Topics**.
    - **Private Topics**: Require strict authentication for Publish/Subscribe.
    - **Public Topics**: Access controlled via configuration (allow/deny anonymous).
- **Secure Password Storage**: Passwords are never stored in plaintext.

### 3. Configurable
All settings are managed via `src/main/resources/mqtt-server.properties`.

## Configuration (`mqtt-server.properties`)

```properties
# Network Configuration
mqtt.port=1883
mqtt.host=0.0.0.0
mqtt.maxConnections=100
mqtt.allowAnonymous=true

# Authentication
mqtt.auth.enabled=true
mqtt.auth.username=admin
# Encrypted Password (AES-256)
mqtt.auth.password=DlOAIcmaFev4XW5iDvqWOuIwA01XX71DjlZBoyQfzchH

# Private Topics
# Comma-separated list of topics that require authentication
mqtt.private.topics=admin/commands,system/config,private/data
```

## Getting Started

### Prerequisites
- Java 8+
- Maven

### Build & Run
```bash
mvn clean compile exec:java -Dexec.mainClass="in.co.abi.dev.mqtt.MqttBroker"
```

### Generating Encrypted Passwords
To generate a new encrypted password for the configuration file:

```bash
mvn compile exec:java -Dexec.mainClass="in.co.abi.dev.mqtt.security.PasswordEncryptor" -Dexec.args="YOUR_PLAINTEXT_PASSWORD"
```
Copy the output `Encrypted` string to the `mqtt.auth.password` field in `mqtt-server.properties`.

## Dependencies
- **Log4j 2**: Logging framework.
- **Jackson**: JSON processing.
- **JUnit 4**: Testing.

## Project Structure
- `in.co.abi.dev.mqtt`: Core broker logic.
- `in.co.abi.dev.mqtt.message`: Message type detection and handling.
- `in.co.abi.dev.mqtt.security`: AES encryption and Authentication management.
- `in.co.abi.dev.mqtt.properties`: Configuration management.
