package com.smartcarepc.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Soạn và gửi email nhắc bảo dưỡng qua Gmail của nhóm (spring.mail.*). Chưa đặt MAIL_USERNAME thì available() = false:
 * web ẩn lựa chọn email, lịch không gửi gì. Nội dung tiếng Anh như giao diện web, chỉ có link về đúng địa chỉ web.
 */
@Component
public class ReminderMailer {
    public record Item(String task, String title, String date, String link, int number) {}

    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    /** Link tải app khay Windows, chỉ có trong email kiểm tra sức khoẻ. Để trống thì bỏ phần này */
    private final String helperUrl;

    public ReminderMailer(ObjectProvider<JavaMailSender> sender, @Value("${spring.mail.username:}") String from,
                          @Value("${app.reminders.helper-url:}") String helperUrl) {
        this.sender = sender; this.from = from == null ? "" : from.trim();
        this.helperUrl = helperUrl == null ? "" : helperUrl.trim();
    }

    public boolean available() { return !from.isEmpty() && sender.getIfAvailable() != null; }

    public void sendConfirm(String to, String confirmLink) {
        String text = """
            Hi,

            Someone, hopefully you, asked SmartCarePC to email PC maintenance reminders to this address.
            To turn them on, open this link and press "Turn on email reminders":
            %s

            If this was not you, ignore this email. Nothing more will be sent.
            """.formatted(confirmLink);
        String html = page("Turn on your SmartCarePC reminders",
            "<p>Someone, hopefully you, asked SmartCarePC to email PC maintenance reminders to this address.</p>"
            + button(confirmLink, "Turn on email reminders")
            + "<p style=\"color:#6b7280;font-size:13px\">If this was not you, ignore this email. Nothing more will be sent.</p>");
        send(to, "Confirm your SmartCarePC reminders", text, html, null);
    }

    /** Dụng cụ cần chuẩn bị, giống bước chuẩn bị trong src/data/maintenance.ts (kiểm tra nhanh không mở máy, không cần dụng cụ) */
    static final Map<String, List<String>> TOOLS = Map.of(
        "dust", List.of("No. 2 Phillips screwdriver", "Dry soft brush", "Compressed air or electronics blower"),
        "paste", List.of("No. 2 Phillips screwdriver", "90% or higher isopropyl alcohol", "Lint-free wipes", "Non-conductive thermal paste"));
    record Mail(String subject, String text, String html) {}

    public void sendReminder(String to, List<Item> items, String stopLink) {
        var m = compose(items, stopLink);
        send(to, m.subject(), m.text(), m.html(), stopLink);
    }

    /**
     * Vệ sinh bụi, thay keo: chỉ nhắc qua email, kèm dụng cụ cần chuẩn bị và link mở lại bài hướng dẫn trên web.
     * Kiểm tra sức khoẻ: kèm thêm link tải app SmartCarePC Helper (tuỳ chọn) để máy tự kiểm tra nhiệt độ, ổ cứng.
     */
    Mail compose(List<Item> items, String stopLink) {
        var first = items.get(0);
        String subject = first.task().equals("dust") ? "Time to clean your PC" : "Time for: " + first.title().toLowerCase();
        var text = new StringBuilder("Hi,\n\nIt is time for your planned PC maintenance. Work at a time that is safe for you.\n\n");
        var body = new StringBuilder("<p>It is time for your planned PC maintenance. Work at a time that is safe for you.</p>");
        for (var i : items) {
            String note = i.number() > 1 ? " (second reminder)" : "";
            var tools = TOOLS.get(i.task());
            text.append("- ").append(i.title()).append(": planned for ").append(i.date()).append(note).append("\n");
            body.append("<div style=\"margin:16px 0;padding:14px;border:1px solid #e5e7eb;border-radius:12px\"><strong>").append(i.title())
                .append("</strong><div style=\"color:#6b7280;font-size:13px;margin-top:4px\">Planned for ").append(i.date()).append(note).append("</div>");
            if (tools != null) {
                text.append("  Get these ready: ").append(String.join(", ", tools)).append("\n");
                body.append("<p style=\"margin:10px 0 4px;font-size:14px\">Get these ready:</p><ul style=\"margin:0;padding-left:20px;font-size:14px\">");
                for (var t : tools) body.append("<li>").append(t).append("</li>");
                body.append("</ul>");
            } else text.append("  About 5 minutes, with the case closed. No tools needed.\n");
            String guide = tools != null ? "The guide shows every step in 3D, from shutting down to closing the case." : "About 5 minutes, with the case closed. No tools needed.";
            text.append("  Open the guide: ").append(i.link()).append("\n");
            body.append("<p style=\"color:#6b7280;font-size:13px;margin:8px 0 0\">").append(guide).append("</p>").append(button(i.link(), "Open the guide")).append("</div>");
        }
        if (!helperUrl.isEmpty() && items.stream().anyMatch(i -> i.task().equals("check"))) {
            text.append("\nWant your PC to check itself? On Windows you can install SmartCarePC Helper (optional, free). It watches temperatures,"
                + " disk health and unexpected shutdowns, and tells you which guide to follow. Download: ").append(helperUrl)
                .append("\nWindows may say \"Windows protected your PC\": click More info, then Run anyway.\n");
            body.append("<div style=\"margin:16px 0;padding:14px;background:#f3f4f6;border-radius:12px\"><strong>Want your PC to check itself?</strong>")
                .append("<p style=\"font-size:14px;margin:6px 0\">On Windows you can install SmartCarePC Helper (optional, free). It watches temperatures, disk health and unexpected shutdowns, and tells you which guide to follow.</p>")
                .append("<p style=\"color:#6b7280;font-size:13px;margin:6px 0\">Windows may say “Windows protected your PC”: click More info, then Run anyway.</p>")
                .append(button(helperUrl, "Download SmartCarePC Helper")).append("</div>");
        }
        text.append("\nIf you already did this, open the guide and press Finish so your next date is updated.\n\nStop these emails: ").append(stopLink).append("\n");
        body.append("<p style=\"color:#6b7280;font-size:13px\">If you already did this, open the guide and press Finish so your next date is updated.</p>")
            .append("<p style=\"color:#6b7280;font-size:13px\">You get this email because you turned on reminders at SmartCarePC. <a href=\"").append(href(stopLink)).append("\">Stop these emails</a>.</p>");
        return new Mail(subject, text.toString(), page(subject, body.toString()));
    }

    /** Link trong thuộc tính href của HTML: & phải viết thành &amp; */
    private static String href(String link) { return link.replace("&", "&amp;"); }

    private static String button(String link, String label) {
        return "<p><a href=\"" + href(link) + "\" style=\"display:inline-block;background:#0a66d8;color:#ffffff;padding:10px 18px;border-radius:999px;text-decoration:none;font-weight:600\">" + label + "</a></p>";
    }

    private static String page(String title, String body) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.5;color:#111827;max-width:560px\"><h2 style=\"font-size:20px\">" + title + "</h2>" + body + "<p style=\"color:#9ca3af;font-size:12px\">SmartCarePC</p></div>";
    }

    private void send(String to, String subject, String text, String html, String unsubscribe) {
        var mail = sender.getObject();
        var message = mail.createMimeMessage();
        try {
            var m = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            m.setFrom(from, "SmartCarePC");
            m.setTo(to);
            m.setSubject(subject);
            m.setText(text, html);
            if (unsubscribe != null) message.setHeader("List-Unsubscribe", "<" + unsubscribe + ">");
        } catch (Exception e) { throw new IllegalStateException("Could not prepare the email", e); }
        mail.send(message);
    }
}
