package store.mongodb;

import com.google.api.client.auth.oauth2.Credential;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.DeleteOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import entities.*;
import entities.mongodb.MongoDbDynamicPassword;
import entities.mongodb.MongoDbServicePasswords;
import entities.mongodb.MongoDbUser;
import entities.mongodb.MongoDbUserSession;
import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import play.mvc.Http;
import services.OpenId;
import store.ServicesStore;
import store.UsersStore;
import util.*;

import java.util.*;
import java.util.stream.Stream;

public class MongoDbUsersStore extends MongoDbStore<MongoDbUser> implements UsersStore {

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
        user.setKey(mongoDb.getEncryptionKey());
        UserSession session = user.getSessionById(sessionId);
        return openId.validateUserSession(request, user, session) ? user : null;
    }

    @Override
    public User getByUid(String uid) {
        MongoDbUser user = query().filter(Filters.eq("uid", uid)).first();
        if (user != null) {
            user.setKey(mongoDb.getEncryptionKey());
        }
        return user;
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
        if (!Objects.equals(mongoDbUser.getEmailQuota(), openIdUser.getEmailQuota())) {
            mongoDbUser.setEmailQuota(openIdUser.getEmailQuota());
            updateOperators.add(UpdateOperators.set("emailQuota", mongoDbUser.getEmailQuota()));
        }
        if (!Objects.equals(mongoDbUser.getAlias(), openIdUser.getAlias())) {
            mongoDbUser.setAlias(openIdUser.getAlias());
            updateOperators.add(UpdateOperators.set("alias", mongoDbUser.getAlias()));
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
        UpdateOperator[] updateOperatorsArray = new UpdateOperator[updateOperators.size()];
        updateOperators.toArray(updateOperatorsArray);
        query(mongoDbUser).update(new UpdateOptions(), updateOperatorsArray);
        return true;
    }

    @Override
    public Stream<? extends User> getByGroupPath(String groupPath) {
        return query().filter(Filters.eq("groupPaths", groupPath)).stream();
    }

    @Override
    public Stream<MongoDbUser> getAll() {
        return super.getAll();
    }

    @Override
    public MongoDbUserSession createSession(User user, String id, String openIdAccessToken, String openIdRefreshToken, Long openIdTokenExpiry) {
        MongoDbUser mongoDbUser = (MongoDbUser)user;
        MongoDbUserSession session = new MongoDbUserSession(getEncryptionKey(), id, openIdAccessToken, openIdRefreshToken, openIdTokenExpiry);
        mongoDbUser.addSession(session);
        query(mongoDbUser).update(new UpdateOptions(), UpdateOperators.push("sessions", session));
        return session;
    }

    @Override
    public byte[] getEncryptionKey() {
        return mongoDb.getEncryptionKey();
    }

    @Override
    public String generateDynamicPassword(User user, Service service) {
        MongoDbUser mongoDbUser = (MongoDbUser)user;
        String password = IdGenerator.generateSessionId();
        byte[] passwordHash = PasswordUtil.createStoragePassword(password, LdapSecurityConstants.HASH_METHOD_SSHA512);

        MongoDbDynamicPassword dynamicPassword = new MongoDbDynamicPassword(passwordHash);

        MongoDbServicePasswords servicePasswords = (MongoDbServicePasswords)mongoDbUser.getServicePasswords(service.getId());
        if (servicePasswords == null) {
            servicePasswords = new MongoDbServicePasswords();
            servicePasswords.addDynamicPassword(dynamicPassword);
            mongoDbUser.setServicePasswords(service.getId(), servicePasswords);
            query(user).update(new UpdateOptions(), UpdateOperators.set("servicePasswords." + service.getId(), servicePasswords));
        } else {
            servicePasswords.addDynamicPassword(dynamicPassword);
            query(user).update(new UpdateOptions(), UpdateOperators.push("servicePasswords." + service.getId() + ".dynamicPasswords", dynamicPassword));
        }

        return password;
    }

    @Override
    public String generateStaticPassword(User user, Service service) {
        MongoDbUser mongoDbUser = (MongoDbUser)user;
        String password = IdGenerator.generateSessionId();
        byte[] passwordHash = PasswordUtil.createStoragePassword(password, LdapSecurityConstants.HASH_METHOD_SSHA512);

        MongoDbServicePasswords servicePasswords = (MongoDbServicePasswords)mongoDbUser.getServicePasswords(service.getId());
        if (servicePasswords == null) {
            servicePasswords = new MongoDbServicePasswords();
            servicePasswords.setStaticPwHash(passwordHash);
            mongoDbUser.setServicePasswords(service.getId(), servicePasswords);
            query(user).update(new UpdateOptions(), UpdateOperators.set("servicePasswords." + service.getId(), servicePasswords));
        } else {
            servicePasswords.setStaticPwHash(passwordHash);
            query(user).update(new UpdateOptions(), UpdateOperators.set("servicePasswords." + service.getId() + ".staticPwHash", servicePasswords.getStaticPwHashBase64()));
        }

        return password;
    }

    @Override
    public void logoutWebOnly(User user, UserSession session) {
        // We do not actually delete the session, instead we just remove the hashedId. This means that the session is
        // unreachable via web, but the OAuth credentials can still be used for updating the user information
        query().filter(Filters.eq("sessions.hashedId", session.getHashedId())).update(new UpdateOptions(), UpdateOperators.unset("sessions.$.hashedId"));
        ((MongoDbUserSession)session).removeHashedId();
    }

    @Override
    public void logout(User user, UserSession session) {
        ((MongoDbUser)user).removeSession((MongoDbUserSession)session);
        query(user).update(new UpdateOptions(), UpdateOperators.pull("sessions", Filters.eq("hashedId", session.getHashedId())));
    }

    @Override
    public void updateLastActive(User user) {
        if (user.getLastActiveNeedsUpdate()) {
            MongoDbUser mongoDbUser = (MongoDbUser)user;
            mongoDbUser.setLastActive(System.currentTimeMillis());
            query(user).update(new UpdateOptions(), UpdateOperators.set("lastActive", user.getLastActive()));
        }
    }

    @Override
    public void updateSession(UserSession session, Credential credential) {
        MongoDbUserSession mongoDbUserSession = (MongoDbUserSession)session;
        mongoDbUserSession.setOpenIdAccessToken(credential.getAccessToken());
        mongoDbUserSession.setOpenIdTokenExpiry(credential.getExpirationTimeMilliseconds());
        UpdateOperator accessTokenIvOp = UpdateOperators.set("sessions.$.openIdAccessTokenIv", mongoDbUserSession.getOpenIdAccessTokenIv());
        UpdateOperator accessTokenEncOp = UpdateOperators.set("sessions.$.openIdAccessTokenEnc", mongoDbUserSession.getOpenIdAccessTokenEnc());
        UpdateOperator expiryOp = UpdateOperators.set("sessions.$.openIdTokenExpiry", mongoDbUserSession.getOpenIdTokenExpiry());
        UpdateOperator lifetimeOp = UpdateOperators.set("sessions.$.openIdTokenLifetime", mongoDbUserSession.getOpenIdTokenLifetime());
        query().filter(Filters.eq("sessions.hashedId", mongoDbUserSession.getHashedId())).update(new UpdateOptions(), accessTokenIvOp, accessTokenEncOp, expiryOp, lifetimeOp);
    }

    @Override
    public boolean shouldRefreshProactively(User user, UserSession session) {
        long remainingLifetime = session.getOpenIdTokenExpiry() - System.currentTimeMillis();
        // We refresh proactively if the token has less than half of its lifetime left.
        // However don't bother if the token expires in less than 10 seconds because that may not be enough time until we'll have to force the refresh anyway.
        if (remainingLifetime > 10000L && remainingLifetime < session.getOpenIdTokenLifetime() / 2) {
            // in principle a proactive refresh makes sense, but we need to acquire a lock. We do this by trying to set openIdTokenLifetime to 0.
            UpdateOperator lifetimeOp = UpdateOperators.set("sessions.$.openIdTokenLifetime", 0L);
            UpdateResult r = query().filter(Filters.eq("sessions.hashedId", session.getHashedId()), Filters.eq("sessions.openIdTokenLifetime", session.getOpenIdTokenLifetime())).update(new UpdateOptions(), lifetimeOp);
            ((MongoDbUserSession)session).setOpenIdTokenLifetime(0L);
            return r.getMatchedCount() > 0; // if no object matched then some other thread was quicker
        }
        return false;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        Long deleteUsers = now - User.EXPIRES;
        Long deleteSessions = now - MongoDbUserSession.SESSION_EXPIRES;

        // remove expired dynamic passwords with service-specific expiry times
        if (!ServicesStore.getAll().isEmpty()) {
            UpdateOperator[] ops = new UpdateOperator[ServicesStore.getAll().size()];
            int i = 0;
            for (Service service : ServicesStore.getAll()) {
                ops[i++] = UpdateOperators.pull("servicePasswords." + service.getId() + ".dynamicPasswords", Filters.lt("timestamp", now - service.getDynamicPasswordExpires()*1000L));
            }
            query().update(new UpdateOptions().multi(true), ops);
        }

        // remove users entirely
        query().filter(Filters.lt("lastActive", deleteUsers), Filters.nin("groupPaths", User.NEVER_EXPIRES_GROUPS)).delete(new DeleteOptions().multi(true));

        // remove sessions if their openId tokens have expired
        query().filter(Filters.lt("sessions.openIdTokenExpiry", deleteSessions)).update(new UpdateOptions().multi(true), UpdateOperators.pull("sessions", Filters.lt("openIdTokenExpiry", deleteSessions)));
    }
}
