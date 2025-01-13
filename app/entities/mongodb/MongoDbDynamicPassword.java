package entities.mongodb;

import dev.morphia.annotations.Entity;
import entities.DynamicPassword;

import java.util.Base64;

@Entity(useDiscriminator = false)
public class MongoDbDynamicPassword implements DynamicPassword {
    private long timestamp;
    private String hash;

    public MongoDbDynamicPassword() {
        // dummy for Morphia
    }

    public MongoDbDynamicPassword(byte[] hash) {
        this.timestamp = System.currentTimeMillis();
        this.hash = Base64.getEncoder().encodeToString(hash);
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getHashBase64() {
        return  hash;
    }

    @Override
    public byte[] getHash() {
        try {
            return Base64.getDecoder().decode(hash);
        } catch (Exception e) {
            return null;
        }
    };
}
