package util;

import play.Logger;
import play.mvc.Http;

public class CustomLogger {
    final Logger.ALogger logger;

    public <T> CustomLogger(Class<T> clazz) {
        logger = Logger.of(clazz);
    }

    private String getInfo(Http.RequestHeader request) {
        try {
            // will throw a RuntimeException if there's no context
            return request.remoteAddress() + " ";
        } catch (RuntimeException e) {
            return "127.0.0.1 ";
        }
    }

    public void error(Http.RequestHeader request, String msg) {
        logger.error(getInfo(request) + msg);
    }

    public void error(Http.RequestHeader request, String msg, Throwable t) {
        logger.error(getInfo(request) + msg, t);
    }

    public void warn(Http.RequestHeader request, String msg) {
        logger.warn(getInfo(request) + msg);
    }

    public void warn(Http.RequestHeader request, String msg, Throwable t) {
        logger.warn(getInfo(request) + msg, t);
    }

    public void info(Http.RequestHeader request, String msg) {
        logger.info(getInfo(request) + msg);
    }

    public void info(Http.RequestHeader request, String msg, Throwable t) {
        logger.info(getInfo(request) + msg, t);
    }

    public void debug(Http.RequestHeader request, String msg) {
        logger.debug(getInfo(request) + msg);
    }

    public void debug(Http.RequestHeader request, String msg, Throwable t) {
        logger.debug(getInfo(request) + msg, t);
    }

}
