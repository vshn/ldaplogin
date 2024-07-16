package store;

import entities.OpenIdUser;
import entities.User;
import entities.UserSession;
import play.mvc.Http;
import services.OpenId;

import java.util.*;
import java.util.stream.Stream;

public interface UsersStore {
    User getFromRequest(Http.Request request, OpenId openId);

    User getByUid(String uid);

    User create(OpenIdUser openIdUser);

    boolean update(User user, OpenIdUser openIdUser);

    Stream<? extends User> getByGroupPath(String groupPath);

    List<? extends User> getAll();

    UserSession createSession(User user, String sessionId, String openIdIdentityToken, String openIdAccessToken, String openIdRefreshToken, Long openIdTokenExpiry);

    byte[] getEncryptionKey();

    String generatePassword(User user);

    void logout(User user, UserSession session);
}
