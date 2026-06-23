package com.smartstock.identity.infrastructure.security;

import com.smartstock.identity.infrastructure.config.SecurityProperties;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class JwtKeyPairGenerator {

    private final KeyPair keyPair;

    public JwtKeyPairGenerator(SecurityProperties securityProperties) {
        this.keyPair = createKeyPair(securityProperties.getJwt());
    }

    public RSAPrivateKey privateKey() {
        return (RSAPrivateKey) keyPair.getPrivate();
    }

    public RSAPublicKey publicKey() {
        return (RSAPublicKey) keyPair.getPublic();
    }

    private KeyPair createKeyPair(SecurityProperties.Jwt jwtProperties) {
        try {
            if (hasConfiguredKeys(jwtProperties)) {
                return new KeyPair(parsePublicKey(jwtProperties.getPublicKey()), parsePrivateKey(jwtProperties.getPrivateKey()));
            }
            if (!jwtProperties.isGenerateKeyPair()) {
                throw new IllegalStateException("JWT RSA keys must be configured when key pair generation is disabled.");
            }
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to initialize JWT RSA key pair.", exception);
        }
    }

    private boolean hasConfiguredKeys(SecurityProperties.Jwt jwtProperties) {
        return jwtProperties.getPrivateKey() != null
                && !jwtProperties.getPrivateKey().isBlank()
                && jwtProperties.getPublicKey() != null
                && !jwtProperties.getPublicKey().isBlank();
    }

    private RSAPrivateKey parsePrivateKey(String value) throws GeneralSecurityException {
        byte[] decoded = Base64.getMimeDecoder().decode(stripPemDecorators(value));
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        return (RSAPrivateKey) key;
    }

    private RSAPublicKey parsePublicKey(String value) throws GeneralSecurityException {
        byte[] decoded = Base64.getMimeDecoder().decode(stripPemDecorators(value));
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
        return (RSAPublicKey) key;
    }

    private String stripPemDecorators(String value) {
        return value
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
    }
}
