package com.smartcarepc.backend.service.ai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Luật lắp chung đọc từ TÊN SẢN PHẨM THẬT (danh mục 3D chỉ có linh kiện chung chung, không biết bo là DDR4 hay DDR5,
 * CPU là AM4 hay AM5). Đọc loại RAM, chipset bo mạch, đời CPU rồi kiểm tra chéo:
 * <ul>
 * <li>socket CPU phải trùng socket bo (đời CPU và chipset quyết định socket);</li>
 * <li>RAM phải cùng loại với bo (DDR4 không cắm được khe DDR5 và ngược lại);</li>
 * <li>CPU chỉ chạy DDR5 (Intel Core Ultra 200, AMD AM5) không đi với bo / RAM DDR4; AM4 chỉ chạy DDR4;</li>
 * <li>RAM laptop (SO-DIMM) không lắp vào máy bàn.</li>
 * </ul>
 * Không đọc ra thì bỏ qua (không báo lỗi đoán mò).
 */
public final class ProductRules {

    private ProductRules() {
    }

    /** Thông số đọc được từ tên; null là không biết */
    public record Spec(String socket, String ramType, String chipset, boolean laptopRam) {

        /** Câu ngắn cho trợ lý, vd "socket LGA1700, DDR5" (rỗng nếu không đọc được gì) */
        public String describe() {
            List<String> parts = new ArrayList<>();
            if (socket != null) parts.add("socket " + socket);
            if (chipset != null) parts.add("chipset " + chipset.toUpperCase());
            if (ramType != null) parts.add(ramType);
            if (laptopRam) parts.add("RAM laptop (SO-DIMM)");
            return String.join(", ", parts);
        }
    }

    /** Chipset bo mạch -> socket */
    private static final Map<String, String> CHIPSETS = new LinkedHashMap<>();
    static {
        for (String c : List.of("h610", "b660", "h670", "z690", "b760", "h770", "z790")) CHIPSETS.put(c, "LGA1700");
        for (String c : List.of("h810", "b860", "z890")) CHIPSETS.put(c, "LGA1851");
        for (String c : List.of("h410", "b460", "h470", "z490", "h510", "b560", "h570", "z590")) CHIPSETS.put(c, "LGA1200");
        for (String c : List.of("a320", "b350", "x370", "b450", "x470", "a520", "b550", "x570")) CHIPSETS.put(c, "AM4");
        for (String c : List.of("a620", "b650", "x670", "b840", "b850", "x870")) CHIPSETS.put(c, "AM5");
    }

    private static final Pattern DDR = Pattern.compile("\\bddr\\s?([345])\\b");
    /** Kiểu viết tắt trong tên bo: "B760M-A D4", "PRO B760M-E D4" */
    private static final Pattern BOARD_D = Pattern.compile("\\bd([45])\\b");
    private static final Pattern CHIPSET = Pattern.compile("\\b([abhxz]\\d{3})[a-z]?\\b");
    /** Intel Core i3 / i5 / i7 / i9 đời 10-14: "i5-12400F", "i5 14600kf", "core i7 13700" */
    private static final Pattern INTEL_CORE = Pattern.compile("\\bi[3579]\\s?(1[0-4])\\d{3}[a-z]*\\b");
    /** Intel Core Ultra 5 / 7 / 9 đời 2 (200S, máy bàn): "Ultra 7 265K", "Core Ultra 5 225F" */
    private static final Pattern INTEL_ULTRA = Pattern.compile("\\bultra\\s?[579]\\s?2\\d{2}[a-z]*\\b");
    /** AMD Ryzen: "Ryzen 5 5600", "Ryzen 7 7800X3D", "R5 7500F" */
    private static final Pattern RYZEN = Pattern.compile("\\b(?:ryzen|r)\\s?[3579]\\s?(\\d)\\d{3}[a-z0-9]*\\b");
    private static final Pattern LAPTOP = Pattern.compile("\\b(laptop|notebook|so\\s?dimm)\\b");

    /** Đọc thông số từ tên sản phẩm theo loại linh kiện (cpu, motherboard, ram; loại khác trả về rỗng) */
    public static Spec read(String category, String title) {
        if (title == null || category == null) return new Spec(null, null, null, false);
        // Bỏ dấu, viết thường, gạch nối thành khoảng trắng: "i5-12400F" -> "i5 12400f"
        String t = Normalizer.normalize(title.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", " ");
        switch (category) {
            case "cpu" -> {
                if (INTEL_ULTRA.matcher(t).find()) return new Spec("LGA1851", "DDR5", null, false);
                Matcher m = INTEL_CORE.matcher(t);
                if (m.find()) {
                    int gen = Integer.parseInt(m.group(1));
                    return new Spec(gen >= 12 ? "LGA1700" : "LGA1200", null, null, false);
                }
                m = RYZEN.matcher(t);
                if (m.find()) {
                    int series = Integer.parseInt(m.group(1));
                    if (series >= 7) return new Spec("AM5", "DDR5", null, false);
                    if (series >= 1) return new Spec("AM4", "DDR4", null, false);
                }
                return new Spec(null, null, null, false);
            }
            case "motherboard" -> {
                String chipset = null, socket = null;
                Matcher m = CHIPSET.matcher(t);
                while (m.find()) {
                    if (CHIPSETS.containsKey(m.group(1))) {
                        chipset = m.group(1);
                        socket = CHIPSETS.get(chipset);
                        break;
                    }
                }
                String ram = ddr(t);
                if (ram == null) {
                    Matcher d = BOARD_D.matcher(t);
                    if (d.find()) ram = "DDR" + d.group(1);
                }
                // Nền tảng chỉ có một loại RAM thì suy ra được
                if (ram == null && ("AM5".equals(socket) || "LGA1851".equals(socket))) ram = "DDR5";
                if (ram == null && ("AM4".equals(socket) || "LGA1200".equals(socket))) ram = "DDR4";
                return new Spec(socket, ram, chipset, false);
            }
            case "ram" -> {
                return new Spec(null, ddr(t), null, LAPTOP.matcher(t).find());
            }
            default -> {
                return new Spec(null, null, null, false);
            }
        }
    }

    /** Thông số đọc được từ tên khi chưa biết loại linh kiện (kết quả tìm kiếm), rỗng nếu không đọc được gì */
    public static String describeAny(String title) {
        Spec board = read("motherboard", title);
        if (board.chipset() != null) return board.describe();
        Spec cpu = read("cpu", title);
        if (cpu.socket() != null) return cpu.describe();
        return read("ram", title).describe();
    }

    private static String ddr(String normalized) {
        Matcher m = DDR.matcher(normalized);
        return m.find() ? "DDR" + m.group(1) : null;
    }

    /**
     * Lỗi lắp chung giữa các sản phẩm thật đã chọn. titles: loại linh kiện (cpu, motherboard, ram) -> tên sản phẩm.
     * Rỗng là không thấy lỗi.
     */
    public static List<String> issues(Map<String, String> titles) {
        Spec cpu = read("cpu", titles.get("cpu"));
        Spec board = read("motherboard", titles.get("motherboard"));
        Spec ram = read("ram", titles.get("ram"));
        List<String> out = new ArrayList<>();
        if (cpu.socket() != null && board.socket() != null && !cpu.socket().equals(board.socket())) {
            out.add("CPU \"" + titles.get("cpu") + "\" dùng socket " + cpu.socket() + " nhưng bo mạch \"" + titles.get("motherboard")
                    + "\" là socket " + board.socket() + ": không lắp được.");
        }
        if (board.ramType() != null && ram.ramType() != null && !board.ramType().equals(ram.ramType())) {
            out.add("Bo mạch dùng " + board.ramType() + " nhưng RAM là " + ram.ramType() + ": RAM không cắm được vào bo.");
        }
        if (cpu.ramType() != null) {
            String other = board.ramType() != null ? board.ramType() : ram.ramType();
            if (other != null && !other.equals(cpu.ramType())) {
                out.add("CPU socket " + cpu.socket() + " chỉ chạy " + cpu.ramType() + " nhưng cấu hình đang dùng " + other + ".");
            }
        }
        if (ram.laptopRam()) out.add("\"" + titles.get("ram") + "\" là RAM laptop (SO-DIMM), không lắp vào máy bàn.");
        return out;
    }
}
