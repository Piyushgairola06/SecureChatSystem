package server;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class EncryptionUtil {

    private static final String KEY = "SecureChatKey128"; // 16 bytes → AES-128
    private static final String IV  = "InitVector123456"; // 16 bytes

    public static String encrypt(String message) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes("UTF-8"), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes("UTF-8"));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return message; // fallback: return plaintext
        }
    }

    public static String decrypt(String encryptedMessage) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes("UTF-8"), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes("UTF-8"));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedMessage);
            return new String(cipher.doFinal(decoded), "UTF-8");
        } catch (Exception e) {
            return encryptedMessage; // fallback: return as-is
        }
    }
}
