package entities;

import services.OpenId;
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

    default UserSession getNewestSession() {
        if (getSessions() == null) {
            return null;
        }
        UserSession newest = null;
        for (UserSession session : getSessions()) {
            if (newest == null || newest.getOpenIdTokenExpiry() < session.getOpenIdTokenExpiry()) {
                newest = session;
            }
        }
        return newest;
    }

    default byte[][] getActivePasswords(Service service, OpenId openId) {
        List<byte[]> activePasswords = new ArrayList<>();

        ServicePasswords servicePasswords = getServicePasswords(service.getId());
        if (servicePasswords == null) {
            return new byte[0][];
        }

        long now = System.currentTimeMillis();

        // The user needs an active session with the IDP in order to be able to log in to LDAP.
        // Because the OpenID sessions are tied to sessionId cookies which have nothing to do with LDAP,
        // we just find the newest session and make sure it's still active.
        if (openId.validateUserSession(null, this, getNewestSession())) {
            for (DynamicPassword dynamicPassword : servicePasswords.getDynamicPasswords()) {
                if (dynamicPassword.getTimestamp() > (now - service.getDynamicPasswordExpires() * 1000L)) {
                    activePasswords.add(dynamicPassword.getHash());
                }
            }

            if (servicePasswords.getStaticPwHash() != null) {
                activePasswords.add(servicePasswords.getStaticPwHash());
            }
        } else if (neverExpires()) {
            // System users keep working even if there is no OAuth session anymore.
            // As long as there is a working session with the IDP we try to keep that up though.
            if (servicePasswords.getStaticPwHash() != null) {
                activePasswords.add(servicePasswords.getStaticPwHash());
            }
        } else {
            System.out.println("Can't validate user session for " + this.getUid() + ", no passwords added to list");
        }

        byte[][] pws = new byte[activePasswords.size()][];
        return activePasswords.toArray(pws);
    }
}
