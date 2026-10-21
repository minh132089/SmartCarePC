package com.smartcarepc.backend.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.smartcarepc.backend.dto.ChatResponse.Line;
import com.smartcarepc.backend.dto.ChatResponse.Verdict;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;

/**
 * Trạng thái của một phương án theo bộ skill docs/pc-build-verifier (mục 1.3 trạng thái cấp cấu hình, mục 7.2 gate):
 * tách kỹ thuật (lắp được, khởi động được) khỏi mua hàng (link, giá, còn hàng, ngân sách), rồi lấy mức xấu nhất.
 * Chạy bằng code để AI không tự "cho qua" một gate (mục 9.2: logic gate và tiền chạy ở ứng dụng).
 *
 * BLOCKED: có lỗi chắc chắn (lắp không được, hết hàng, vượt trần phương án). HOLD: thiếu dữ liệu bắt buộc (chưa có
 * link mua, thiếu giá, thiếu linh kiện). CONDITIONAL: còn việc cụ thể phải làm trước khi mua, đã biết cách làm (hỏi cửa
 * hàng CPU Support List / gá tản, cập nhật BIOS, xem lại giá trên trang). READY: các gate trên đều qua trong phạm vi
 * app kiểm được, tại thời điểm kiểm tra; không phải bảo đảm máy thực tế không lỗi.
 *
 * Điểm app không tự xác minh được (CPU Support List, BIOS khi giao, gá tản) ghi thành điều kiện cụ thể cho người
 * dùng hỏi cửa hàng, không chặn link mua (link mua là bắt buộc với người dùng app).
 */
final class BuildVerifier {

    static final String READY = "READY", CONDITIONAL = "CONDITIONAL", HOLD = "HOLD", BLOCKED = "BLOCKED";
    private static final List<String> ORDER = List.of(READY, CONDITIONAL, HOLD, BLOCKED);
    /** Linh kiện máy nào cũng phải có để khởi động (card và tản tùy cấu hình: CPU có đồ họa, quạt kèm CPU) */
    private static final Set<String> REQUIRED = Set.of("cpu", "motherboard", "ram", "ssd", "psu", "case");

    private BuildVerifier() {
    }

    static Verdict verify(List<Line> lines, List<Issue> errors, List<Issue> warnings, List<Issue> unknowns, Long cap) {
        List<String> blockers = new ArrayList<>(), conditions = new ArrayList<>();

        // Kỹ thuật (gate G3)
        String technical = READY;
        for (var e : errors) {
            blockers.add(e.message());
            technical = BLOCKED;
        }
        Set<String> have = new java.util.HashSet<>();
        lines.forEach(l -> have.add(l.category()));
        for (String cat : REQUIRED) {
            if (have.contains(cat)) continue;
            // Danh sách trống / thiếu loại bắt buộc không được coi là "không có lỗi" (mục 9.2, ca T28)
            blockers.add("Cấu hình thiếu " + label(cat) + ".");
            technical = worst(technical, HOLD);
        }
        for (var w : warnings) {
            if (w.rule() != null && w.rule().contains("BIOS")) {
                conditions.add("Hỏi cửa hàng cập nhật BIOS hỗ trợ CPU trước khi giao: " + w.message());
                technical = worst(technical, CONDITIONAL);
            }
        }
        for (var u : unknowns) {
            conditions.add("Hỏi cửa hàng xác nhận trước khi mua: " + u.message());
            technical = worst(technical, CONDITIONAL);
        }

        // Mua hàng (gate G4, G5)
        String purchase = READY;
        long total = 0;
        for (var l : lines) {
            String name = l.productName() != null ? l.productName() : label(l.category());
            Long price = l.priceMaxVnd() != null ? l.priceMaxVnd() : l.priceMinVnd();
            if (l.sourceUrl() == null) {
                blockers.add("Chưa có link mua cho " + name + ".");
                purchase = worst(purchase, HOLD);
            }
            if (price == null) {
                blockers.add("Chưa có giá cho " + name + ".");
                purchase = worst(purchase, HOLD);
            } else {
                total += price;
            }
            if (Boolean.FALSE.equals(l.inStock())) {
                blockers.add(name + " đang hết hàng ở cửa hàng đã chọn.");
                purchase = worst(purchase, BLOCKED);
            }
            if (l.sourceUrl() != null && !Boolean.TRUE.equals(l.priceChecked())) {
                conditions.add("Xem lại giá " + name + " trên trang cửa hàng (giá tham khảo, chưa đọc được trên trang).");
                purchase = worst(purchase, CONDITIONAL);
            } else if (l.sourceUrl() != null && l.inStock() == null) {
                conditions.add("Hỏi cửa hàng còn hàng " + name + " không (trang không ghi tình trạng hàng).");
                purchase = worst(purchase, CONDITIONAL);
            }
        }
        if (cap != null && total > cap) {
            blockers.add("Tổng " + Math.round(total / 1e5) / 10.0 + " triệu vượt mức tối đa " + Math.round(cap / 1e5) / 10.0
                    + " triệu của phương án.");
            purchase = worst(purchase, BLOCKED);
        }
        return new Verdict(worst(technical, purchase), technical, purchase, blockers, conditions);
    }

    static String worst(String a, String b) {
        return ORDER.indexOf(a) >= ORDER.indexOf(b) ? a : b;
    }

    private static String label(String category) {
        return switch (category) {
            case "cpu" -> "CPU";
            case "motherboard" -> "bo mạch chủ";
            case "ram" -> "RAM";
            case "ssd" -> "ổ SSD";
            case "psu" -> "nguồn";
            case "case" -> "vỏ máy";
            case "gpu" -> "card đồ họa";
            case "cooler" -> "tản nhiệt";
            default -> category;
        };
    }
}
