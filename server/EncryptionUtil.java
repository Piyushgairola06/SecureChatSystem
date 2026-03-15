package server;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.util.Base64;

public class EncryptionUtil {

    // 16 character key (AES-128 requires 16 bytes)
    private static final String KEY = "MySecureChatKey!";

    // 16 character IV
    private static final String IV = "RandomInitVector";

    // --------------------------------------
    // Encrypt message
    // --------------------------------------
    public static String encrypt(String message) {

        try {

            SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), "AES");

            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encryptedBytes = cipher.doFinal(message.getBytes());

            String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);

            return encryptedText;

        } catch (Exception e) {

            System.out.println("Encryption error");

            return message; // fallback
        }
    }

    // --------------------------------------
    // Decrypt message
    // --------------------------------------
    public static String decrypt(String encryptedMessage) {

        try {

            SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), "AES");

            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedMessage);

            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            String decryptedText = new String(decryptedBytes);

            return decryptedText;

        } catch (Exception e) {

            // if message was not encrypted (like during login)
            return encryptedMessage;
        }
    }
}
