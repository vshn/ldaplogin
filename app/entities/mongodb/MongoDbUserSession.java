package entities.mongodb;

import dev.morphia.annotations.*;
import entities.UserSession;
import util.Config;
import util.SimpleAes;
import util.SimpleSHA512;

import java.util.Objects;

@Entity(useDiscriminator = false)
public class MongoDbUserSession implements UserSession {

    @Transient
    public static final long SESSION_EXPIRES = Config.Option.USER_SESSION_EXPIRES.getLong() * 1000L;

    @Indexed(options = @IndexOptions(unique = true, sparse = true))
    private String hashedId;
    private String openIdIdentityTokenEnc;
    private String openIdIdentityTokenIv;
    private String openIdAccessTokenEnc;
    private String openIdAccessTokenIv;
    private String openIdRefreshTokenEnc;
    private String openIdRefreshTokenIv;
    @Indexed // for fast cleanup
    private Long openIdTokenExpiry;
    private Long openIdTokenLifetime;

    @Transient
    private byte[] key;

    @Transient
    private String openIdIdentityTokenDecrypted;

    @Transient
    private String openIdAccessTokenDecrypted;

    @Transient
    private String openIdRefreshTokenDecrypted;

    public MongoDbUserSession() {
        // constructor for Morphia
    }

    public MongoDbUserSession(byte[] key, String id, String openIdIdentityToken, String openIdAccessToken, String openIdRefreshToken, Long openIdTokenExpiry) {
        this.key = key;
        this.hashedId = new SimpleSHA512().hash(id);

        if (openIdIdentityToken != null) {
            SimpleAes aes = new SimpleAes(key);
            this.openIdIdentityTokenIv = SimpleAes.generateIvHex();
            this.openIdIdentityTokenEnc = aes.encryptHex(openIdIdentityTokenIv, openIdIdentityToken);
            this.openIdIdentityTokenDecrypted = openIdIdentityToken;
        }

        if (openIdAccessToken != null) {
            SimpleAes aes = new SimpleAes(key);
            this.openIdAccessTokenIv = SimpleAes.generateIvHex();
            this.openIdAccessTokenEnc = aes.encryptHex(openIdAccessTokenIv, openIdAccessToken);
            this.openIdAccessTokenDecrypted = openIdAccessToken;
        }

        if (openIdRefreshToken != null) {
            SimpleAes aes = new SimpleAes(key);
            this.openIdRefreshTokenIv = SimpleAes.generateIvHex();
            this.openIdRefreshTokenEnc = aes.encryptHex(openIdRefreshTokenIv, openIdRefreshToken);
            this.openIdRefreshTokenDecrypted = openIdRefreshToken;
        }

        this.openIdTokenExpiry = openIdTokenExpiry;
        this.openIdTokenLifetime = openIdTokenExpiry - System.currentTimeMillis();
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    @Override
    public String getHashedId() {
        return hashedId;
    }

    @Override
    public String getOpenIdIdentityToken() {
        if (openIdIdentityTokenDecrypted == null) {
            SimpleAes aes = new SimpleAes(key);
            openIdIdentityTokenDecrypted = aes.decryptHex(openIdIdentityTokenIv, openIdIdentityTokenEnc);
        }
        return openIdIdentityTokenDecrypted;
    }

    @Override
    public String getOpenIdAccessToken() {
        if (openIdAccessTokenDecrypted == null && openIdAccessTokenIv != null && openIdAccessTokenEnc != null) {
            SimpleAes aes = new SimpleAes(key);
            openIdAccessTokenDecrypted = aes.decryptHex(openIdAccessTokenIv, openIdAccessTokenEnc);
        }
        return openIdAccessTokenDecrypted;
    }

    public void setOpenIdAccessToken(String openIdAccessToken) {
        if (openIdAccessToken == null) {
            this.openIdAccessTokenIv = null;
            this.openIdAccessTokenEnc = null;
            this.openIdAccessTokenDecrypted = null;
        } else {
            SimpleAes aes = new SimpleAes(key);
            this.openIdAccessTokenIv = SimpleAes.generateIvHex();
            this.openIdAccessTokenEnc = aes.encryptHex(openIdAccessTokenIv, openIdAccessToken);
            this.openIdAccessTokenDecrypted = openIdAccessToken;
        }
    }

    public String getOpenIdAccessTokenIv() {
        return openIdAccessTokenIv;
    }

    public String getOpenIdAccessTokenEnc() {
        return openIdAccessTokenEnc;
    }

    @Override
    public String getOpenIdRefreshToken() {
        if (openIdRefreshTokenDecrypted == null && openIdRefreshTokenIv != null && openIdRefreshTokenEnc != null) {
            SimpleAes aes = new SimpleAes(key);
            openIdRefreshTokenDecrypted = aes.decryptHex(openIdRefreshTokenIv, openIdRefreshTokenEnc);
        }
        return openIdRefreshTokenDecrypted;
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
        MongoDbUserSession that = (MongoDbUserSession) o;
        return Objects.equals(hashedId, that.hashedId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hashedId);
    }
}
