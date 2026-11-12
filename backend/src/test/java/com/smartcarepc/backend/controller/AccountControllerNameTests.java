package com.smartcarepc.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AccountControllerNameTests {
    private static AccountController.Credentials of(String first, String last) {
        return new AccountController.Credentials("minh@example.com", "12345678", first, last);
    }

    @Test
    void dungHoTenNguoiDungNhap() {
        assertThat(AccountController.fullName(of("  Huy  Minh ", " Chu "), "minh@example.com")).isEqualTo("Huy Minh Chu");
    }

    @Test
    void khongNhapTenThiLayPhanTruocAcongEmail() {
        assertThat(AccountController.fullName(of(null, "  "), "minh@example.com")).isEqualTo("minh");
    }

    @Test
    void tenQuaDaiBiCatCon100KyTu() {
        assertThat(AccountController.fullName(of("a".repeat(80), "b".repeat(80)), "minh@example.com")).hasSize(100);
    }
}
