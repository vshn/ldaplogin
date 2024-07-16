package services.ldap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.*;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.*;

import com.google.common.base.Charsets;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.filter.ssl.SslFilter;
import util.Config;
import util.CustomLogger;
import util.InputUtils;

public final class LdapsInitializer
{
    private final CustomLogger logger = new CustomLogger(this.getClass());

    private final SecureRandom secureRandom = new SecureRandom();

    public IoFilterChainBuilder init(LdapServer ldapServer, TcpTransport transport) throws LdapException
    {
        SSLContext sslContext;

        try {
            TrustManager[] trustManagers;
            KeyManager[] keyManagers;

            Certificate[] certs = getCertificates(Config.Option.LDAP_TLS_CERTIFICATE.get());
            RSAPrivateKey privateKey = getPrivateKey(Config.Option.LDAP_TLS_PRIVATE_KEY.get());
            if (certs == null || certs.length == 0 || privateKey == null) {
                return null;
            }

            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(null, null);
            TrustManagerFactory customCaTrustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            customCaTrustManager.init(trustStore);
            trustManagers = customCaTrustManager.getTrustManagers();

            KeyManagerFactory customCaKeyManager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            keyStore.setKeyEntry("ldaps-key-cert", privateKey, new char[0], certs);
            customCaKeyManager.init(keyStore, new char[0]);
            keyManagers = customCaKeyManager.getKeyManagers();

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, secureRandom);
        } catch (NoSuchAlgorithmException | KeyManagementException | IOException | KeyStoreException |
                 CertificateException | InvalidKeySpecException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }

        DefaultIoFilterChainBuilder chain = new DefaultIoFilterChainBuilder();
        SslFilter sslFilter = new SslFilter(sslContext);

        List<String> cipherSuites = transport.getCipherSuite();
        if ((cipherSuites != null) && !cipherSuites.isEmpty()) {
            sslFilter.setEnabledCipherSuites( cipherSuites.toArray(new String[cipherSuites.size()]));
        }

        List<String> enabledProtocols = transport.getEnabledProtocols();
        if ((enabledProtocols != null) && !enabledProtocols.isEmpty()) {
            sslFilter.setEnabledProtocols( enabledProtocols.toArray(new String[enabledProtocols.size()]));
        } else {
            // Disable SSLV3
            sslFilter.setEnabledProtocols("TLSv1", "TLSv1.1", "TLSv1.2");
        }

        sslFilter.setNeedClientAuth( transport.isNeedClientAuth() );
        sslFilter.setWantClientAuth( transport.isWantClientAuth() );
        chain.addLast( "sslFilter", sslFilter );
        return chain;
    }


    private Certificate[] getCertificates(String certs) {
        if (certs == null) {
            return null;
        }

        final String CERT_START= "-----BEGIN CERTIFICATE-----";
        List<Certificate> certificates = new LinkedList<>();
        certs = certs.replace("\r", ""); // some normalization

        for (String certStr : certs.split(CERT_START)) {
            certStr = InputUtils.trimToNull(certStr);
            if (certStr == null) {
                // split will generate an empty element at the first position, that's normal
                continue;
            }
            try {
                certStr = CERT_START + "\n" + certStr; // split has removed the separator, re-add it
                Certificate cert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certStr.getBytes(Charsets.UTF_8)));
                certificates.add(cert);
            } catch (Exception e) {
                logger.warn(null, "Certificate not in X.509 format: " + certStr);
            }
        }

        return certificates.toArray(new Certificate[0]);
    }

    private RSAPrivateKey getPrivateKey(String key) throws InvalidKeySpecException, NoSuchAlgorithmException {
        try {
            final String RSA_START = "-----BEGIN RSA PRIVATE KEY-----";
            final String RSA_END = "-----END RSA PRIVATE KEY-----";
            if (key.startsWith(RSA_START)) {
                key = key.trim();
                if (!key.endsWith(RSA_END)) {
                    throw new IllegalArgumentException("Private key is not in RSA/PKCS1 format, starts with " + RSA_START + " but does not end with " + RSA_END);
                }
                key = key.substring(RSA_START.length(), key.length() - RSA_END.length());
                key = key.replace("\n", "");
                key = key.replace("\r", "");
                key = key.replace(" ", "");

                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(pkcs8FromPkcs1(Base64.getDecoder().decode(key)));
                KeyFactory kf = KeyFactory.getInstance("RSA");
                RSAPrivateKey pk = (RSAPrivateKey)kf.generatePrivate(privateKeySpec);
                return pk;
            }

            final String PKCS8_START = "-----BEGIN PRIVATE KEY-----";
            final String PKCS8_END = "-----END PRIVATE KEY-----";
            if (key.startsWith(PKCS8_START)) {
                key = key.trim();
                if (!key.endsWith(PKCS8_END)) {
                    throw new IllegalArgumentException("Private key is not in PKCS8 format, starts with " + PKCS8_START + " but does not end with " + PKCS8_END);
                }
                key = key.substring(PKCS8_START.length(), key.length() - PKCS8_END.length());
                key = key.replace("\n", "");
                key = key.replace("\r", "");
                key = key.replace(" ", "");

                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key));
                KeyFactory kf = KeyFactory.getInstance("RSA");
                return (RSAPrivateKey) kf.generatePrivate(privateKeySpec);
            }
            throw new IllegalArgumentException("Private key does not look like RSA/PKCS1/PKCS8 format");
        } catch (Exception e) {
            logger.error(null, "Private key is not in RSA/PKCS1/PKCS8 format: " + e.getMessage());
            return null;
        }
    }

    private byte[] pkcs8FromPkcs1(byte[] innerKey) {
        var result = new byte[innerKey.length + 26];
        System.arraycopy(Base64.getDecoder().decode("MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKY="), 0, result, 0, 26);
        System.arraycopy(BigInteger.valueOf(result.length - 4).toByteArray(), 0, result, 2, 2);
        System.arraycopy(BigInteger.valueOf(innerKey.length).toByteArray(), 0, result, 24, 2);
        System.arraycopy(innerKey, 0, result, 26, innerKey.length);
        return result;
    }
}
