package entities.memory;

import entities.OpenIdUser;
import entities.User;
import entities.UserSession;
import util.SimpleSHA512;

import java.util.ArrayList;
import java.util.List;

public class MemoryUser implements User {

    private String uid;

    private String firstName;

    private String lastName;

    private String email;

    private boolean emailVerified;

    private List<String> groupPaths;

    private List<MemoryUserSession> sessions = new ArrayList<>();

    private byte[] passwordHash;

    private Long lastActive;

    public MemoryUser(OpenIdUser openIdUser) {
        this.uid = openIdUser.getUid();
        this.firstName = openIdUser.getFirstName();
        this.lastName = openIdUser.getLastName();
        this.email = openIdUser.getEmail();
        this.emailVerified = openIdUser.isEmailVerified();
        this.groupPaths = openIdUser.getGroupPaths();
        lastActive = System.currentTimeMillis();
    }


    @Override
    public List<? extends UserSession> getSessions() {
        return sessions;
    }

    public void addSession(MemoryUserSession session) {
        sessions.add(session);
    }

    public void removeSession(MemoryUserSession session) {
        sessions.remove(session);
    }

    @Override
    public MemoryUserSession getSessionById(String sessionId) {
        String hashedId = new SimpleSHA512().hash(sessionId);
        return sessions.stream().filter(s -> s.getHashedId().equals(hashedId)).findFirst().orElse(null);
    }

    @Override
    public String getUid() {
        return uid;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public List<String> getGroupPaths() {
        return groupPaths;
    }

    public void setGroupPaths(List<String> groupPaths) {
        this.groupPaths = new ArrayList<>(groupPaths);
    }

    @Override
    public String getPasswordHashHexEncoded() {
        throw new IllegalStateException();
    }

    @Override
    public byte[] getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(byte[] passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public boolean getLastActiveNeedsUpdate() {
        return lastActive == null ? true : lastActive < (System.currentTimeMillis() - LASTACTIVE_UPDATE_INTERVAL);
    }

    public void setLastActive(Long lastActive) {
        this.lastActive = lastActive;
    }

    @Override
    public Long getLastActive() {
        return lastActive;
    }

    @Override
    public String toString() {
        return uid;
    }
}