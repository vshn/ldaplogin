package entities.memory;

import entities.DynamicPassword;

import java.util.Base64;

public class MemoryDynamicPassword implements DynamicPassword {
    private long timestamp;
    private String hash;

    public MemoryDynamicPassword() {
        // dummy for Morphia
    }

    public MemoryDynamicPassword(byte[] hash) {
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
