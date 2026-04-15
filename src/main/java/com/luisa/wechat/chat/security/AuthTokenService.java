package com.luisa.wechat.chat.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {

    public static final String COOKIE_NAME = "wechat_auth";
    private static final Duration TOKEN_TTL = Duration.ofHours(6);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    public AuthTokenService(@Value("${app.auth.token-secret:wechat-dev-token-secret-change-me}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String createToken(String username) {
        long expiresAt = Instant.now().plus(TOKEN_TTL).getEpochSecond();
        String payload = username + ":" + expiresAt;
        String signature = sign(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "."
                + signature;
    }

    public Optional<String> verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }

        if (!MessageDigest.isEqual(parts[1].getBytes(StandardCharsets.UTF_8), sign(payload).getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        String[] payloadParts = payload.split(":", 2);
        if (payloadParts.length != 2) {
            return Optional.empty();
        }

        long expiresAt;
        try {
            expiresAt = Long.parseLong(payloadParts[1]);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }

        if (Instant.now().getEpochSecond() >= expiresAt) {
            return Optional.empty();
        }

        return Optional.of(payloadParts[0]);
    }

    public ResponseCookie createCookie(String token, boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(TOKEN_TTL)
                .build();
    }

    public ResponseCookie clearCookie(boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign auth token", ex);
        }
    }
}
