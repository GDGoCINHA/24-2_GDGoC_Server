package inha.gdgoc.global.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class EncryptUtil {

    public static String encrypt(String oldPassword, byte[] salt) throws NoSuchAlgorithmException, InvalidKeyException {
        return generateHashedValue(oldPassword, salt);
    }

    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    public static String generateHashedValue(String oldPassword, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(salt, "HmacSHA256");
        mac.init(secretKeySpec);

        byte[] hashedBytes = mac.doFinal(oldPassword.getBytes());
        return Base64.getEncoder().encodeToString(hashedBytes);

    }
}
