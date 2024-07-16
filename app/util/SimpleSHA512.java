package util;

import org.apache.commons.codec.binary.Hex;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SimpleSHA512 {

    private MessageDigest hash;

    public SimpleSHA512() {
        try {
            hash = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] hash(byte[] data) {
        hash.update(data);
        return hash.digest();
    }

    public String hash(String data) {
        return Hex.encodeHexString(hash(data.getBytes(Charset.forName("UTF-8"))));
    }
}
