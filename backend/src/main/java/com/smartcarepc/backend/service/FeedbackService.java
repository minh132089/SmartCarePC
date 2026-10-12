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

    @Transactional
    public FeedbackSaved save(FeedbackRequest request) {
        String answers = json.writeValueAsString(request.answers());
        if (answers.length() > MAX_ANSWERS_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Câu trả lời quá dài");
        }
        Feedback saved = repository.save(Feedback.builder()
                .participantCode(blankToNull(request.participantCode()))
                .phase(blankToNull(request.phase()))
                .machine(blankToNull(request.machine()))
                .answers(answers)
                .build());
        return new FeedbackSaved(saved.getId());
    }

    /** Toàn bộ phản hồi dạng CSV (UTF-8 có BOM để Excel đọc đúng tiếng Việt). Cần khoá FEEDBACK_EXPORT_KEY. */
    @Transactional(readOnly = true)
    public String exportCsv(String key) {
        if (exportKey.length == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chưa đặt FEEDBACK_EXPORT_KEY nên chưa xuất được dữ liệu");
        }
        byte[] given = (key == null ? "" : key).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(exportKey, given)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sai khoá xuất dữ liệu");
        }

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
