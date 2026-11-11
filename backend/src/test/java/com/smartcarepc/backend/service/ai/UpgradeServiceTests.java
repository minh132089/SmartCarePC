package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.ChatRequest.Message;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;
import com.smartcarepc.backend.dto.UpgradeInfo;
import com.smartcarepc.backend.exception.AiUnavailableException;
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

    private ChatRequest followUp(long formBudget, String... questions) {
        var history = new ArrayList<Message>();
        history.add(new Message("user", "Upgrade my PC"));
        for (String question : questions) {
            history.add(new Message("assistant", "Here is an option."));
            history.add(new Message("user", question));
        }
        return new ChatRequest(history, null, null, "upgrade", pc("650W", formBudget));
    }

    @Test
    void followUpLowerBudgetOverridesTheUnchangedForm() {
        reply(toolCall("s", "search_products", "{\"query\":\"RTX\"}"),
                toolCall("p", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-2\"}]}"),
                answer("Buy the RTX card."), answer("Buy the RTX card."));
        var res = service.reply(followUp(12_000_000L, "My total budget is now 3 million VND."), s -> { });
        assertThat(toolResult(2)).contains("over the budget");
        assertThat(res.build()).isNull();
    }

    @Test
    void newestExplicitBudgetPersistsAcrossUnrelatedFollowUps() {
        reply(toolCall("s", "search_products", "{\"query\":\"RTX\"}"),
                toolCall("p", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-2\"}]}"),
                answer("Use this card."));
        var res = service.reply(followUp(3_000_000L, "Ngân sách còn 2 triệu.",
                "Tăng ngân sách lên 9.000.000 đồng.", "The old card cost 2 million. Keep NVIDIA."), s -> { });
        assertThat(res.build()).isNotNull();
        assertThat(res.build().budgetVnd()).isEqualTo(9_000_000L);
    }

    @Test
    void budgetParserDoesNotTreatQuotedPricesOrNegatedChangesAsANewTotal() {
        assertThat(UpgradeService.followUpBudget(followUp(12_000_000L,
                "The GPU costs 3 million; my PSU budget is 1 million.").messages())).isNull();
        assertThat(UpgradeService.followUpBudget(followUp(12_000_000L,
                "Don't change my budget to 3 million.").messages())).isNull();
        assertThat(UpgradeService.followUpBudget(followUp(12_000_000L,
                "Total budget: VND 300,000").messages())).isEqualTo(300_000L);
        assertThat(UpgradeService.followUpBudget(followUp(12_000_000L,
                "Ngân sách còn 0,1 triệu.").messages())).isEqualTo(100_000L);
    }

    @Test
    void followUpMemoryTargetCannotBeReplacedByASmallerKit() {
        reply(toolCall("s", "search_products", "{\"query\":\"RAM DDR4 32GB\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"ram\",\"product_id\":\"s1-5\"}]}"),
                toolCall("p2", "propose_upgrade", "{\"items\":[{\"category\":\"ram\",\"product_id\":\"s1-5\",\"buy_qty\":2}]}"),
                answer("Use these kits."));
        var res = service.reply(followUp(12_000_000L, "Upgrade RAM to 32GB, keep the other parts."), s -> { });
        assertThat(toolResult(2)).contains("requested 32 GB");
        assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.quantity()).isEqualTo(4));
        assertThat(res.reply()).contains("32 GB");
    }

    @Test
    void explicitMemoryTargetOverridesTheDefaultGamingSixteenGbRule() {
        reply(toolCall("s", "search_products", "{\"query\":\"RAM DDR4 32GB\"}"),
                toolCall("p", "propose_upgrade", "{\"items\":[{\"category\":\"ram\",\"product_id\":\"s1-5\",\"buy_qty\":2}]}"),
                answer("Use these kits."), answer("Use these kits."));
        var request = followUp(12_000_000L, "Upgrade RAM to 32GB, keep the other parts.");
        var old = request.upgrade();
        var pc = new UpgradeInfo(old.cpu(), old.motherboard(), "16GB DDR4, 2 sticks", old.gpu(), old.storage(), old.psu(),
                old.caseSize(), old.purpose(), old.budgetVnd());
        var res = service.reply(new ChatRequest(request.messages(), null, null, "upgrade", pc), s -> { });
        assertThat(res.build()).isNotNull();
        assertThat(res.reply()).contains("32 GB");
    }

    @Test
    void exactRamTargetDoesNotBuyFortyEightGbOrRepeatTheModelsWrongCount() {
        reply(toolCall("s", "search_products", "{\"query\":\"RAM DDR4\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"ram\",\"action\":\"add\",\"product_id\":\"s1-5\",\"buy_qty\":2}]}"),
                toolCall("p2", "propose_upgrade", "{\"summary\":\"48GB upgrade\",\"items\":[{\"category\":\"ram\",\"action\":\"add\",\"product_id\":\"s1-5\",\"reason\":\"48GB after upgrading\"}]}"),
                answer("Upgrade to 48GB."));
        var request = followUp(12_000_000L, "Upgrade RAM to 32GB only.");
        var old = request.upgrade();
        var pc = new UpgradeInfo(old.cpu(), old.motherboard(), "16GB DDR4, 2 sticks", old.gpu(), old.storage(), old.psu(),
                old.caseSize(), old.purpose(), old.budgetVnd());
        var res = service.reply(new ChatRequest(request.messages(), null, null, "upgrade", pc), s -> { });
        assertThat(toolResult(2)).contains("requested 32 GB");
        assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.reason()).contains("32 GB").doesNotContain("48"));
        assertThat(res.build().summary()).doesNotContain("48");
        assertThat(res.reply()).contains("32 GB").doesNotContain("48");
    }

    @Test
    void explicitlyMinimumRamTargetStillAllowsMoreCapacity() {
        var pc = new UpgradeInfo("i5-10400", "ASUS PRIME B460M-A", "16GB DDR4, 2 sticks", "none", "SSD SATA 1TB", "650W",
                "standard", "Programming; RAM at least 32GB", 12_000_000L);
        var turn = new UpgradeService.Turn(pc);
        turn.results.put("ram", DDR4);
        var result = service.runTool(Map.of("name", "propose_upgrade", "arguments",
                "{\"items\":[{\"category\":\"ram\",\"action\":\"add\",\"product_id\":\"ram\",\"buy_qty\":2}]}"), turn);
        assertThat(result).contains("\"ok\":true");
    }

    @Test
    void sharedRamTargetKeepsTheNewestRequestAndRejectsOverflow() {
        assertThat(ChatService.requestedRam(List.of("RAM to 16GB", "RAM to 32GB"))).isEqualTo(16);
        assertThat(ChatService.requestedRam(List.of("RTX with 16GB VRAM"))).isNull();
        assertThat(ChatService.requestedRam(List.of("RAM to 99999999999999999999GB"))).isNull();
    }

    @Test
    void providerFailureAfterAcceptanceKeepsTheCheckedPlan() {
        // The provider disconnects while writing, after both successful tool turns.
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var searchReply = toolCall("s", "search_products", "{\"query\":\"RTX\"}");
        var planReply = toolCall("p", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-2\"}]}");
        when(client.complete(any())).thenAnswer(inv -> switch (calls.getAndIncrement()) {
            case 0 -> searchReply;
            case 1 -> planReply;
            default -> throw new AiUnavailableException(HttpStatus.BAD_GATEWAY, "Provider disconnected");
        });
        var res = service.reply(ask(pc("650W", 12_000_000L)), s -> { });
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.productName()).contains("RTX 4060"));
    }

    @Test
    void providerFailureBeforeAcceptanceCannotInventAPurchase() {
        when(client.complete(any())).thenThrow(new AiUnavailableException(HttpStatus.BAD_GATEWAY, "Provider disconnected"));
        assertThatThrownBy(() -> service.reply(ask(pc("650W", 12_000_000L)), s -> { }))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void latestOfferIsUsedBeforeReturningTheCard() {
        for (long changedPrice : List.of(8_200_000L, 13_000_000L)) {
            reply(toolCall("s", "search_products", "{\"query\":\"RTX\"}"),
                    toolCall("p", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-2\"}]}"),
                    answer("Use this card."));
            var initial = new Offer(RTX4060.title(), RTX4060.shop(), RTX4060.priceVnd(), null, RTX4060.url(), null, true, 36, Instant.now());
            var changed = new Offer(RTX4060.title(), RTX4060.shop(), changedPrice, null, RTX4060.url(), null, true, 36, Instant.now());
            when(search.offer(RTX4060.url())).thenReturn(initial);
            var res = service.reply(ask(pc("650W", 12_000_000L)), stage -> {
                if (stage.equals(UpgradeService.STAGE_WRITE)) when(search.offer(RTX4060.url())).thenReturn(changed);
            });
            if (changedPrice > 12_000_000L) assertThat(res.build()).isNull();
            else assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.priceMaxVnd()).isEqualTo(changedPrice));
        }
    }

    @Test
    void stockLostWhileWritingCannotBeSoldAsInStock() {
        reply(toolCall("s", "search_products", "{\"query\":\"RTX\"}"),
                toolCall("p", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-2\"}]}"),
                answer("Use this card."));
        var res = service.reply(ask(pc("650W", 12_000_000L)), stage -> {
            if (stage.equals(UpgradeService.STAGE_WRITE)) when(search.offer(RTX4060.url())).thenReturn(
                    new Offer(RTX4060.title(), RTX4060.shop(), RTX4060.priceVnd(), null, RTX4060.url(), null, false, 36, Instant.now()));
        });
        assertThat(res.build()).isNull();
        assertThat(res.reply()).containsIgnoringCase("stock");
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
        assertThat(res.reply()).doesNotContain("8.5 million").contains("not recommending a purchase");
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
        var kits = new UpgradeService.Pick("ram", "replace", DDR4, pick("ram", "replace", DDR4).offer(), 2, null, null);
        assertThat(UpgradeService.physicalQuantity(kits)).isEqualTo(4);
        assertThat(UpgradeService.physicalQuantity(pick("ram", "replace", DDR4))).isEqualTo(2);
        assertThat(kits.total()).isEqualTo(DDR4.priceVnd() * 2);
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
        // Đổi bo cùng loại DDR4 (B460 -> B560): RAM cũ dùng tiếp được, vẫn chặn mua thêm
        var b560 = List.of(pick("motherboard", "replace", product("Mainboard MSI MAG B560M BAZOOKA DDR4", 2_490_000L)), pick("ram", "add", ram16));
        assertThat(UpgradeService.fit(game, b560, new ArrayList<>(), new ArrayList<>())).anySatisfy(e -> assertThat(e).contains("enough for gaming"));
    }

    @Test
    void coCardManhHonMaKhongDatHonThiBatDoi() {
        var rtx3060 = product("VGA ASUS DUAL RTX 3060 12GB 2 Fan V2", 7_890_000L);
        var rtx4060 = product("VGA ASUS Dual GeForce RTX 4060 EVO 8GB", 7_990_000L);
        for (var p : List.of(rtx3060, rtx4060)) {
            when(search.offer(p.url())).thenReturn(new Offer(p.title(), p.shop(), p.priceVnd(), null, p.url(), null, true, 36, Instant.now()));
        }
        when(search.search(any())).thenReturn(List.of(rtx3060, PSU650, rtx4060));
        reply(toolCall("s", "search_products", "{\"query\":\"RTX\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-1\"},{\"category\":\"psu\",\"product_id\":\"s1-2\"}]}"),
                toolCall("p2", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-3\"}]}"),
                answer("An RTX 4060 runs on your power supply."));
        var res = service.reply(ask(new UpgradeInfo("Intel Core i5-10400F", "ASUS PRIME B460M-A", "16GB DDR4", "GTX 1650 4GB", "SSD 256GB",
                "450W", "standard", "Gaming. GTA V", 12_000_000L)), s -> { });
        assertThat(toolResult(2)).contains("RTX 4060", "faster");
        assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.productName()).contains("RTX 4060"));
    }

    @Test
    void card3QuatKhongVaoVoMiniVaBoCuKhongCoM2() {
        var mini = new UpgradeInfo("Intel Core i5-12400", "ASUS ROG STRIX B660-I GAMING WIFI", "16GB DDR4, 2 sticks", "none", "", "SFX 750W", "mini",
                "Gaming", 20_000_000L);
        var trio = product("VGA MSI GeForce RTX 4070 SUPER GAMING X TRIO 12G", 17_000_000L);
        List<Issue> warn = new ArrayList<>();
        assertThat(UpgradeService.fit(mini, List.of(pick("gpu", "replace", trio)), warn, new ArrayList<>())).isEmpty();
        assertThat(warn).extracting(Issue::rule).contains("GPU_LENGTH");
        List<Issue> lpWarn = new ArrayList<>();
        UpgradeService.fit(mini, List.of(pick("gpu", "replace", product("GIGABYTE RTX 4060 OC Low Profile 8G 3x WINDFORCE fans", 9_000_000L))), lpWarn, new ArrayList<>());
        assertThat(lpWarn).extracting(Issue::rule).doesNotContain("GPU_LENGTH");
        var haswell = new UpgradeInfo("Intel Core i7-4790", "Gigabyte B85M-D3H", "8GB DDR3", "none", "HDD 1TB", "450W", "standard", "Office", 3_000_000L);
        var nvme = product("SSD Samsung 970 EVO Plus 1TB PCIe NVMe M.2 2280", 1_950_000L);
        List<Issue> m2 = new ArrayList<>();
        assertThat(UpgradeService.fit(haswell, List.of(pick("ssd", "add", nvme)), m2, new ArrayList<>())).isEmpty();
        assertThat(m2).extracting(Issue::rule).contains("M2_OLD_BOARD");
        // Bo ITX 2 khe đã đủ 2 thanh: không thêm được thanh nữa
        var stick = product("RAM Kingston Fury Beast 16GB DDR4 3200", 1_590_000L);
        assertThat(UpgradeService.fit(mini, List.of(pick("ram", "add", stick)), new ArrayList<>(), new ArrayList<>()))
                .anySatisfy(e -> assertThat(e).contains("2 RAM slots"));
        assertThat(UpgradeService.sticks("16GB DDR4, 2 sticks")).isEqualTo(2);
        assertThat(UpgradeService.boardSlots("MSI MAG B550M MORTAR")).isNull();
        assertThat(UpgradeService.kitSticks("Kingston Fury 32GB (2x16GB) DDR4")).isEqualTo(2);
        assertThat(UpgradeService.kitSticks("RAM Corsair 32GB (16GB x 2) DDR4")).isEqualTo(2);
        assertThat(UpgradeService.kitSticks("RAM Kingston 32GB kit DDR4")).isNull();
        // Thay bằng kit 4 thanh trên bo ITX 2 khe: không vừa
        assertThat(UpgradeService.fit(mini, List.of(pick("ram", "replace", product("RAM G.Skill 32GB (4x8GB) DDR4 3200", 2_900_000L))),
                new ArrayList<>(), new ArrayList<>())).anySatisfy(e -> assertThat(e).contains("needs 4 sticks"));
    }

    @Test
    void goiLaiAiNhanLuotXongTruoc() throws Exception {
        var slow = new java.util.concurrent.CompletableFuture<String>();
        var fast = new java.util.concurrent.CompletableFuture<String>();
        var either = UpgradeService.firstSuccess(slow, fast);
        slow.complete("first");
        assertThat(either.get()).isEqualTo("first");
        var a = new java.util.concurrent.CompletableFuture<String>();
        var b = new java.util.concurrent.CompletableFuture<String>();
        var one = UpgradeService.firstSuccess(a, b);
        a.completeExceptionally(new IllegalStateException("x"));
        b.complete("second");
        assertThat(one.get()).isEqualTo("second");
    }

    @Test
    void themRamKhacDdrKhongDuocGiuThanhCuKhiDoiBo() {
        var old = new UpgradeInfo("Ryzen 5 3600", "ASUS B450M", "16GB DDR4, 2 sticks", "GTX 1650", "SSD 1TB", "650W",
                "standard", "Programming", 20_000_000L);
        var parts = List.of(pick("cpu", "replace", product("CPU AMD Ryzen 5 7600", 4_990_000L)),
                pick("motherboard", "replace", product("Mainboard ASUS TUF B650M-PLUS DDR5", 4_500_000L)),
                pick("ram", "add", DDR5));
        assertThat(UpgradeService.fit(old, parts, new ArrayList<>(), new ArrayList<>()))
                .anySatisfy(e -> assertThat(e).contains("ram", "replace", "DDR4"));
        var replaced = new ArrayList<>(parts);
        replaced.set(2, pick("ram", "replace", DDR5));
        assertThat(UpgradeService.fit(old, replaced, new ArrayList<>(), new ArrayList<>())).isEmpty();
    }

    @Test
    void khongMuaHaiCardChoMotMay() {
        var turn = new UpgradeService.Turn(pc("650W", 30_000_000L));
        turn.results.put("gpu1", RTX4060);
        var result = service.runTool(Map.of("name", "propose_upgrade", "arguments",
                "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"gpu1\",\"buy_qty\":2}]}"), turn);
        assertThat(result).contains("false", "quantity");
        assertThat(turn.plan).isNull();
    }

    @Test
    void followUpRtxKhongDuocDoiSangAmd() {
        var rx = product("VGA ASUS Dual Radeon RX 7600 8GB", 6_990_000L);
        when(search.search(any())).thenReturn(List.of(rx, RTX4060));
        when(search.offer(rx.url())).thenReturn(new Offer(rx.title(), rx.shop(), rx.priceVnd(), null, rx.url(), null, true, 36, Instant.now()));
        reply(toolCall("s", "search_products", "{\"query\":\"RTX 4060\"}"),
                toolCall("p1", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-1\"}]}"),
                toolCall("p2", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-2\"}]}"),
                answer("Use the RTX card."));
        var request = new ChatRequest(List.of(new Message("user", "Upgrade my PC"), new Message("assistant", "Here is an option."),
                new Message("user", "Please use RTX, not AMD.")), null, null, "upgrade", pc("650W", 12_000_000L));
        var res = service.reply(request, s -> { });
        assertThat(toolResult(2)).contains("NVIDIA");
        assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.productName()).contains("RTX"));
    }

    @Test
    void soLuongMuaKhongDuocTuLamTronHoacCatBot() {
        for (String qty : List.of("0", "-1", "1.5", "5", "4294967297", "\"two\"")) {
            var turn = new UpgradeService.Turn(pc("650W", 30_000_000L));
            turn.results.put("ram1", DDR4);
            var result = service.runTool(Map.of("name", "propose_upgrade", "arguments",
                    "{\"items\":[{\"category\":\"ram\",\"product_id\":\"ram1\",\"buy_qty\":" + qty + "}]}"), turn);
            assertThat(result).as("quantity %s", qty).contains("false", "quantity");
            assertThat(turn.plan).isNull();
        }
    }

    @Test
    void followUpChoPhepMoiHangThiGoYeuCauRtxCu() {
        var rx = product("VGA ASUS Dual Radeon RX 7600 8GB", 6_990_000L);
        when(search.search(any())).thenReturn(List.of(rx));
        when(search.offer(rx.url())).thenReturn(new Offer(rx.title(), rx.shop(), rx.priceVnd(), null, rx.url(), null, true, 36, Instant.now()));
        reply(toolCall("s", "search_products", "{\"query\":\"RX 7600\"}"),
                toolCall("p", "propose_upgrade", "{\"items\":[{\"category\":\"gpu\",\"product_id\":\"s1-1\"}]}"),
                answer("Use the Radeon card."));
        var request = new ChatRequest(List.of(new Message("user", "Upgrade my PC"), new Message("assistant", "Which brand?"),
                new Message("user", "RTX please"), new Message("assistant", "Here is an RTX option."),
                new Message("user", "Any brand is fine now.")), null, null, "upgrade", pc("650W", 12_000_000L));
        var res = service.reply(request, s -> { });
        assertThat(res.build().items()).singleElement().satisfies(l -> assertThat(l.productName()).contains("RX 7600"));
    }

    @Test
    void lanCuoiChiMuaSsdNeuThemRamLamVuotNganSach() {
        var office = new UpgradeInfo("Intel Core i5-6500", "ASUS H110M-K", "8GB DDR4, 1 stick", "none", "HDD 1TB", "300W",
                "standard", "Office Word Excel; Windows starts slowly", 3_500_000L);
        var ssd = product("SSD Kingston A400 480GB 2.5 inch SATA", 2_500_000L);
        when(search.offer(ssd.url())).thenReturn(new Offer(ssd.title(), ssd.shop(), ssd.priceVnd(), null, ssd.url(), null, true, 36, Instant.now()));
        var turn = new UpgradeService.Turn(office);
        turn.proposals.set(UpgradeService.MAX_PROPOSE_ROUNDS - 1);
        turn.results.put("ssd", ssd);
        turn.results.put("ram", DDR4);
        var result = service.runTool(Map.of("name", "propose_upgrade", "arguments",
                "{\"items\":[{\"category\":\"ssd\",\"product_id\":\"ssd\",\"action\":\"add\"},"
                + "{\"category\":\"ram\",\"product_id\":\"ram\"}]}"), turn);
        assertThat(result).contains("\"ok\":true");
        assertThat(turn.plan.items()).singleElement().satisfies(p -> assertThat(p.category()).isEqualTo("ssd"));
        assertThat(turn.plan.warnings()).extracting(Issue::rule).contains("BUDGET_TRIM");
    }

    @Test
    void b650eITwoOccupiedSlotsCannotTakeAThirdStickOrReplaceWithLessMemory() {
        var pc = new UpgradeInfo("AMD Ryzen 5 7600", "ASUS ROG STRIX B650E-I GAMING WIFI", "16GB DDR5, 2 sticks",
                "RTX 4060 8GB", "NVMe 1TB", "SFX 650W", "mini", "Programming Docker Android Studio", 12_000_000L);
        assertThat(UpgradeService.boardSlots(pc.motherboard())).isEqualTo(2);
        assertThat(UpgradeService.fit(pc, List.of(pick("ram", "add", DDR5)), new ArrayList<>(), new ArrayList<>()))
                .anySatisfy(e -> assertThat(e).contains("2 RAM slots", "3 sticks"));
        assertThat(UpgradeService.fit(pc, List.of(pick("ram", "replace", DDR5)), new ArrayList<>(), new ArrayList<>()))
                .anySatisfy(e -> assertThat(e).contains("does not increase capacity"));
    }

    @Test
    void affordableSubsetStillRequiresTheMatchingCpuBoardAndPowerSupply() {
        var cpu = pick("cpu", "replace", product("CPU Intel Core i5-12400F", 2_990_000L));
        assertThat(UpgradeService.affordableSubset(pc("300W", 4_000_000L), List.of(cpu, pick("psu", "replace", PSU650)))).isEmpty();
        assertThat(UpgradeService.affordableSubset(pc("300W", 9_000_000L), List.of(pick("gpu", "replace", RTX4070), pick("psu", "replace", PSU650)))).isEmpty();
    }

    @Test
    void betterChoiceRespectsTheSameVendorConstraintAsThePurchaseGate() {
        var rx = product("VGA ASUS Dual Radeon RX 7600 XT 16GB", 6_990_000L);
        when(search.offer(rx.url())).thenReturn(new Offer(rx.title(), rx.shop(), rx.priceVnd(), null, rx.url(), null, true, 36, Instant.now()));
        var turn = new UpgradeService.Turn(pc("650W", 12_000_000L));
        turn.results.put("rx", rx);
        turn.requiredGpuVendor = "nvidia";
        assertThat(service.betterChoice(turn, List.of(pick("gpu", "replace", RTX4060)))).isNull();
        turn.requiredGpuVendor = null;
        assertThat(service.betterChoice(turn, List.of(pick("gpu", "replace", RTX4060)))).contains("RX 7600");
    }

    @Test
    void memoryExplanationUsesTotalCapacityNotSixteenGbPerOldStick() {
        var old = new UpgradeInfo("Ryzen 5 7600", "ASUS B650E-I", "16GB DDR5, 2 sticks", "RTX 4060", "SSD 1TB", "650W",
                "mini", "Programming", 16_000_000L);
        var kit = pick("ram", "replace", product("RAM Kingston 32GB (2x16GB) DDR5 6000", 13_000_000L));
        assertThat(UpgradeService.memoryReply(old, List.of(kit))).contains("Replace the existing RAM", "32 GB")
                .doesNotContain("two 16GB", "64 GB");
        var one = pick("ram", "add", DDR5);
        assertThat(UpgradeService.memoryReply(old, List.of(one))).contains("Add ", "32 GB");
        var pair = new UpgradeService.Pick("ram", "replace", DDR5, one.offer(), 2, null, null);
        assertThat(UpgradeService.memoryReply(old, List.of(pair))).contains("2 × ", "32 GB");
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
        doAnswer(inv -> {
            Map<String, Object> body = inv.getArgument(0);
            var copy = new LinkedHashMap<>(body);
            copy.put("messages", new ArrayList<>((List<Object>) body.get("messages")));
            sent.add(copy);
            return queue.removeFirst();
        }).when(client).complete(any());
    }

    /** Nội dung kết quả công cụ cuối cùng mà AI nhận được ở lần gọi thứ call */
    @SuppressWarnings("unchecked")
    private String toolResult(int call) {
        var messages = (List<Map<String, Object>>) sent.get(call).get("messages");
        return String.valueOf(messages.getLast().get("content"));
    }
}
