package services;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import entities.NotAllowedException;
import entities.OpenIdUser;
import entities.User;
import entities.UserSession;
import org.apache.commons.lang3.tuple.Pair;
import play.mvc.Http;
import store.GroupsStore;
import store.UsersStore;
import util.*;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class OpenId {
    private final OpenIdConfig openIdConfig;

    private final CustomLogger logger = new CustomLogger(this.getClass());

    @Inject
    private UsersStore usersStore;

    @Inject
    private GroupsStore groupsStore;

    public OpenId() {
        OpenIdConfig cfg = null;
        try {
            cfg = new OpenIdConfig();
        } catch (Exception e) {
            logger.error(null, "OpenId could not be configured: " + e.getMessage());
        }
        openIdConfig = cfg;
    }


    public Credential getCredentialFromSession(UserSession session, CredentialRefreshListener credentialRefreshListener) {
        Credential.Builder builder = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod()).setJsonFactory(new GsonFactory()).setTransport(new NetHttpTransport()).setRefreshListeners(Set.of(credentialRefreshListener));
        if (openIdConfig != null) {
            builder.setClientAuthentication(new ClientParametersAuthentication(openIdConfig.getClientId(), openIdConfig.getSecret()));
            builder.setTokenServerUrl(new GenericUrl(openIdConfig.getUrlToken()));
        }
        Credential credential = builder.build();
        credential.setAccessToken(session.getOpenIdAccessToken());
        credential.setRefreshToken(session.getOpenIdRefreshToken());
        credential.setExpirationTimeMilliseconds(session.getOpenIdTokenExpiry());
        return credential;
    }


    public String getUrlLogout(String returnUrl, String idTokenHint) {
        // With Keycloak 18.0.0+ we can only use post_logout_redirect_uri together with the original ID token received upon login.
        // If that original ID token is not available we can only provide a fall-back with an explicit logout button and no redirect back.
        if (idTokenHint == null) {
            return openIdConfig == null ? null : openIdConfig.getUrlLogout();
        } else {
            return openIdConfig == null ? null : openIdConfig.getUrlLogout() + "?id_token_hint=" + Encode.url(idTokenHint) + "&post_logout_redirect_uri=" + Encode.url(returnUrl);
        }
    }

    public String getOpenIdRedirectUrl(Http.Request request, String state) {
        if (openIdConfig != null) {
            return openIdConfig.getFlow().newAuthorizationUrl().setRedirectUri(openIdCallbackUrl(request)).setState(state).build();
        }
        return null;
    }

    private String openIdCallbackUrl(Http.RequestHeader request) {
        return AbsoluteUrlGenerator.self(request, "/login/callback");
    }

    public Pair<User, String> openIdLogin(Http.Request request, String state, String code) {
        // First check if the "state" parameter is valid.
        // The cookie value is considered trusted because it can only be manipulated by the user who's trying to log in,
        // not by a third party. Expiration is provided by the browser's cookie expiration mechanism.
        // We could also work with timestamps and signatures (HMAC), but that wouldn't increase security much
        // because the user who's trying to log in could still use the same 'state' more than once. In order to ensure
        // single use only we'd have to store the state in the DB, which seems like overkill.
        if (!request.getCookie("openIdLoginState").isPresent()) {
            throw new NotAllowedException("Your login state has expired. Please try again.");
        }
        if (state == null || !state.equals(request.getCookie("openIdLoginState").get().value())) {
            throw new NotAllowedException("openIdLoginState cookie does not match 'state' parameter. Please try again.");
        }

        // So far we just have the 'code' string from a source that isn't trustworthy (the user's browser).
        // We now actually try to use the 'code' to get the id_token.
        // Any sort of tampering with the 'code' parameter will lead the openId server to reject the request.
        // Also, because we're sending a request to a known and TLS encrypted URL, we know that we can trust the
        // response.
        TokenResponse tokenResponse;
        try {
            tokenResponse = openIdConfig.getFlow().newTokenRequest(code).setRedirectUri(openIdCallbackUrl(request)).execute();
        } catch ( Exception e) {
            throw new NotAllowedException("Could not verify the code with the OpenId server");
        }

        OpenIdUser openIdUser = OpenIdUser.fromTokenResponse(tokenResponse);
        if (openIdUser == null) {
            throw new NotAllowedException("invalid token response");
        }
        if (InputUtils.trimToNull(openIdUser.getUid()) == null) {
            throw new NotAllowedException("OpenID user must have uid set");
        }
        if (openIdUser.getEmail() == null || !openIdUser.isEmailVerified()) {
            throw new NotAllowedException("OpenID user email address not found or not verified");
        }
        if (InputUtils.trimToNull(openIdUser.getFirstName()) == null) {
            throw new NotAllowedException("OpenID user must have firstName set");
        }
        if (InputUtils.trimToNull(openIdUser.getLastName()) == null) {
            throw new NotAllowedException("OpenID user must have lastName set");
        }

        Credential cred;
        try {
            cred = openIdConfig.getFlow().createAndStoreCredential(tokenResponse, openIdUser.getEmail());
        } catch (IOException e) {
            // we're not using a storage back-end here so there's not much reason why this should go wrong
            throw new RuntimeException(e);
        }

        boolean created = false;
        boolean updated = false;
        User user = usersStore.getByUid(openIdUser.getUid());
        if (user == null) {
            user = usersStore.create(openIdUser);
            created = true;
        } else {
            updated = usersStore.update(user, openIdUser);
        }
        groupsStore.ensure(openIdUser.getGroupPaths());
        groupsStore.updateMetadata(openIdUser.getGroupsMetadata());

        String sessionId = IdGenerator.generateSessionId();
        usersStore.createSession(user, sessionId, openIdUser.getIdToken(), cred.getAccessToken(), cred.getRefreshToken(), cred.getExpirationTimeMilliseconds());

        logger.info(request, user + " logged in" + ((created || updated) ? (", shadow user " + (created ? "created" : "updated")) : ""));
        return Pair.of(user, sessionId);
    }

    /**
     * This method checks if the user has a valid session with the IDP and refreshes the session if necessary.
     * If the refresh succeeds, then the local shadow user is updated.
     * If the refresh fails with a 4xx status code, then that means that the IDP considers the user to be logged out, and therefore the session is terminated.
     * If the refresh fails with some other error (e.g. 5xx or connection refused) then the user can't authenticate, but the session remains in the database and we'll try to refresh the session again next time.
     * @param request
     * @param user
     * @param session
     * @return
     */
    public boolean validateUserSession(Http.Request request, User user, UserSession session) {
        if (user == null || session == null) {
            return false;
        }
        Credential credential = getCredentialFromSession(session, new CredentialRefreshListener() {
            @Override
            public void onTokenResponse(Credential credential, TokenResponse tokenResponse) throws IOException {
                usersStore.update(user, OpenIdUser.fromTokenResponse(tokenResponse));
            }

            @Override
            public void onTokenErrorResponse(Credential credential, TokenErrorResponse tokenErrorResponse) throws IOException {
                // we don't do anything, the session will expire
            }
        });
        if (credential == null) {
            // session doesn't have valid OpenID tokens. Not sure what happened but let's play it safe.
            return false;
        }
        // session was created via OpenId, verify that it is still valid
        if (credential.getExpiresInSeconds() == null || credential.getExpiresInSeconds() <= 0) {
            if (credentialRefresh(request, user, session, credential)) {
                usersStore.updateLastActive(user);
                return true;
            } else {
                return false;
            }
        }

        // token is still valid but may expire soon(ish)
        if (usersStore.shouldRefreshProactively(user, session)) {
            new Thread(() -> credentialRefresh(request, user, session, credential)).start();
        }
        usersStore.updateLastActive(user);
        return true;
    }

    private boolean credentialRefresh(Http.Request request, User user, UserSession session, Credential credential) {
        try {
            if (!credential.refreshToken()) {
                logger.info(request, "Unexpected response from IDP when refreshing user session for " + user.getUid() + " (will try again)");
                return false;
            }
        } catch (TokenResponseException e) {
            logger.info(request, "Couldn't refresh user session, IDP responded with 4xx, logging out " + user.getUid());
            usersStore.logout(user, session);
            return false;
        } catch (Exception e) {
            logger.info(request, "Can't contact IDP to refresh user session for " + user.getUid() + " (will try again): " + e.getMessage());
            return false;
        }
        usersStore.updateSession(session, credential);
        logger.info(request, "Refreshed OpenID session of " + user);
        return true;
    }


    private static class OpenIdConfig {
        private final String clientId;
        private final String secret;
        private final String urlAuth;
        private final String urlToken;
        private final String urlLogout;
        private final AuthorizationCodeFlow flow;

        protected OpenIdConfig() {
            this.clientId = Config.Option.OPENID_CLIENT_ID.get();
            if (this.clientId == null) {
                throw new IllegalArgumentException("Environment variable " + Config.Option.OPENID_CLIENT_ID + " is not set");
            }

            this.secret = Config.Option.OPENID_SECRET.get(); // can be null, which is fine

            this.urlAuth = Config.Option.OPENID_URL_AUTH.get();
            if (this.urlAuth == null) {
                throw new IllegalArgumentException("Environment variable " + Config.Option.OPENID_URL_AUTH + " is not set");
            }

            this.urlToken = Config.Option.OPENID_URL_TOKEN.get();
            if (this.urlToken == null) {
                throw new IllegalArgumentException("Environment variable " + Config.Option.OPENID_URL_TOKEN + " is not set");
            }

            this.urlLogout = Config.Option.OPENID_URL_LOGOUT.get();
            if (this.urlToken == null) {
                throw new IllegalArgumentException("Environment variable " + Config.Option.OPENID_URL_LOGOUT + " is not set");
            }

            this.flow = new AuthorizationCodeFlow.Builder(
                    BearerToken.authorizationHeaderAccessMethod(),
                    new NetHttpTransport(),
                    new GsonFactory(),
                    new GenericUrl(this.urlToken),
                    new ClientParametersAuthentication(this.clientId, this.secret),
                    this.clientId,
                    this.urlAuth)
                    .setScopes(List.of("email", "profile", "openid"))
                    .enablePKCE()
                    .build();
        }

        public AuthorizationCodeFlow getFlow() {
            return flow;
        }

        public String getClientId() {
            return clientId;
        }

        public String getSecret() {
            return secret;
        }

        public String getUrlToken() {
            return urlToken;
        }

        public String getUrlLogout() {
            return urlLogout;
        }
    }
}
