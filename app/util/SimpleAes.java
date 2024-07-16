package util;

import java.nio.charset.*;
import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import org.apache.commons.codec.*;
import org.apache.commons.codec.binary.*;

public class SimpleAes {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    Cipher cipher;
    SecretKeySpec key;

    public SimpleAes(byte[] key) {
        if (key.length != 32) {
            throw new IllegalArgumentException("Encryption key must be 32 bytes long (256 bit) for AES-256");
        }
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }

        this.key = new SecretKeySpec(key, "AES");
    }

    public byte[] encrypt(byte[] iv, byte[] plain) {
        if (iv.length != 16) {
            throw new IllegalArgumentException("IV must be 16 bytes long (128 bit) for AES-256");
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            return cipher.doFinal(plain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String encryptHex(String iv, String plain) {
        try {
            return Hex.encodeHexString(encrypt(Hex.decodeHex(iv.toCharArray()), plain.getBytes(UTF8)));
        } catch (DecoderException e) {
            throw new IllegalArgumentException("iv is not a valid hex string");
        }
    }

    public byte[] decrypt(byte[] iv, byte[] input) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            return cipher.doFinal(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decryptHex(String iv, String encrypted) {
        try {
            return new String(decrypt(Hex.decodeHex(iv.toCharArray()), Hex.decodeHex(encrypted.toCharArray())), UTF8);
        } catch (DecoderException e) {
            return null;
        }
    }

    public static byte[] generateIv() {
        return IdGenerator.generateBytes(16);
    }

    public static String generateIvHex() {
        return Hex.encodeHexString(generateIv());
    }
}
