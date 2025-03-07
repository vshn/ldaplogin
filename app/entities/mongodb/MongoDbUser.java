package entities.mongodb;

import dev.morphia.annotations.*;
import entities.OpenIdUser;
import entities.ServicePasswords;
import entities.User;
import org.bson.types.ObjectId;
import util.SimpleSHA512;

import java.util.*;

@Entity(value = "users", useDiscriminator = false)
public class MongoDbUser implements MongoDbEntity, User {
    @Id
    private ObjectId _id;

    @Indexed(options = @IndexOptions(unique = true))
    private String uid;

    private String firstName;

    private String lastName;

    private String email;

    private Integer emailQuota;

    private boolean emailVerified;

    private List<String> alias = new ArrayList<>();

    @Indexed
    private List<String> groupPaths = new ArrayList<>();

    private List<MongoDbUserSession> sessions = new ArrayList<>();

    private Map<String, MongoDbServicePasswords> servicePasswords = new HashMap<>();

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
        this.emailQuota = openIdUser.getEmailQuota();
        this.emailVerified = openIdUser.isEmailVerified();
        this.alias = openIdUser.getAlias();
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
        return sessions.stream().filter(s -> hashedId.equals(s.getHashedId())).findFirst().orElse(null);
    }

    public void setKey(byte[] key) {
        if (sessions != null) {
            sessions.forEach(s -> s.setKey(key));
        }
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
    public Integer getEmailQuota() {
        return emailQuota;
    }

    public void setEmailQuota(Integer emailQuota) {
        this.emailQuota = emailQuota;
    }

    @Override
    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @Override
    public List<String> getAlias() {
        return alias;
    }

    public void setAlias(List<String> alias) {
        this.alias = alias;
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

    @Override
    public ServicePasswords getServicePasswords(String serviceId) {
        return servicePasswords.get(serviceId);
    }

    public void setServicePasswords(String serviceId, ServicePasswords servicePasswords) {
        this.servicePasswords.put(serviceId, (MongoDbServicePasswords)servicePasswords);
    }

    public void setGroupPaths(List<String> groupPaths) {
        this.groupPaths = new ArrayList<>(groupPaths);
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