package util;

import play.mvc.Http;
import play.mvc.Http.Cookie;

import java.util.Map;

public class InputUtils {
    public static String getSessionIdFromRequest(Http.Request request) {
        Http.Cookie sessionIdCookie = request.cookies().get("sessionId").orElse(null);
        return sessionIdCookie == null ? null : sessionIdCookie.value();
    }

    public static String trimToNull(String string) {
        if (string == null) {
            return null;
        }
        string = string.trim();
        if (string.isEmpty()) {
            return null;
        }
        return string;
    }

    public static String trimToNull(String[] strings) {
        return strings.length == 0 ? null : trimToNull(strings[0]);
    }

    public static boolean validateCsrfToken(Http.Request request) {
        String csrfTokenFromRequest = null;
        Map<String, String[]> data = request.body().asFormUrlEncoded();
        csrfTokenFromRequest = data != null ? InputUtils.trimToNull(data.get("csrfToken")) : null;
        Cookie csrfTokenCookie = request.cookies().get("csrfToken").orElse(null);
        String csrfTokenFromCookie = csrfTokenCookie != null ? csrfTokenCookie.value() : null;
        return csrfTokenFromRequest != null && csrfTokenFromRequest.length() >= 32 && csrfTokenFromRequest.equals(csrfTokenFromCookie);
    }

}
