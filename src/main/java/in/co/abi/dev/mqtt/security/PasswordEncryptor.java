package in.co.abi.dev.mqtt.security;

/**
 * Simple utility to generate encrypted password for properties file.
 * Run this to encrypt your password, then copy the output to
 * mqtt-server.properties
 */
public class PasswordEncryptor {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java PasswordEncryptor <password>");
            System.out.println("\nExample:");
            System.out.println("  java PasswordEncryptor admin123");
            return;
        }

        String password = args[0];
        String encrypted = CryptoUtil.encrypt(password);

        System.out.println("=== Password Encryption ===");
        System.out.println("Plaintext:  " + password);
        System.out.println("Encrypted:  " + encrypted);
        System.out.println("\nCopy the encrypted value to mqtt.auth.password in mqtt-server.properties");
    }
}
