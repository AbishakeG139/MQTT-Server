package in.co.abi.dev.mqtt;

import in.co.abi.dev.mqtt.message.DefaultMessageHandler;
import in.co.abi.dev.mqtt.message.MessageHandler;
import in.co.abi.dev.mqtt.message.MqttMessage;
import in.co.abi.dev.mqtt.security.AuthenticationManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {
    private static final Logger logger = LogManager.getLogger(ClientHandler.class);
    // topic -> list of subscribers
    private static final Map<String, CopyOnWriteArrayList<ClientSession>> TOPICS = new ConcurrentHashMap<>();
    // Message handler for type-specific processing
    private static final MessageHandler messageHandler = new DefaultMessageHandler();
    // Authentication manager
    private static final AuthenticationManager authManager = new AuthenticationManager();

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream in = socket.getInputStream()) {
            DataInputStream din = new DataInputStream(in);
            String clientId = socket.getRemoteSocketAddress().toString();
            ClientSession session = new ClientSession(socket, clientId);

            while (!socket.isClosed()) {
                int first = din.read();
                if (first < 0)
                    break;
                int packetType = (first >> 4) & 0x0F;

                int remaining = readRemainingLength(din);
                byte[] payload = new byte[remaining];
                din.readFully(payload);

                ByteArrayInputStream bin = new ByteArrayInputStream(payload);
                switch (packetType) {
                    case 1: // CONNECT
                        handleConnect(bin, session);
                        break;
                    case 3: // PUBLISH
                        handlePublish(first, bin, session);
                        break;
                    case 8: // SUBSCRIBE
                        handleSubscribe(payload, session);
                        break;
                    case 12: // PINGREQ
                        sendPingResp(session);
                        break;
                    default:
                        logger.warn("Unsupported packet type: {}", packetType);
                        break;
                }
            }
        } catch (IOException e) {
            logger.info("Client disconnected: {}", socket.getRemoteSocketAddress());
        } finally {
            removeSessionFromAllTopics(socket);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handleConnect(InputStream in, ClientSession session) throws IOException {
        DataInputStream din = new DataInputStream(in);
        int protoNameLen = din.readUnsignedShort();
        byte[] protoName = new byte[protoNameLen];
        din.readFully(protoName);
        int protoLevel = din.readUnsignedByte();
        int connectFlags = din.readUnsignedByte();
        int keepAlive = din.readUnsignedShort();

        String clientId = readUTF8(din);

        // Parse username and password if present
        String username = null;
        String password = null;
        boolean hasUsername = (connectFlags & 0x80) != 0;
        boolean hasPassword = (connectFlags & 0x40) != 0;

        if (hasUsername) {
            username = readUTF8(din);
        }
        if (hasPassword) {
            password = readUTF8(din);
        }

        // Authenticate
        boolean authenticated = false;
        byte returnCode = 0x00; // Connection Accepted

        if (authManager.isAuthEnabled()) {
            if (username != null && password != null) {
                authenticated = authManager.authenticate(username, password);
                if (!authenticated) {
                    returnCode = 0x04; // Bad username or password
                    logger.warn("Authentication failed for user '{}'", username);
                }
            } else if (!authManager.isAnonymousAllowed()) {
                returnCode = 0x05; // Not authorized
                logger.warn("Anonymous connection rejected (auth required)");
            }
        }

        // Update session
        if (authenticated) {
            session.setUsername(username);
            session.setAuthenticated(true);
        }

        // Send CONNACK
        byte[] connack = new byte[] { 0x20, 0x02, 0x00, returnCode };
        session.sendBytes(connack);

        if (returnCode == 0x00) {
            logger.info("Client CONNECTED: {} (proto={}, level={}, keepAlive={}, user={})",
                    clientId, new String(protoName), protoLevel, keepAlive,
                    username != null ? username : "anonymous");
        } else {
            // Close connection on auth failure
            socket.close();
        }
    }

    private void handlePublish(int firstByte, InputStream in, ClientSession session) throws IOException {
        DataInputStream din = new DataInputStream(in);
        int topicLen = din.readUnsignedShort();
        byte[] topicBytes = new byte[topicLen];
        din.readFully(topicBytes);
        String topic = new String(topicBytes, "UTF-8");

        // QoS 0 => no packet id
        int payloadLen = in.available();
        byte[] payload = new byte[payloadLen];
        din.readFully(payload);

        // Check authorization
        if (!authManager.canPublish(session.getUsername(), topic)) {
            logger.warn("Publish denied: user '{}' to topic '{}'",
                    session.getUsername() != null ? session.getUsername() : "anonymous", topic);
            // Silently drop or send DISCONNECT
            return;
        }

        // Create typed message wrapper with auto-detection
        MqttMessage message = new MqttMessage(topic, payload);

        // Log with type information
        logger.info("PUBLISH topic='{}' type={} size={} bytes from={}",
                topic, message.getType(), message.getPayloadSize(),
                session.getUsername() != null ? session.getUsername() : "anonymous");

        // Process message through type-specific handler
        messageHandler.handle(message, session);

        // forward to subscribers
        List<ClientSession> subs = TOPICS.get(topic);
        if (subs != null) {
            byte[] publishFrame = buildPublishFrame(topic, payload);
            for (ClientSession s : subs) {
                try {
                    s.sendBytes(publishFrame);
                } catch (IOException e) {
                    logger.warn("Failed to forward to {}: {}", s.getClientId(), e.getMessage());
                }
            }
        }
    }

    private void handleSubscribe(byte[] payload, ClientSession session) throws IOException {
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(payload));
        int packetId = din.readUnsignedShort();
        String topic = readUTF8(din);
        int requestedQos = din.readUnsignedByte();

        // Check authorization
        byte grantedQos = 0x00; // QoS 0
        if (!authManager.canSubscribe(session.getUsername(), topic)) {
            logger.warn("Subscribe denied: user '{}' to topic '{}'",
                    session.getUsername() != null ? session.getUsername() : "anonymous", topic);
            grantedQos = (byte) 0x80; // Failure
        } else {
            TOPICS.computeIfAbsent(topic, t -> new CopyOnWriteArrayList<>()).add(session);
            logger.info("Client {} subscribed to '{}'", session.getClientId(), topic);
        }

        // Send SUBACK
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bout.write(0x90);
        bout.write(0x03);
        bout.write((packetId >> 8) & 0xFF);
        bout.write(packetId & 0xFF);
        bout.write(grantedQos);
        session.sendBytes(bout.toByteArray());
    }

    private void sendPingResp(ClientSession session) throws IOException {
        byte[] resp = new byte[] { (byte) 0xD0, 0x00 };
        session.sendBytes(resp);
    }

    private static int readRemainingLength(DataInputStream in) throws IOException {
        int multiplier = 1;
        int value = 0;
        int digit;
        do {
            digit = in.readUnsignedByte();
            value += (digit & 127) * multiplier;
            multiplier *= 128;
            if (multiplier > 128 * 128 * 128)
                throw new IOException("Malformed Remaining Length");
        } while ((digit & 128) != 0);
        return value;
    }

    private static String readUTF8(DataInputStream din) throws IOException {
        int len = din.readUnsignedShort();
        byte[] b = new byte[len];
        din.readFully(b);
        return new String(b, "UTF-8");
    }

    private static byte[] buildPublishFrame(String topic, byte[] payload) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream variable = new ByteArrayOutputStream();

        byte[] topicBytes = topic.getBytes("UTF-8");
        variable.write((topicBytes.length >> 8) & 0xFF);
        variable.write(topicBytes.length & 0xFF);
        variable.write(topicBytes);
        // QoS 0 => no packet identifier
        variable.write(payload);

        byte[] variableBytes = variable.toByteArray();
        bout.write(0x30); // PUBLISH, QoS 0
        writeRemainingLength(bout, variableBytes.length);
        bout.write(variableBytes);
        return bout.toByteArray();
    }

    private static void writeRemainingLength(ByteArrayOutputStream out, int length) {
        do {
            int digit = length % 128;
            length /= 128;
            if (length > 0)
                digit |= 0x80;
            out.write(digit);
        } while (length > 0);
    }

    private void removeSessionFromAllTopics(Socket socket) {
        for (Map.Entry<String, CopyOnWriteArrayList<ClientSession>> e : TOPICS.entrySet()) {
            List<ClientSession> list = e.getValue();
            for (ClientSession s : list) {
                if (s != null && s.getClientId().equals(socket.getRemoteSocketAddress().toString())) {
                    list.remove(s);
                }
            }
        }
    }
}
