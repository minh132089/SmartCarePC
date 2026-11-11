package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.ChatRequest.Message;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;
import com.smartcarepc.backend.dto.UpgradeInfo;
import com.smartcarepc.backend.service.price.Offer;

import tools.jackson.databind.json.JsonMapper;

/** Tư vấn nâng cấp: app tự kiểm giá, còn hàng, ngân sách và lắp được với máy cũ; AI giả lập bằng mock. */
class UpgradeServiceTests {

    private final LlmClient client = mock(LlmClient.class);
    private final ProductSearch search = mock(ProductSearch.class);
    private final List<Map<String, Object>> sent = new ArrayList<>();
    private UpgradeService service;

    private static final ProductSearch.Product RTX4070 = product("VGA ASUS Dual GeForce RTX 4070 12GB GDDR6X", 15_490_000L);
    private static final ProductSearch.Product RTX4060 = product("VGA GIGABYTE GeForce RTX 4060 WINDFORCE OC 8GB", 7_990_000L);
    private static final ProductSearch.Product PSU650 = product("Nguồn MSI MAG A650BN 650W 80 Plus Bronze", 1_390_000L);
    private static final ProductSearch.Product DDR5 = product("RAM Kingston Fury Beast 16GB DDR5 5600", 1_590_000L);
    private static final ProductSearch.Product DDR4 = product("RAM Kingston Fury Beast 16GB (2x8GB) DDR4 3200", 1_150_000L);

    @BeforeEach
    void setUp() {
        when(search.enabled()).thenReturn(true);
        when(search.pageChecked(any())).thenReturn(true);
        for (var p : List.of(RTX4070, RTX4060, PSU650, DDR5, DDR4)) {
            when(search.offer(p.url())).thenReturn(new Offer(p.title(), p.shop(), p.priceVnd(), null, p.url(), null, true, 36, Instant.now()));
        }
        when(search.search(any())).thenReturn(List.of(RTX4070, RTX4060, PSU650, DDR5, DDR4));
        service = new UpgradeService(client, search, JsonMapper.builder().build(), "test-model", 512, false);
    }

    private static UpgradeInfo pc(String psu, long budget) {
        return new UpgradeInfo("Intel Core i5-10400", "ASUS PRIME B460M-A", "8GB DDR4 2666", "none", "HDD 1TB", psu,
                "standard", "Gaming: Valorant and GTA V at Full HD", budget);
    }

    private ChatRequest ask(UpgradeInfo info) {
        return new ChatRequest(List.of(new Message("user", "Please suggest an upgrade for my PC.")), null, null, "upgrade", info);
    }

    @Test
    void cardMoiCanNguonDuCongSuatVaTongKhongVuotNganSach() {
        reply(toolCall("s", "search_products", "{\"query\":\"RTX 4070\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-1\"}]}"),
                toolCall("p2", "propose_upgrade", """
                        {"summary":"Smoother Full HD gaming","items":[{"category":"gpu","product_id":"s1-1"},{"category":"psu","product_id":"s1-3"}]}"""),
                answer("Upgrade the graphics card and power supply."));
        var res = service.reply(ask(pc("450W", 18_000_000L)), s -> { });

        assertThat(toolResult(2)).contains("450W power supply is below the 650W");
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).extracting(l -> l.category()).containsExactly("gpu", "psu");
        assertThat(res.build().items()).allSatisfy(l -> {
            assertThat(l.sourceUrl()).startsWith("https://");
            assertThat(l.priceChecked()).isTrue();
        });
        assertThat(res.build().budgetVnd()).isEqualTo(18_000_000L);
        assertThat(res.build().unknowns()).extracting(Issue::rule).contains("GPU_CLEARANCE");
        assertThat(res.reply()).isEqualTo("Upgrade the graphics card and power supply.");
    }

    @Test
    void vuotNganSachThiBaoLoiDeChonLai() {
        reply(toolCall("s", "search_products", "{\"query\":\"RTX\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-1\"},{\"category\":\"psu\",\"product_id\":\"s1-3\"}]}"),
                toolCall("p2", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-2\"}]}"),
                answer("An RTX 4060 fits your budget."));
        var res = service.reply(ask(pc("550W", 9_000_000L)), s -> { });

        assertThat(toolResult(2)).contains("over the budget");
        assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.productName()).contains("RTX 4060"));
    }

    @Test
    void ramKhacLoaiDdrVoiBoMachBiTuChoi() {
        reply(toolCall("s", "search_products", "{\"query\":\"RAM\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"ram\",\"product_id\":\"s1-4\"}]}"),
                toolCall("p2", "propose_upgrade", "{\"items\":[{\"category\":\"ram\",\"product_id\":\"s1-5\"}]}"),
                answer("Replace the RAM with a 16 GB DDR4 kit."));
        var res = service.reply(ask(pc("550W", 3_000_000L)), s -> { });

        assertThat(toolResult(2)).contains("DDR4").contains("DDR5");
        assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.productName()).contains("DDR4"));
    }

    @Test
    void giaChuaDocTrenTrangCuaHangKhongDuocChot() {
        when(search.pageChecked(RTX4060.url())).thenReturn(false);
        reply(toolCall("s", "search_products", "{\"query\":\"RTX 4060\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-2\"}]}"),
                answer("Here is the plan."), answer("Here is the plan."));
        var res = service.reply(ask(pc("550W", 9_000_000L)), s -> { });

        assertThat(toolResult(2)).contains("could not be confirmed on the store page");
        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("could not confirm an upgrade", "store prices or stock could not be confirmed");
    }

    @Test
    void khongGoiCongCuThiKhongTuBaoGia() {
        reply(answer("Buy an RTX 4060 for about 8 million."), answer("Buy an RTX 4060 for about 8 million."));
        var res = service.reply(ask(pc("550W", 9_000_000L)), s -> { });

        assertThat(res.build()).isNull();
        assertThat(res.reply()).doesNotContain("8 million");
    }

    @Test
    void cpuMoiMaKhongBietBoMachThiGhiCanKiemTra() {
        var cpu = product("CPU Intel Core i5-12400F", 2_990_000L);
        var offer = new Offer(cpu.title(), "GEARVN", cpu.priceVnd(), null, cpu.url(), null, true, 36, Instant.now());
        List<Issue> warnings = new ArrayList<>(), unknowns = new ArrayList<>();
        var info = new UpgradeInfo("Intel Core i3-12100", "", "", "", "", "", "unknown", "Office work", 5_000_000L);
        var errors = UpgradeService.fit(info, List.of(new UpgradeService.Pick("cpu", "replace", cpu, offer, 1, null, null)), warnings, unknowns);

        assertThat(errors).isEmpty();
        assertThat(unknowns).extracting(Issue::rule).contains("CPU_BOARD_UNKNOWN");
        assertThat(unknowns.stream().filter(i -> i.rule().equals("CPU_BOARD_UNKNOWN")).findFirst().orElseThrow().message()).contains("BIOS");
    }

    @Test
    void boKhongRoThiSocketTheoCpuDangDungVaKhongRoCaHaiThiKhongDeXuat() {
        var cpu = product("CPU Intel Core i5-12400F", 2_990_000L);
        var pick = List.of(pick("cpu", "replace", cpu));
        var oldCpu = new UpgradeInfo("Intel Core i3-10100", "", "", "", "", "", "unknown", "Office work", 5_000_000L);
        assertThat(UpgradeService.fit(oldCpu, pick, new ArrayList<>(), new ArrayList<>())).singleElement().asString()
                .contains("LGA1700", "LGA1200");
        var nothing = new UpgradeInfo("", "", "", "", "", "", "unknown", "Office work", 5_000_000L);
        assertThat(UpgradeService.fit(nothing, pick, new ArrayList<>(), new ArrayList<>())).singleElement().asString().contains("cannot be matched");
    }

    @Test
    void khongBietLoaiRamThiKhongDeXuatRamVaMayDdr3KhongNhanDdr4() {
        var nothing = new UpgradeInfo("", "", "", "", "", "", "unknown", "Video editing", 8_000_000L);
        assertThat(UpgradeService.fit(nothing, List.of(pick("ram", "add", DDR4)), new ArrayList<>(), new ArrayList<>()))
                .singleElement().asString().contains("RAM type", "unknown");
        var ddr3 = new UpgradeInfo("Intel Core i5-4460", "ASUS H81M-K", "8GB DDR3", "GTX 750 Ti", "", "400W", "standard", "Gaming", 6_000_000L);
        assertThat(UpgradeService.fit(ddr3, List.of(pick("ram", "replace", DDR4)), new ArrayList<>(), new ArrayList<>()))
                .singleElement().asString().contains("DDR3", "DDR4");
        // Bo AM4 không ghi DDR: suy theo CPU
        var am4 = new UpgradeInfo("AMD Ryzen 5 5600", "", "", "", "", "", "unknown", "Programming", 3_000_000L);
        assertThat(UpgradeService.ramType(am4)).isEqualTo("DDR4");
    }

    @Test
    void nguonDangCoDuThiKhongMuaThemVaNguonSfxPhaiGiuSfx() {
        var info = new UpgradeInfo("Intel Core i5-12400", "ASUS ROG STRIX B660-I GAMING WIFI", "16GB DDR4", "none", "", "SFX 450W", "mini",
                "Gaming", 10_000_000L);
        var errors = UpgradeService.fit(info, List.of(pick("gpu", "replace", RTX4060), pick("psu", "replace", PSU650)), new ArrayList<>(), new ArrayList<>());
        assertThat(errors).anySatisfy(e -> assertThat(e).contains("already covers"));
        assertThat(errors).anySatisfy(e -> assertThat(e).contains("SFX"));
        assertThat(UpgradeService.fit(info, List.of(pick("gpu", "replace", RTX4060)), new ArrayList<>(), new ArrayList<>())).isEmpty();
        assertThat(UpgradeService.power(info, List.of(pick("gpu", "replace", RTX4060)))).contains("current 450W", "already covers");
    }

    @Test
    void congSuatCardTheoDungMucCuaCardChoMayCu() {
        assertThat(UpgradeService.gpuNeed("VGA ASUS Dual GeForce RTX 3050 6GB GDDR6")).isEqualTo(300);
        assertThat(UpgradeService.gpuNeed("MSI GeForce RTX 3050 VENTUS 2X 8G")).isEqualTo(450);
        assertThat(UpgradeService.gpuNeed("GIGABYTE GeForce GTX 1650 D6 4G")).isEqualTo(300);
        assertThat(UpgradeService.gpuNeed("GT 1030 2GB")).isEqualTo(300);
        assertThat(UpgradeService.gpuNeed("ASUS Dual RTX 4070 12GB")).isEqualTo(650);
        var ddr3 = new UpgradeInfo("Intel Core i5-4460", "ASUS H81M-K", "8GB DDR3", "GTX 750 Ti", "", "400W", "standard", "Gaming", 6_000_000L);
        assertThat(UpgradeService.fit(ddr3, List.of(pick("gpu", "replace", product("VGA ASUS Dual GeForce RTX 3050 6GB GDDR6", 4_700_000L))),
                new ArrayList<>(), new ArrayList<>())).isEmpty();
    }

    @Test
    void aiCanCardNvidiaTu12GbVram() {
        var info = new UpgradeInfo("AMD Ryzen 5 5600", "MSI B550M PRO-VDH", "16GB DDR4", "GTX 1660 Super", "", "650W", "standard",
                "AI and machine learning. Stable Diffusion", 15_000_000L);
        assertThat(UpgradeService.fit(info, List.of(pick("gpu", "replace", RTX4060)), new ArrayList<>(), new ArrayList<>()))
                .singleElement().asString().contains("12 GB");
        assertThat(UpgradeService.fit(info, List.of(pick("gpu", "replace", product("VGA MSI GeForce RTX 3060 VENTUS 2X 12G OC", 7_800_000L))),
                new ArrayList<>(), new ArrayList<>())).isEmpty();
    }

    @Test
    void khongChotDuocThiNoiDungLyDo() {
        var turn = new UpgradeService.Turn(pc("550W", 6_000_000L));
        turn.lastErrors = List.of("The total 9.08 million VND is over the budget 6 million VND by 3.08 million VND.");
        assertThat(UpgradeService.noPlan(turn)).contains("cost more than your 6 million VND budget").doesNotContain("9.08");
    }

    @Test
    void boDoiCuKhongDocDuocSocketThiKhongDeXuatCpu() {
        var ddr3 = new UpgradeInfo("Intel Core i5-4460", "ASUS H81M-K", "8GB DDR3", "GTX 750 Ti", "", "400W", "standard", "Gaming", 6_000_000L);
        assertThat(UpgradeService.fit(ddr3, List.of(pick("cpu", "replace", product("CPU Intel Core i5-12400", 3_500_000L))),
                new ArrayList<>(), new ArrayList<>())).singleElement().asString().contains("could not be identified");
    }

    @Test
    void cpuKhongCoDoHoaTichHopMaMayKhongCoCardThiChan() {
        var info = new UpgradeInfo("Intel Core i3-12100", "ASUS PRIME H610M-K D4", "16GB DDR4", "none", "", "450W", "standard", "Office work", 5_000_000L);
        assertThat(UpgradeService.fit(info, List.of(pick("cpu", "replace", product("CPU Intel Core i5-12400F", 2_990_000L))),
                new ArrayList<>(), new ArrayList<>())).singleElement().asString().contains("no built-in graphics");
        assertThat(UpgradeService.fit(info, List.of(pick("cpu", "replace", product("CPU Intel Core i5-12400", 3_490_000L))),
                new ArrayList<>(), new ArrayList<>())).isEmpty();
    }

    @Test
    void nguonMoiPhaiDuChoCardDangGiu() {
        var info = new UpgradeInfo("AMD Ryzen 7 5700X", "MSI B550M PRO-VDH", "32GB DDR4", "RTX 4070 12GB", "", "450W", "standard", "Gaming", 3_000_000L);
        var psu500 = product("Nguồn Xigmatek X-Power III 500W", 900_000L);
        assertThat(UpgradeService.fit(info, List.of(pick("psu", "replace", psu500)), new ArrayList<>(), new ArrayList<>()))
                .singleElement().asString().contains("below the 650W");
        assertThat(UpgradeService.fit(info, List.of(pick("psu", "replace", PSU650)), new ArrayList<>(), new ArrayList<>())).isEmpty();
    }

    @Test
    void keHoachRongVanKhongDuocBiaGia() {
        reply(toolCall("s", "search_products", "{\"query\":\"RTX\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[]}"),
                answer("Nothing useful fits. An RTX 4060 costs 8.5 million VND, so save up."));
        var res = service.reply(ask(pc("450W", 500_000L)), s -> { });
        assertThat(res.build()).isNull();
        assertThat(res.reply()).doesNotContain("8.5 million").contains("Nothing useful fits.");
    }

    @Test
    void soQuaDaiKhongLamHongLuot() {
        assertThat(UpgradeService.amounts("VND 99999999999999999999")).containsExactly(Long.MAX_VALUE);
        var turn = new UpgradeService.Turn(pc("450W", 10_000_000L));
        assertThat(UpgradeService.checkedReply("You wrote VND 99999999999999999999. Keep your PC.", turn)).isEqualTo("Keep your PC.");
    }

    @Test
    void doiVoMoiThiKhongBatNguonSfxCuaVoCu() {
        var info = new UpgradeInfo("Intel Core i5-12400", "ASUS ROG STRIX B660-I GAMING WIFI", "16GB DDR4", "RTX 4070 12GB", "", "SFX 450W", "mini",
                "Gaming", 10_000_000L);
        var atxCase = product("Vỏ case Xigmatek Gaming X ATX Mid Tower", 900_000L);
        assertThat(UpgradeService.fit(info, List.of(pick("case", "replace", atxCase), pick("psu", "replace", PSU650)), new ArrayList<>(), new ArrayList<>()))
                .isEmpty();
        assertThat(UpgradeService.fit(info, List.of(pick("psu", "replace", PSU650)), new ArrayList<>(), new ArrayList<>()))
                .anySatisfy(e -> assertThat(e).contains("SFX"));
    }

    @Test
    void cauTraLoiCuoiKhongDuocCoSoTienHayLink() {
        var turn = new UpgradeService.Turn(pc("450W", 10_000_000L));
        turn.plan = new UpgradeService.Plan("Gaming", List.of(pick("gpu", "replace", RTX4060)), List.of(), List.of());
        String reply = UpgradeService.checkedReply("""
                Gaming on 10 million VND. An RTX 4060 costs 10 million VND. Buy the RTX 4060.
                See [the store](https://shop.example/x) or www.shop.example/y for details. It leaves 1.190.000đ spare.
                Check your B450M DS3H board has 2 M.2 slots.""", turn);
        assertThat(reply).contains("Buy the RTX 4060.", "See the store or  for details.", "B450M DS3H board has 2 M.2 slots.")
                .doesNotContain("million", "https://", "www.", "1.190.000");
        assertThat(UpgradeService.amounts("VND 500,000 or 1.190.000đ, and 8.45M")).containsExactly(8_450_000L, 500_000L, 1_190_000L);
        assertThat(UpgradeService.amounts("Your B450M DS3H, a B760M-A or H610M board, 2 M.2 slots")).isEmpty();
        assertThat(UpgradeService.checkedReply("Budget 10 million VND.", turn)).isNull();
    }

    @Test
    void voMoiNhoChiNhanNguonSfx() {
        var info = new UpgradeInfo("Intel Core i5-12400", "ASUS ROG STRIX B660-I GAMING WIFI", "16GB DDR4", "RTX 4070 12GB", "", "SFX 450W", "mini",
                "Gaming", 10_000_000L);
        var itxCase = product("Vỏ case Jonsbo A4 Mini ITX SFF", 2_000_000L);
        assertThat(UpgradeService.fit(info, List.of(pick("case", "replace", itxCase), pick("psu", "replace", PSU650)), new ArrayList<>(), new ArrayList<>()))
                .anySatisfy(e -> assertThat(e).contains("SFX"));
    }

    @Test
    void khongBietBoMachThiChiNhanSsdSata() {
        var nothing = new UpgradeInfo("", "", "", "", "", "", "unknown", "Video editing", 8_000_000L);
        var nvme = product("SSD Samsung 970 EVO Plus 1TB PCIe NVMe M.2 2280", 1_950_000L);
        var sata = product("SSD SSTC M100 512GB SATA III 2.5 inch", 1_790_000L);
        assertThat(UpgradeService.fit(nothing, List.of(pick("ssd", "add", nvme)), new ArrayList<>(), new ArrayList<>()))
                .singleElement().asString().contains("2.5-inch SATA");
        assertThat(UpgradeService.fit(nothing, List.of(pick("ssd", "add", sata)), new ArrayList<>(), new ArrayList<>())).isEmpty();
        var turn = new UpgradeService.Turn(nothing);
        assertThat(UpgradeService.checkedReply("Plan accepted. Add a SATA SSD.", turn)).isEqualTo("Add a SATA SSD.");
        assertThat(UpgradeService.checkedReply("Your gaming upgrade is accepted. Add a SATA SSD.", turn)).isEqualTo("Add a SATA SSD.");
    }

    @Test
    void soTienKhongDonViVanBiBo() {
        var turn = new UpgradeService.Turn(pc("450W", 10_000_000L));
        assertThat(UpgradeService.checkedReply("RTX 4060 costs 10,000,000. The card is priced at 8.45. Keep your DDR4 3200 RAM, "
                + "it runs at 3200MHz with 16GB at 1080p and 240Hz. Buy it.", turn))
                .isEqualTo("Keep your DDR4 3200 RAM, it runs at 3200MHz with 16GB at 1080p and 240Hz. Buy it.");
        assertThat(UpgradeService.checkedReply("Keep your i5-12400 and Core i5 13400 with a 120mm fan. It costs 8450000.", turn))
                .isEqualTo("Keep your i5-12400 and Core i5 13400 with a 120mm fan.");
    }

    @Test
    void tenVoGhiSfxHoacSffThiCanNguonSfx() {
        var info = new UpgradeInfo("Intel Core i5-12400", "MSI PRO B760M-A DDR4", "16GB DDR4", "RTX 4060", "", "SFX 450W", "mini", "Gaming", 10_000_000L);
        for (String name : List.of("Vỏ case SFF Micro ATX, SFX PSU only", "Case Mini ITX nhôm")) {
            assertThat(UpgradeService.fit(info, List.of(pick("case", "replace", product(name, 1_500_000L)), pick("psu", "replace", PSU650)),
                    new ArrayList<>(), new ArrayList<>())).as(name).anySatisfy(e -> assertThat(e).contains("SFX"));
        }
        for (String name : List.of("Vỏ case Xigmatek NYX Air mATX Mini Tower", "Case Mini ITX hỗ trợ ATX PSU")) {
            assertThat(UpgradeService.fit(info, List.of(pick("case", "replace", product(name, 900_000L)), pick("psu", "replace", PSU650)),
                    new ArrayList<>(), new ArrayList<>())).as(name).noneSatisfy(e -> assertThat(e).contains("SFX"));
        }
    }

    @Test
    void thayCpuHayCardNgangHangThiKhongDeXuat() {
        var amd = new UpgradeInfo("AMD Ryzen 5 3600", "Gigabyte B450M DS3H", "16GB DDR4", "RTX 3060 12GB", "", "650W", "standard",
                "Gaming. CS2 at 240Hz", 5_000_000L);
        assertThat(UpgradeService.fit(amd, List.of(pick("cpu", "replace", product("CPU AMD Ryzen 5 5500", 2_490_000L))), new ArrayList<>(), new ArrayList<>()))
                .singleElement().asString().contains("barely faster");
        assertThat(UpgradeService.fit(amd, List.of(pick("cpu", "replace", product("CPU AMD Ryzen 7 5700X3D", 4_990_000L))), new ArrayList<>(), new ArrayList<>()))
                .isEmpty();
        var game = pc("650W", 12_000_000L);
        var oldCard = new UpgradeInfo(game.cpu(), game.motherboard(), game.ram(), "RTX 3060 12GB", game.storage(), "650W", "standard", "Gaming", 12_000_000L);
        assertThat(UpgradeService.fit(oldCard, List.of(pick("gpu", "replace", RTX4060)), new ArrayList<>(), new ArrayList<>()))
                .singleElement().asString().contains("not clearly faster");
        assertThat(UpgradeService.fit(oldCard, List.of(pick("gpu", "replace", RTX4070)), new ArrayList<>(), new ArrayList<>())).isEmpty();
        assertThat(UpgradeService.cpuScore("CPU Intel Core i5-12400F")).isEqualTo(120);
        assertThat(UpgradeService.gpuRank("GTX 750 Ti 2GB")).isEqualTo(4);
    }

    @Test
    void laptopThiKhongTuVan() {
        var laptop = new UpgradeInfo("Intel Core i5-1135G7", "", "8GB", "Intel Iris Xe", "", "", "unknown", "Study. It is slow", 4_000_000L);
        var res = service.reply(ask(laptop), s -> { });
        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("desktop PCs");
        assertThat(UpgradeService.laptop(new UpgradeInfo("Ryzen 5 5500U", "", "", "", "", "", null, "Office", 2_000_000L))).isTrue();
        assertThat(UpgradeService.laptop(new UpgradeInfo("", "", "", "", "", "", null, "Học bài trên máy tính xách tay", 2_000_000L))).isTrue();
        assertThat(UpgradeService.laptop(pc("450W", 2_000_000L))).isFalse();
    }

    @Test
    void hoiReHonThiKeHoachMoiPhaiReHon() {
        reply(toolCall("s", "search_products", "{\"query\":\"RTX\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-1\"},{\"category\":\"psu\",\"product_id\":\"s1-3\"}]}"),
                toolCall("p2", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-2\"}]}"),
                answer("The RTX 4060 is the cheaper pick."));
        var history = List.of(new Message("user", "Please suggest an upgrade for my PC."),
                new Message("assistant", "Buy an RTX 4060.\n\n[Previous upgrade plan: gpu: VGA GIGABYTE GeForce RTX 4060 WINDFORCE OC 8GB (8450000 VND)]"),
                new Message("user", "Is there a cheaper graphics card?"));
        var res = service.reply(new ChatRequest(history, null, null, "upgrade", pc("450W", 20_000_000L)), s -> { });
        assertThat(toolResult(2)).contains("cheaper option");
        assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.priceMaxVnd()).isLessThan(8_450_000L));
    }

    @Test
    void lanDeXuatCuoiBoMonChuaKiemTraDuocGia() {
        var hdd = new UpgradeInfo("Intel Core i5-9400F", "ASUS PRIME B365M-K", "8GB DDR4", "GTX 1050 2GB", "HDD 1TB", "350W", "standard",
                "Gaming. League of Legends", 5_000_000L);
        var sata = product("SSD SSTC M100 512GB SATA III 2.5 inch", 1_790_000L);
        when(search.offer(sata.url())).thenReturn(new Offer(sata.title(), sata.shop(), sata.priceVnd(), null, sata.url(), null, true, 36, Instant.now()));
        when(search.pageChecked(RTX4060.url())).thenReturn(false);
        when(search.search(any())).thenReturn(List.of(RTX4060, sata));
        String both = "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-1\"},{\"category\":\"ssd\",\"product_id\":\"s1-2\"}]}";
        reply(toolCall("s", "search_products", "{\"query\":\"x\"}"), toolCall("p1", "propose_upgrade", both), toolCall("p2", "propose_upgrade", both),
                toolCall("p3", "propose_upgrade", both), toolCall("p4", "propose_upgrade", both), answer("Add the SSD now."));
        var res = service.reply(ask(hdd), s -> { });
        assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.category()).isEqualTo("ssd"));
        assertThat(res.build().unknowns()).extracting(Issue::rule).contains("DROPPED");
    }

    @Test
    void thongSoRamGhiDungSoThanh() {
        var stick = product("RAM PC Kingmax Blade X 8GB DDR4 3200MHz", 1_430_000L);
        var p = new UpgradeService.Pick("ram", "add", stick, new Offer(stick.title(), "GEARVN", stick.priceVnd(), null, stick.url(), null, true, 36, Instant.now()),
                2, "8 GB DDR4 3200, single stick", null);
        assertThat(UpgradeService.spec(p)).isEqualTo("2 × 8 GB DDR4 (16 GB)");
    }

    @Test
    void chiChoiGameDaCo16GbThiKhongMuaThemRam() {
        var ram16 = product("Ram SSTC 16GB DDR4 3200MHz", 2_990_000L);
        var game = new UpgradeInfo("Intel Core i5-10400F", "ASUS PRIME B460M-A", "16GB DDR4", "GTX 1650", "", "450W", "standard",
                "Gaming. Cyberpunk 2077", 12_000_000L);
        assertThat(UpgradeService.fit(game, List.of(pick("ram", "add", ram16)), new ArrayList<>(), new ArrayList<>()))
                .anySatisfy(e -> assertThat(e).contains("enough for gaming"));
        var stream = new UpgradeInfo("Intel Core i5-10400F", "ASUS PRIME B460M-A", "16GB DDR4", "GTX 1650", "", "450W", "standard",
                "Gaming and streaming on Twitch", 12_000_000L);
        assertThat(UpgradeService.fit(stream, List.of(pick("ram", "add", ram16)), new ArrayList<>(), new ArrayList<>()))
                .noneSatisfy(e -> assertThat(e).contains("enough for gaming"));
        // Đổi sang nền tảng DDR5 (CPU + bo + RAM): RAM mới là bắt buộc, không bị chặn
        var am5 = List.of(pick("cpu", "replace", product("CPU AMD Ryzen 5 7600", 4_990_000L)),
                pick("motherboard", "replace", product("Mainboard ASUS TUF GAMING B650M-PLUS", 4_590_000L)),
                pick("ram", "replace", DDR5));
        assertThat(UpgradeService.fit(game, am5, new ArrayList<>(), new ArrayList<>())).noneSatisfy(e -> assertThat(e).contains("enough for gaming"));
    }

    private static UpgradeService.Pick pick(String category, String action, ProductSearch.Product p) {
        return new UpgradeService.Pick(category, action, p, new Offer(p.title(), p.shop(), p.priceVnd(), null, p.url(), null, true, 36, Instant.now()),
                1, null, null);
    }

    @Test
    void cpuKhacSocketVoiBoHienTaiLaLoi() {
        var cpu = product("CPU Intel Core i5-12400F", 2_990_000L);
        var offer = new Offer(cpu.title(), "GEARVN", cpu.priceVnd(), null, cpu.url(), null, true, 36, Instant.now());
        var errors = UpgradeService.fit(pc("550W", 5_000_000L), List.of(new UpgradeService.Pick("cpu", "replace", cpu, offer, 1, null, null)),
                new ArrayList<>(), new ArrayList<>());

        assertThat(errors).anySatisfy(e -> assertThat(e).contains("LGA1700", "LGA1200"));
    }

    @Test
    void docCongSuatNguonNguoiDungGhi() {
        assertThat(UpgradeService.watts("Cooler Master 650 W")).isEqualTo(650);
        assertThat(UpgradeService.watts("550W bronze")).isEqualTo(550);
        assertThat(UpgradeService.watts("I don't know")).isNull();
        assertThat(UpgradeService.watts("")).isNull();
    }

    @Test
    void moTaMayChoTroLyGhiRoOChuaBiet() {
        String text = UpgradeService.describe(new UpgradeInfo("", "B460M", null, "none", "", "", null, "Study", 2_000_000L));
        assertThat(text).contains("- CPU: unknown", "none (uses the CPU's built-in graphics)", "socket LGA1200", "UPGRADE BUDGET");
    }

    // ---- Phản hồi giả theo chuẩn OpenAI chat/completions ---------------

    private static ProductSearch.Product product(String title, long price) {
        return new ProductSearch.Product(title, "GEARVN", price, "https://gearvn.com/products/" + Math.abs(title.hashCode()), null);
    }

    private static Map<String, Object> answer(String text) {
        return Map.of("choices", List.of(Map.of("finish_reason", "stop", "message", Map.of("role", "assistant", "content", text))));
    }

    private static Map<String, Object> toolCall(String id, String name, String arguments) {
        return Map.of("choices", List.of(Map.of("finish_reason", "tool_calls", "message", Map.of("role", "assistant", "content", "",
                "tool_calls", List.of(Map.of("id", id, "type", "function", "function", Map.of("name", name, "arguments", arguments)))))));
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private void reply(Map<String, Object>... responses) {
        var queue = new ArrayList<>(List.of(responses));
        when(client.complete(any())).thenAnswer(inv -> {
            Map<String, Object> body = inv.getArgument(0);
            var copy = new LinkedHashMap<>(body);
            copy.put("messages", new ArrayList<>((List<Object>) body.get("messages")));
            sent.add(copy);
            return queue.removeFirst();
        });
    }

    /** Nội dung kết quả công cụ cuối cùng mà AI nhận được ở lần gọi thứ call */
    @SuppressWarnings("unchecked")
    private String toolResult(int call) {
        var messages = (List<Map<String, Object>>) sent.get(call).get("messages");
        return String.valueOf(messages.getLast().get("content"));
    }
}
