package store.memory;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import entities.OpenIdUser;
import entities.User;
import entities.UserSession;
import entities.memory.MemoryUser;
import entities.memory.MemoryUserSession;
import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import play.mvc.Http;
import services.OpenId;
import store.UsersStore;
import util.CustomLogger;
import util.IdGenerator;
import util.InputUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class MemoryUsersStore implements UsersStore {

    private final CustomLogger logger = new CustomLogger(this.getClass());

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
        MemoryUser user = users.stream().filter(u -> u.getSessionById(sessionId) != null).findFirst().orElse(null);
        if (user == null) {
            return null;
        }
        MemoryUserSession session = user.getSessionById(sessionId);
        if (session == null) {
            return null;
        }

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
            logger.info(request, "Refreshed OpenID session of " + user);
        }

        if (user.getLastActiveNeedsUpdate()) {
            user.setLastActive(System.currentTimeMillis());
        }

        return user;
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
        boolean updated = false;
        if (!Objects.equals(memoryUser.getEmail(), openIdUser.getEmail())) {
            memoryUser.setEmail(openIdUser.getEmail());
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
    public List<User> getAll() {
        List<User> usersList = new ArrayList<>(users);
        Collections.sort(usersList, Comparator.comparing(User::getUid));
        return usersList;
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
    public String generatePassword(User user) {
        MemoryUser memoryUser = (MemoryUser)user;
        String password = IdGenerator.generateSessionId();
        memoryUser.setPasswordHash(PasswordUtil.createStoragePassword(password, LdapSecurityConstants.HASH_METHOD_SSHA512));
        return password;
    }

    @Override
    public void logout(User user, UserSession session) {
        ((MemoryUser)user).removeSession((MemoryUserSession)session);
    }
}
