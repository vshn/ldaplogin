package util;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class Encode {
    public static String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
