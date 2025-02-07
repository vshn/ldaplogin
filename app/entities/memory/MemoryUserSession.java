package entities.memory;

import entities.UserSession;
import util.SimpleSHA512;

import java.util.Objects;

public class MemoryUserSession implements UserSession {
    private String hashedId;
    private String openIdIdentityToken;
    private String openIdAccessToken;
    private String openIdRefreshToken;
    private Long openIdTokenExpiry;
    private Long openIdTokenLifetime;

    public MemoryUserSession(String id, String openIdIdentityToken, String openIdAccessToken, String openIdRefreshToken, Long openIdTokenExpiry) {
        this.hashedId = new SimpleSHA512().hash(id);
        this.openIdIdentityToken = openIdIdentityToken;
        this.openIdAccessToken = openIdAccessToken;
        this.openIdRefreshToken = openIdRefreshToken;
        this.openIdTokenExpiry = openIdTokenExpiry;
        this.openIdTokenLifetime = openIdTokenExpiry - System.currentTimeMillis();
    }

    @Override
    public String getHashedId() {
        return hashedId;
    }

    @Override
    public String getOpenIdIdentityToken() {
        return openIdIdentityToken;
    }

    @Override
    public String getOpenIdAccessToken() {
        return openIdAccessToken;
    }

    public void setOpenIdAccessToken(String openIdIdentityToken) {
        this.openIdAccessToken = openIdIdentityToken;
    }

    @Override
    public String getOpenIdRefreshToken() {
        return openIdRefreshToken;
    }

    @Override
    public Long getOpenIdTokenExpiry() {
        return openIdTokenExpiry;
    }

    @Override
    public Long getOpenIdTokenLifetime() {
        return openIdTokenLifetime;
    }

    public void setOpenIdTokenExpiry(Long openIdTokenExpiry) {
        this.openIdTokenExpiry = openIdTokenExpiry;
        this.openIdTokenLifetime = openIdTokenExpiry - System.currentTimeMillis();
    }

    public void setOpenIdTokenLifetime(Long openIdTokenLifetime) {
        this.openIdTokenLifetime = openIdTokenLifetime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryUserSession that = (MemoryUserSession) o;
        return Objects.equals(hashedId, that.hashedId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(hashedId);
    }
}
