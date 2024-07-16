package util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class IdGenerator {
    private static final char[] num = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
    private static final char[] alpha = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
    private static final char[] alphaUpper = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
    private static final char[] alphaNum;
    private static final char[] alphaNumCase;
    private static final SecureRandom secureRandom;

    static {
        alphaNum = new char[num.length + alpha.length];
        System.arraycopy(num, 0, alphaNum, 0, num.length);
        System.arraycopy(alpha, 0, alphaNum, num.length, alpha.length);
        alphaNumCase = new char[alphaNum.length + alphaUpper.length];
        System.arraycopy(alphaNum, 0, alphaNumCase, 0, alphaNum.length);
        System.arraycopy(alphaUpper, 0, alphaNumCase, alphaNum.length, alphaUpper.length);
        try {
            secureRandom = SecureRandom.getInstance("NativePRNGNonBlocking");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateSessionId() {
        String id = "";
        for (int i = 0; i < 32; i++) {
            id += alphaNumCase[secureRandom.nextInt(alphaNumCase.length)];
        }
        return id;
    }

    public static String generateCsrfToken() {
        return generateSessionId();
    }

    public static byte[] generateBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }
}
