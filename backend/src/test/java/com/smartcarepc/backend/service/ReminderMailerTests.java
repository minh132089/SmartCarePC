package com.smartcarepc.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

class ReminderMailerTests {
    static final String HELPER = "https://smartcarepc.pages.dev/download/helper";
    @SuppressWarnings("unchecked")
    final ReminderMailer mailer = new ReminderMailer(mock(ObjectProvider.class), "sender@example.com", HELPER);
    static ReminderMailer.Item item(String task, String title) { return new ReminderMailer.Item(task, title, "2026-12-05", "https://smartcarepc.pages.dev/?baotri=" + task + "&nguon=email", 1); }

    @Test void veSinhBuiChiCoDungCuVaLinkBaiHuongDan() {
        var m = mailer.compose(List.of(item("dust", "Clean out dust")), "https://smartcarepc.pages.dev/?huyemail=t");
        assertThat(m.subject()).isEqualTo("Time to clean your PC");
        assertThat(m.text()).contains("Get these ready: No. 2 Phillips screwdriver, Dry soft brush", "?baotri=dust&nguon=email").doesNotContain(HELPER);
        assertThat(m.html()).contains("<li>Compressed air or electronics blower</li>", "baotri=dust&amp;nguon=email").doesNotContain("SmartCarePC Helper");
    }

    @Test void thayKeoCoDungCuRieng() {
        var m = mailer.compose(List.of(item("paste", "Replace thermal paste")), "x");
        assertThat(m.subject()).isEqualTo("Time for: replace thermal paste");
        assertThat(m.text()).contains("isopropyl alcohol", "Non-conductive thermal paste").doesNotContain(HELPER);
    }

    @Test void kiemTraSucKhoeKemLinkTaiApp() {
        var m = mailer.compose(List.of(item("check", "Quick health check")), "x");
        assertThat(m.text()).contains("No tools needed", HELPER, "More info, then Run anyway");
        assertThat(m.html()).contains("Download SmartCarePC Helper", HELPER);
        // Chưa có link tải thì bỏ hẳn phần app
        @SuppressWarnings("unchecked")
        var noHelper = new ReminderMailer(mock(ObjectProvider.class), "sender@example.com", "");
        assertThat(noHelper.compose(List.of(item("check", "Quick health check")), "x").html()).doesNotContain("SmartCarePC Helper");
    }

    @Test void chuaCoGmailThiKhongGuiDuoc() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        assertThat(new ReminderMailer(provider, "", HELPER).available()).isFalse();
    }
}
