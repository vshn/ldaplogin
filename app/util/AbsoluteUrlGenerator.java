package util;

import play.mvc.Http;

public class AbsoluteUrlGenerator {
    public static String selfBase(Http.RequestHeader request) {
        // we're in a web context; we assume that the host header is correct, even behind a reverse proxy
        String host = request.host();

        // The scheme must be http if we're in a local development environment, and https otherwise.
        // This isn't entirely unproblematic because a misconfigured proxy server could use the same host header,
        // but we don't have a good way to distinguish these two cases
        String scheme = (host.startsWith("localhost:") || host.startsWith("127.0.0.1:"))? "http" : "https";
        return scheme + "://" + host;
    }

    public static String self(Http.RequestHeader request, String path) {
        return selfBase(request) + path;
    }

}
