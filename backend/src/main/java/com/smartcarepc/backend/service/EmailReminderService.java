package com.smartcarepc.backend.service;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.smartcarepc.backend.entity.EmailReminder;
import com.smartcarepc.backend.repository.EmailReminderRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Email nhắc bảo dưỡng (thay cho app khay Windows). Luồng:
 * 1. Web (người đã đăng nhập) gửi email + ngày hẹn: tạo bản chờ xác nhận, trả manage token cho trình duyệt, gửi thư xác nhận.
 * 2. Chủ hộp thư bấm link xác nhận: bắt đầu nhận nhắc. Server không kiểm tra đăng nhập nên bước này chống bật hộ email người khác.
 *    Đăng nhập bằng Google thì có bằng chứng Google đã xác minh email (EmailProofs): bật ngay, không gửi thư xác nhận.
 * 3. Web gửi lại ngày hẹn mỗi khi lịch đổi (làm xong, hoãn). Mỗi giờ server xem việc nào tới hạn thì gửi thư.
 * Lặp tự động theo chu kỳ của từng việc (vd vệ sinh bụi 6 tháng): người dùng không quay lại web thì server vẫn tự nhắc lần
 * sau, lần sau nữa, cho tới khi tắt. Làm xong trên web thì web gửi ngày mới, chu kỳ tính lại từ ngày đó.
 * Mỗi lần tới hạn gửi tối đa 2 thư: đúng hạn và 7 ngày sau nếu ngày hẹn vẫn chưa đổi (chưa làm, chưa hoãn).
 */
@Service
public class EmailReminderService {
    private static final Logger log = LoggerFactory.getLogger(EmailReminderService.class);
    static final List<String> TASKS = List.of("dust", "paste", "check");
    static final Map<String, String> TITLES = Map.of("dust", "Clean out dust", "paste", "Replace thermal paste", "check", "Quick health check");
    /** Chu kỳ mặc định (tháng), giống TASKS trong src/data/maintenanceSchedule.ts; web gửi chu kỳ thật (vd bụi 3 tháng nếu nhà có thú nuôi) */
    static final Map<String, Integer> MONTHS = Map.of("dust", 6, "paste", 30, "check", 1);
    static final int REPEAT_AFTER_DAYS = 7, MAX_SENDS = 2, SIGNUPS_PER_DAY = 3;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final SecureRandom RANDOM = new SecureRandom();

    /** months: chu kỳ lặp từng việc (tháng). Không gửi thì dùng MONTHS */
    public record Plan(Map<String, String> dates, Map<String, Integer> months, String origin) {}
    /** Việc cần nhắc: lần tới hạn (ngày hẹn gốc cộng n chu kỳ) và là thư thứ mấy cho lần đó */
    record Due(String task, String date, int number) {}
    public record Status(String status, String email) {}
    public record Created(String token, String status) {}
    /** Lần gửi gần nhất của một việc: cho ngày hẹn nào, gửi hôm nào, là thư thứ mấy */
    public record Sent(String due, String on, int n) {}

    private final EmailReminderRepository repository;
    private final ReminderMailer mailer;
    private final EmailProofs proofs;
    private final JsonMapper json;
    private final List<String> origins;
    private final ZoneId zone;
    private Clock clock;

    @Autowired
    public EmailReminderService(EmailReminderRepository repository, ReminderMailer mailer, EmailProofs proofs, JsonMapper json,
                                @Value("${app.reminders.origins:https://smartcarepc.pages.dev}") List<String> origins,
                                @Value("${app.reminders.zone:Asia/Ho_Chi_Minh}") String zone) {
        this.repository = repository; this.mailer = mailer; this.proofs = proofs; this.json = json;
        this.origins = origins.stream().map(String::trim).toList();
        this.zone = ZoneId.of(zone);
        this.clock = Clock.system(this.zone);
    }

    void setClock(Clock clock) { this.clock = clock; }

    public boolean available() { return mailer.available(); }

    @Transactional
    public Created create(String rawEmail, Plan plan, String proof) {
        if (!available()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email reminders are not set up on this server");
        String email = email(rawEmail);
        var dates = dates(plan.dates());
        String origin = origin(plan.origin());
        // Chặn dùng form để gửi thư hàng loạt tới một địa chỉ
        if (repository.countByEmailAndCreatedAtAfter(email, LocalDateTime.now(clock).minusDays(1)) >= SIGNUPS_PER_DAY)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests for this email today. Try again tomorrow.");
        boolean verified = email.equals(proofs.verifiedEmail(proof));
        if (verified) dropActive(email, null);
        var row = repository.save(EmailReminder.builder().email(email).manageToken(token()).confirmToken(token()).confirmed(verified)
            .confirmedAt(verified ? LocalDateTime.now(clock) : null).origin(origin).dates(json.writeValueAsString(dates)).months(json.writeValueAsString(months(plan.months()))).sent("{}").build());
        if (verified) return new Created(row.getManageToken(), "active");
        mailer.sendConfirm(email, origin + "/?nhacemail=" + row.getConfirmToken());
        return new Created(row.getManageToken(), "pending");
    }

    @Transactional(readOnly = true)
    public Status status(String token) {
        var row = byToken(token);
        return new Status(row.isConfirmed() ? "active" : "pending", row.getEmail());
    }

    @Transactional
    public void update(String token, Plan plan) {
        var row = byToken(token);
        row.setDates(json.writeValueAsString(dates(plan.dates())));
        if (plan.months() != null) row.setMonths(json.writeValueAsString(months(plan.months())));
        if (plan.origin() != null) row.setOrigin(origin(plan.origin()));
        repository.save(row);
    }

    /** Tắt: xoá hẳn email khỏi máy chủ */
    @Transactional
    public void stop(String token) { repository.delete(byToken(token)); }

    @Transactional
    public Status confirm(String confirmToken) {
        var row = repository.findByConfirmToken(confirmToken == null ? "" : confirmToken)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This link has expired. Turn on email reminders again in SmartCarePC."));
        if (!row.isConfirmed()) {
            dropActive(row.getEmail(), row.getId());
            row.setConfirmed(true);
            row.setConfirmedAt(LocalDateTime.now(clock));
            repository.save(row);
        }
        return new Status("active", row.getEmail());
    }

    /** Mỗi email chỉ một lịch đang bật: bản mới thay bản cũ */
    private void dropActive(String email, Long keep) {
        for (var old : repository.findByEmailAndConfirmedTrue(email)) if (!old.getId().equals(keep)) repository.delete(old);
    }

    @Scheduled(cron = "${app.reminders.cron:0 0 8-20 * * *}", zone = "${app.reminders.zone:Asia/Ho_Chi_Minh}")
    public void sendDue() {
        if (!available()) return;
        repository.deleteByConfirmedFalseAndCreatedAtBefore(LocalDateTime.now(clock).minusDays(7));
        var today = LocalDate.now(clock);
        for (var row : repository.findByConfirmedTrue()) {
            try { sendFor(row, today); }
            catch (Exception e) { log.warn("Không gửi được email nhắc #{}: {}", row.getId(), e.getMessage()); }
        }
    }

    void sendFor(EmailReminder row, LocalDate today) {
        Map<String, String> dates = json.readValue(row.getDates(), new TypeReference<LinkedHashMap<String, String>>() {});
        Map<String, Integer> months = row.getMonths() == null ? Map.of() : json.readValue(row.getMonths(), new TypeReference<LinkedHashMap<String, Integer>>() {});
        Map<String, Sent> sent = new LinkedHashMap<>(json.readValue(row.getSent(), new TypeReference<LinkedHashMap<String, Sent>>() {}));
        var due = due(dates, months, sent, today);
        if (due.isEmpty()) return;
        var items = due.stream().map(d -> new ReminderMailer.Item(d.task(), TITLES.get(d.task()), d.date(),
            row.getOrigin() + "/?baotri=" + d.task() + "&nguon=email", d.number())).toList();
        mailer.sendReminder(row.getEmail(), items, row.getOrigin() + "/?huyemail=" + row.getManageToken());
        // Gửi xong mới ghi: gửi lỗi thì giờ sau thử lại
        for (var i : items) sent.put(i.task(), new Sent(i.date(), today.toString(), i.number()));
        row.setSent(json.writeValueAsString(sent));
        repository.save(row);
    }

    /**
     * Việc cần nhắc hôm nay. Lần tới hạn hiện tại = ngày hẹn + k chu kỳ, lấy lần gần nhất không sau hôm nay (người dùng không
     * quay lại web thì vẫn nhắc lần sau). Nhắc nếu chưa gửi cho lần này, hoặc đã gửi 1 thư từ 7 ngày trước.
     */
    static List<Due> due(Map<String, String> dates, Map<String, Integer> months, Map<String, Sent> sent, LocalDate today) {
        var out = new ArrayList<Due>();
        for (var task : TASKS) {
            String date = dates.get(task);
            if (date == null) continue;
            var current = occurrence(LocalDate.parse(date), months.getOrDefault(task, MONTHS.get(task)), today);
            if (current == null) continue;
            String on = current.toString();
            var s = sent.get(task);
            if (s == null || !on.equals(s.due())) out.add(new Due(task, on, 1));
            else if (s.n() < MAX_SENDS && !LocalDate.parse(s.on()).plusDays(REPEAT_AFTER_DAYS).isAfter(today)) out.add(new Due(task, on, s.n() + 1));
        }
        return out;
    }

    /** Lần tới hạn gần nhất không sau hôm nay: start + k * months tháng (tính từ ngày gốc để không trôi ngày cuối tháng) */
    static LocalDate occurrence(LocalDate start, int months, LocalDate today) {
        if (start.isAfter(today)) return null;
        long k = java.time.temporal.ChronoUnit.MONTHS.between(start, today) / months;
        var o = start.plusMonths(k * months);
        while (o.isAfter(today)) o = start.plusMonths(--k * months);
        return o;
    }

    private EmailReminder byToken(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{20,64}")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return repository.findByManageToken(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    static String email(String raw) {
        String e = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (e.length() > 150 || !EMAIL.matcher(e).matches() || e.endsWith("@smartcarepc.local"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email address.");
        return e;
    }

    /** Chỉ ba việc, ngày dạng yyyy-MM-dd có thật, trong khoảng hợp lý */
    static Map<String, String> dates(Map<String, String> raw) {
        if (raw == null || raw.isEmpty() || raw.size() > TASKS.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reminder dates are required");
        var out = new LinkedHashMap<String, String>();
        for (var task : TASKS) {
            String v = raw.get(task);
            if (v == null) continue;
            try {
                var d = LocalDate.parse(v);
                if (d.getYear() < 2020 || d.getYear() > 2100) throw new DateTimeException("range");
                out.put(task, d.toString());
            } catch (DateTimeException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reminder date"); }
        }
        if (out.size() != raw.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown maintenance task");
        return out;
    }

    /** Chu kỳ 1–60 tháng cho đúng ba việc; thiếu việc nào thì dùng MONTHS */
    static Map<String, Integer> months(Map<String, Integer> raw) {
        var out = new LinkedHashMap<String, Integer>();
        for (var task : TASKS) {
            Integer m = raw == null ? null : raw.get(task);
            if (m != null && (m < 1 || m > 60)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reminder interval");
            out.put(task, m == null ? MONTHS.get(task) : m);
        }
        if (raw != null && raw.keySet().stream().anyMatch(k -> !TASKS.contains(k))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown maintenance task");
        return out;
    }

    private String origin(String o) {
        if (o == null || !origins.contains(o)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown website address");
        return o;
    }

    private static String token() {
        var bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
