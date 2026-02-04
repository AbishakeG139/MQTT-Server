package in.co.abi.dev.mqtt.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256 encryption/decryption utility using GCM mode for authenticated
 * encryption.
 * Thread-safe and provides both default key and custom key operations.
 */
public final class CryptoUtil {

    private static final Logger logger = LogManager.getLogger(CryptoUtil.class);

    // AES-256-GCM parameters
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // 128 bits authentication tag

    /**
     * Default AES key (Base64 encoded).
     * WARNING: In production, externalize this to environment variables or secure
     * key store!
     */
    private static final String DEFAULT_KEY_BASE64 = "YWJpMzEtbXF0dC1zZXJ2ZXItZGVmYXVsdC1hZXMtMjU2LWtleS0yMDI2"; // "abi31-mqtt-server-default-aes-256-key-2026"

    private static final SecretKey DEFAULT_KEY;

    static {
        try {
            // Decode the default key
            byte[] keyBytes = Base64.getDecoder().decode(DEFAULT_KEY_BASE64);
            // Ensure it's 32 bytes for AES-256
            byte[] key32 = new byte[32];
            System.arraycopy(keyBytes, 0, key32, 0, Math.min(keyBytes.length, 32));
            DEFAULT_KEY = new SecretKeySpec(key32, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize default encryption key", e);
        }
    }

    private CryptoUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Encrypts plaintext using the default AES key.
     *
     * @param plaintext The text to encrypt
     * @return Base64-encoded encrypted data (IV + ciphertext + tag)
     * @throws RuntimeException if encryption fails
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext cannot be null");
        }
        return encrypt(plaintext, DEFAULT_KEY);
    }

    /**
     * Decrypts encrypted data using the default AES key.
     *
     * @param encrypted Base64-encoded encrypted data
     * @return Decrypted plaintext
     * @throws RuntimeException if decryption fails
     */
    public static String decrypt(String encrypted) {
        if (encrypted == null) {
            throw new IllegalArgumentException("Encrypted data cannot be null");
        }
        return decrypt(encrypted, DEFAULT_KEY);
    }

    /**
     * Encrypts plaintext using a custom key.
     *
     * @param plaintext The text to encrypt
     * @param keyBase64 Base64-encoded AES key (must be 256 bits)
     * @return Base64-encoded encrypted data
     * @throws RuntimeException if encryption fails
     */
    public static String encrypt(String plaintext, String keyBase64) {
        SecretKey key = decodeKey(keyBase64);
        return encrypt(plaintext, key);
    }

    /**
     * Decrypts encrypted data using a custom key.
     *
     * @param encrypted Base64-encoded encrypted data
     * @param keyBase64 Base64-encoded AES key
     * @return Decrypted plaintext
     * @throws RuntimeException if decryption fails
     */
    public static String decrypt(String encrypted, String keyBase64) {
        SecretKey key = decodeKey(keyBase64);
        return decrypt(encrypted, key);
    }

    /**
     * Generates a new random AES-256 key.
     *
     * @return Base64-encoded key
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE, new SecureRandom());
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }

    // ========== Internal Implementation ==========

    private static String encrypt(String plaintext, SecretKey key) {
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            // Encrypt
            byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plainBytes);

            // Combine IV + ciphertext for storage
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            // Encode to Base64
            return Base64.getEncoder().encodeToString(buffer.array());

        } catch (Exception e) {
            logger.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    private static String decrypt(String encrypted, SecretKey key) {
        try {
            // Decode from Base64
            byte[] decoded = Base64.getDecoder().decode(encrypted);

            // Extract IV and ciphertext
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            // Decrypt
            byte[] plainBytes = cipher.doFinal(ciphertext);
            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    private static SecretKey decodeKey(String keyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("Key must be 256 bits (32 bytes)");
            }
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode key", e);
        }
    }

    /**
     * Main method for testing and generating encrypted passwords.
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            String password = args[0];
            String encrypted = encrypt(password);
            System.out.println("Plaintext: " + password);
            System.out.println("Encrypted: " + encrypted);
            System.out.println("Decrypted: " + decrypt(encrypted));
        } else {
            // Demo
            System.out.println("=== AES-256 Encryption Demo ===");
            String testPassword = "admin";
            String encrypted = encrypt(testPassword);
            System.out.println("Password: " + testPassword);
            System.out.println("Encrypted: " + encrypted);
            System.out.println("Decrypted: " + decrypt(encrypted));
            System.out.println("\nGenerated Key: " + generateKey());
            System.out.println("\nUsage: java CryptoUtil <password>");
        }
    }
}
