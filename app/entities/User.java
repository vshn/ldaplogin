package entities;

import util.Config;
import java.util.List;

public interface User {
    long LASTACTIVE_UPDATE_INTERVAL = 1000L * 60L;

    long EXPIRES = Config.Option.USER_EXPIRES.getLong() * 1000L;

    List<? extends UserSession> getSessions();

    UserSession getSessionById(String sessionId);

    String getUid();

    String getEmail();

    boolean isEmailVerified();

    String getFirstName();

    String getLastName();

    List<String> getGroupPaths();

    String getPasswordHashHexEncoded();

    byte[] getPasswordHash();

    boolean getLastActiveNeedsUpdate();

    Long getLastActive();
}
