package entities.mongodb;

import dev.morphia.annotations.*;
import entities.OpenIdUser;
import entities.User;
import org.apache.commons.codec.binary.Hex;
import org.bson.types.ObjectId;
import util.SimpleSHA512;

import java.util.ArrayList;
import java.util.List;

@Entity(value = "users", useDiscriminator = false)
public class MongoDbUser implements User {
    @Id
    private ObjectId _id;

    @Indexed(options = @IndexOptions(unique = true))
    private String uid;

    private String firstName;

    private String lastName;

    private String email;

    private boolean emailVerified;

    private List<String> groupPaths;

    private List<MongoDbUserSession> sessions = new ArrayList<>();

    private String passwordHash;

    @Indexed // for fast cleanup
    private Long lastActive;

    public MongoDbUser() {
        // dummy constructor for Morphia
    }

    public MongoDbUser(OpenIdUser openIdUser) {
        this.uid = openIdUser.getUid();
        this.firstName = openIdUser.getFirstName();
        this.lastName = openIdUser.getLastName();
        this.email = openIdUser.getEmail();
        this.emailVerified = openIdUser.isEmailVerified();
        this.groupPaths = openIdUser.getGroupPaths();
        lastActive = System.currentTimeMillis();
    }

    public ObjectId getObjectId() {
        return _id;
    }

    @Override
    public List<MongoDbUserSession> getSessions() {
        return sessions;
    }

    public void addSession(MongoDbUserSession session) {
        sessions.add(session);
    }

    public void removeSession(MongoDbUserSession session) {
        sessions.remove(session);
    }


    @Override
    public MongoDbUserSession getSessionById(String sessionId) {
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
        return passwordHash;
    }

    @Override
    public byte[] getPasswordHash() {
        try {
            return Hex.decodeHex(passwordHash);
        } catch (Exception e) {
            return null;
        }
    }

    public void setPasswordHash(byte[] passwordHash) {
        this.passwordHash = new String(Hex.encodeHex(passwordHash));
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