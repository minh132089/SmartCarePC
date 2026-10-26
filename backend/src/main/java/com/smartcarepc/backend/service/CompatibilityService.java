package com.smartcarepc.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcarepc.backend.dto.BuildCheckRequest;
import com.smartcarepc.backend.dto.CompatibilityResponse;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;
import com.smartcarepc.backend.entity.component.CaseFan;
import com.smartcarepc.backend.entity.component.Component;
import com.smartcarepc.backend.entity.component.Cooler;
import com.smartcarepc.backend.entity.component.Cpu;
import com.smartcarepc.backend.entity.component.Gpu;
import com.smartcarepc.backend.entity.component.Motherboard;
import com.smartcarepc.backend.entity.component.PcCase;
import com.smartcarepc.backend.entity.component.Psu;
import com.smartcarepc.backend.entity.component.Ram;
import com.smartcarepc.backend.entity.component.Ssd;
import com.smartcarepc.backend.entity.enums.CoolerType;
import com.smartcarepc.backend.entity.enums.PsuType;
import com.smartcarepc.backend.entity.enums.SsdInterface;
import com.smartcarepc.backend.exception.ResourceNotFoundException;
import com.smartcarepc.backend.repository.ComponentRepository;
import com.smartcarepc.backend.repository.PcBuildRepository;

import lombok.RequiredArgsConstructor;

/**
 * Kiểm tra các linh kiện có lắp chung được với nhau không.
 * Quy tắc nào thiếu số liệu (giá trị null) thì bỏ qua, không báo lỗi oan.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompatibilityService {

    /** Cộng thêm cho bo mạch, RAM, ổ, quạt khi ước tính công suất */
    private static final int BASE_WATT = 100;
    /** Nguồn nên dư ít nhất 30% so với mức tiêu thụ ước tính */
    private static final double PSU_HEADROOM = 1.3;

    private final ComponentRepository componentRepository;
    private final PcBuildRepository pcBuildRepository;

    /** Kiểm tra cấu hình người dùng gửi lên */
    public CompatibilityResponse check(BuildCheckRequest request) {
        Map<String, Integer> qtyByCode = new LinkedHashMap<>();
        request.items().forEach(i -> qtyByCode.merge(i.code(), i.qty(), Integer::sum));

        Map<String, Component> found = new LinkedHashMap<>();
        componentRepository.findByCodeIn(qtyByCode.keySet()).forEach(c -> found.put(c.getCode(), c));
        List<String> missing = qtyByCode.keySet().stream().filter(code -> !found.containsKey(code)).toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("component", String.join(", ", missing));
        }

        Map<Component, Integer> parts = new LinkedHashMap<>();
        qtyByCode.forEach((code, qty) -> parts.put(found.get(code), qty));
        return new Checker(parts).run();
    }

    /** Kiểm tra một máy có sẵn, vd /api/builds/mini_sff/check */
    public CompatibilityResponse checkBuild(String code) {
        var build = pcBuildRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("PC build", code));
        Map<Component, Integer> parts = new LinkedHashMap<>();
        build.getItems().forEach(i -> parts.put(i.getComponent(), i.getQuantity()));
        return new Checker(parts).run();
    }

    /** Một lần kiểm tra: gom linh kiện theo loại rồi chạy lần lượt từng quy tắc */
    private static class Checker {

        private final Map<Component, Integer> parts = new LinkedHashMap<>();
        private final List<Issue> errors = new ArrayList<>();
        private final List<Issue> warnings = new ArrayList<>();

        private PcCase pcCase;
        private Motherboard board;
        private Cpu cpu;
        private Cooler cooler;
        private Psu psu;

        Checker(Map<Component, Integer> input) {
            // Bỏ lớp proxy của Hibernate để instanceof nhận đúng lớp con (PcCase, Gpu, ...)
            input.forEach((c, qty) -> parts.put((Component) Hibernate.unproxy(c), qty));
        }

        CompatibilityResponse run() {
            pcCase = single(PcCase.class, "case", "case", true);
            board = single(Motherboard.class, "motherboard", "motherboard", true);
            cpu = single(Cpu.class, "cpu", "CPU", true);
            cooler = single(Cooler.class, "cooler", "CPU cooler", false); // thiếu thì checkCooler() cảnh báo
            psu = single(Psu.class, "psu", "power supply", true);

            checkRequired();
            checkBoardFitsCase();
            checkPsuFitsCase();
            checkSocket();
            checkRam();
            checkGpu();
            checkCooler();
            checkStorage();
            checkFans();
            checkPower();
            return new CompatibilityResponse(errors.isEmpty(), errors, warnings);
        }

        // ---- Các quy tắc ---------------------------------------------------

        private void checkRequired() {
            if (count(Ram.class) == 0) error("required.ram", "No RAM selected. At least one RAM module is required to boot.");
            if (count(Ssd.class) == 0) error("required.ssd", "No storage selected. An SSD is needed to install the operating system.");
            if (count(Gpu.class) == 0 && cpu != null && !cpu.isHasIgpu()) {
                error("required.gpu", "This CPU has no integrated graphics. Add a graphics card to output an image.");
            }
        }

        private void checkBoardFitsCase() {
            if (pcCase == null || board == null) return;
            if (!board.getFormFactor().fitsIn(pcCase.getMaxFormFactor())) {
                error("case.form_factor", "The " + board.getFormFactor().getLabel() + " motherboard is too large: this case supports up to "
                        + pcCase.getMaxFormFactor().getLabel() + ".");
            }
        }

        private void checkPsuFitsCase() {
            if (pcCase == null || psu == null || psu.getPsuType() == pcCase.getPsuType()) return;
            if (psu.getPsuType() == PsuType.ATX) {
                error("case.psu", "An ATX PSU is too large for this case, which only supports SFX power supplies.");
            } else {
                warning("case.psu", "An SFX PSU is smaller than the ATX mounting space. An SFX-to-ATX adapter bracket is required.");
            }
        }

        private void checkSocket() {
            String cpuSocket = cpu == null ? null : cpu.getSocket();
            if (cpuSocket == null) return;
            if (board != null && board.getSocket() != null && !board.getSocket().equals(cpuSocket)) {
                error("socket.board", "The CPU uses socket " + cpuSocket + " but the motherboard uses " + board.getSocket()
                        + ", so they are incompatible.");
            }
            if (cooler != null && !cooler.getSupportedSockets().isEmpty() && !cooler.getSupportedSockets().contains(cpuSocket)) {
                error("socket.cooler", "The cooler has no mounting kit for socket " + cpuSocket + ".");
            }
        }

        private void checkRam() {
            int sticks = count(Ram.class);
            if (board == null || sticks == 0) return;
            if (sticks > board.getRamSlots()) {
                error("ram.slots", "There are " + sticks + " RAM modules but the board has only " + board.getRamSlots() + " khe.");
            }
            all(Ram.class).stream()
                    .filter(r -> r.getRamType() != board.getRamType())
                    .findFirst()
                    .ifPresent(r -> error("ram.type", "RAM " + r.getRamType() + " cannot fit a motherboard using "
                            + board.getRamType() + ". The keying notches are in different positions."));
        }

        private void checkGpu() {
            int gpus = count(Gpu.class);
            if (board != null && gpus > board.getPcieX16Slots()) {
                error("gpu.slots", "There are " + gpus + " graphics cards but the board has only " + board.getPcieX16Slots() + " khe PCIe x16.");
            }
            if (pcCase == null || pcCase.getMaxGpuLengthMm() == null) return;
            for (Gpu gpu : all(Gpu.class)) {
                if (gpu.getLengthMm() > pcCase.getMaxGpuLengthMm()) {
                    error("gpu.length", "The graphics card is " + gpu.getLengthMm() + " mm long; the case supports up to "
                            + pcCase.getMaxGpuLengthMm() + " mm.");
                }
            }
        }

        private void checkCooler() {
            if (cooler == null) {
                warning("cooler.missing", "No CPU cooler selected. If the CPU does not include one, it can overheat and shut down.");
                return;
            }
            if (pcCase == null) return;
            if (cooler.getCoolerType() == CoolerType.AIO) {
                Integer max = pcCase.getMaxRadiatorMm();
                if (max == null) {
                    error("cooler.radiator", "This case has no radiator mount. Choose an air cooler.");
                } else if (cooler.getRadiatorMm() != null && cooler.getRadiatorMm() > max) {
                    error("cooler.radiator", "The radiator is " + cooler.getRadiatorMm() + " mm long; the case supports radiators up to "
                            + max + " mm.");
                }
            } else if (cooler.getHeightMm() != null && pcCase.getMaxCoolerHeightMm() != null
                    && cooler.getHeightMm() > pcCase.getMaxCoolerHeightMm()) {
                error("cooler.height", "The cooler is " + cooler.getHeightMm() + " mm tall; the case supports coolers up to "
                        + pcCase.getMaxCoolerHeightMm() + " mm. The side panel will not close.");
            }
        }

        private void checkStorage() {
            if (board == null) return;
            int m2 = all(Ssd.class).stream()
                    .filter(s -> s.getInterfaceType() != SsdInterface.SATA_25)
                    .mapToInt(s -> parts.get(s))
                    .sum();
            if (m2 > board.getM2Slots()) {
                error("ssd.m2_slots", "There are " + m2 + " M.2 drives but the board has only " + board.getM2Slots() + " khe M.2.");
            }
        }

        private void checkFans() {
            int fans = count(CaseFan.class);
            if (pcCase == null || fans == 0) return;
            if (fans > pcCase.getFanSlots()) {
                error("fans.slots", "There are " + fans + " fans but the case has only " + pcCase.getFanSlots() + " fan mounts.");
            }
            boolean hasArgb = all(CaseFan.class).stream().anyMatch(CaseFan::isArgb);
            if (hasArgb && board != null && !board.isHasArgbHeader() && !pcCase.isHasFanHub()) {
                warning("fans.argb", "The fans have ARGB lighting but neither the board nor the case has an ARGB header or hub. The fans will spin, but the lights will not work.");
            }
        }

        private void checkPower() {
            if (psu == null || psu.getWattage() == null || cpu == null || cpu.getTdpW() == null) return;
            int gpuWatt = all(Gpu.class).stream().map(Gpu::getTdpW).filter(Objects::nonNull).mapToInt(w -> w).sum();
            int need = (int) Math.ceil((cpu.getTdpW() + gpuWatt + BASE_WATT) * PSU_HEADROOM);
            if (psu.getWattage() < need) {
                // docs/pc_compatibility_rules.md mục 6: ước lượng công suất chỉ là cảnh báo, không phải điều kiện tương thích tuyệt đối
                warning("psu.wattage", "A " + psu.getWattage() + " W PSU may be insufficient. A preliminary estimate suggests at least " + need
                        + " W; also check the graphics card manufacturer's recommended PSU rating.");
            }
        }

        // ---- Hàm phụ ---------------------------------------------------------

        /** Lấy linh kiện chỉ được có một cái trong máy; có nhiều hơn thì báo lỗi */
        private <T extends Component> T single(Class<T> type, String rule, String label, boolean required) {
            List<T> list = all(type);
            if (list.isEmpty()) {
                if (required) error("required." + rule, "Missing " + label + ".");
                return null;
            }
            if (count(type) > 1) {
                error("duplicate." + rule, "A PC should have only one " + label + ", but you selected " + count(type) + ".");
            }
            return list.get(0);
        }

        private <T extends Component> List<T> all(Class<T> type) {
            return parts.keySet().stream().filter(type::isInstance).map(type::cast).toList();
        }

        /** Tổng số lượng, vd 2 thanh RAM tính là 2 */
        private int count(Class<? extends Component> type) {
            return parts.entrySet().stream().filter(e -> type.isInstance(e.getKey())).mapToInt(Map.Entry::getValue).sum();
        }

        private void error(String rule, String message) {
            errors.add(new Issue(rule, message));
        }

        private void warning(String rule, String message) {
            warnings.add(new Issue(rule, message));
        }
    }
}
