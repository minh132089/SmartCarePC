package com.smartcarepc.backend.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.smartcarepc.backend.entity.AssemblyStep;
import com.smartcarepc.backend.entity.ModelCredit;
import com.smartcarepc.backend.entity.PartCategory;
import com.smartcarepc.backend.entity.PcBuild;
import com.smartcarepc.backend.entity.component.Accessory;
import com.smartcarepc.backend.entity.component.CaseFan;
import com.smartcarepc.backend.entity.component.Cooler;
import com.smartcarepc.backend.entity.component.Cpu;
import com.smartcarepc.backend.entity.component.Gpu;
import com.smartcarepc.backend.entity.component.Motherboard;
import com.smartcarepc.backend.entity.component.PcCase;
import com.smartcarepc.backend.entity.component.Psu;
import com.smartcarepc.backend.entity.component.Ram;
import com.smartcarepc.backend.entity.component.Ssd;
import com.smartcarepc.backend.entity.enums.BuildType;
import com.smartcarepc.backend.entity.enums.CoolerType;
import com.smartcarepc.backend.entity.enums.FormFactor;
import com.smartcarepc.backend.entity.enums.GpuMount;
import com.smartcarepc.backend.entity.enums.PsuType;
import com.smartcarepc.backend.entity.enums.RamType;
import com.smartcarepc.backend.entity.enums.SidePanel;
import com.smartcarepc.backend.entity.enums.SsdInterface;
import com.smartcarepc.backend.repository.AssemblyStepRepository;
import com.smartcarepc.backend.repository.ComponentRepository;
import com.smartcarepc.backend.repository.ModelCreditRepository;
import com.smartcarepc.backend.repository.PartCategoryRepository;
import com.smartcarepc.backend.repository.PcBuildRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Nạp dữ liệu mẫu lấy từ frontend (src/data/*.ts, families.json, design/families/00_families_spec.md)
 * khi database còn trống. Đã có dữ liệu thì bỏ qua, nên chạy lại backend nhiều lần không bị trùng.
 * Socket, giá, công suất nguồn... frontend chưa có nên để null.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PartCategoryRepository categoryRepo;
    private final AssemblyStepRepository stepRepo;
    private final ModelCreditRepository creditRepo;
    private final ComponentRepository componentRepo;
    private final PcBuildRepository buildRepo;

    private final Map<String, PartCategory> cat = new HashMap<>();
    private final Map<String, com.smartcarepc.backend.entity.component.Component> part = new HashMap<>();

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepo.count() > 0) {
            log.info("Database already contains data; skipping sample data import");
            return;
        }
        seedCategories();
        seedAssemblySteps();
        seedCredits();
        seedComponents();
        seedBuilds();
        log.info("Loaded sample data: {} categories, {} components, {} PC builds",
                categoryRepo.count(), componentRepo.count(), buildRepo.count());
    }

    // ---- Loại linh kiện (src/data/parts.ts) ------------------------------

    private void seedCategories() {
        category("case", "PC case", "Holds and protects the components, with fan mounts for cooling.", false);
        category("motherboard", "Motherboard", "The main circuit board connects all the components. Each type has its own socket or slot.", true);
        category("cpu", "CPU (processor)", "Processes instructions and sits beneath the CPU cooler.", true);
        category("cooler", "CPU cooler", "Removes heat from the CPU to prevent overheating.", true);
        category("ram", "RAM", "Temporary working memory. More RAM lets you run more programs at once.", true);
        category("gpu", "Graphics card (GPU)", "Handles graphics for gaming, video editing and 3D work.", true);
        category("ssd", "SSD", "Stores your operating system, programs and files.", true);
        category("psu", "Power supply (PSU)", "Converts mains electricity into power for the components.", true);
        category("cables", "Cables", "Thick power cables feed the motherboard (24-pin), CPU (8-pin) and GPU (PCIe). Fan cables use FAN headers. Case button leads connect to F_PANEL; the blue cable connects front USB.", false);
        category("fans", "Case fans", "Bring cool air into the case and push warm air out.", true);
        category("tools", "Assembly tools", "Tools for assembly: a screwdriver, anti-static strap, thermal paste, screw organiser, cable ties, cutters and power cable. Select Explode view to see them beside the PC.", true);
    }

    private void category(String code, String name, String about, boolean inspectable) {
        PartCategory c = categoryRepo.save(PartCategory.builder()
                .code(code).name(name).about(about).inspectable(inspectable).sortOrder(cat.size() + 1).build());
        cat.put(code, c);
    }

    // ---- Các bước lắp (src/data/assembly.ts) -----------------------------

    private void seedAssemblySteps() {
        String[][] steps = {
                { "psu", "Install the power supply", "Remove the power supply" },
                { "cpu", "Place the CPU in its socket", "Lift out the CPU" },
                { "ram", "Install the RAM", "Remove the RAM" },
                { "ssd", "Install the M.2 SSD", "Remove the SSD" },
                { "cooler", "Apply thermal paste and install the cooler", "Remove the cooler" },
                { "fans", "Install the case fans", "Remove the case fans" },
                { "gpu", "Install the graphics card", "Remove the graphics card" },
                { "cables", "Connect and organise the cables", "Disconnect the cables" },
        };
        for (int i = 0; i < steps.length; i++) {
            stepRepo.save(AssemblyStep.builder()
                    .stepNo(i + 1).category(cat.get(steps[i][0])).installText(steps[i][1]).removeText(steps[i][2]).build());
        }
    }

    // ---- Nguồn model 3D (src/data/credits.ts) ----------------------------

    private void seedCredits() {
        String[][] credits = {
                { "Mainboard Mini-ITX", "Motherboard", "Alice Solo", "https://sketchfab.com/3d-models/motherboard-02acc04c6ac743b28d6591763afe0a50" },
                { "Graphics card", "GPU", "foreverautumn97", "https://sketchfab.com/3d-models/gpu-42393304578e4f1ab0ea3c7338aeea88" },
                { "Tower CPU cooler", "CPU Cooler", "Fochdog", "https://sketchfab.com/3d-models/cpu-cooler-672a0a74a98c452a862016bee99f3579" },
                { "Power supply", "Power Supply - Basic", "Up1x", "https://sketchfab.com/3d-models/power-supply-basic-02db33d66a784c82a6202a3ec6850498" },
                { "Mainboard ATX / Micro-ATX", "X570 Prime motherboard [HQ PBR]", "Igor.Jop", "https://sketchfab.com/models/25b3659935774b1e9d73d8954be065d8" },
                { "Fan", "computer fan", "Temoor", "https://sketchfab.com/models/5360bd331c5848eeb9338b4f894e78e5" },
                { "RAM with heat spreader", "RAM", "foreverautumn97", "https://sketchfab.com/models/fc241b31e4e747f7b5029beeb8a23173" },
                { "Standard RAM", "Random Access Memory - RAM Stick - PC", "Diego G.", "https://sketchfab.com/models/ab2b1c25c31b44c1a757911734bdf942" },
                { "SSD", "NVME SSD 2280", "TDD", "https://sketchfab.com/models/3d478ac74728408890c2a03363138375" },
                { "AIO liquid cooler", "Liquid CPU Cooling", "Denzerru", "https://sketchfab.com/models/7a0014c1141a4c25bc69912075526ee5" },
        };
        for (String[] c : credits) {
            creditRepo.save(ModelCredit.builder().partLabel(c[0]).title(c[1]).author(c[2]).sourceUrl(c[3]).build());
        }
    }

    // ---- Linh kiện -------------------------------------------------------

    private void seedComponents() {
        // Vỏ máy: kích thước từ families.json, giới hạn lắp đặt từ 00_families_spec.md
        save(PcCase.builder().category(cat.get("case")).code("case_standard_tower").name("Standard tower case")
                .modelUrl("/models/f_case_standard_tower.glb").imageUrl("/families/01_standard_tower.jpg")
                .widthMm(230).heightMm(460).depthMm(430).maxFormFactor(FormFactor.ATX).sidePanel(SidePanel.GLASS)
                .psuType(PsuType.ATX).maxGpuLengthMm(350).maxCoolerHeightMm(175).maxRadiatorMm(240)
                .gpuMount(GpuMount.HORIZONTAL).fanSlots(3).build());
        save(PcCase.builder().category(cat.get("case")).code("case_compact_tower").name("Compact tower case")
                .modelUrl("/models/f_case_compact_tower.glb").imageUrl("/families/02_compact_tower.jpg")
                .widthMm(210).heightMm(380).depthMm(370).maxFormFactor(FormFactor.MATX).sidePanel(SidePanel.SOLID)
                .psuType(PsuType.ATX).maxGpuLengthMm(280).maxCoolerHeightMm(160)
                .gpuMount(GpuMount.HORIZONTAL).fanSlots(3).build());
        save(PcCase.builder().category(cat.get("case")).code("case_showcase_dual_chamber").name("Dual-chamber showcase case")
                .modelUrl("/models/f_case_showcase_dual_chamber.glb").imageUrl("/families/03_showcase_dual_chamber.jpg")
                .widthMm(300).heightMm(450).depthMm(450).maxFormFactor(FormFactor.ATX).sidePanel(SidePanel.GLASS)
                .psuType(PsuType.ATX).maxGpuLengthMm(330).maxCoolerHeightMm(150).maxRadiatorMm(240)
                .gpuMount(GpuMount.VERTICAL_RISER).fanSlots(6).hasFanHub(true).build());
        save(PcCase.builder().category(cat.get("case")).code("case_mini_sff").name("Mini / SFF case")
                .modelUrl("/models/f_case_mini_sff.glb").imageUrl("/families/04_mini_sff.jpg")
                .widthMm(180).heightMm(250).depthMm(350).maxFormFactor(FormFactor.ITX).sidePanel(SidePanel.MESH)
                .psuType(PsuType.SFX).maxGpuLengthMm(260).maxCoolerHeightMm(55)
                .gpuMount(GpuMount.SANDWICH_RISER).fanSlots(1).build());
        save(PcCase.builder().category(cat.get("case")).code("case_full_tower").name("Full-tower case")
                .modelUrl("/models/f_case_full_tower.glb").imageUrl("/families/06_full_tower.jpg")
                .widthMm(250).heightMm(620).depthMm(550).maxFormFactor(FormFactor.ATX).sidePanel(SidePanel.GLASS)
                .psuType(PsuType.ATX).maxGpuLengthMm(450).maxCoolerHeightMm(180).maxRadiatorMm(360)
                .gpuMount(GpuMount.HORIZONTAL).fanSlots(5).hasFanHub(true).build());
        // Vỏ Gaming: kích thước theo src/data/builds.ts, chưa có bảng giới hạn lắp đặt
        save(PcCase.builder().category(cat.get("case")).code("case_gaming").name("Glass-panel mid-tower case (Gaming)")
                .modelUrl("/models/g_case.glb")
                .widthMm(220).heightMm(470).depthMm(450).maxFormFactor(FormFactor.ATX).sidePanel(SidePanel.GLASS)
                .psuType(PsuType.ATX).maxRadiatorMm(240).gpuMount(GpuMount.HORIZONTAL).fanSlots(4).build());

        // Bo mạch chủ (src/data/families.ts, boards.json). Spec dùng RAM DDR4
        save(Motherboard.builder().category(cat.get("motherboard")).code("mb_atx").name("ATX motherboard")
                .modelUrl("/models/g_motherboard.glb").formFactor(FormFactor.ATX).ramType(RamType.DDR4)
                .ramSlots(4).pcieX16Slots(3).sysFanHeaders(4).hasArgbHeader(true).hasPumpHeader(true).build());
        save(Motherboard.builder().category(cat.get("motherboard")).code("mb_matx").name("Micro-ATX motherboard")
                .modelUrl("/models/f_motherboard_matx.glb").formFactor(FormFactor.MATX).ramType(RamType.DDR4)
                .ramSlots(4).pcieX16Slots(2).sysFanHeaders(3).build());
        save(Motherboard.builder().category(cat.get("motherboard")).code("mb_itx").name("Mini-ITX motherboard")
                .modelUrl("/models/f_motherboard_itx.glb").formFactor(FormFactor.ITX).ramType(RamType.DDR4)
                .ramSlots(2).pcieX16Slots(1).sysFanHeaders(2).hasUsbCHeader(true).build());

        save(Cpu.builder().category(cat.get("cpu")).code("cpu_std").name("CPU").modelUrl("/models/g_cpu.glb").build());

        // Tản nhiệt
        save(Cooler.builder().category(cat.get("cooler")).code("tower_air_155").name("Tower air cooler")
                .modelUrl("/models/f_tower_air_155.glb").coolerType(CoolerType.TOWER_AIR).heightMm(155).build());
        save(Cooler.builder().category(cat.get("cooler")).code("low_profile_47").name("Low-profile air cooler")
                .modelUrl("/models/f_low_profile_47.glb").coolerType(CoolerType.LOW_PROFILE).heightMm(47).build());
        save(Cooler.builder().category(cat.get("cooler")).code("aio_240").name("240 mm liquid cooler")
                .modelUrl("/models/g_aio_radiator.glb").coolerType(CoolerType.AIO).radiatorMm(240).fanCount(2).build());
        save(Cooler.builder().category(cat.get("cooler")).code("aio_360").name("360 mm liquid cooler")
                .modelUrl("/models/f_aio_360.glb").coolerType(CoolerType.AIO).radiatorMm(360).fanCount(3).build());

        save(Ram.builder().category(cat.get("ram")).code("ram_ddr4").name("RAM DDR4")
                .modelUrl("/models/g_ram.glb").ramType(RamType.DDR4).heightMm(42).build());

        // Card đồ họa: gpu_280 = 280 × 125 × 55 (builds.ts), gpu_240 = 240 × 120 × 45 (spec)
        save(Gpu.builder().category(cat.get("gpu")).code("gpu_280").name("280 mm graphics card, 3 fans")
                .modelUrl("/models/g_gpu.glb").lengthMm(280).heightMm(125).thicknessMm(55).fanCount(3)
                .powerConnector("8-pin").build());
        save(Gpu.builder().category(cat.get("gpu")).code("gpu_240").name("240 mm graphics card, 2 fans")
                .modelUrl("/models/f_gpu_240_dual.glb").lengthMm(240).heightMm(120).thicknessMm(45).fanCount(2)
                .powerConnector("8-pin").build());

        save(Ssd.builder().category(cat.get("ssd")).code("ssd_m2_2280").name("M.2 2280 SSD")
                .modelUrl("/models/g_ssd.glb").interfaceType(SsdInterface.M2_NVME).formSize("2280").build());

        save(Psu.builder().category(cat.get("psu")).code("psu_atx").name("ATX power supply")
                .modelUrl("/models/g_psu.glb").psuType(PsuType.ATX).build());
        save(Psu.builder().category(cat.get("psu")).code("psu_sfx").name("Compact SFX power supply")
                .modelUrl("/models/f_psu_sfx_100.glb").psuType(PsuType.SFX).build());

        // Quạt case
        save(CaseFan.builder().category(cat.get("fans")).code("fan120").name("120 mm fan")
                .modelUrl("/models/f_fan120.glb").sizeMm(120).build());
        save(CaseFan.builder().category(cat.get("fans")).code("fan140").name("140 mm fan")
                .modelUrl("/models/f_fan140.glb").sizeMm(140).build());
        save(CaseFan.builder().category(cat.get("fans")).code("fan120_argb").name("120 mm ARGB fan")
                .modelUrl("/models/f_fan120_argb.glb").sizeMm(120).argb(true).build());
        save(CaseFan.builder().category(cat.get("fans")).code("fan140_argb").name("140 mm ARGB fan")
                .modelUrl("/models/g_case_fan_argb.glb").sizeMm(140).argb(true).build());

        save(Accessory.builder().category(cat.get("tools")).code("tool_kit").name("PC assembly toolkit")
                .modelUrl("/models/t_kit.glb").build());
    }

    private void save(com.smartcarepc.backend.entity.component.Component c) {
        part.put(c.getCode(), componentRepo.save(c));
    }

    // ---- Các máy trên thanh "Chọn kiểu máy" ------------------------------

    private void seedBuilds() {
        preset(1, "standard_tower", "Standard tower", "A mid-sized tower with a ventilated mesh front, easy assembly and room to upgrade.",
                "/families/01_standard_tower.jpg", "case_standard_tower", "mb_atx", "tower_air_155", "gpu_280", "psu_atx",
                Map.of("fan140", 2, "fan120", 1));
        preset(2, "compact_tower", "Compact tower", "A shorter, compact tower with solid side panels and a familiar PC layout.",
                "/families/02_compact_tower.jpg", "case_compact_tower", "mb_matx", "tower_air_155", "gpu_240", "psu_atx",
                Map.of("fan120", 3));
        preset(3, "showcase_dual_chamber", "Dual-chamber showcase", "Front and side glass panels, two chambers and illuminated fans to show off your build.",
                "/families/03_showcase_dual_chamber.jpg", "case_showcase_dual_chamber", "mb_atx", "aio_240", "gpu_280", "psu_atx",
                Map.of("fan120_argb", 6));
        preset(4, "mini_sff", "Mini / SFF", "A tiny aluminium case with the GPU behind the motherboard, connected by a riser cable.",
                "/families/04_mini_sff.jpg", "case_mini_sff", "mb_itx", "low_profile_47", "gpu_240", "psu_sfx",
                Map.of("fan120", 1));
        preset(5, "full_tower", "Full tower", "A tall, deep tower with four front fans, a 360 mm top radiator and room to expand.",
                "/families/06_full_tower.jpg", "case_full_tower", "mb_atx", "aio_360", "gpu_280", "psu_atx",
                Map.of("fan140", 5));
        preset(6, "gaming", "Gaming (complete)", "Built for graphics, with plenty of cooling and a glass side panel.",
                null, "case_gaming", "mb_atx", "aio_240", "gpu_280", "psu_atx",
                Map.of("fan140_argb", 3, "fan120", 1));
    }

    /** Mọi máy mẫu đều có 1 CPU, 2 thanh RAM và 1 ổ SSD; phần khác nhau truyền vào */
    private void preset(int order, String code, String name, String tagline, String thumb,
            String pcCase, String board, String cooler, String gpu, String psu, Map<String, Integer> fans) {
        PcBuild b = PcBuild.builder().code(code).name(name).tagline(tagline).thumbnailUrl(thumb)
                .buildType(BuildType.PRESET).isPublic(true).sortOrder(order).build();
        for (String c : List.of(pcCase, board, "cpu_std", cooler, gpu, "ssd_m2_2280", psu)) {
            b.addItem(part.get(c), 1);
        }
        b.addItem(part.get("ram_ddr4"), 2);
        fans.forEach((fan, qty) -> b.addItem(part.get(fan), qty));
        buildRepo.save(b);
    }
}
