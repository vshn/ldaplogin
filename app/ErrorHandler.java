import com.google.inject.Inject;
import com.typesafe.config.Config;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

import javax.inject.Provider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import entities.NotAllowedException;
import play.mvc.Results;

public class ErrorHandler extends DefaultHttpErrorHandler {
    @Inject
    public ErrorHandler(Config config, Environment environment, OptionalSourceMapper sourceMapper, Provider<Router> routes) {
        super(config, environment, sourceMapper, routes);
    }

    @Override
    protected CompletionStage<Result> onForbidden(RequestHeader requestHeaders, String message) {
        return CompletableFuture.completedFuture(Results.forbidden(views.html.error.render(requestHeaders, Http.Status.FORBIDDEN, "Forbidden", message)));
    }

    @Override
    protected CompletionStage<Result> onNotFound(RequestHeader requestHeaders, String message) {
        return CompletableFuture.completedFuture(Results.forbidden(views.html.error.render(requestHeaders, Http.Status.NOT_FOUND, "Not Found", message)));
    }

    @Override
    protected CompletionStage<Result> onBadRequest(RequestHeader requestHeaders, String message) {
        return CompletableFuture.completedFuture(Results.forbidden(views.html.error.render(requestHeaders, Http.Status.BAD_REQUEST, "Bad Request", message)));
    }

    @Override
    public CompletionStage<Result> onServerError(RequestHeader requestHeaders, Throwable exception) {
        if (exception instanceof NotAllowedException) {
            return onForbidden(requestHeaders, exception.getMessage());
        }
        return super.onServerError(requestHeaders, exception);
    }
}
