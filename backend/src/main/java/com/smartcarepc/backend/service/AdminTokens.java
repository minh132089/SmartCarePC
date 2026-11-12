package com.smartcarepc.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Vé xem dashboard cho tài khoản quản trị: "adm.<email|hạn>.<chữ ký>", ký HMAC-SHA256 bằng khoá ngẫu nhiên tạo lúc
 * khởi động (khởi động lại backend thì vé cũ hết hiệu lực, chỉ cần đăng nhập lại). Vé chỉ cho xem, không cho tải CSV.
 */
@Component
public class AdminTokens {
    public static final String PREFIX = "adm.";
    static final long TTL_SECONDS = 12 * 3600;
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final byte[] secret = new byte[32];
    private final Clock clock;

    public AdminTokens() { this(Clock.systemUTC()); }

    AdminTokens(Clock clock) {
        new SecureRandom().nextBytes(secret);
        this.clock = clock;
    }

    public record Issued(String token, long expiresAt) {}

    public Issued issue(String email) {
        long exp = clock.instant().getEpochSecond() + TTL_SECONDS;
        String body = ENC.encodeToString((email + "|" + exp).getBytes(StandardCharsets.UTF_8));
        return new Issued(PREFIX + body + "." + ENC.encodeToString(sign(body)), exp);
    }

    public boolean valid(String token) {
        if (token == null || !token.startsWith(PREFIX)) return false;
        String rest = token.substring(PREFIX.length());
        int dot = rest.indexOf('.');
        if (dot <= 0) return false;
        String body = rest.substring(0, dot);
        try {
            if (!MessageDigest.isEqual(sign(body), DEC.decode(rest.substring(dot + 1)))) return false;
            String plain = new String(DEC.decode(body), StandardCharsets.UTF_8);
            long exp = Long.parseLong(plain.substring(plain.lastIndexOf('|') + 1));
            return clock.instant().getEpochSecond() < exp;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return false;
        }
    }

    private byte[] sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
