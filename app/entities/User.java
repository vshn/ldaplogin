package entities;

import scala.Dynamic;
import util.Config;

import java.util.ArrayList;
import java.util.List;

public interface User {
    long LASTACTIVE_UPDATE_INTERVAL = 1000L * 60L;

    long EXPIRES = Config.Option.USER_EXPIRES.getLong() * 1000L;

    List<String> NEVER_EXPIRES_GROUPS = Config.Option.USER_NEVER_EXPIRES_GROUPS.getStringList();

    List<? extends UserSession> getSessions();

    UserSession getSessionById(String sessionId);

    String getUid();

    String getEmail();

    Integer getEmailQuota();

    boolean isEmailVerified();

    String getFirstName();

    String getLastName();

    List<String> getGroupPaths();

    ServicePasswords getServicePasswords(String serviceId);

    boolean getLastActiveNeedsUpdate();

    Long getLastActive();

    default boolean neverExpires() {
        return getGroupPaths().stream().filter(NEVER_EXPIRES_GROUPS::contains).findFirst().isPresent();
    }

    default byte[][] getActivePasswords(String serviceId) {
        List<byte[]> activePasswords = new ArrayList<>();

        ServicePasswords servicePasswords = getServicePasswords(serviceId);

        if (servicePasswords == null) {
            return new byte[0][];
        }

        long now = System.currentTimeMillis();

        for (DynamicPassword dynamicPassword : servicePasswords.getDynamicPasswords()) {
            if (dynamicPassword.getTimestamp() > (now - ServicePasswords.DYNAMIC_PASSWORD_EXPIRES)) {
                activePasswords.add(dynamicPassword.getHash());
            }
        }

        if (servicePasswords.getStaticPwHash() != null && (neverExpires() || getLastActive() > (now - ServicePasswords.STATIC_PASSWORD_EXPIRES))) {
            activePasswords.add(servicePasswords.getStaticPwHash());
        }

        byte[][] pws = new byte[activePasswords.size()][];
        return activePasswords.toArray(pws);
    }
}
