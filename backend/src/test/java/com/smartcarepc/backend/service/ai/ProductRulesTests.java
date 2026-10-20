package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ProductRulesTests {

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
}
