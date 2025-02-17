package store;

import com.google.api.client.auth.oauth2.Credential;
import entities.OpenIdUser;
import entities.Service;
import entities.User;
import entities.UserSession;
import play.mvc.Http;
import services.OpenId;

import java.util.stream.Stream;

public interface UsersStore {
    User getFromRequest(Http.Request request, OpenId openId);

    User getByUid(String uid);

    User create(OpenIdUser openIdUser);

    boolean update(User user, OpenIdUser openIdUser);

    Stream<? extends User> getByGroupPath(String groupPath);

    UserSession createSession(User user, String sessionId, String openIdAccessToken, String openIdRefreshToken, Long openIdTokenExpiry);

    byte[] getEncryptionKey();

    String generateDynamicPassword(User user, Service service);

    String generateStaticPassword(User user, Service service);

    void logoutWebOnly(User user, UserSession session);

    void logout(User user, UserSession session);

    void updateLastActive(User user);

    void updateSession(UserSession session, Credential credential);

    boolean shouldRefreshProactively(User user, UserSession session);
}
