package com.smartcarepc.backend.service.ai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Luật lắp chung đọc từ TÊN SẢN PHẨM THẬT, theo bộ luật chuẩn docs/pc_compatibility_rules.md. Danh mục 3D chỉ có
 * linh kiện chung chung (không biết bo là DDR4 hay DDR5, CPU là AM4 hay AM5), nên phải đọc tên sản phẩm thật:
 * loại RAM, chipset bo mạch, đời CPU, rồi kiểm tra chéo và xếp mức như bộ luật:
 * <ul>
 * <li>ERROR: socket CPU khác socket bo; RAM khác loại DDR với bo; CPU chỉ chạy DDR5 đi với DDR4 (hoặc ngược lại);
 * RAM laptop (SO-DIMM) cho máy bàn;</li>
 * <li>WARNING: bo đời cũ có thể cần BIOS mới cho CPU đời mới; CPU Intel K muốn ép xung cần bo Z;</li>
 * <li>UNKNOWN: app không có CPU Support List và BIOS tối thiểu của từng mã bo, không có danh sách gá của từng tản,
 * nên không kết luận là tương thích (mục 11: "không suy đoán từ socket rằng mọi CPU đều được hỗ trợ").</li>
 * </ul>
 * Chỉ dùng tên chipset / đời CPU để phát hiện chỗ CHẮC CHẮN không lắp được; không đọc ra thì không báo (không đoán).
 */
public final class ProductRules {

    private ProductRules() {
    }

    /** Số khe theo đúng SKU, nguồn hãng ghi tại docs/ai-advice-standard.md; không suy từ chipset. */
    public static Integer memorySlots(String title) {
        if (title == null) return null;
        String t = title.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", " ");
        if (t.contains("msi") && (t.contains("pro b760m e") || t.contains("pro h610m e")) && t.contains("ddr4")) return 2;
        if (t.contains("gigabyte") && t.contains("h610m h v3") && t.contains("ddr4")) return 2;
        if (t.contains("gigabyte") && t.contains("b760m gaming plus wifi") && t.contains("ddr4")) return 4;
        if (t.contains("gigabyte") && t.contains("b760 gaming x") && t.contains("ddr4")) return 4;
        return null;
    }

    public enum Severity { ERROR, WARNING, RECOMMENDATION, UNKNOWN }

    /** Một kết quả kiểm tra, theo cấu trúc mục 10 của bộ luật (không có source_url: app chưa có nguồn hãng) */
    public record Finding(String ruleId, Severity severity, List<String> affectedParts, String reason, String requiredAction) {

        /** Câu cho người dùng: lý do, rồi việc cần làm */
        public String message() {
            return requiredAction == null ? reason : reason + " " + requiredAction;
        }
    }

    /**
     * Thông số đọc được từ tên; null là không biết.
     *
     * @param family    đời CPU: "core12".."core14", "ultra2", "ryzen1".."ryzen9" (số đầu của mã Ryzen)
     * @param unlocked  CPU Intel mở khoá hệ số nhân (đuôi K / KF)
     */
    public record Spec(String socket, String ramType, String chipset, boolean laptopRam, String family, boolean unlocked) {

        Spec(String socket, String ramType, String chipset, boolean laptopRam) {
            this(socket, ramType, chipset, laptopRam, null, false);
        }

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

    /** Chipset bo mạch -> socket (mục 2, ví dụ nền tảng) */
    private static final Map<String, String> CHIPSETS = new LinkedHashMap<>();
    static {
        for (String c : List.of("h610", "b660", "h670", "z690", "b760", "h770", "z790")) CHIPSETS.put(c, "LGA1700");
        for (String c : List.of("h810", "b860", "z890")) CHIPSETS.put(c, "LGA1851");
        for (String c : List.of("h410", "b460", "h470", "z490", "h510", "b560", "h570", "z590")) CHIPSETS.put(c, "LGA1200");
        for (String c : List.of("a320", "b350", "x370", "b450", "x470", "a520", "b550", "x570")) CHIPSETS.put(c, "AM4");
        for (String c : List.of("a620", "a620a", "b650", "x670", "b840", "b850", "x870")) CHIPSETS.put(c, "AM5");
    }
    /** Bo ra đời trước CPU: thường cần BIOS mới (mục 2, WARNING) */
    private static final Set<String> INTEL_600 = Set.of("h610", "b660", "h670", "z690");
    private static final Set<String> AM4_OLD = Set.of("a320", "b350", "x370", "b450", "x470", "a520", "b550", "x570");
    private static final Set<String> AM5_600 = Set.of("a620", "b650", "x670");

    private static final Pattern DDR = Pattern.compile("\\bddr\\s?([345])\\b");
    /** Kiểu viết tắt trong tên bo: "B760M-A D4", "PRO B760M-E D4" */
    private static final Pattern BOARD_D = Pattern.compile("\\bd([45])\\b");
    private static final Pattern CHIPSET = Pattern.compile("\\b(a620a|[abhxz]\\d{3})[a-z]?\\b");
    /** Intel Core i3 / i5 / i7 / i9 đời 10-14: "i5-12400F", "i5 14600kf", "core i7 13700" */
    private static final Pattern INTEL_CORE = Pattern.compile("\\bi[3579]\\s?(1[0-4])\\d{3}([a-z]*)\\b");
    /** Intel Core Ultra 5 / 7 / 9 đời 2 (200S, máy bàn): "Ultra 7 265K", "Core Ultra 5 225F" */
    private static final Pattern INTEL_ULTRA = Pattern.compile("\\bultra\\s?[579]\\s?2\\d{2}([a-z]*)\\b");
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
                Matcher m = INTEL_ULTRA.matcher(t);
                if (m.find()) return new Spec("LGA1851", "DDR5", null, false, "ultra2", m.group(1).startsWith("k"));
                m = INTEL_CORE.matcher(t);
                if (m.find()) {
                    int gen = Integer.parseInt(m.group(1));
                    return new Spec(gen >= 12 ? "LGA1700" : "LGA1200", null, null, false, "core" + gen, m.group(2).startsWith("k"));
                }
                m = RYZEN.matcher(t);
                if (m.find()) {
                    int series = Integer.parseInt(m.group(1));
                    if (series >= 7) return new Spec("AM5", "DDR5", null, false, "ryzen" + series, false);
                    if (series >= 1) return new Spec("AM4", "DDR4", null, false, "ryzen" + series, false);
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
                // Nền tảng chỉ có một loại RAM thì suy ra được. LGA1700 có cả DDR4 lẫn DDR5: không suy ra (mục 3)
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
        String kind = com.smartcarepc.backend.service.price.PriceEngine.kind(com.smartcarepc.backend.service.price.PriceEngine.norm(title));
        if (kind != null) return read("mainboard".equals(kind) ? "motherboard" : kind, title).describe();
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
     * Mọi kết quả kiểm tra cho các sản phẩm thật đã chọn. titles: loại linh kiện (cpu, motherboard, ram, cooler) ->
     * tên sản phẩm.
     */
    public static List<Finding> findings(Map<String, String> titles) {
        Spec cpu = read("cpu", titles.get("cpu"));
        Spec board = read("motherboard", titles.get("motherboard"));
        Spec ram = read("ram", titles.get("ram"));
        List<Finding> out = new ArrayList<>();
        String cpuName = titles.get("cpu"), boardName = titles.get("motherboard");

        String gpuName = titles.getOrDefault("gpu", "").toLowerCase(java.util.Locale.ROOT);
        String caseName = titles.getOrDefault("case", "").toLowerCase(java.util.Locale.ROOT);
        boolean titan = gpuName.matches(".*\\bsb580t-12goc\\b.*")
                || gpuName.matches(".*\\bsparkle\\b.*\\bb580\\b.*\\btitan\\b.*");
        if (titan && caseName.contains("silverstone") && caseName.matches(".*\\bsg13(?:b(?:-q)?|wb)?\\b.*")) {
            out.add(new Finding("GPU_CASE_LENGTH", Severity.ERROR, List.of("gpu", "case"),
                    "The SPARKLE B580 TITAN OC is 315 mm long, while the SilverStone SG13 supports around 267 mm. It does not fit.",
                    "Choose a shorter card within the case limit, or change the case and recheck the dimensions."));
        }

        // ERROR (mục 2, 3)
        if (cpu.socket() != null && board.socket() != null && !cpu.socket().equals(board.socket())) {
            out.add(new Finding("CPU_SOCKET", Severity.ERROR, List.of("cpu", "motherboard"),
                    "CPU \"" + cpuName + "\" uses socket " + cpu.socket() + " but motherboard \"" + boardName + "\" uses "
                            + board.socket() + ": they are incompatible.",
                    "Choose a motherboard with socket " + cpu.socket() + "."));
        }
        if (board.ramType() != null && ram.ramType() != null && !board.ramType().equals(ram.ramType())) {
            out.add(new Finding("RAM_DDR_TYPE", Severity.ERROR, List.of("motherboard", "ram"),
                    "The motherboard uses " + board.ramType() + " but the RAM is " + ram.ramType() + ": the RAM does not fit.",
                    "Choose RAM " + board.ramType() + " or a motherboard version for " + ram.ramType() + "."));
        }
        if (cpu.ramType() != null) {
            String other = board.ramType() != null ? board.ramType() : ram.ramType();
            if (other != null && !other.equals(cpu.ramType())) {
                out.add(new Finding("CPU_DDR_SUPPORT", Severity.ERROR, List.of("cpu", board.ramType() != null ? "motherboard" : "ram"),
                        "CPU socket " + cpu.socket() + " supports only " + cpu.ramType() + " but this build uses " + other + ".",
                        "Change the motherboard and RAM to " + cpu.ramType() + "."));
            }
        }
        if (ram.laptopRam()) {
            out.add(new Finding("RAM_FORM_FACTOR", Severity.ERROR, List.of("ram"),
                    "\"" + titles.get("ram") + "\" is laptop SO-DIMM memory and does not fit a desktop DIMM slot.", "Choose desktop DIMM RAM."));
        }

        // WARNING (mục 2): bo ra đời trước CPU thường cần BIOS mới; ép xung CPU K cần bo Z
        boolean sameSocket = cpu.socket() != null && cpu.socket().equals(board.socket());
        if (sameSocket && board.chipset() != null && cpu.family() != null) {
            boolean oldBios = INTEL_600.contains(board.chipset()) && Set.of("core13", "core14").contains(cpu.family())
                    || AM4_OLD.contains(board.chipset()) && Set.of("ryzen5").contains(cpu.family())
                    || AM5_600.contains(board.chipset()) && Set.of("ryzen9").contains(cpu.family());
            if (oldBios) {
                out.add(new Finding("CPU_BIOS_UPDATE", Severity.WARNING, List.of("cpu", "motherboard"),
                        "Motherboard " + board.chipset().toUpperCase() + " predates this CPU and may need a BIOS update to support it.",
                        "Ask whether the store has updated the BIOS, or choose a board with BIOS Flashback."));
            }
            if (cpu.unlocked() && board.chipset().startsWith("b") || cpu.unlocked() && board.chipset().startsWith("h")) {
                out.add(new Finding("CPU_OC_CHIPSET", Severity.WARNING, List.of("cpu", "motherboard"),
                        "The unlocked K-series CPU supports overclocking, but the motherboard " + board.chipset().toUpperCase() + " does not support multiplier overclocking.",
                        "This is fine without overclocking; choose a Z-series chipset if you intend to overclock."));
            }
        }

        // UNKNOWN (mục 10, 11): app không có danh sách hỗ trợ của hãng, không tự kết luận là tương thích
        if (sameSocket) {
            out.add(new Finding("CPU_SUPPORT_LIST", Severity.UNKNOWN, List.of("cpu", "motherboard"),
                    "The exact motherboard's CPU Support List and minimum BIOS version have not been verified.",
                    "Check the manufacturer's CPU support page for the exact motherboard model before buying."));
        }
        if (cpu.socket() != null && titles.get("cooler") != null) {
            out.add(new Finding("COOLER_MOUNT", Severity.UNKNOWN, List.of("cooler", "cpu"),
                    "The cooler mounting kit has not been verified for socket " + cpu.socket() + "; Intel and AMD mounting hardware differs.",
                    "Check the cooler specification page for support for " + cpu.socket() + "."));
        }
        if (titles.get("gpu") != null && titles.get("case") != null) {
            out.add(new Finding("GPU_CLEARANCE", Severity.UNKNOWN, List.of("gpu", "case"),
                    "The exact GPU dimensions have not been checked against the case's GPU clearance.",
                    "Confirm dimensions and power-cable clearance with the store. Fan count alone does not prove the card fits."));
        }
        if (titles.get("fans") != null && titles.get("case") != null) {
            out.add(new Finding("FAN_MOUNTS", Severity.UNKNOWN, List.of("fans", "case"),
                    "The exact case's remaining fan mounting positions have not been verified.",
                    "Count included and additional fans before buying; do not exceed the case's supported mounts."));
        }
        return out;
    }

    /**
     * CPU có đồ họa tích hợp theo mã sản phẩm (bộ luật mục 4: xét theo SKU, không suy từ danh mục). Intel Core đời 12 -
     * 14 và Core Ultra 200: có, trừ đuôi F. Ryzen 1000 - 5000: chỉ bản G. Ryzen 7000 / 8000 / 9000: có, trừ đuôi F (vd
     * 7500F). Không đọc ra thì null.
     */
    public static Boolean hasIgpu(String cpuTitle) {
        if (cpuTitle == null) return null;
        String t = Normalizer.normalize(cpuTitle.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", " ");
        Matcher m = INTEL_ULTRA.matcher(t);
        if (m.find()) return !m.group(1).contains("f");
        m = INTEL_CORE.matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1)) >= 12 ? !m.group(2).contains("f") : null;
        m = Pattern.compile("\\b(?:ryzen|r)\\s?[3579]\\s?(\\d)\\d{3}([a-z0-9]*)\\b").matcher(t);
        if (m.find()) {
            int series = Integer.parseInt(m.group(1));
            String suffix = m.group(2);
            return series >= 7 ? !suffix.contains("f") : suffix.contains("g");
        }
        return null;
    }

    /** Chỉ các lỗi (ERROR): lắp không được, phải đổi sản phẩm */
    public static List<String> issues(Map<String, String> titles) {
        return findings(titles).stream().filter(f -> f.severity() == Severity.ERROR).map(Finding::reason).toList();
    }
}
