package com.smartcarepc.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcarepc.backend.dto.BuildCheckRequest;
import com.smartcarepc.backend.dto.CompatibilityResponse;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;
import com.smartcarepc.backend.dto.ComponentResponse;
import com.smartcarepc.backend.dto.PcBuildResponse;
import com.smartcarepc.backend.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Giữ cấu hình trợ lý đề xuất khớp với mô hình 3D. Mỗi vỏ máy có một máy mẫu dựng sẵn (bảng pc_builds, mã máy = mã vỏ
 * bỏ tiền tố "case_"), với bo mạch, tản nhiệt, card đồ họa và nguồn cố định. Trang "Hướng dẫn lắp máy" mở đúng máy mẫu
 * đó và dạy lắp theo các món của nó, nên chọn món khác (vd tản khí trong vỏ có mẫu tản nước) thì người dùng sẽ được dạy
 * lắp sai linh kiện: coi là lỗi để trợ lý đổi lại hoặc đổi vỏ.
 * Số quạt và số thanh RAM được phép khác máy mẫu, chỉ kèm một ghi chú cho người dùng.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelMatchService {

    /** Các loại linh kiện mô hình 3D và hướng dẫn lắp dựng cố định theo từng vỏ */
    static final List<String> FIXED = List.of("motherboard", "cooler", "gpu", "psu");
    private static final Map<String, String> LABEL = Map.of(
            "motherboard", "bo mạch", "cooler", "tản nhiệt", "gpu", "card đồ họa", "psu", "nguồn");

    private final PcBuildService pcBuildService;
    private final ComponentService componentService;

    /** errors: món lệch máy mẫu (phải sửa). note: ghi chú khi số quạt / thanh RAM khác máy mẫu, null nếu giống. */
    public record Result(List<Issue> errors, String note) {
    }

    public Result check(BuildCheckRequest request) {
        Map<String, ComponentResponse> catalog = componentService.findAll(null).stream()
                .collect(Collectors.toMap(ComponentResponse::code, c -> c, (a, b) -> a));
        Map<String, ComponentResponse> chosen = new LinkedHashMap<>();
        Map<String, Integer> chosenQty = new LinkedHashMap<>();
        for (var i : request.items()) {
            ComponentResponse c = catalog.get(i.code());
            if (c == null) continue; // mã lạ: CompatibilityService đã báo
            chosen.putIfAbsent(c.category(), c);
            chosenQty.merge(c.category(), i.qty(), Integer::sum);
        }
        ComponentResponse pcCase = chosen.get("case");
        Optional<PcBuildResponse> model = pcCase == null ? Optional.empty() : modelOf(pcCase.code());
        if (model.isEmpty()) return new Result(List.of(), null); // chưa có vỏ hoặc vỏ chưa có mô hình: không so

        Map<String, ComponentResponse> fixed = fixedParts(model.get());
        List<Issue> errors = new ArrayList<>();
        for (String category : FIXED) {
            ComponentResponse want = fixed.get(category);
            ComponentResponse have = chosen.get(category);
            if (want == null || (have != null && have.code().equals(want.code()))) continue;
            // Không có card rời: máy dùng đồ họa tích hợp (kiểm tra tương thích báo lỗi nếu CPU không có), bỏ bước lắp card.
            // Không có tản thấp: dùng quạt tản kèm CPU bản Box (app chỉ bỏ dòng này khi CPU đã kèm tản)
            if (have == null && ("gpu".equals(category) || "cooler".equals(category))) continue;
            errors.add(new Issue("model." + category, mismatch(pcCase, category, want, have)));
        }
        String note = note(model.get(), chosenQty);
        if (fixed.containsKey("cooler") && !chosen.containsKey("cooler")) {
            String stock = "Máy mẫu 3D có tản nhiệt rời; cấu hình này dùng quạt tản đi kèm CPU (bản Box) nên bạn lắp quạt đó thay cho tản rời.";
            note = note == null ? stock : note + " " + stock;
        }
                if (fixed.containsKey("gpu") && !chosen.containsKey("gpu")) {
            String noGpu = "Máy mẫu 3D có card đồ họa rời; cấu hình này dùng đồ họa tích hợp của CPU nên bạn bỏ qua bước lắp card.";
            note = note == null ? noGpu : note + " " + noGpu;
        }
        return new Result(errors, note);
    }

    /**
     * Vỏ có máy mẫu dựng đúng các món cố định đã chọn (bo, tản, card, nguồn; không có card thì bỏ qua card), khác vỏ
     * đang chọn. Dùng để tự sửa khi trợ lý chọn vỏ không khớp món: đổi vỏ mẫu thì không phải mua thêm gì.
     */
    public Optional<String> matchingCase(BuildCheckRequest request) {
        Map<String, ComponentResponse> catalog = componentService.findAll(null).stream()
                .collect(Collectors.toMap(ComponentResponse::code, c -> c, (a, b) -> a));
        Map<String, String> chosen = new LinkedHashMap<>();
        for (var i : request.items()) {
            ComponentResponse c = catalog.get(i.code());
            if (c != null) chosen.putIfAbsent(c.category(), c.code());
        }
        String now = chosen.get("case");
        for (var p : pcBuildService.findPresets()) {
            String code = "case_" + p.code();
            if (code.equals(now)) continue;
            var fixed = modelOf(code).map(ModelMatchService::fixedParts).orElse(null);
            if (fixed == null) continue;
            boolean all = FIXED.stream().allMatch(cat -> {
                String want = codeOf(fixed.get(cat));
                String have = chosen.get(cat);
                return want == null || (have == null ? "gpu".equals(cat) : have.equals(want));
            });
            if (all) return Optional.of(code);
        }
        return Optional.empty();
    }

    /** Món cố định của vỏ: loại linh kiện -> mã trong danh mục (rỗng nếu vỏ chưa có mô hình) */
    public Map<String, String> fixedCodes(String caseCode) {
        Map<String, String> out = new LinkedHashMap<>();
        modelOf(caseCode).ifPresent(b -> fixedParts(b).forEach((cat, c) -> out.put(cat, c.code())));
        return out;
    }

    /** Gộp lỗi lệch máy mẫu vào kết quả kiểm tra tương thích */
    public static CompatibilityResponse merge(CompatibilityResponse r, List<Issue> extra) {
        if (extra.isEmpty()) return r;
        List<Issue> errors = new ArrayList<>(r.errors());
        errors.addAll(extra);
        return new CompatibilityResponse(false, errors, r.warnings());
    }

    /** Mỗi vỏ đi kèm những món cố định nào, để đưa sẵn vào lời dặn của trợ lý */
    public String presetParts() {
        StringBuilder sb = new StringBuilder();
        for (var p : pcBuildService.findPresets()) {
            modelOf("case_" + p.code()).ifPresent(b -> sb.append("- vỏ [case_").append(p.code()).append("] ")
                    .append(p.name()).append(": ")
                    .append(fixedParts(b).entrySet().stream()
                            .map(e -> LABEL.get(e.getKey()) + " " + e.getValue().name() + " [" + e.getValue().code() + "]")
                            .collect(Collectors.joining(", ")))
                    .append('\n'));
        }
        return sb.toString();
    }

    private Optional<PcBuildResponse> modelOf(String caseCode) {
        if (!caseCode.startsWith("case_")) return Optional.empty();
        try {
            return Optional.of(pcBuildService.findByCode(caseCode.substring("case_".length())));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    private static Map<String, ComponentResponse> fixedParts(PcBuildResponse model) {
        Map<String, ComponentResponse> m = new LinkedHashMap<>();
        for (String category : FIXED) {
            model.items().stream().map(PcBuildResponse.Item::component)
                    .filter(c -> category.equals(c.category())).findFirst()
                    .ifPresent(c -> m.put(category, c));
        }
        return m;
    }

    /** Lời nhắn cho trợ lý: đổi lại món của máy mẫu, hoặc đổi sang vỏ có sẵn món đang chọn */
    private String mismatch(ComponentResponse pcCase, String category, ComponentResponse want, ComponentResponse have) {
        String label = LABEL.get(category);
        String head = "Mô hình 3D và hướng dẫn lắp của vỏ " + pcCase.name() + " dùng " + label + " " + want.name()
                + " [" + want.code() + "]. ";
        if (have == null) return head + "Hãy thêm món này để hướng dẫn lắp khớp với máy.";
        List<String> others = new ArrayList<>();
        for (var p : pcBuildService.findPresets()) {
            modelOf("case_" + p.code())
                    .filter(b -> !("case_" + p.code()).equals(pcCase.code()))
                    .filter(b -> have.code().equals(codeOf(fixedParts(b).get(category))))
                    .ifPresent(b -> others.add(p.name() + " [case_" + p.code() + "]"));
        }
        return head + "Hãy đổi " + have.name() + " [" + have.code() + "] thành món đó"
                + (others.isEmpty() ? "." : ", hoặc đổi sang vỏ có sẵn " + label + " này: " + String.join(", ", others) + ".");
    }

    private static String codeOf(ComponentResponse c) {
        return c == null ? null : c.code();
    }

    /** Ghi chú cho người dùng khi số quạt hoặc số thanh RAM khác máy mẫu (các bước lắp vẫn như nhau) */
    private static String note(PcBuildResponse model, Map<String, Integer> chosenQty) {
        int modelFans = qty(model, "fans");
        int modelRam = qty(model, "ram");
        int fans = chosenQty.getOrDefault("fans", 0);
        int ram = chosenQty.getOrDefault("ram", 0);
        if (fans == modelFans && ram == modelRam) return null;
        return "Mô hình 3D là máy minh họa có " + modelRam + " thanh RAM và " + modelFans + " quạt lắp thêm; cấu hình này có "
                + ram + " thanh RAM và " + (fans == 0 ? "không lắp thêm quạt" : fans + " quạt") + ". Cách lắp vẫn giống nhau.";
    }

    private static int qty(PcBuildResponse model, String category) {
        return model.items().stream().filter(i -> category.equals(i.component().category()))
                .mapToInt(PcBuildResponse.Item::quantity).sum();
    }
}
