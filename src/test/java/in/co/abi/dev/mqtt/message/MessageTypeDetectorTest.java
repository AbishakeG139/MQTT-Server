package in.co.abi.dev.mqtt.message;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for MessageTypeDetector.
 */
public class MessageTypeDetectorTest {

    @Test
    public void testDetectJson_Object() {
        byte[] payload = "{\"name\":\"test\",\"value\":123}".getBytes();
        assertEquals(MessageType.JSON, MessageTypeDetector.detect(payload));
    }

    @Test
    public void testDetectJson_Array() {
        byte[] payload = "[1, 2, 3]".getBytes();
        assertEquals(MessageType.JSON, MessageTypeDetector.detect(payload));
    }

    @Test
    public void testDetectJson_NestedObject() {
        byte[] payload = "{\"user\":{\"id\":1,\"name\":\"test\"},\"items\":[1,2,3]}".getBytes();
        assertEquals(MessageType.JSON, MessageTypeDetector.detect(payload));
    }

    @Test
    public void testDetectJson_WithWhitespace() {
        byte[] payload = "   { \"name\": \"test\" }  ".getBytes();
        assertEquals(MessageType.JSON, MessageTypeDetector.detect(payload));
    }

    @Test
    public void testDetectString_PlainText() {
        byte[] payload = "Hello, World!".getBytes();
        assertEquals(MessageType.STRING, MessageTypeDetector.detect(payload));
    }

    @Test
    public void testDetectString_MultiLine() {
        byte[] payload = "Line 1\nLine 2\nLine 3".getBytes();
        assertEquals(MessageType.STRING, MessageTypeDetector.detect(payload));
    }

    @Test
    public void testDetectString_Unicode() {
        byte[] payload = "こんにちは世界".getBytes();
        assertEquals(MessageType.STRING, MessageTypeDetector.detect(payload));
    }

    @Test
    public void testDetectBytes_Binary() {
        // Binary data with invalid UTF-8 sequences
        byte[] payload = new byte[] { 0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE, (byte) 0xFD };
        assertEquals(MessageType.BYTES, MessageTypeDetector.detect(payload));
    }

    @Test
    public void testDetectBytes_MixedBinary() {
        // Mostly binary with some text-like bytes
        byte[] payload = new byte[100];
        for (int i = 0; i < 100; i++) {
            payload[i] = (byte) (i * 3); // Creates non-text binary sequence
        }
        assertEquals(MessageType.BYTES, MessageTypeDetector.detect(payload));
    }

    @Test
    public void testDetectBytes_Empty() {
        assertEquals(MessageType.BYTES, MessageTypeDetector.detect(new byte[0]));
    }

    @Test
    public void testDetectBytes_Null() {
        assertEquals(MessageType.BYTES, MessageTypeDetector.detect(null));
    }

    @Test
    public void testDetectString_InvalidJson() {
        // Starts with { but is not valid JSON - should be STRING
        byte[] payload = "{this is not json}".getBytes();
        assertEquals(MessageType.STRING, MessageTypeDetector.detect(payload));
    }

    @Test
    public void testDetectString_PartialJson() {
        byte[] payload = "{\"name\": \"test\"".getBytes(); // Missing closing brace
        assertEquals(MessageType.STRING, MessageTypeDetector.detect(payload));
    }
}
