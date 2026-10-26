package com.smartcarepc.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartcarepc.backend.dto.FeedbackRequest;
import com.smartcarepc.backend.dto.FeedbackSaved;
import com.smartcarepc.backend.entity.Feedback;
import com.smartcarepc.backend.repository.FeedbackRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Lưu form phản hồi của thử nghiệm và xuất ra CSV cho nhóm nghiên cứu. Mỗi câu hỏi trong answers thành một cột riêng
 * (tiền tố "a."), nên mở bằng Excel là phân tích được ngay, không phải tách JSON bằng tay.
 */
@Service
public class FeedbackService {

    /** Đủ cho 7 câu kèm hai đoạn góp ý dài; lớn hơn thì từ chối để không ai đổ rác vào bảng */
    static final int MAX_ANSWERS_CHARS = 20_000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FeedbackRepository repository;
    private final JsonMapper json;
    private final byte[] exportKey;

    public FeedbackService(FeedbackRepository repository, JsonMapper json,
            @Value("${app.feedback.export-key:}") String exportKey) {
        this.repository = repository;
        this.json = json;
        this.exportKey = exportKey.strip().getBytes(StandardCharsets.UTF_8);
    }

    private static final java.util.regex.Pattern SUBMISSION_ID = java.util.regex.Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Transactional
    public FeedbackSaved save(FeedbackRequest request) {
        validateParticipant(request.answers());
        // Cùng một lần gửi (mã submission_id do app tạo) tới hai lần (bấm hai lần, gửi lại từ hàng đợi): trả bản đã lưu
        Object sid = request.answers().get("submission_id");
        String submission = sid instanceof String id && SUBMISSION_ID.matcher(id).matches() ? id : null;
        if (submission != null) {
            var dup = findSubmission(submission);
            if (dup.isPresent()) return new FeedbackSaved(dup.get().getId());
        }
        String answers = json.writeValueAsString(request.answers());
        if (answers.length() > MAX_ANSWERS_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The response is too long");
        }
        Feedback saved = repository.save(Feedback.builder()
                .participantCode(blankToNull(request.participantCode()))
                .phase(blankToNull(request.phase()))
                .machine(blankToNull(request.machine()))
                .answers(answers)
                .submissionId(submission)
                .build());
        return new FeedbackSaved(saved.getId());
    }

    /** Hồ sơ tùy chọn cho client cũ/local; nếu có phải đầy đủ, đúng kiểu và có giới hạn. */
    private static void validateParticipant(Map<String, Object> answers) {
        if (!answers.containsKey("participant_name") && !answers.containsKey("participant_age")
                && !answers.containsKey("participant_occupation")) return;
        Object name = answers.get("participant_name"), age = answers.get("participant_age"), job = answers.get("participant_occupation");
        if (!(name instanceof String n) || n.strip().length() < 2 || n.strip().length() > 80
                || !(job instanceof String j) || j.strip().length() < 2 || j.strip().length() > 80
                || !(age instanceof Number a) || !Double.isFinite(a.doubleValue()) || a.doubleValue() != a.intValue()
                || a.intValue() < 1 || a.intValue() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid participant details");
        }
    }

    /** Bản đã lưu cùng submission_id với request (sau khi vướng ràng buộc UNIQUE do gửi trùng cùng lúc) */
    @Transactional(readOnly = true)
    public java.util.Optional<FeedbackSaved> existing(FeedbackRequest request) {
        Object sid = request.answers().get("submission_id");
        if (!(sid instanceof String id) || !SUBMISSION_ID.matcher(id).matches()) return java.util.Optional.empty();
        return findSubmission(id).map(f -> new FeedbackSaved(f.getId()));
    }

    /** Tìm theo cột submission_id, rồi trong JSON câu trả lời (bản lưu trước khi có cột) */
    private java.util.Optional<Feedback> findSubmission(String id) {
        var byColumn = repository.findFirstBySubmissionId(id);
        return byColumn.isPresent() ? byColumn : repository.findFirstByAnswersContaining("\"submission_id\":\"" + id + "\"");
    }

    /** Một dòng phản hồi cho trang xem dữ liệu (mới nhất trước) */
    public record Row(long id, String createdAt, String participantCode, String phase, String machine, Map<String, Object> answers) {
    }

    /** Toàn bộ phản hồi cho trang xem dữ liệu /feedback, mới nhất trước. Cần khoá FEEDBACK_EXPORT_KEY. */
    @Transactional(readOnly = true)
    public List<Row> list(String key) {
        checkKey(key);
        List<Row> out = new ArrayList<>();
        for (Feedback f : repository.findAllByOrderByIdAsc().reversed()) {
            out.add(new Row(f.getId(), f.getCreatedAt() == null ? null : f.getCreatedAt().format(TIME), f.getParticipantCode(),
                    f.getPhase(), f.getMachine(), parse(f.getAnswers())));
        }
        return out;
    }

    private void checkKey(String key) {
        if (exportKey.length == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Feedback export is unavailable because the server has no access key configured");
        }
        byte[] given = (key == null ? "" : key).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(exportKey, given)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect feedback access key");
        }
    }

    /** Toàn bộ phản hồi dạng CSV (UTF-8 có BOM để Excel đọc đúng tiếng Việt). Cần khoá FEEDBACK_EXPORT_KEY. */
    @Transactional(readOnly = true)
    public String exportCsv(String key) {
        checkKey(key);

        List<Feedback> rows = repository.findAllByOrderByIdAsc();
        List<Map<String, Object>> answers = new ArrayList<>(rows.size());
        Set<String> keys = new LinkedHashSet<>();
        for (Feedback f : rows) {
            Map<String, Object> a = parse(f.getAnswers());
            answers.add(a);
            keys.addAll(a.keySet());
        }

        StringBuilder out = new StringBuilder("﻿");
        List<String> header = new ArrayList<>(List.of("id", "created_at", "participant_code", "phase", "machine"));
        keys.forEach(k -> header.add("a." + k));
        line(out, header);
        for (int i = 0; i < rows.size(); i++) {
            Feedback f = rows.get(i);
            List<String> cells = new ArrayList<>(List.of(
                    String.valueOf(f.getId()),
                    f.getCreatedAt() == null ? "" : f.getCreatedAt().format(TIME),
                    nz(f.getParticipantCode()), nz(f.getPhase()), nz(f.getMachine())));
            Map<String, Object> a = answers.get(i);
            for (String k : keys) cells.add(cell(a.get(k)));
            line(out, cells);
        }
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        try {
            return json.readValue(raw, LinkedHashMap.class);
        } catch (RuntimeException e) {
            return Map.of("raw", raw);
        }
    }

    /** Số và chữ giữ nguyên; danh sách hoặc đối tượng lồng nhau (vd cấu hình được tư vấn) để dạng JSON trong một ô */
    private String cell(Object v) {
        if (v == null) return "";
        if (v instanceof Map<?, ?> || v instanceof List<?>) return json.writeValueAsString(v);
        return v.toString();
    }

    private static void line(StringBuilder out, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) out.append(',');
            out.append(escape(cells.get(i)));
        }
        out.append("\r\n");
    }

    /**
     * Bọc ngoặc kép mọi ô. Ô bắt đầu bằng = + - @ thì thêm dấu ' để Excel không chạy nó như công thức,
     * vì nội dung góp ý do người dùng tự gõ.
     */
    static String escape(String s) {
        String v = s == null ? "" : s;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        return '"' + v.replace("\"", "\"\"") + '"';
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
