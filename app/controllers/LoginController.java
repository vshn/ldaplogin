package controllers;

import entities.User;
import entities.UserSession;
import services.OpenId;
import org.apache.commons.lang3.tuple.Pair;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Cookie;
import play.mvc.Result;
import store.UsersStore;
import util.*;

import javax.inject.Inject;
import java.time.Duration;

public class LoginController extends Controller {

    private final CustomLogger logger = new CustomLogger(this.getClass());

    @Inject
    private OpenId openId;

    @Inject
    private UsersStore usersStore;

    public Result login(Http.Request request) {
        String state = IdGenerator.generateCsrfToken();
        String redirectUrl = openId.getOpenIdRedirectUrl(request, state);
        Cookie openIdLoginStateCookie = Cookie.builder("openIdLoginState", state).withMaxAge(Duration.ofSeconds(300)).build();
        return redirect(redirectUrl).withCookies(openIdLoginStateCookie);
    }

    public Result logout(Http.Request request) {
        // Get the identityToken. We only need it for a transparent Keycloak logout
        User user = usersStore.getFromRequest(request, openId);
        UserSession session = user == null ? null : user.getSessionById(InputUtils.getSessionIdFromRequest(request));
        String identityToken = session == null ? null : session.getOpenIdIdentityToken();
        if (session != null) {
            usersStore.logout(user, session);
        }
        logger.info(request, "logged out");
        Cookie c1 = Cookie.builder("sessionId", "").withMaxAge(Duration.ZERO).build();
        Cookie c2 = Cookie.builder("csrfToken", "").withMaxAge(Duration.ZERO).withHttpOnly(false).build();
        return redirect(openId.getUrlLogout(AbsoluteUrlGenerator.self(request, "/loggedout"), identityToken)).withCookies(c1, c2);
    }

    public Result loggedout(Http.Request request) {
        User user = usersStore.getFromRequest(request, openId);
        if (user != null) {
            // just to make sure that what the page says is actually true...
            return logout(request);
        }
        return ok(views.html.loggedout.render());
    }

    public static Result loginFirst(Http.Request request) {
        return redirect(controllers.routes.LoginController.login());
    }

    public Result loginCallback(Http.Request request, String state, String code) {
        Pair<User, String> login = openId.openIdLogin(request, state, code);

        Http.Cookie sessionIdCookie = Http.Cookie.builder("sessionId", login.getRight()).build();
        Http.Cookie csrfTokenCookie = Http.Cookie.builder("csrfToken", IdGenerator.generateCsrfToken()).withHttpOnly(false).build();
        Http.Cookie openIdLoginStateCookie = Cookie.builder("openIdLoginState", state).withMaxAge(Duration.ofSeconds(0)).build(); // cookie reset

        return redirect(controllers.routes.HomeController.index()).withCookies(sessionIdCookie, csrfTokenCookie, openIdLoginStateCookie);
    }

}
