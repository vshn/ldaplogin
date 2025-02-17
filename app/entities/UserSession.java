package entities;

public interface UserSession {
    String getHashedId();

    String getOpenIdAccessToken();

    String getOpenIdRefreshToken();

    Long getOpenIdTokenExpiry();

    Long getOpenIdTokenLifetime();
}
