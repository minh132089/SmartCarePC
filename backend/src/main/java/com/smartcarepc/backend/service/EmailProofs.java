package com.smartcarepc.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Bằng chứng "Google đã xác minh email này": cấp lúc đăng nhập Google (GoogleAuthController), dạng "ep.<email|hạn>.<chữ ký>"
 * ký HMAC-SHA256. Bật email nhắc kèm bằng chứng đúng email thì không cần thư xác nhận.
 * Khoá lấy từ EMAIL_PROOF_SECRET; để trống thì tạo ngẫu nhiên lúc khởi động (khởi động lại backend thì bằng chứng cũ hết hạn,
 * người dùng chỉ phải bấm link xác nhận trong thư như tài khoản email / mật khẩu).
 */
@Component
public class EmailProofs {
    static final String PREFIX = "ep.";
    static final long TTL_SECONDS = 30L * 24 * 3600;
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final byte[] secret;
    private final Clock clock;

    @Autowired
    public EmailProofs(@Value("${app.reminders.proof-secret:}") String configured) { this(configured, Clock.systemUTC()); }

    EmailProofs(String configured, Clock clock) {
        if (configured != null && configured.length() >= 16) secret = configured.getBytes(StandardCharsets.UTF_8);
        else { secret = new byte[32]; new SecureRandom().nextBytes(secret); }
        this.clock = clock;
    }

    public String issue(String email) {
        long exp = clock.instant().getEpochSecond() + TTL_SECONDS;
        String body = ENC.encodeToString((normalize(email) + "|" + exp).getBytes(StandardCharsets.UTF_8));
        return PREFIX + body + "." + ENC.encodeToString(sign(body));
    }

    /** Email đã được Google xác minh nếu bằng chứng đúng chữ ký và còn hạn, không thì null */
    public String verifiedEmail(String proof) {
        if (proof == null || !proof.startsWith(PREFIX) || proof.length() > 1024) return null;
        String rest = proof.substring(PREFIX.length());
        int dot = rest.indexOf('.');
        if (dot <= 0) return null;
        String body = rest.substring(0, dot);
        try {
            if (!MessageDigest.isEqual(sign(body), DEC.decode(rest.substring(dot + 1)))) return null;
            String plain = new String(DEC.decode(body), StandardCharsets.UTF_8);
            int bar = plain.lastIndexOf('|');
            long exp = Long.parseLong(plain.substring(bar + 1));
            return clock.instant().getEpochSecond() < exp ? plain.substring(0, bar) : null;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    static String normalize(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }

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
