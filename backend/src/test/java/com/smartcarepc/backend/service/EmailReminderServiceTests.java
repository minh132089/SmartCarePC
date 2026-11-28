package com.smartcarepc.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import com.smartcarepc.backend.entity.EmailReminder;
import com.smartcarepc.backend.repository.EmailReminderRepository;
import com.smartcarepc.backend.service.EmailReminderService.*;
import tools.jackson.databind.json.JsonMapper;

class EmailReminderServiceTests {
    static final String ORIGIN = "https://smartcarepc.pages.dev";
    final EmailReminderRepository repo = mock(EmailReminderRepository.class);
    final ReminderMailer mailer = mock(ReminderMailer.class);
    final EmailProofs proofs = new EmailProofs("test-secret-0123456789", Clock.systemUTC());
    final EmailReminderService service = new EmailReminderService(repo, mailer, proofs, JsonMapper.builder().build(), List.of(ORIGIN, "http://localhost:5173"), "Asia/Ho_Chi_Minh");
    static final LocalDate TODAY = LocalDate.of(2026, 12, 5);

    @BeforeEach void setUp() {
        service.setClock(Clock.fixed(TODAY.atTime(9, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(), ZoneId.of("Asia/Ho_Chi_Minh")));
        when(mailer.available()).thenReturn(true);
        when(repo.save(any())).thenAnswer(i -> { EmailReminder e = i.getArgument(0); if (e.getId() == null) e.setId(1L); return e; });
    }

    static List<String> tasks(List<Due> due) { return due.stream().map(Due::task).toList(); }

    @Test void nhacDungHanRoiNhacLaiMotLanSau7Ngay() {
        var dates = Map.of("dust", "2026-12-05", "paste", "2029-05-28", "check", "2026-12-06");
        Map<String, Integer> months = Map.of();
        assertThat(tasks(EmailReminderService.due(dates, months, Map.of(), TODAY))).containsExactly("dust");
        var once = Map.of("dust", new Sent("2026-12-05", "2026-12-05", 1));
        assertThat(EmailReminderService.due(dates, months, once, TODAY)).isEmpty();
        assertThat(tasks(EmailReminderService.due(dates, months, once, TODAY.plusDays(6)))).containsExactly("check");
        assertThat(EmailReminderService.due(dates, months, once, TODAY.plusDays(7))).first().isEqualTo(new Due("dust", "2026-12-05", 2));
        var twice = Map.of("dust", new Sent("2026-12-05", "2026-12-12", 2));
        assertThat(EmailReminderService.due(Map.of("dust", "2026-12-05"), months, twice, TODAY.plusDays(30))).isEmpty();
        // Làm xong / hoãn: ngày hẹn mới thì nhắc lại từ đầu khi tới hạn
        assertThat(tasks(EmailReminderService.due(Map.of("dust", "2026-12-12"), months, twice, TODAY.plusDays(7)))).containsExactly("dust");
    }

    @Test void khongQuayLaiWebThiVanTuNhacTheoChuKy() {
        var dates = Map.of("dust", "2026-12-05");
        var twice = Map.of("dust", new Sent("2026-12-05", "2026-12-12", 2));
        // Vệ sinh bụi 6 tháng: tới 5/6/2027 server tự nhắc lần sau, dù web vẫn gửi ngày gốc 5/12/2026
        assertThat(EmailReminderService.due(dates, Map.of(), twice, LocalDate.of(2027, 6, 4))).isEmpty();
        assertThat(EmailReminderService.due(dates, Map.of(), twice, LocalDate.of(2027, 6, 5))).containsExactly(new Due("dust", "2027-06-05", 1));
        // Nhà có thú nuôi: web gửi chu kỳ 3 tháng
        assertThat(EmailReminderService.due(dates, Map.of("dust", 3), twice, LocalDate.of(2027, 3, 5))).containsExactly(new Due("dust", "2027-03-05", 1));
        // Tắt máy lâu: chỉ nhắc lần gần nhất, không dồn nhiều thư
        assertThat(EmailReminderService.due(dates, Map.of(), Map.of(), LocalDate.of(2029, 1, 1))).containsExactly(new Due("dust", "2028-12-05", 1));
        // Ngày cuối tháng không trôi: 31/1 + 1 tháng = 28/2, + 2 tháng = 31/3
        assertThat(EmailReminderService.occurrence(LocalDate.of(2027, 1, 31), 1, LocalDate.of(2027, 3, 31))).isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(EmailReminderService.occurrence(LocalDate.of(2027, 1, 31), 1, LocalDate.of(2027, 3, 30))).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test void chuKySaiThiTuChoi() {
        assertThat(EmailReminderService.months(Map.of("dust", 3))).containsEntry("dust", 3).containsEntry("paste", 30).containsEntry("check", 1);
        assertThatThrownBy(() -> EmailReminderService.months(Map.of("dust", 0))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> EmailReminderService.months(Map.of("wash", 3))).isInstanceOf(ResponseStatusException.class);
    }

    @Test void batGuiThuXacNhanVaChuaNhacTruocKhiXacNhan() {
        var created = service.create(" Minh@Example.com ", new Plan(Map.of("dust", "2026-12-05"), null, ORIGIN), null);
        var row = ArgumentCaptor.forClass(EmailReminder.class);
        verify(repo).save(row.capture());
        assertThat(row.getValue().getEmail()).isEqualTo("minh@example.com");
        assertThat(row.getValue().isConfirmed()).isFalse();
        assertThat(created.token()).isEqualTo(row.getValue().getManageToken()).isNotEqualTo(row.getValue().getConfirmToken());
        verify(mailer).sendConfirm("minh@example.com", ORIGIN + "/?nhacemail=" + row.getValue().getConfirmToken());
        when(repo.findByConfirmedTrue()).thenReturn(List.of());
        service.sendDue();
        verify(mailer, never()).sendReminder(any(), any(), any());
    }

    @Test void dangNhapGoogleThiBatNgayKhongCanThuXacNhan() {
        var old = EmailReminder.builder().id(9L).email("minh@example.com").confirmed(true).build();
        when(repo.findByEmailAndConfirmedTrue("minh@example.com")).thenReturn(List.of(old));
        var created = service.create("Minh@example.com", new Plan(Map.of("dust", "2026-12-05"), null, ORIGIN), proofs.issue("minh@example.com"));
        assertThat(created.status()).isEqualTo("active");
        var row = ArgumentCaptor.forClass(EmailReminder.class);
        verify(repo).save(row.capture());
        assertThat(row.getValue().isConfirmed()).isTrue();
        verify(repo).delete(old);
        verify(mailer, never()).sendConfirm(any(), any());
    }

    @Test void bangChungCuaEmailKhacHoacGiaThiVanPhaiXacNhan() {
        assertThat(service.create("a@b.co", new Plan(Map.of("dust", "2026-12-05"), null, ORIGIN), proofs.issue("other@b.co")).status()).isEqualTo("pending");
        var forged = new EmailProofs("another-secret-0123456789", Clock.systemUTC()).issue("a@b.co");
        assertThat(service.create("a@b.co", new Plan(Map.of("dust", "2026-12-05"), null, ORIGIN), forged).status()).isEqualTo("pending");
        assertThat(service.create("a@b.co", new Plan(Map.of("dust", "2026-12-05"), null, ORIGIN), "ep.garbage").status()).isEqualTo("pending");
        verify(mailer, times(3)).sendConfirm(eq("a@b.co"), any());
    }

    @Test void tuChoiDuLieuSaiVaGioiHanSoLanBat() {
        assertThatThrownBy(() -> service.create("not-an-email", new Plan(Map.of("dust", "2026-12-05"), null, ORIGIN), null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.create("a@b.co", new Plan(Map.of("dust", "2026-12-05"), null, "https://evil.example"), null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.create("a@b.co", new Plan(Map.of("wash", "2026-12-05"), null, ORIGIN), null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.create("a@b.co", new Plan(Map.of("dust", "2026-02-30"), null, ORIGIN), null)).isInstanceOf(ResponseStatusException.class);
        when(repo.countByEmailAndCreatedAtAfter(eq("a@b.co"), any())).thenReturn(3L);
        assertThatThrownBy(() -> service.create("a@b.co", new Plan(Map.of("dust", "2026-12-05"), null, ORIGIN), null))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(429));
        verify(mailer, never()).sendConfirm(any(), any());
    }

    @Test void chuaCauHinhGmailThiKhongBatDuoc() {
        when(mailer.available()).thenReturn(false);
        assertThatThrownBy(() -> service.create("a@b.co", new Plan(Map.of("dust", "2026-12-05"), null, ORIGIN), null))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(503));
        service.sendDue();
        verify(repo, never()).findByConfirmedTrue();
    }

    @Test void xacNhanThayLichCuCuaCungEmail() {
        var old = EmailReminder.builder().id(1L).email("a@b.co").confirmed(true).build();
        var fresh = EmailReminder.builder().id(2L).email("a@b.co").confirmToken("confirm-token-abcdefghijk").confirmed(false).build();
        when(repo.findByConfirmToken("confirm-token-abcdefghijk")).thenReturn(Optional.of(fresh));
        when(repo.findByEmailAndConfirmedTrue("a@b.co")).thenReturn(List.of(old));
        assertThat(service.confirm("confirm-token-abcdefghijk").status()).isEqualTo("active");
        verify(repo).delete(old);
        assertThat(fresh.isConfirmed()).isTrue();
        assertThatThrownBy(() -> service.confirm("unknown")).isInstanceOf(ResponseStatusException.class);
    }

    @Test void guiNhacKemLinkBaiHuongDanVaLinkHuy() {
        var row = EmailReminder.builder().id(5L).email("a@b.co").manageToken("manage-token-abcdefghijk").confirmed(true).origin(ORIGIN)
            .dates("{\"dust\":\"2026-12-05\",\"check\":\"2026-12-28\"}").sent("{}").build();
        when(repo.findByConfirmedTrue()).thenReturn(List.of(row));
        service.sendDue();
        verify(mailer).sendReminder("a@b.co", List.of(new ReminderMailer.Item("dust", "Clean out dust", "2026-12-05", ORIGIN + "/?baotri=dust&nguon=email", 1)),
            ORIGIN + "/?huyemail=manage-token-abcdefghijk");
        assertThat(row.getSent()).contains("\"dust\"", "\"n\":1");
        // Gửi rồi thì lần chạy sau trong ngày không gửi lại
        service.sendDue();
        verify(mailer, times(1)).sendReminder(any(), any(), any());
    }

    @Test void guiLoiThiKhongGhiLaDaGui() {
        var row = EmailReminder.builder().id(5L).email("a@b.co").manageToken("manage-token-abcdefghijk").confirmed(true).origin(ORIGIN)
            .dates("{\"dust\":\"2026-12-05\"}").sent("{}").build();
        when(repo.findByConfirmedTrue()).thenReturn(List.of(row));
        doThrow(new RuntimeException("smtp down")).when(mailer).sendReminder(any(), any(), any());
        service.sendDue();
        assertThat(row.getSent()).isEqualTo("{}");
    }

    @Test void capNhatVaTatBangToken() {
        var row = EmailReminder.builder().id(5L).email("a@b.co").manageToken("manage-token-abcdefghijk").confirmed(true).origin(ORIGIN).dates("{}").sent("{}").build();
        when(repo.findByManageToken("manage-token-abcdefghijk")).thenReturn(Optional.of(row));
        service.update("manage-token-abcdefghijk", new Plan(Map.of("dust", "2027-06-05"), null, null));
        assertThat(row.getDates()).contains("2027-06-05");
        assertThat(service.status("manage-token-abcdefghijk").status()).isEqualTo("active");
        service.stop("manage-token-abcdefghijk");
        verify(repo).delete(row);
        assertThatThrownBy(() -> service.status("../etc")).isInstanceOf(ResponseStatusException.class);
    }
}
