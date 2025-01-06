package entities;

public interface DynamicPassword {
    long getTimestamp();

    String getHashBase64();

    byte[] getHash();
}
