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
            log.info("Database đã có dữ liệu, bỏ qua bước nạp dữ liệu mẫu");
            return;
        }
        seedCategories();
        seedAssemblySteps();
        seedCredits();
        seedComponents();
        seedBuilds();
        log.info("Đã nạp dữ liệu mẫu: {} loại, {} linh kiện, {} bộ máy",
                categoryRepo.count(), componentRepo.count(), buildRepo.count());
    }

    // ---- Loại linh kiện (src/data/parts.ts) ------------------------------

    private void seedCategories() {
        category("case", "Vỏ máy (case)", "Giữ và bảo vệ mọi linh kiện bên trong, có chỗ gắn quạt để lấy gió.", false);
        category("motherboard", "Bo mạch chủ", "Tấm mạch lớn nối mọi linh kiện với nhau. Mỗi linh kiện có một khe cắm riêng trên tấm này.", true);
        category("cpu", "CPU (bộ xử lý)", "Bộ não của máy. Nằm ngay dưới tản nhiệt.", true);
        category("cooler", "Tản nhiệt CPU", "Hút nhiệt từ CPU ra ngoài để máy không bị quá nóng.", true);
        category("ram", "RAM", "Bộ nhớ tạm. Càng nhiều thì mở được càng nhiều chương trình cùng lúc.", true);
        category("gpu", "Card đồ họa (GPU)", "Xử lý hình ảnh. Cần cho chơi game, dựng phim và làm 3D.", true);
        category("ssd", "Ổ SSD", "Nơi lưu hệ điều hành, phần mềm và dữ liệu của bạn.", true);
        category("psu", "Nguồn (PSU)", "Lấy điện từ ổ cắm rồi chia cho từng linh kiện.", true);
        category("cables", "Dây cáp", "Dây nguồn đen to đưa điện tới bo mạch (24 pin), CPU (8 pin) và card đồ họa (PCIe). Dây quạt nhỏ cắm vào các cổng FAN trên bo. Bó dây nhiều màu từ nút nguồn cắm vào cổng F_PANEL ở góc dưới bên phải, dây xanh dẹt là USB mặt trước.", false);
        category("fans", "Quạt case", "Đưa gió mát vào và đẩy gió nóng ra khỏi thùng máy.", true);
        category("tools", "Đồ nghề lắp máy", "Những thứ cần cầm trên tay khi tự lắp: tua vít, dây chống tĩnh điện, keo tản nhiệt, hộp vít, dây rút, kìm cắt và dây điện. Bấm \"Tách rời linh kiện\" để thấy cả bộ bày trên thảm cạnh máy.", true);
    }

    private void category(String code, String name, String about, boolean inspectable) {
        PartCategory c = categoryRepo.save(PartCategory.builder()
                .code(code).name(name).about(about).inspectable(inspectable).sortOrder(cat.size() + 1).build());
        cat.put(code, c);
    }

    // ---- Các bước lắp (src/data/assembly.ts) -----------------------------

    private void seedAssemblySteps() {
        String[][] steps = {
                { "psu", "Lắp nguồn vào case", "Tháo nguồn" },
                { "cpu", "Đặt CPU vào socket", "Nhấc CPU ra" },
                { "ram", "Cắm RAM", "Rút RAM" },
                { "ssd", "Lắp ổ SSD M.2", "Tháo ổ SSD" },
                { "cooler", "Bôi keo, lắp tản nhiệt", "Tháo tản nhiệt" },
                { "fans", "Bắt quạt case", "Tháo quạt case" },
                { "gpu", "Cắm card đồ họa", "Rút card đồ họa" },
                { "cables", "Cắm và gom dây", "Rút dây cáp" },
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
                { "Card đồ họa", "GPU", "foreverautumn97", "https://sketchfab.com/3d-models/gpu-42393304578e4f1ab0ea3c7338aeea88" },
                { "Tản nhiệt tháp", "CPU Cooler", "Fochdog", "https://sketchfab.com/3d-models/cpu-cooler-672a0a74a98c452a862016bee99f3579" },
                { "Nguồn", "Power Supply - Basic", "Up1x", "https://sketchfab.com/3d-models/power-supply-basic-02db33d66a784c82a6202a3ec6850498" },
                { "Mainboard ATX / Micro-ATX", "X570 Prime motherboard [HQ PBR]", "Igor.Jop", "https://sketchfab.com/models/25b3659935774b1e9d73d8954be065d8" },
                { "Quạt", "computer fan", "Temoor", "https://sketchfab.com/models/5360bd331c5848eeb9338b4f894e78e5" },
                { "RAM có tản nhiệt", "RAM", "foreverautumn97", "https://sketchfab.com/models/fc241b31e4e747f7b5029beeb8a23173" },
                { "RAM thường", "Random Access Memory - RAM Stick - PC", "Diego G.", "https://sketchfab.com/models/ab2b1c25c31b44c1a757911734bdf942" },
                { "Ổ SSD", "NVME SSD 2280", "TDD", "https://sketchfab.com/models/3d478ac74728408890c2a03363138375" },
                { "Tản nhiệt nước AIO", "Liquid CPU Cooling", "Denzerru", "https://sketchfab.com/models/7a0014c1141a4c25bc69912075526ee5" },
        };
        for (String[] c : credits) {
            creditRepo.save(ModelCredit.builder().partLabel(c[0]).title(c[1]).author(c[2]).sourceUrl(c[3]).build());
        }
    }

    // ---- Linh kiện -------------------------------------------------------

    private void seedComponents() {
        // Vỏ máy: kích thước từ families.json, giới hạn lắp đặt từ 00_families_spec.md
        save(PcCase.builder().category(cat.get("case")).code("case_standard_tower").name("Vỏ Tower tiêu chuẩn")
                .modelUrl("/models/f_case_standard_tower.glb").imageUrl("/families/01_standard_tower.jpg")
                .widthMm(230).heightMm(460).depthMm(430).maxFormFactor(FormFactor.ATX).sidePanel(SidePanel.GLASS)
                .psuType(PsuType.ATX).maxGpuLengthMm(350).maxCoolerHeightMm(175).maxRadiatorMm(240)
                .gpuMount(GpuMount.HORIZONTAL).fanSlots(3).build());
        save(PcCase.builder().category(cat.get("case")).code("case_compact_tower").name("Vỏ Tower nhỏ gọn")
                .modelUrl("/models/f_case_compact_tower.glb").imageUrl("/families/02_compact_tower.jpg")
                .widthMm(210).heightMm(380).depthMm(370).maxFormFactor(FormFactor.MATX).sidePanel(SidePanel.SOLID)
                .psuType(PsuType.ATX).maxGpuLengthMm(280).maxCoolerHeightMm(160)
                .gpuMount(GpuMount.HORIZONTAL).fanSlots(3).build());
        save(PcCase.builder().category(cat.get("case")).code("case_showcase_dual_chamber").name("Vỏ Showcase hai khoang")
                .modelUrl("/models/f_case_showcase_dual_chamber.glb").imageUrl("/families/03_showcase_dual_chamber.jpg")
                .widthMm(300).heightMm(450).depthMm(450).maxFormFactor(FormFactor.ATX).sidePanel(SidePanel.GLASS)
                .psuType(PsuType.ATX).maxGpuLengthMm(330).maxCoolerHeightMm(150).maxRadiatorMm(240)
                .gpuMount(GpuMount.VERTICAL_RISER).fanSlots(6).hasFanHub(true).build());
        save(PcCase.builder().category(cat.get("case")).code("case_mini_sff").name("Vỏ Mini / SFF")
                .modelUrl("/models/f_case_mini_sff.glb").imageUrl("/families/04_mini_sff.jpg")
                .widthMm(180).heightMm(250).depthMm(350).maxFormFactor(FormFactor.ITX).sidePanel(SidePanel.MESH)
                .psuType(PsuType.SFX).maxGpuLengthMm(260).maxCoolerHeightMm(55)
                .gpuMount(GpuMount.SANDWICH_RISER).fanSlots(1).build());
        save(PcCase.builder().category(cat.get("case")).code("case_full_tower").name("Vỏ Tower lớn")
                .modelUrl("/models/f_case_full_tower.glb").imageUrl("/families/06_full_tower.jpg")
                .widthMm(250).heightMm(620).depthMm(550).maxFormFactor(FormFactor.ATX).sidePanel(SidePanel.GLASS)
                .psuType(PsuType.ATX).maxGpuLengthMm(450).maxCoolerHeightMm(180).maxRadiatorMm(360)
                .gpuMount(GpuMount.HORIZONTAL).fanSlots(5).hasFanHub(true).build());
        // Vỏ Gaming: kích thước theo src/data/builds.ts, chưa có bảng giới hạn lắp đặt
        save(PcCase.builder().category(cat.get("case")).code("case_gaming").name("Vỏ Mid-tower mặt kính (Gaming)")
                .modelUrl("/models/g_case.glb")
                .widthMm(220).heightMm(470).depthMm(450).maxFormFactor(FormFactor.ATX).sidePanel(SidePanel.GLASS)
                .psuType(PsuType.ATX).maxRadiatorMm(240).gpuMount(GpuMount.HORIZONTAL).fanSlots(4).build());

        // Bo mạch chủ (src/data/families.ts, boards.json). Spec dùng RAM DDR4
        save(Motherboard.builder().category(cat.get("motherboard")).code("mb_atx").name("Bo mạch chủ ATX")
                .modelUrl("/models/g_motherboard.glb").formFactor(FormFactor.ATX).ramType(RamType.DDR4)
                .ramSlots(4).pcieX16Slots(3).sysFanHeaders(4).hasArgbHeader(true).hasPumpHeader(true).build());
        save(Motherboard.builder().category(cat.get("motherboard")).code("mb_matx").name("Bo mạch chủ Micro-ATX")
                .modelUrl("/models/f_motherboard_matx.glb").formFactor(FormFactor.MATX).ramType(RamType.DDR4)
                .ramSlots(4).pcieX16Slots(2).sysFanHeaders(3).build());
        save(Motherboard.builder().category(cat.get("motherboard")).code("mb_itx").name("Bo mạch chủ Mini-ITX")
                .modelUrl("/models/f_motherboard_itx.glb").formFactor(FormFactor.ITX).ramType(RamType.DDR4)
                .ramSlots(2).pcieX16Slots(1).sysFanHeaders(2).hasUsbCHeader(true).build());

        save(Cpu.builder().category(cat.get("cpu")).code("cpu_std").name("CPU").modelUrl("/models/g_cpu.glb").build());

        // Tản nhiệt
        save(Cooler.builder().category(cat.get("cooler")).code("tower_air_155").name("Tản khí dạng tháp")
                .modelUrl("/models/f_tower_air_155.glb").coolerType(CoolerType.TOWER_AIR).heightMm(155).build());
        save(Cooler.builder().category(cat.get("cooler")).code("low_profile_47").name("Tản khí thấp")
                .modelUrl("/models/f_low_profile_47.glb").coolerType(CoolerType.LOW_PROFILE).heightMm(47).build());
        save(Cooler.builder().category(cat.get("cooler")).code("aio_240").name("Tản nước 240 mm")
                .modelUrl("/models/g_aio_radiator.glb").coolerType(CoolerType.AIO).radiatorMm(240).fanCount(2).build());
        save(Cooler.builder().category(cat.get("cooler")).code("aio_360").name("Tản nước 360 mm")
                .modelUrl("/models/f_aio_360.glb").coolerType(CoolerType.AIO).radiatorMm(360).fanCount(3).build());

        save(Ram.builder().category(cat.get("ram")).code("ram_ddr4").name("RAM DDR4")
                .modelUrl("/models/g_ram.glb").ramType(RamType.DDR4).heightMm(42).build());

        // Card đồ họa: gpu_280 = 280 × 125 × 55 (builds.ts), gpu_240 = 240 × 120 × 45 (spec)
        save(Gpu.builder().category(cat.get("gpu")).code("gpu_280").name("Card đồ họa 280 mm, 3 quạt")
                .modelUrl("/models/g_gpu.glb").lengthMm(280).heightMm(125).thicknessMm(55).fanCount(3)
                .powerConnector("8-pin").build());
        save(Gpu.builder().category(cat.get("gpu")).code("gpu_240").name("Card đồ họa 240 mm, 2 quạt")
                .modelUrl("/models/f_gpu_240_dual.glb").lengthMm(240).heightMm(120).thicknessMm(45).fanCount(2)
                .powerConnector("8-pin").build());

        save(Ssd.builder().category(cat.get("ssd")).code("ssd_m2_2280").name("Ổ SSD M.2 2280")
                .modelUrl("/models/g_ssd.glb").interfaceType(SsdInterface.M2_NVME).formSize("2280").build());

        save(Psu.builder().category(cat.get("psu")).code("psu_atx").name("Nguồn ATX")
                .modelUrl("/models/g_psu.glb").psuType(PsuType.ATX).build());
        save(Psu.builder().category(cat.get("psu")).code("psu_sfx").name("Nguồn SFX nhỏ gọn")
                .modelUrl("/models/f_psu_sfx_100.glb").psuType(PsuType.SFX).build());

        // Quạt case
        save(CaseFan.builder().category(cat.get("fans")).code("fan120").name("Quạt 120 mm")
                .modelUrl("/models/f_fan120.glb").sizeMm(120).build());
        save(CaseFan.builder().category(cat.get("fans")).code("fan140").name("Quạt 140 mm")
                .modelUrl("/models/f_fan140.glb").sizeMm(140).build());
        save(CaseFan.builder().category(cat.get("fans")).code("fan120_argb").name("Quạt 120 mm đèn ARGB")
                .modelUrl("/models/f_fan120_argb.glb").sizeMm(120).argb(true).build());
        save(CaseFan.builder().category(cat.get("fans")).code("fan140_argb").name("Quạt 140 mm đèn ARGB")
                .modelUrl("/models/g_case_fan_argb.glb").sizeMm(140).argb(true).build());

        save(Accessory.builder().category(cat.get("tools")).code("tool_kit").name("Bộ đồ nghề lắp máy")
                .modelUrl("/models/t_kit.glb").build());
    }

    private void save(com.smartcarepc.backend.entity.component.Component c) {
        part.put(c.getCode(), componentRepo.save(c));
    }

    // ---- Các máy trên thanh "Chọn kiểu máy" ------------------------------

    private void seedBuilds() {
        preset(1, "standard_tower", "Tower tiêu chuẩn", "Dáng tháp vừa, mặt lưới thoáng, dễ lắp và dễ nâng cấp.",
                "/families/01_standard_tower.jpg", "case_standard_tower", "mb_atx", "tower_air_155", "gpu_280", "psu_atx",
                Map.of("fan140", 2, "fan120", 1));
        preset(2, "compact_tower", "Tower nhỏ gọn", "Thấp và gọn hơn, hai hông kín, vẫn giữ dáng PC quen thuộc.",
                "/families/02_compact_tower.jpg", "case_compact_tower", "mb_matx", "tower_air_155", "gpu_240", "psu_atx",
                Map.of("fan120", 3));
        preset(3, "showcase_dual_chamber", "Showcase / Bể cá", "Kính trước và hông, hai khoang, nhiều quạt đèn để trưng bày.",
                "/families/03_showcase_dual_chamber.jpg", "case_showcase_dual_chamber", "mb_atx", "aio_240", "gpu_280", "psu_atx",
                Map.of("fan120_argb", 6));
        preset(4, "mini_sff", "Mini / SFF", "Hộp nhôm rất nhỏ: card đồ họa nằm sau lưng bo, nối bằng cáp riser.",
                "/families/04_mini_sff.jpg", "case_mini_sff", "mb_itx", "low_profile_47", "gpu_240", "psu_sfx",
                Map.of("fan120", 1));
        preset(5, "full_tower", "Tower lớn", "Tháp cao và sâu, bốn quạt trước, két nước 360 trên nóc, nhiều chỗ mở rộng.",
                "/families/06_full_tower.jpg", "case_full_tower", "mb_atx", "aio_360", "gpu_280", "psu_atx",
                Map.of("fan140", 5));
        preset(6, "gaming", "Gaming (đầy đủ)", "Mạnh về hình ảnh, nhiều quạt, mặt kính để nhìn thấy bên trong.",
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
