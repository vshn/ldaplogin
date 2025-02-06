package entities;

public interface UserSession {
    String getHashedId();

    String getOpenIdIdentityToken();

    String getOpenIdAccessToken();

    String getOpenIdRefreshToken();

    Long getOpenIdTokenExpiry();

    Long getOpenIdTokenLifetime();
}
