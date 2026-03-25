package server;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.util.Base64;

public class EncryptionUtil {

    private static final String KEY = "MySecureChatKey!";  // 16 bytes = AES-128
    private static final String IV  = "RandomInitVector";  // 16 bytes

    public static String encrypt(String message) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), "AES");
            IvParameterSpec ivSpec  = new IvParameterSpec(IV.getBytes());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes()));
        } catch (Exception e) {
            System.out.println("[Encryption] Error: " + e.getMessage());
            return message; // fallback: send plain
        }
    }

    public static String decrypt(String encryptedMessage) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), "AES");
            IvParameterSpec ivSpec  = new IvParameterSpec(IV.getBytes());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedMessage);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            // Message wasn't encrypted (e.g. plain control signals) — return as-is
            return encryptedMessage;
        }
    }
}