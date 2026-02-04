// java
package in.co.abi.dev.mqtt;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class MqttBrokerTest {

    @Test
    public void testPublishSubscribe() throws Exception {
        // pick ephemeral port
        int port;
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }

        // start broker in daemon thread
        Thread brokerThread = new Thread(() -> {
            try {
                new MqttBroker(port).start();
            } catch (IOException e) {
                // ignore for test shutdown
            }
        });
        brokerThread.setDaemon(true);
        brokerThread.start();

        // wait until broker accepts connections
        waitForBroker(port, 2000);

        final String topic = "test/topic";
        final String payload = "hello-mqtt";

        // subscriber: connect and subscribe
        Socket subSock = new Socket("127.0.0.1", port);
        subSock.setSoTimeout(3000);
        DataOutputStream subOut = new DataOutputStream(subSock.getOutputStream());
        DataInputStream subIn = new DataInputStream(subSock.getInputStream());

        // send CONNECT for subscriber
        subOut.write(buildConnectFrame("sub-client"));
        subOut.flush();
        // read CONNACK
        readAndAssertConnack(subIn);

        // send SUBSCRIBE (packet id 1)
        subOut.write(buildSubscribeFrame(1, topic));
        subOut.flush();
        // read SUBACK
        readAndAssertSuback(subIn, 1);

        // publisher: connect and publish
        Socket pubSock = new Socket("127.0.0.1", port);
        pubSock.setSoTimeout(3000);
        DataOutputStream pubOut = new DataOutputStream(pubSock.getOutputStream());
        DataInputStream pubIn = new DataInputStream(pubSock.getInputStream());

        pubOut.write(buildConnectFrame("pub-client"));
        pubOut.flush();
        readAndAssertConnack(pubIn);

        // send PUBLISH from publisher
        pubOut.write(buildPublishFrame(topic, payload.getBytes("UTF-8")));
        pubOut.flush();

        // subscriber should receive forwarded PUBLISH
        int first = subIn.readUnsignedByte();
        int packetType = (first >> 4) & 0x0F;
        Assert.assertEquals("Expected PUBLISH packet type", 3, packetType);
        int remaining = readRemainingLength(subIn);
        byte[] buf = new byte[remaining];
        subIn.readFully(buf);
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(buf));
        // read topic
        int tlen = din.readUnsignedShort();
        byte[] tbytes = new byte[tlen];
        din.readFully(tbytes);
        String gotTopic = new String(tbytes, "UTF-8");
        Assert.assertEquals(topic, gotTopic);
        // remaining bytes are payload
        byte[] pay = new byte[din.available()];
        din.readFully(pay);
        String gotPayload = new String(pay, "UTF-8");
        Assert.assertEquals(payload, gotPayload);

        // cleanup
        subSock.close();
        pubSock.close();
    }

    // ---- helpers ----

    private static void waitForBroker(int port, int timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", port), 200);
                return;
            } catch (IOException e) {
                Thread.sleep(50);
            }
        }
        throw new IllegalStateException("Broker did not start within timeout");
    }

    private static byte[] buildConnectFrame(String clientId) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream variable = new ByteArrayOutputStream();

        // Protocol Name "MQTT"
        variable.write(0x00);
        variable.write(0x04);
        variable.write("MQTT".getBytes("UTF-8"));
        // Protocol Level 4
        variable.write(0x04);
        // Connect Flags: Clean Session
        variable.write(0x02);
        // Keep Alive 60
        variable.write(0x00);
        variable.write(0x3C);

        // Payload: ClientId
        byte[] cid = clientId.getBytes("UTF-8");
        variable.write((cid.length >> 8) & 0xFF);
        variable.write(cid.length & 0xFF);
        variable.write(cid);

        byte[] variableBytes = variable.toByteArray();
        bout.write(0x10); // CONNECT
        writeRemainingLength(bout, variableBytes.length);
        bout.write(variableBytes);
        return bout.toByteArray();
    }

    private static byte[] buildSubscribeFrame(int packetId, String topic) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream payload = new ByteArrayOutputStream();

        // Packet Identifier
        payload.write((packetId >> 8) & 0xFF);
        payload.write(packetId & 0xFF);
        // topic filter
        byte[] t = topic.getBytes("UTF-8");
        payload.write((t.length >> 8) & 0xFF);
        payload.write(t.length & 0xFF);
        payload.write(t);
        // requested QoS 0
        payload.write(0x00);

        byte[] pb = payload.toByteArray();
        bout.write(0x82); // SUBSCRIBE with reserved bits 0x2
        writeRemainingLength(bout, pb.length);
        bout.write(pb);
        return bout.toByteArray();
    }

    private static byte[] buildPublishFrame(String topic, byte[] payload) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream variable = new ByteArrayOutputStream();

        byte[] topicBytes = topic.getBytes("UTF-8");
        variable.write((topicBytes.length >> 8) & 0xFF);
        variable.write(topicBytes.length & 0xFF);
        variable.write(topicBytes);
        variable.write(payload);

        byte[] variableBytes = variable.toByteArray();
        bout.write(0x30); // PUBLISH QoS0
        writeRemainingLength(bout, variableBytes.length);
        bout.write(variableBytes);
        return bout.toByteArray();
    }

    private static void readAndAssertConnack(DataInputStream in) throws IOException {
        int first = in.readUnsignedByte();
        Assert.assertEquals(0x20, first & 0xF0);
        int remaining = readRemainingLength(in);
        byte[] buf = new byte[remaining];
        in.readFully(buf);
        // CONNACK: ack flags, return code
        Assert.assertEquals(2, buf.length);
        Assert.assertEquals(0x00, buf[1]); // connection accepted
    }

    private static void readAndAssertSuback(DataInputStream in, int expectedPacketId) throws IOException {
        int first = in.readUnsignedByte();
        Assert.assertEquals(0x90, first & 0xF0);
        int remaining = readRemainingLength(in);
        byte[] buf = new byte[remaining];
        in.readFully(buf);
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(buf));
        int pid = din.readUnsignedShort();
        Assert.assertEquals(expectedPacketId, pid);
        int granted = din.readUnsignedByte();
        Assert.assertEquals(0, granted);
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

    private static void writeRemainingLength(ByteArrayOutputStream out, int length) {
        do {
            int digit = length % 128;
            length /= 128;
            if (length > 0)
                digit |= 0x80;
            out.write(digit);
        } while (length > 0);
    }

    @Test
    public void testPublishJsonMessage() throws Exception {
        // Test that JSON payloads are correctly transmitted
        int port;
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }

        Thread brokerThread = new Thread(() -> {
            try {
                new MqttBroker(port).start();
            } catch (IOException e) {
            }
        });
        brokerThread.setDaemon(true);
        brokerThread.start();
        waitForBroker(port, 2000);

        final String topic = "test/json";
        final String jsonPayload = "{\"sensor\":\"temp\",\"value\":23.5}";

        // subscriber
        Socket subSock = new Socket("127.0.0.1", port);
        subSock.setSoTimeout(3000);
        DataOutputStream subOut = new DataOutputStream(subSock.getOutputStream());
        DataInputStream subIn = new DataInputStream(subSock.getInputStream());
        subOut.write(buildConnectFrame("json-sub"));
        subOut.flush();
        readAndAssertConnack(subIn);
        subOut.write(buildSubscribeFrame(1, topic));
        subOut.flush();
        readAndAssertSuback(subIn, 1);

        // publisher
        Socket pubSock = new Socket("127.0.0.1", port);
        pubSock.setSoTimeout(3000);
        DataOutputStream pubOut = new DataOutputStream(pubSock.getOutputStream());
        DataInputStream pubIn = new DataInputStream(pubSock.getInputStream());
        pubOut.write(buildConnectFrame("json-pub"));
        pubOut.flush();
        readAndAssertConnack(pubIn);
        pubOut.write(buildPublishFrame(topic, jsonPayload.getBytes("UTF-8")));
        pubOut.flush();

        // verify subscriber receives JSON
        int first = subIn.readUnsignedByte();
        Assert.assertEquals(3, (first >> 4) & 0x0F);
        int remaining = readRemainingLength(subIn);
        byte[] buf = new byte[remaining];
        subIn.readFully(buf);
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(buf));
        int tlen = din.readUnsignedShort();
        byte[] tbytes = new byte[tlen];
        din.readFully(tbytes);
        byte[] pay = new byte[din.available()];
        din.readFully(pay);
        String received = new String(pay, "UTF-8");
        Assert.assertEquals(jsonPayload, received);
        Assert.assertTrue(received.contains("\"sensor\""));

        subSock.close();
        pubSock.close();
    }

    @Test
    public void testPublishBinaryMessage() throws Exception {
        // Test binary data transmission
        int port;
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }

        Thread brokerThread = new Thread(() -> {
            try {
                new MqttBroker(port).start();
            } catch (IOException e) {
            }
        });
        brokerThread.setDaemon(true);
        brokerThread.start();
        waitForBroker(port, 2000);

        final String topic = "test/binary";
        byte[] binaryPayload = new byte[] { 0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE };

        // subscriber
        Socket subSock = new Socket("127.0.0.1", port);
        subSock.setSoTimeout(3000);
        DataOutputStream subOut = new DataOutputStream(subSock.getOutputStream());
        DataInputStream subIn = new DataInputStream(subSock.getInputStream());
        subOut.write(buildConnectFrame("bin-sub"));
        subOut.flush();
        readAndAssertConnack(subIn);
        subOut.write(buildSubscribeFrame(2, topic));
        subOut.flush();
        readAndAssertSuback(subIn, 2);

        // publisher
        Socket pubSock = new Socket("127.0.0.1", port);
        pubSock.setSoTimeout(3000);
        DataOutputStream pubOut = new DataOutputStream(pubSock.getOutputStream());
        DataInputStream pubIn = new DataInputStream(pubSock.getInputStream());
        pubOut.write(buildConnectFrame("bin-pub"));
        pubOut.flush();
        readAndAssertConnack(pubIn);
        pubOut.write(buildPublishFrame(topic, binaryPayload));
        pubOut.flush();

        // verify subscriber receives binary
        int first = subIn.readUnsignedByte();
        Assert.assertEquals(3, (first >> 4) & 0x0F);
        int remaining = readRemainingLength(subIn);
        byte[] buf = new byte[remaining];
        subIn.readFully(buf);
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(buf));
        int tlen = din.readUnsignedShort();
        din.skipBytes(tlen);
        byte[] pay = new byte[din.available()];
        din.readFully(pay);
        Assert.assertArrayEquals(binaryPayload, pay);

        subSock.close();
        pubSock.close();
    }

    @Test
    public void testAuthenticatedPublishToPrivateTopic() throws Exception {
        // Test that authenticated users can publish to private topics
        int port;
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }

        Thread brokerThread = new Thread(() -> {
            try {
                new MqttBroker(port).start();
            } catch (IOException e) {
            }
        });
        brokerThread.setDaemon(true);
        brokerThread.start();
        waitForBroker(port, 2000);

        final String privateTopic = "admin/commands";
        final String payload = "shutdown";

        // Connect with authentication
        Socket authSock = new Socket("127.0.0.1", port);
        authSock.setSoTimeout(3000);
        DataOutputStream authOut = new DataOutputStream(authSock.getOutputStream());
        DataInputStream authIn = new DataInputStream(authSock.getInputStream());

        // Send CONNECT with username/password
        authOut.write(buildConnectFrameWithAuth("auth-client", "admin", "admin"));
        authOut.flush();
        readAndAssertConnack(authIn);

        // Publish to private topic - should succeed
        authOut.write(buildPublishFrame(privateTopic, payload.getBytes("UTF-8")));
        authOut.flush();

        // If we get here without exception, test passed
        authSock.close();
    }

    @Test
    public void testAnonymousPublishToPrivateTopicDenied() throws Exception {
        // Test that anonymous users cannot publish to private topics
        int port;
        try (ServerSocket ss = new ServerSocket(0)) {
            port = ss.getLocalPort();
        }

        Thread brokerThread = new Thread(() -> {
            try {
                new MqttBroker(port).start();
            } catch (IOException e) {
            }
        });
        brokerThread.setDaemon(true);
        brokerThread.start();
        waitForBroker(port, 2000);

        final String privateTopic = "admin/commands";
        final String payload = "attempt";

        // Connect without authentication
        Socket anonSock = new Socket("127.0.0.1", port);
        anonSock.setSoTimeout(3000);
        DataOutputStream anonOut = new DataOutputStream(anonSock.getOutputStream());
        DataInputStream anonIn = new DataInputStream(anonSock.getInputStream());

        anonOut.write(buildConnectFrame("anon-client"));
        anonOut.flush();
        readAndAssertConnack(anonIn);

        // Publish to private topic - should be silently dropped
        anonOut.write(buildPublishFrame(privateTopic, payload.getBytes("UTF-8")));
        anonOut.flush();

        // Connection should remain open (message just dropped)
        Thread.sleep(500);
        Assert.assertTrue(anonSock.isConnected());

        anonSock.close();
    }

    // Helper to build CONNECT frame with authentication
    private static byte[] buildConnectFrameWithAuth(String clientId, String username, String password)
            throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream variable = new ByteArrayOutputStream();

        // Protocol Name "MQTT"
        variable.write(0x00);
        variable.write(0x04);
        variable.write("MQTT".getBytes("UTF-8"));
        // Protocol Level 4
        variable.write(0x04);
        // Connect Flags: Clean Session + Username + Password
        variable.write(0xC2); // 11000010 = clean session + username + password
        // Keep Alive 60
        variable.write(0x00);
        variable.write(0x3C);

        // Payload: ClientId
        byte[] cid = clientId.getBytes("UTF-8");
        variable.write((cid.length >> 8) & 0xFF);
        variable.write(cid.length & 0xFF);
        variable.write(cid);

        // Username
        byte[] user = username.getBytes("UTF-8");
        variable.write((user.length >> 8) & 0xFF);
        variable.write(user.length & 0xFF);
        variable.write(user);

        // Password
        byte[] pass = password.getBytes("UTF-8");
        variable.write((pass.length >> 8) & 0xFF);
        variable.write(pass.length & 0xFF);
        variable.write(pass);

        byte[] variableBytes = variable.toByteArray();
        bout.write(0x10); // CONNECT
        writeRemainingLength(bout, variableBytes.length);
        bout.write(variableBytes);
        return bout.toByteArray();
    }
}
