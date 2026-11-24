package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ProductRulesTests {

    @Test
    void tenNguonTrungMaChipsetKhongDuocGuiThongSoBoChoAi() {
        assertThat(ProductRules.describeAny("Nguồn máy tính Jetek Revolve B650 (650W)")).isEmpty();
        assertThat(ProductRules.describeAny("Nguồn máy tính Jetek Revolve B550 (550W)")).isEmpty();
        assertThat(ProductRules.describeAny("Mainboard ASUS B650M DDR5")).contains("AM5").contains("B650");
        assertThat(ProductRules.describeAny("CPU Intel Core i5 12400F")).contains("LGA1700");
        assertThat(ProductRules.describeAny("RAM Corsair 16GB DDR4")).isEqualTo("DDR4");
    }

    @Test
    void a620aKhongBiBoSotKhiModelCoHauToAM() {
        // https://www.msi.com/Motherboard/PRO-A620AM-B-EVO/Specification
        var title = "Bo mạch chủ MSI PRO A620AM-B EVO";
        assertThat(ProductRules.read("motherboard", title).describe()).isEqualTo("socket AM5, chipset A620A, DDR5");
        assertThat(ChatService.boardSize(title)).isEqualTo(2);
        assertThat(ProductRules.issues(Map.of("cpu", "CPU Intel Core i5 14400F", "motherboard", title)))
                .anySatisfy(i -> assertThat(i).contains("LGA1700").contains("AM5"));
    }

    @Test
    void soQuatCardKhongDuDeKetLuanVuaVoVaQuatLapThemCanKiemViTri() {
        assertThat(ProductRules.findings(Map.of("gpu","ASUS Dual RTX 4060 8GB","case","Silverstone SG13","fans","Bộ3quạt Xigmatek")))
                .filteredOn(f -> f.severity()==ProductRules.Severity.UNKNOWN).extracting(ProductRules.Finding::ruleId)
                .contains("GPU_CLEARANCE","FAN_MOUNTS");
        assertThat(ProductRules.issues(Map.of("gpu","SPARKLE Intel Arc B580 TITAN OC SB580T-12GOC",
                "case","Vỏ Silverstone Sugo SG13B-Q"))).anySatisfy(issue -> assertThat(issue).contains("315 mm", "267 mm"));
    }

    @Test
    void soKheRamTheoDungMaBoKhongSuyTuChipset() {
        assertThat(ProductRules.memorySlots("Mainboard MSI PRO B760M-E DDR4")).isEqualTo(2);
        // Mã bo hay gặp mà tên không ghi số khe: theo trang thông số của hãng
        assertThat(ProductRules.memorySlots("Mainboard MSI PRO B760M-P DDR4")).isEqualTo(4);
        assertThat(ProductRules.memorySlots("Mainboard MSI PRO A520M-S")).isEqualTo(2);
        assertThat(ProductRules.memorySlots("Mainboard ASUS PRIME B550M-A")).isEqualTo(4);
        assertThat(ProductRules.memorySlots("Mainboard ASUS PRIME B660M-K (LGA1700, DDR4) — không tấm chắn I/O")).isEqualTo(2);
        assertThat(ProductRules.memorySlots("Mainboard Gigabyte B760M DS3H DDR4")).isEqualTo(4);
        assertThat(ProductRules.memorySlots("Mainboard Gigabyte B760M D DDR4")).isEqualTo(2);
        assertThat(ChatService.JUNK.matcher("Mainboard ASUS PRIME B660M-K (LGA1700, DDR4) — không tấm chắn I/O").find()).isTrue();
        assertThat(ProductRules.memorySlots("Mainboard MSI PRO H610M-E DDR4 | LGA 1700, mATX, 2 khe RAM")).isEqualTo(2);
        assertThat(ProductRules.memorySlots("Main GIGABYTE H610M-H V3 DDR4")).isEqualTo(2);
        assertThat(ProductRules.memorySlots("GIGABYTE B760M GAMING PLUS WIFI DDR4")).isEqualTo(4);
        assertThat(ProductRules.memorySlots("GIGABYTE B760 GAMING X DDR4")).isEqualTo(4);
        assertThat(ProductRules.memorySlots("Mainboard B760M DDR4")).isNull();
    }

    @Test
    void docThongSoTuTenSanPham() {
        assertThat(ProductRules.read("motherboard", "Bo mạch chính/ Mainboard MSI B760 GAMING PLUS WIFI DDR5").describe())
                .isEqualTo("socket LGA1700, chipset B760, DDR5");
        assertThat(ProductRules.read("motherboard", "Mainboard ASUS PRIME B760M-A D4-CSM").ramType()).isEqualTo("DDR4");
        // Nền tảng chỉ có một loại RAM thì tự suy ra
        assertThat(ProductRules.read("motherboard", "Mainboard Gigabyte B650M AORUS ELITE AX").ramType()).isEqualTo("DDR5");
        assertThat(ProductRules.read("motherboard", "Bo Mạch Chủ Gigabyte B550M H ARGB").describe()).isEqualTo("socket AM4, chipset B550, DDR4");
        assertThat(ProductRules.read("cpu", "Bộ vi xử lý Intel Core i5-12400F").socket()).isEqualTo("LGA1700");
        assertThat(ProductRules.read("cpu", "CPU Intel Core Ultra 7 265K").describe()).isEqualTo("socket LGA1851, DDR5");
        assertThat(ProductRules.read("cpu", "AMD Ryzen 5 5600 3.5GHz").describe()).isEqualTo("socket AM4, DDR4");
        assertThat(ProductRules.read("cpu", "CPU AMD Ryzen 7 7800X3D").describe()).isEqualTo("socket AM5, DDR5");
        assertThat(ProductRules.read("ram", "Ram Laptop Kingston 16GB DDR4 3200").laptopRam()).isTrue();
        assertThat(ProductRules.read("ram", "RAM Corsair Vengeance LPX 16GB DDR4 3200").describe()).isEqualTo("DDR4");
        // Không đọc ra thì không đoán
        assertThat(ProductRules.read("motherboard", "Bo mạch chủ ATX").describe()).isEmpty();
    }

    @Test
    void boDdr5ThiKhongDiVoiRamDdr4() {
        var issues = ProductRules.issues(Map.of(
                "cpu", "Bộ vi xử lý Intel Core i5 12400F",
                "motherboard", "Bo mạch chính/ Mainboard MSI B760 GAMING PLUS WIFI DDR5",
                "ram", "Bộ nhớ/ RAM Corsair DDR4 Vengeance LPX 16GB"));
        assertThat(issues).singleElement().asString().contains("DDR5").contains("DDR4");
    }

    @Test
    void socketVaDoiCpuPhaiKhopBo() {
        // Ryzen 7000 (AM5) trên bo B550 (AM4)
        assertThat(ProductRules.issues(Map.of("cpu", "AMD Ryzen 5 7600X", "motherboard", "MSI B550M PRO-VDH WIFI")))
                .anySatisfy(s -> assertThat(s).contains("AM5").contains("AM4"));
        // Core Ultra chỉ chạy DDR5
        assertThat(ProductRules.issues(Map.of("cpu", "Intel Core Ultra 5 225F", "ram", "RAM Kingston Fury 16GB DDR4 3200")))
                .anySatisfy(s -> assertThat(s).contains("DDR5"));
        // Core i5 đời 13 trên bo Z890 (LGA1851)
        assertThat(ProductRules.issues(Map.of("cpu", "Intel Core i5-13400F", "motherboard", "ASUS PRIME Z890-P WIFI")))
                .isNotEmpty();
        // Cấu hình đúng: không báo gì
        assertThat(ProductRules.issues(Map.of("cpu", "Intel Core i5-12400F", "motherboard", "MSI PRO B760M-E DDR4",
                "ram", "RAM Corsair Vengeance LPX 16GB DDR4 3200"))).isEmpty();
        assertThat(ProductRules.issues(Map.of("cpu", "AMD Ryzen 5 7500F", "motherboard", "Gigabyte B650M D3HP",
                "ram", "RAM Kingston Fury Beast 32GB DDR5 6000"))).isEmpty();
    }

    @Test
    void ramLaptopKhongLapVaoMayBan() {
        assertThat(ProductRules.issues(Map.of("ram", "Ram Laptop Crucial DDR4 16GB Bus 3200"))).singleElement().asString()
                .contains("SO-DIMM");
    }

    @Test
    void mucDoTheoBoLuatChuan() {
        // Bo B660 (đời 600) với Core i5 đời 14: lắp được nhưng có thể cần BIOS mới
        var f = ProductRules.findings(Map.of("cpu", "Intel Core i5-14400F", "motherboard", "MSI PRO B660M-A DDR4"));
        assertThat(f).anySatisfy(x -> {
            assertThat(x.ruleId()).isEqualTo("CPU_BIOS_UPDATE");
            assertThat(x.severity()).isEqualTo(ProductRules.Severity.WARNING);
        });
        // CPU đuôi K trên bo B: chỉ cảnh báo, không phải lỗi
        assertThat(ProductRules.findings(Map.of("cpu", "Intel Core i5-14600KF", "motherboard", "ASUS PRIME B760M-A D4")))
                .anySatisfy(x -> assertThat(x.ruleId()).isEqualTo("CPU_OC_CHIPSET"))
                .noneSatisfy(x -> assertThat(x.severity()).isEqualTo(ProductRules.Severity.ERROR));
        // Cùng socket vẫn chưa đủ: luôn có UNKNOWN cho CPU Support List; có tản thì chưa xác minh gá
        assertThat(ProductRules.findings(Map.of("cpu", "AMD Ryzen 5 7600", "motherboard", "Gigabyte B650M D3HP",
                "cooler", "Tản nhiệt Cooler Master Hyper 212")))
                .extracting(ProductRules.Finding::ruleId).contains("CPU_SUPPORT_LIST", "COOLER_MOUNT");
        // Lỗi có đủ trường như mục 10: rule_id, severity, affected_parts, reason, required_action
        var err = ProductRules.findings(Map.of("cpu", "AMD Ryzen 5 7600X", "motherboard", "MSI B550M PRO-VDH WIFI")).getFirst();
        assertThat(err.ruleId()).isEqualTo("CPU_SOCKET");
        assertThat(err.severity()).isEqualTo(ProductRules.Severity.ERROR);
        assertThat(err.affectedParts()).containsExactly("cpu", "motherboard");
        assertThat(err.requiredAction()).contains("AM5");
    }

    @Test
    void soKheRamGhiTrenTenSanPham() {
        assertThat(ProductRules.memorySlots("Mainboard ASRock H610M-H2/M.2 DDR4 | LGA 1700, mATX, 2 khe RAM")).isEqualTo(2);
        assertThat(ProductRules.memorySlots("Mainboard ASROCK B550M PRO4 (AMD B550, Socket AM4, m-ATX, 4 khe RAM DDR4)")).isEqualTo(4);
        assertThat(ProductRules.memorySlots("Mainboard ASUS PRIME B550M-A (AMD B550, Socket AM4,m- ATX, 4 khe RAM DRR4)")).isEqualTo(4);
        assertThat(ProductRules.memorySlots("Mainboard Gigabyte A520M K V2 (AMD A520/ Socket AM4/ M-ATX/ 2 khe ram)")).isEqualTo(2);
        assertThat(ProductRules.memorySlots("MSI PRO B760M-P DDR4, 4 x DIMM")).isEqualTo(4);
        // Khe M.2 / PCIe không phải khe RAM; tên không ghi thì không đoán
        assertThat(ProductRules.memorySlots("Mainboard ASRock B760M-HDV/M.2 D4 2 khe M.2")).isNull();
        assertThat(ProductRules.memorySlots("Mainboard ASRock H610M-H2/M.2 DDR4")).isNull();
        assertThat(ProductRules.memorySlots("Mainboard MSI PRO B760M-E DDR4")).isEqualTo(2);
    }

    @Test
    void tanChiCoGaIntelKhongLapDuocChoCpuAmd() {
        // Ca thật 24/11: trang nguyenvu.store ghi "Socket hỗ trợ: Intel LGA 1851 / 1700" cho tản này
        var alta = "Bộ Cooler cho CPU Deepcool ALTA 9 PWM 1700 (DP-ICAP-AT9-1700)";
        assertThat(ProductRules.findings(Map.of("cpu", "CPU AMD Ryzen 5 5600 (Socket AM4)", "motherboard", "MSI B550M PRO", "cooler", alta)))
                .anySatisfy(f -> {
                    assertThat(f.ruleId()).isEqualTo("COOLER_SOCKET");
                    assertThat(f.severity()).isEqualTo(ProductRules.Severity.ERROR);
                    assertThat(f.requiredAction()).contains("AM4");
                });
        assertThat(ProductRules.findings(Map.of("cpu", "Intel Core i5 12400F", "cooler", alta)))
                .noneSatisfy(f -> assertThat(f.ruleId()).isEqualTo("COOLER_SOCKET"));
        assertThat(ProductRules.coolerVendor("Air cooler Noctua NH-L9I-17XX (Mini-ITX và HTPC)")).isEqualTo("Intel");
        assertThat(ProductRules.coolerVendor("Noctua NH-L9a-AM5 chromax.black")).isEqualTo("AMD");
        // Số model không phải socket, tên ghi cả hai hãng, hay không ghi gì: không kết luận
        assertThat(ProductRules.coolerVendor("Air cooler CPU JONSBO CR-1200E RGB BLACK")).isNull();
        assertThat(ProductRules.coolerVendor("Tản ID-Cooling SE-214-XT (LGA1700/AM4/AM5)")).isNull();
        assertThat(ProductRules.coolerVendor("Cooler DEEPCOOL AK400 DIGITAL ARGB")).isNull();
        assertThat(ProductRules.coolerVendor("Air cooler CPU ID-COOLING IS-40X V3 - BLACK (Low Profile / ITX mini / Cao 45mm)")).isNull();
    }

    @Test
    void cungHangOChoBanKhacDeDungGiaReHon() {
        // Rà soát 24/11: hai tên của cùng i3-12100 ở GEARVN (3,39 triệu) và AnDuy (2,45 triệu)
        assertThat(ChatService.sameOffer("Processor Intel Core i3 12100 / 3.3GHz Turbo 4.3GHz / 4 Cores 8 Threads / 12MB / LGA 1700",
                "CPU INTEL CORE I3- 12100 (3.3GHZ TURBO UP TO 4.3GHZ, 4 cores 8 threads, 12MB CACHE, 58W)- SOCKET INTEL LGA 1700)")).isTrue();
        assertThat(ChatService.sameOffer("KM CPU Intel Core i5 12400 (Up to 4.4 GHz, 6C/12T, LGA 1700)",
                "CPU Intel Core i5 12400 Box (Socket 1700/ Base 2.5Ghz/ Turbo 4.4GHz/ 6 Cores/ 12 Threads/ Cache 18MB)")).isTrue();
        // Đuôi F (không có đồ họa tích hợp), bản tray, đời khác: không phải cùng hàng
        assertThat(ChatService.sameOffer("CPU Intel Core i5 12400F", "CPU Intel Core i5 12400 Box")).isFalse();
        assertThat(ChatService.sameOffer("CPU Intel Core i5 12400 Tray", "CPU Intel Core i5 12400 Box")).isFalse();
        assertThat(ChatService.sameOffer("CPU Intel Core i3 12100", "CPU Intel Core i3 14100 Box")).isFalse();
        assertThat(ChatService.sameOffer("Power supply Cooler Master MWE V2 230V 450 450W Plus Bronze",
                "Power supply Cooler Master MWE V2 230V 550 550W Plus Bronze")).isFalse();
    }

    @Test
    void nguonChoMayCoCardPhaiCoChungNhanHoacHangCoTen() {
        // Rà soát 24/11 a06: nguồn hãng ít tên tuổi, không chứng nhận, đi với card RX 9060 XT
        assertThat(ChatService.psuTrusted("Infinity ECO 750W – DC to DC – Hiệu suất 85% – Active PFC – Single Rail")).isFalse();
        assertThat(ChatService.psuTrusted("Power supply Jetek MaxWatt 550 V2 (550W)")).isFalse();
        assertThat(ChatService.psuTrusted("Power supply AIGO VK450 - 450W (Black)")).isFalse();
        assertThat(ChatService.psuTrusted("Power supply Aigo GB550 - 550W (80 Plus Bronze)")).isTrue();
        assertThat(ChatService.psuTrusted("Power supply VSP VGP650BRU PRO (650W / 80 Plus Bronze)")).isTrue();
        assertThat(ChatService.psuTrusted("Power supply Corsair RM750e White ATX 3.1 750W Cybenetics Gold (CP-9020")).isTrue();
        assertThat(ChatService.psuTrusted("Power supply MSI MAG A650BN 650W Official giá rẻ")).isTrue();
        assertThat(ChatService.psuTrusted("DeepCool PF450D - 450W 80 Plus Power Supply")).isTrue();
    }

    @Test
    void doHoaTichHopTheoMaSanPham() {
        assertThat(ProductRules.hasIgpu("Bộ vi xử lý Intel Core i3-12100")).isTrue();
        assertThat(ProductRules.hasIgpu("Bộ vi xử lý Intel Core i3 14100F")).isFalse();
        assertThat(ProductRules.hasIgpu("CPU Intel Core Ultra 5 225F")).isFalse();
        assertThat(ProductRules.hasIgpu("CPU Intel Core Ultra 7 265K")).isTrue();
        assertThat(ProductRules.hasIgpu("AMD Ryzen 5 5600G")).isTrue();
        assertThat(ProductRules.hasIgpu("AMD Ryzen 5 5600X")).isFalse();
        assertThat(ProductRules.hasIgpu("AMD Ryzen 5 7600")).isTrue();
        assertThat(ProductRules.hasIgpu("AMD Ryzen 5 7500F")).isFalse();
        assertThat(ProductRules.hasIgpu("CPU")).isNull();
    }
}
