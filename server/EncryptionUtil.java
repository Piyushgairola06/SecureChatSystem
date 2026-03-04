package server;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * AES-128 CBC encryption shared between server and client.
 * Key and IV are hardcoded here for the academic phase.
 * In production, exchange the key via asymmetric encryption (RSA/DH).
 */
public class EncryptionUtil {

    // 16-byte key and IV for AES-128
    private static final String SECRET_KEY = "MySecureChatKey!";  // exactly 16 chars
    private static final String INIT_VECTOR = "RandomInitVector";  // exactly 16 chars

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    public static String encrypt(String message) {
        try {
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8"));
            SecretKeySpec key  = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            // Fallback — never silently drop the message
            System.err.println("[EncryptionUtil] Encrypt error: " + e.getMessage());
            return message;
        }
    }

    public static String decrypt(String encryptedMessage) {
        try {
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8"));
            SecretKeySpec key  = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);

            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
            return new String(original, "UTF-8");
        } catch (Exception e) {
            // Could be a plain-text message during handshake — return as-is
            return encryptedMessage;
        }
    }
}