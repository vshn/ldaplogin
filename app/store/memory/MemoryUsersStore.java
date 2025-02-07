package store.memory;

import com.google.api.client.auth.oauth2.Credential;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.UpdateOptions;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import entities.OpenIdUser;
import entities.Service;
import entities.User;
import entities.UserSession;
import entities.memory.MemoryDynamicPassword;
import entities.memory.MemoryServicePasswords;
import entities.memory.MemoryUser;
import entities.memory.MemoryUserSession;
import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import play.mvc.Http;
import services.OpenId;
import store.UsersStore;
import util.IdGenerator;
import util.InputUtils;

import java.util.*;
import java.util.stream.Stream;

public class MemoryUsersStore implements UsersStore {

    private final static Set<MemoryUser> users = new HashSet<>();

    @Override
    public User getFromRequest(Http.Request request, OpenId openId) {
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
        User user = users.stream().filter(u -> u.getSessionById(sessionId) != null).findFirst().orElse(null);
        if (user == null) {
            return null;
        }
        UserSession session = user.getSessionById(sessionId);
        if (session == null) {
            return null;
        }

        return openId.validateUserSession(request, user, session) ? user : null;
    }

    @Override
    public User getByUid(String uid) {
        return users.stream().filter(u -> u.getUid().equals(uid)).findFirst().orElse(null);
    }

    @Override
    public User create(OpenIdUser openIdUser) {
        MemoryUser user = new MemoryUser(openIdUser);
        users.add(user);
        return user;
    }

    @Override
    public boolean update(User user, OpenIdUser openIdUser) {
        MemoryUser memoryUser = (MemoryUser)user;

        String fromUserEmail = openIdUser.isEmailVerified() ? openIdUser.getEmail() : memoryUser.getEmail();

        boolean updated = false;
        if (!Objects.equals(memoryUser.getEmail(), fromUserEmail)) {
            memoryUser.setEmail(fromUserEmail);
            updated = true;
        }
        if (!Objects.equals(memoryUser.getEmailQuota(), openIdUser.getEmailQuota())) {
            memoryUser.setEmailQuota(openIdUser.getEmailQuota());
            updated = true;
        }
        if (memoryUser.isEmailVerified() != openIdUser.isEmailVerified()) {
            memoryUser.setEmailVerified(openIdUser.isEmailVerified());
            updated = true;
        }
        if (!Objects.equals(memoryUser.getFirstName(), openIdUser.getFirstName())) {
            memoryUser.setFirstName(openIdUser.getFirstName());
            updated = true;
        }
        if (!Objects.equals(memoryUser.getLastName(), openIdUser.getLastName())) {
            memoryUser.setLastName(openIdUser.getLastName());
            updated = true;
        }
        if (!Objects.equals(memoryUser.getGroupPaths(), openIdUser.getGroupPaths())) {
            memoryUser.setGroupPaths(openIdUser.getGroupPaths());
            updated = true;
        }
        return updated;
    }

    @Override
    public Stream<MemoryUser> getByGroupPath(String groupPath) {
        return users.stream().filter(u -> u.getGroupPaths().contains(groupPath));
    }

    @Override
    public UserSession createSession(User user, String id, String openIdIdentityToken, String openIdAccessToken, String openIdRefreshToken, Long openIdTokenExpiry) {
        MemoryUser memoryUser = (MemoryUser)user;
        MemoryUserSession session = new MemoryUserSession(id, openIdIdentityToken, openIdAccessToken, openIdRefreshToken, openIdTokenExpiry);
        memoryUser.addSession(session);
        return session;
    }

    @Override
    public byte[] getEncryptionKey() {
        return new byte[32];
    }

    @Override
    public String generateDynamicPassword(User user, Service service) {
        MemoryUser memoryUser = (MemoryUser)user;
        String password = IdGenerator.generateSessionId();
        byte[] passwordHash = PasswordUtil.createStoragePassword(password, LdapSecurityConstants.HASH_METHOD_SSHA512);

        MemoryDynamicPassword dynamicPassword = new MemoryDynamicPassword(passwordHash);

        MemoryServicePasswords servicePasswords = (MemoryServicePasswords)memoryUser.getServicePasswords(service.getId());
        if (servicePasswords == null) {
            servicePasswords = new MemoryServicePasswords();
            servicePasswords.addDynamicPassword(dynamicPassword);
            memoryUser.setServicePasswords(service.getId(), servicePasswords);
        } else {
            servicePasswords.addDynamicPassword(dynamicPassword);
        }

        return password;
    }

    @Override
    public String generateStaticPassword(User user, Service service) {
        MemoryUser memoryUser = (MemoryUser)user;
        String password = IdGenerator.generateSessionId();
        byte[] passwordHash = PasswordUtil.createStoragePassword(password, LdapSecurityConstants.HASH_METHOD_SSHA512);

        MemoryServicePasswords servicePasswords = (MemoryServicePasswords)memoryUser.getServicePasswords(service.getId());
        if (servicePasswords == null) {
            servicePasswords = new MemoryServicePasswords();
            servicePasswords.setStaticPwHash(passwordHash);
            memoryUser.setServicePasswords(service.getId(), servicePasswords);
        } else {
            servicePasswords.setStaticPwHash(passwordHash);
        }

        return password;
    }

    @Override
    public void logout(User user, UserSession session) {
        ((MemoryUser)user).removeSession((MemoryUserSession)session);
    }

    @Override
    public void updateLastActive(User user) {
        ((MemoryUser)user).setLastActive(System.currentTimeMillis());
    }

    @Override
    public void updateSession(UserSession session, Credential credential) {
        MemoryUserSession memoryUserSession = (MemoryUserSession)session;
        memoryUserSession.setOpenIdAccessToken(credential.getAccessToken());
        memoryUserSession.setOpenIdTokenExpiry(credential.getExpirationTimeMilliseconds());
    }

    @Override
    public boolean shouldRefreshProactively(User user, UserSession session) {
        long remainingLifetime = session.getOpenIdTokenExpiry() - System.currentTimeMillis();
        // We refresh proactively if the token has less than half of its lifetime left.
        // However don't bother if the token expires in less than 10 seconds because that may not be enough time until we'll have to force the refresh anyway.
        if (remainingLifetime > 10000L && remainingLifetime < session.getOpenIdTokenLifetime() / 2) {
            // to avoid parallel refreshes we set the remainingLifetime to 0. This is racy, but we don't care because worst case we'll do two refreshes in parallel, which doesn't really hurt.
            ((MemoryUserSession)session).setOpenIdTokenLifetime(0L);
        }
        return false;
    }
}
