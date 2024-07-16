package store.mongodb;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import entities.OpenIdUser;
import entities.User;
import entities.UserSession;
import entities.mongodb.MongoDbUser;
import entities.mongodb.MongoDbUserSession;
import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import play.mvc.Http;
import services.MongoDb;
import services.OpenId;
import store.UsersStore;
import util.CustomLogger;
import util.IdGenerator;
import util.InputUtils;
import util.SimpleSHA512;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class MongoDbUsersStore implements UsersStore {

    private final CustomLogger logger = new CustomLogger(this.getClass());

    @Inject
    private MongoDb mongoDb;

    private Query<MongoDbUser> query() {
        return mongoDb.getDS().find(MongoDbUser.class);
    }

    private Query<MongoDbUser> query(User user) {
        MongoDbUser mongoDbUser = (MongoDbUser)user;
        return query().filter(Filters.eq("_id", mongoDbUser.getObjectId()));
    }

    @Override
    public User getFromRequest(Http.Request request, OpenId openId) {
        cleanup();
        if (!"GET".equals(request.method()) && !"HEAD".equals(request.method()) && !"OPTIONS".equals(request.method())) {
            // if in doubt, check the CSRF token
            if (!InputUtils.validateCsrfToken(request)) {
                return null;
            }
        }
        String sessionId = InputUtils.getSessionIdFromRequest(request);
        if (sessionId == null) {
            return null;
        }
        String hashedSessionId = new SimpleSHA512().hash(sessionId);
        MongoDbUser user = query().filter(Filters.eq("sessions.hashedId", hashedSessionId)).first();
        if (user == null) {
            return null;
        }

        MongoDbUserSession session = user.getSessionById(sessionId);
        if (session == null) {
            return null;
        }
        session.setKey(mongoDb.getEncryptionKey());

        Credential credential = openId.getCredentialFromSession(session, new CredentialRefreshListener() {
            @Override
            public void onTokenResponse(Credential credential, TokenResponse tokenResponse) throws IOException {
                update(user, OpenIdUser.fromTokenResponse(tokenResponse));
            }

            @Override
            public void onTokenErrorResponse(Credential credential, TokenErrorResponse tokenErrorResponse) throws IOException {
                // we don't do anything, the session will expire
            }
        });
        if (credential == null) {
            // session doesn't have valid OpenID tokens. Not sure what happened but let's play it safe.
            return null;
        }
        // session was created via OpenId, verify that it is still valid
        if (credential.getExpiresInSeconds() == null || credential.getExpiresInSeconds() <= 0) {
            try {
                if (!credential.refreshToken()) {
                    logger.info(request, "Could not refresh openIdAccessToken, logging out user");
                    return null;
                }
            } catch (Exception e) {
                logger.info(request, "Could not refresh openIdAccessToken, logging out user");
                return null;
            }
            session.setOpenIdAccessToken(credential.getAccessToken());
            session.setOpenIdTokenExpiry(credential.getExpirationTimeMilliseconds());
            UpdateOperator accessTokenIvOp = UpdateOperators.set("sessions.$.openIdAccessTokenIv", session.getOpenIdAccessTokenIv());
            UpdateOperator accessTokenEncOp = UpdateOperators.set("sessions.$.openIdAccessTokenEnc", session.getOpenIdAccessTokenEnc());
            UpdateOperator expiryOp = UpdateOperators.set("sessions.$.openIdTokenExpiry", session.getOpenIdTokenExpiry());
            query().filter(Filters.eq("sessions.hashedId", session.getHashedId())).update(accessTokenIvOp, accessTokenEncOp, expiryOp).execute();
            logger.info(request, "Refreshed OpenID session of " + user);
        }

        if (user.getLastActiveNeedsUpdate()) {
            user.setLastActive(System.currentTimeMillis());
            query(user).update(UpdateOperators.set("lastActive", user.getLastActive())).execute();
        }

        return user;
    }

    @Override
    public User getByUid(String uid) {
        return query().filter(Filters.eq("uid", uid)).first();
    }

    @Override
    public User create(OpenIdUser openIdUser) {
        MongoDbUser user = new MongoDbUser(openIdUser);
        mongoDb.getDS().save(user);
        return user;
    }

    @Override
    public boolean update(User user, OpenIdUser openIdUser) {
        MongoDbUser mongoDbUser = (MongoDbUser)user;

        String fromUserEmail = openIdUser.isEmailVerified() ? openIdUser.getEmail() : mongoDbUser.getEmail();

        List<UpdateOperator> updateOperators = new LinkedList<>();
        if (!Objects.equals(mongoDbUser.getEmail(), fromUserEmail)) {
            mongoDbUser.setEmail(fromUserEmail);
            updateOperators.add(UpdateOperators.set("email", mongoDbUser.getEmail()));
        }
        if (!Objects.equals(mongoDbUser.getFirstName(), openIdUser.getFirstName())) {
            mongoDbUser.setFirstName(openIdUser.getFirstName());
            updateOperators.add(UpdateOperators.set("firstName", mongoDbUser.getFirstName()));
        }
        if (!Objects.equals(mongoDbUser.getLastName(), openIdUser.getLastName())) {
            mongoDbUser.setLastName(openIdUser.getLastName());
            updateOperators.add(UpdateOperators.set("lastName", mongoDbUser.getLastName()));
        }
        Set<String> oldGroupPaths = new HashSet<>(mongoDbUser.getGroupPaths());
        Set<String> newGroupPaths = new HashSet<>(openIdUser.getGroupPaths());
        if (!Objects.equals(oldGroupPaths, newGroupPaths)) {
            mongoDbUser.setGroupPaths(openIdUser.getGroupPaths());
            updateOperators.add(UpdateOperators.set("groupPaths", mongoDbUser.getGroupPaths()));
        }
        if (updateOperators.isEmpty()) {
            return false;
        }
        query(mongoDbUser).update(updateOperators).execute();
        return true;
    }

    @Override
    public Stream<? extends User> getByGroupPath(String groupPath) {
        return query().filter(Filters.eq("groupPaths", groupPath)).stream();
    }

    @Override
    public List<? extends User> getAll() {
        return query().iterator().toList();
    }

    @Override
    public MongoDbUserSession createSession(User user, String id, String openIdIdentityToken, String openIdAccessToken, String openIdRefreshToken, Long openIdTokenExpiry) {
        MongoDbUser mongoDbUser = (MongoDbUser)user;
        MongoDbUserSession session = new MongoDbUserSession(getEncryptionKey(), id, openIdIdentityToken, openIdAccessToken, openIdRefreshToken, openIdTokenExpiry);
        mongoDbUser.addSession(session);
        query(mongoDbUser).update(UpdateOperators.push("sessions", session)).execute();
        return session;
    }

    @Override
    public byte[] getEncryptionKey() {
        return mongoDb.getEncryptionKey();
    }

    @Override
    public String generatePassword(User user) {
        MongoDbUser mongoDbUser = (MongoDbUser)user;
        String password = IdGenerator.generateSessionId();
        mongoDbUser.setPasswordHash(PasswordUtil.createStoragePassword(password, LdapSecurityConstants.HASH_METHOD_SSHA512));
        query(user).update(UpdateOperators.set("passwordHash", mongoDbUser.getPasswordHashHexEncoded())).execute();
        return password;
    }

    @Override
    public void logout(User user, UserSession session) {
        ((MongoDbUser)user).removeSession((MongoDbUserSession)session);
    }

    private void cleanup() {
        Long deleteUsers = System.currentTimeMillis() - User.EXPIRES;
        query().filter(Filters.lt("lastActive", deleteUsers)).delete();
        Long deleteSessions = System.currentTimeMillis() - MongoDbUserSession.SESSION_EXPIRES;
        query().filter(Filters.lt("sessions.openIdTokenExpiry", deleteSessions)).update(UpdateOperators.pull("sessions", Filters.lt("openIdTokenExpiry", deleteSessions))).execute();
    }
}
