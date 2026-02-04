package in.co.abi.dev.mqtt.security;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for CryptoUtil AES-256 encryption.
 */
public class CryptoUtilTest {

    @Test
    public void testEncryptDecrypt_RoundTrip() {
        String plaintext = "admin";

        String encrypted = CryptoUtil.encrypt(plaintext);
        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty());
        assertNotEquals(plaintext, encrypted);

        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    public void testEncryptDecrypt_DifferentPasswords() {
        String password1 = "password1";
        String password2 = "password2";

        String encrypted1 = CryptoUtil.encrypt(password1);
        String encrypted2 = CryptoUtil.encrypt(password2);

        // Different plaintexts should produce different ciphertexts
        assertNotEquals(encrypted1, encrypted2);

        // Each should decrypt correctly
        assertEquals(password1, CryptoUtil.decrypt(encrypted1));
        assertEquals(password2, CryptoUtil.decrypt(encrypted2));
    }

    @Test
    public void testEncrypt_SameInputDifferentOutput() {
        // Due to random IV, same input should produce different ciphertext
        String plaintext = "test123";

        String encrypted1 = CryptoUtil.encrypt(plaintext);
        String encrypted2 = CryptoUtil.encrypt(plaintext);

        assertNotEquals(encrypted1, encrypted2);

        // But both should decrypt to same plaintext
        assertEquals(plaintext, CryptoUtil.decrypt(encrypted1));
        assertEquals(plaintext, CryptoUtil.decrypt(encrypted2));
    }

    @Test
    public void testEncryptDecrypt_EmptyString() {
        String plaintext = "";

        String encrypted = CryptoUtil.encrypt(plaintext);
        String decrypted = CryptoUtil.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    public void testEncryptDecrypt_SpecialCharacters() {
        String plaintext = "p@ssw0rd!#$%^&*()";

        String encrypted = CryptoUtil.encrypt(plaintext);
        String decrypted = CryptoUtil.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    public void testEncryptDecrypt_Unicode() {
        String plaintext = "パスワード123";

        String encrypted = CryptoUtil.encrypt(plaintext);
        String decrypted = CryptoUtil.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    public void testEncryptDecrypt_LongString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("a");
        }
        String plaintext = sb.toString();

        String encrypted = CryptoUtil.encrypt(plaintext);
        String decrypted = CryptoUtil.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncrypt_NullInput() {
        CryptoUtil.encrypt(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecrypt_NullInput() {
        CryptoUtil.decrypt(null);
    }

    @Test(expected = RuntimeException.class)
    public void testDecrypt_InvalidData() {
        CryptoUtil.decrypt("invalid-base64-data!!!");
    }

    @Test
    public void testGenerateKey() {
        String key = CryptoUtil.generateKey();

        assertNotNull(key);
        assertFalse(key.isEmpty());

        // Should be valid Base64
        byte[] decoded = java.util.Base64.getDecoder().decode(key);
        assertEquals(32, decoded.length); // 256 bits = 32 bytes
    }

    @Test
    public void testEncryptDecrypt_CustomKey() {
        String customKey = CryptoUtil.generateKey();
        String plaintext = "test-with-custom-key";

        String encrypted = CryptoUtil.encrypt(plaintext, customKey);
        String decrypted = CryptoUtil.decrypt(encrypted, customKey);

        assertEquals(plaintext, decrypted);
    }

    @Test(expected = RuntimeException.class)
    public void testDecrypt_WrongKey() {
        String key1 = CryptoUtil.generateKey();
        String key2 = CryptoUtil.generateKey();

        String plaintext = "secret";
        String encrypted = CryptoUtil.encrypt(plaintext, key1);

        // Should fail with wrong key
        CryptoUtil.decrypt(encrypted, key2);
    }
}
