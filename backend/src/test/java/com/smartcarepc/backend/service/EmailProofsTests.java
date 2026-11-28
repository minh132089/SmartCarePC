package com.smartcarepc.backend.service;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.Test;

class EmailProofsTests {
    @Test void chiNhanBangChungDungChuKyVaConHan() {
        var start = Instant.parse("2026-11-28T00:00:00Z");
        var proofs = new EmailProofs("test-secret-0123456789", Clock.fixed(start, ZoneOffset.UTC));
        String proof = proofs.issue(" Minh@Example.com ");
        assertThat(proofs.verifiedEmail(proof)).isEqualTo("minh@example.com");
        // Sửa email trong bằng chứng thì chữ ký sai
        String[] parts = proof.split("\\.");
        String other = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(("attacker@x.co|" + (start.getEpochSecond() + 999999)).getBytes());
        assertThat(proofs.verifiedEmail(parts[0] + "." + other + "." + parts[2])).isNull();
        assertThat(proofs.verifiedEmail(null)).isNull();
        assertThat(proofs.verifiedEmail("ep.abc")).isNull();
        var later = new EmailProofs("test-secret-0123456789", Clock.fixed(start.plus(Duration.ofDays(31)), ZoneOffset.UTC));
        assertThat(later.verifiedEmail(proof)).isNull();
    }
}
