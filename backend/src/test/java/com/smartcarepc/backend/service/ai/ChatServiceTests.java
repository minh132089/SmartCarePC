package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import com.smartcarepc.backend.exception.AiUnavailableException;
import org.springframework.http.HttpStatus;
import com.smartcarepc.backend.dto.BuildCheckRequest;
import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.ChatResponse;
import com.smartcarepc.backend.dto.ChatRequest.Message;
import com.smartcarepc.backend.dto.ChatResponse.Line;
import com.smartcarepc.backend.dto.CompatibilityResponse;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;
import com.smartcarepc.backend.dto.ComponentResponse;
import com.smartcarepc.backend.dto.PcBuildResponse;
import com.smartcarepc.backend.entity.enums.BuildType;
import com.smartcarepc.backend.service.CompatibilityService;
import com.smartcarepc.backend.service.ComponentService;
import com.smartcarepc.backend.service.ModelMatchService;
import com.smartcarepc.backend.service.PcBuildService;

import tools.jackson.databind.json.JsonMapper;

/** Vòng gọi công cụ của trợ lý, giả lập API AI bằng mock nên không cần mạng hay khoá API. */
class ChatServiceTests {

    private final LlmClient client = mock(LlmClient.class);
    private final ProductSearch search = mock(ProductSearch.class);
    private final ComponentService components = mock(ComponentService.class);
    private final CompatibilityService compatibility = mock(CompatibilityService.class);
    private final PcBuildService builds = mock(PcBuildService.class);
    /** Mockito giữ tham chiếu tới cùng một danh sách messages qua các lượt, nên chụp lại nội dung ngay lúc gọi */
    private final List<Map<String, Object>> sent = new ArrayList<>();
    private ChatService service;

    @BeforeEach
    void setUp() {
        when(builds.findPresets()).thenReturn(List.of());
        // Mặc định: link có offer là đã đọc trang sản phẩm (test riêng cho trường hợp chỉ thấy trên trang tìm kiếm)
        when(search.pageChecked(any())).thenReturn(true);
        service = new ChatService(client, search, components, compatibility, builds, new ModelMatchService(builds, components),
                JsonMapper.builder().build(), "test-model", 512, false);
    }

    @Test
    void thongSoHienThiTheoHangThatKhongGiuBaoHanhHayGenAiDoan() {
        assertThat(ChatService.purchasedSpec("psu", "JETEK Revolve B650 650W 80 Plus Bronze", "650W Bronze, bảo hành 60 tháng"))
                .isEqualTo("650W");
        assertThat(ChatService.purchasedSpec("ssd", "SSD Kingston 1 TB M.2 NVMe - Đọc 2100 MB/s", "NVMe Gen4 1TB"))
                .isEqualTo("1000GB");
        assertThat(ChatService.purchasedSpec("ram", "Lexar THOR 32GB (2x16GB)", "4x16GB DDR4, 2 bộ kit"))
                .isEqualTo("4x16GB DDR4, 2 bộ kit");
        assertThat(ChatService.purchasedSpec("cooler", "Jonsbo CR-1200", "3 ống đồng, 155mm" )).isNull();
    }

    @Test
    void boBaQuatTinhGiaTheoBoVaGiuSoQuatSuDung() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true,List.of(),List.of()));
        when(components.findByCode("fan120_argb")).thenReturn(part("fan120_argb","fans","Quạt"));
        when(search.enabled()).thenReturn(true);
        var product = new ProductSearch.Product("Bộ 3 quạt Xigmatek STARLINK ARGB BLACK","A",490_000L,"https://a.vn/fans",null);
        when(search.search(any())).thenReturn(List.of(product));
        when(search.offer(product.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(product.title(),"A",490_000L,null,product.url(),null,true,12,java.time.Instant.now()));
        when(search.directLink(any(),any(),any())).thenAnswer(inv -> inv.getArgument(2));
        for (int qty : List.of(1,3,4,6)) {
            org.mockito.Mockito.reset(client);
            reply(toolCall("s","search_products","{\"query\":\"quạt\"}"),toolCall("p","propose_build","""
                    {"tier":"balanced","budget_vnd":1000000,"items":[{"code":"fan120_argb","quantity":%d,"product_id":"s1-1"}]}
                    """.formatted(qty)),answer("Xong."),answer("Xong."));
            var res=service.reply(new ChatRequest(List.of(new Message("user","Mua quạt vỏ, ngân sách 1 triệu")),null,null,"advice"));
            assertThat(res.build()).isNotNull();
            var fan=res.build().items().getFirst();
            assertThat(fan.quantity()).isEqualTo(qty);
            assertThat(fan.priceMaxVnd()).isEqualTo(((qty+2)/3)*490_000L);
            assertThat(fan.spec()).contains(qty+" quạt sử dụng", "mua "+((qty+2)/3)+" bộ");
            if(qty%3!=0)assertThat(fan.spec()).contains("dư");
        }
    }

    @Test
    void sparkleTitanBaQuatKhongDuocGanMauGpu240() {
        String title="Card màn hình SPARKLE Intel Arc B580 TITAN OC 12GB GDDR6 (SB580T-12GOC)";
        assertThat(ChatService.gpuFans(title)).isEqualTo(3);
        assertThat(ChatService.fitsCode("gpu_240",title)).isFalse();
        assertThat(ChatService.fitsCode("gpu_280",title)).isTrue();
        assertThat(ChatService.gpuFans("SPARKLE Intel Arc B580 GUARDIAN 2 fans")).isEqualTo(2);
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true,List.of(),List.of()));
        var gpu=part("gpu_240","gpu","Card");
        when(components.findByCode("gpu_240")).thenReturn(gpu);
        var gpuLarge=part("gpu_280","gpu","Card lớn");
        var casePart=part("case_mini_sff","case","Mini");
        when(components.findByCode("gpu_280")).thenReturn(gpuLarge);
        when(components.findByCode("case_mini_sff")).thenReturn(casePart);
        when(components.findAll(null)).thenReturn(List.of(gpu,gpuLarge,casePart));
        when(builds.findByCode("mini_sff")).thenReturn(new PcBuildResponse(1L,"mini_sff","Mini","",BuildType.PRESET,null,
                List.of(new PcBuildResponse.Item(1,gpu))));
        when(search.enabled()).thenReturn(true);
        var bad=new ProductSearch.Product(title,"A",9_490_000L,"https://a.vn/b580",null);
        var good=new ProductSearch.Product("VGA ASUS Dual GeForce RTX 4060 8GB","A",8_450_000L,"https://a.vn/4060",null);
        var pcCase=new ProductSearch.Product("Vỏ Silverstone SG13 Mini ITX","A",1_090_000L,"https://a.vn/sg13",null);
        when(search.search(any())).thenReturn(List.of(bad,good,pcCase));
        for(var product:List.of(bad,good,pcCase))when(search.offer(product.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(product.title(),"A",product.priceVnd(),null,product.url(),null,true,36,java.time.Instant.now()));
        when(search.directLink(any(),any(),any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s","search_products","{\"query\":\"VGA\"}"),toolCall("p","propose_build","""
                {"tier":"balanced","budget_vnd":24000000,"items":[{"code":"case_mini_sff","product_id":"s1-3"},{"code":"gpu_240","product_id":"s1-1"}]}
                """),answer("Xong."),answer("Xong."));
        var res=service.reply(new ChatRequest(List.of(new Message("user","PC mini ITX game 24 triệu")),null,null,"advice"));
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).noneSatisfy(line -> assertThat(line.productName()).contains("TITAN"));
    }

    @Test
    void chuaTraGiaThiKhongTuBaoGiaThiTruongHayNganSachThayThe() {
        when(search.enabled()).thenReturn(true);
        for (String draft : List.of("RTX 4090 giá khoảng 40 - 50 triệu; bạn cần 60 triệu để dựng máy.",
                "RTX 4090 giá 40.000.000đ.", "RTX 4090 giá khoảng 40 củ.")) {
            org.mockito.Mockito.reset(client);
            reply(answer(draft), answer(draft));
            var res = service.reply(new ChatRequest(List.of(new Message("user",
                    "Máy game 1 triệu, phải có RTX 4090 24GB mới.")), null, null, "advice"));
            assertThat(res.build()).isNull();
            assertThat(res.reply()).contains("1 triệu", "RTX 4090", "chưa có cấu hình và giá bán đã xác minh")
                    .doesNotContain("40 - 50", "60 triệu", "40.000.000", "40 củ");
        }
        org.mockito.Mockito.verifyNoInteractions(compatibility);
    }

    @Test
    void traLoiThangKhiKhongCanCongCu() {
        reply(answer("Chào bạn!"));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "xin chào")), null));

        assertThat(res.reply()).isEqualTo("Chào bạn!");
        assertThat(res.build()).isNull();
        assertThat(sent.getFirst()).containsEntry("model", "test-model").containsEntry("max_tokens", 512);
        assertThat(messages(0).getFirst()).containsEntry("role", "system");
    }

    @Test
    void cauTraLoiRongThiThuLaiMotLanKhongTraXinLoiGiaThanhCong() {
        reply(answer(""), answer("Đặt RAM thẳng theo khe."));
        var res = service.reply(new ChatRequest(List.of(new Message("user", "lắp RAM thế nào?")), null));
        assertThat(res.reply()).isEqualTo("Đặt RAM thẳng theo khe.");
        assertThat(sent).hasSize(2);
        assertThat(sent.get(1)).containsEntry("max_tokens", 8192);
    }

    @Test
    void congCuBiCatGiuaChungThiKhongThucThiDanhSachHong() {
        var truncated = Map.<String, Object>of("choices", List.of(Map.of("finish_reason", "length", "message",
                Map.of("role", "assistant", "content", "", "tool_calls", List.of(Map.of("id", "cut"))))));
        reply(truncated, answer("Mình cần kiểm tra lại trước khi chốt."));
        var res = service.reply(new ChatRequest(List.of(new Message("user", "xem hộ máy này")), null));
        assertThat(res.reply()).isEqualTo("Mình cần kiểm tra lại trước khi chốt.");
        org.mockito.Mockito.verifyNoInteractions(compatibility);
    }

    @Test
    void itemsLaChuoiThiGuiLaiLoiSchemaDeAiSua() {
        reply(toolCall("bad", "propose_build", "{\"tier\":\"balanced\",\"items\":\"như lần trước\"}"), answer("Mình sẽ gửi đúng danh sách."));
        service.reply(new ChatRequest(List.of(new Message("user", "xem cấu hình")), null, null, "advice"));
        assertThat((String) messages(1).get(3).get("content")).contains("items phải là mảng");
        org.mockito.Mockito.verifyNoInteractions(compatibility);
    }

    @Test
    void yeuCauMoiBoMiniVaDungLuongKitRamKhongBiDocSai() {
        assertThat(ChatService.needs(List.of("không cần mini ITX, dùng mATX", "máy mini game 30 triệu")).miniItx()).isFalse();
        assertThat(ChatService.needs(List.of("máy game RAM 2x16GB 30 triệu")).ramMin()).isEqualTo(32);
        assertThat(ChatService.needs(List.of("RAM 32GB đủ rồi", "RAM 2x32GB")).ramMin()).isEqualTo(32);
        assertThat(ChatService.needs(List.of("game AAA và dựng video, card VRAM 8GB, 29 triệu")).ramMin()).isEqualTo(16);
        assertThat(ChatService.needs(List.of("chơi game VRAM 8GB, RAM 32GB, 30 triệu")).ramMin()).isEqualTo(32);
        for (String text : List.of("dạy Teams, soạn giáo án", "PowerPoint, trình chiếu", "xem bài giảng và bảng tính", "luận văn và tra cứu web")) {
            assertThat(ChatService.needs(List.of(text)).office()).as(text).isTrue();
        }
        assertThat(ChatService.noDiscreteGpu(List.of("Đổi sang chơi game AAA, phải có card rời", "không cần card rời"))).isFalse();
    }

    @Test
    void doiDongBoNenTangDeGiuDungLuongVaCardTrongNganSach() {
        var res = platformBudgetResponse("PC chơi game 24 triệu, đúng RX 7600 8GB, RAM 32GB");
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).anySatisfy(line -> {
            assertThat(line.category()).isEqualTo("ram");
            assertThat(line.productName()).contains("16GB DDR4");
            assertThat(line.quantity()).isEqualTo(2);
        });
        assertThat(res.build().items().stream().mapToLong(l -> l.priceMaxVnd()).sum()).isLessThanOrEqualTo(24_000_000);
    }

    @Test
    void thayDoiDungLuongTrongCauNoiMoiGhiDeMucCu() {
        assertThat(ChatService.needs(List.of("Đổi RAM lên 32GB, giữ ngân sách đó.", "PC game 30 triệu, RAM 16GB.")).ramMin()).isEqualTo(32);
        assertThat(ChatService.needs(List.of("ĐỔI RAM XUỐNG 16GB", "PC game RAM 32GB 30 triệu")).ramMin()).isEqualTo(16);
        assertThat(ChatService.needs(List.of("SSD lên 1TB, giữ ngân sách", "PC game SSD 512GB 30 triệu")).ssdMin()).isEqualTo(1000);
        assertThat(ChatService.needs(List.of("PC game VRAM ít nhất 8GB 30 triệu")).ramMin()).isEqualTo(16);
        assertThat(ChatService.noDiscreteGpu(List.of("Bỏ card rời, chỉ Word và học online", "PC game có RTX 4060"))).isTrue();
        assertThat(ChatService.noDiscreteGpu(List.of("Không bỏ card rời", "PC không cần card rời"))).isFalse();
        assertThat(ChatService.requestedGpu(List.of("Bỏ card rời", "PC game RTX 4060 8GB"))).isNull();
    }

    @Test
    void nguoiDungChotDdr5ThiKhongDoiDdr4DeLamDepNganSach() {
        assertThat(platformBudgetResponse("PC chơi game 24 triệu, đúng RX 7600 8GB, RAM 32GB DDR5").build()).isNull();
    }

    private ChatResponse platformBudgetResponse(String user) {
        return platformBudgetResponse(user, false);
    }

    @Test
    void boItxAm4KhongMuaDuocThiDoiDongBoIntelGiuMaItx() {
        var res = platformBudgetResponse("PC mini ITX chơi Valorant 36 triệu, RAM16GB", true);
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).anySatisfy(line -> {
            assertThat(line.code()).isEqualTo("mb_itx");
            assertThat(line.productName()).contains("H610I");
        });
        assertThat(res.build().items()).anySatisfy(line -> {
            assertThat(line.category()).isEqualTo("cpu");
            assertThat(line.productName()).contains("12400F");
        });
        assertThat(res.reply()).contains("giữ vỏ Mini-ITX");
    }

    @Test
    void khongTimDuocBoItxVanGiuCpuNguoiDungChot() {
        assertThat(platformBudgetResponse("PC mini ITX Valorant36 triệu, phải dùng CPU AMD Ryzen5 5600X, RAM16GB", true).build()).isNull();
    }

    private ChatResponse platformBudgetResponse(String user, boolean missingMini) {
        return platformBudgetResponse(user, missingMini, false);
    }

    @Test
    void cuuBoItxThieuThiDoiRamDdr5DongBoGiuDungLuong() {
        var res = platformBudgetResponse("PC mini ITX chơi Valorant36 triệu, RAM32GB", true, true);
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).anySatisfy(line -> {
            assertThat(line.code()).isEqualTo("mb_itx");
            assertThat(line.productName()).contains("H610I");
        });
        assertThat(res.build().items()).anySatisfy(line -> {
            assertThat(line.category()).isEqualTo("ram");
            assertThat(line.productName()).contains("16GB DDR4");
            assertThat(line.quantity()).isEqualTo(2);
        });
    }

    private ChatResponse platformBudgetResponse(String user, boolean missingMini, boolean ddr5) {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true,List.of(),List.of()));
        for(String[] p:List.of(new String[]{"cpu_std","cpu"},new String[]{missingMini ? "mb_itx" : "mb_matx","motherboard"},new String[]{"ram_ddr4","ram"},new String[]{"gpu_240","gpu"})) {
            when(components.findByCode(p[0])).thenReturn(part(p[0],p[1],p[1]));
        }
        when(search.enabled()).thenReturn(true);
        var products=List.of(new ProductSearch.Product(missingMini ? "CPU AMD Ryzen 5 5600X" : "CPU AMD Ryzen 5 7500F","A",4_600_000L,"https://a.vn/cpu",null),
                new ProductSearch.Product("Mainboard ASUS A620M-K DDR5","A",2_500_000L,"https://a.vn/mb",null),
                new ProductSearch.Product(missingMini && !ddr5 ? "RAM Corsair 32GB (2x16GB) DDR4" : "RAM Corsair 32GB (2x16GB) DDR5","A",13_890_000L,"https://a.vn/ram",null),
                new ProductSearch.Product("VGA ASUS RX 7600 8GB","A",7_790_000L,"https://a.vn/gpu",null),
                new ProductSearch.Product("CPU Intel Core i5 12400F","A",3_790_000L,"https://a.vn/intel",null),
                new ProductSearch.Product("Mainboard GIGABYTE H610M-H DDR4","A",1_790_000L,"https://a.vn/h610",null),
                new ProductSearch.Product("RAM SSTC 16GB DDR4","A",2_990_000L,"https://a.vn/ddr4",null),
                new ProductSearch.Product("Mainboard ASUS PRIME H610I-PLUS D4 ITX LGA1700 DDR4","A",2_390_000L,"https://a.vn/h610i",null));
        when(search.search(any())).thenReturn(products);
        for(var p:products)when(search.offer(p.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(p.title(),"A",p.priceVnd(),null,p.url(),null,true,36,java.time.Instant.now()));
        when(search.directLink(any(),any(),any())).thenAnswer(inv -> inv.getArgument(2));
        String proposal = """
                {"tier":"balanced","summary":"Game","budget_vnd":24000000,"items":[{"code":"cpu_std","product_id":"s1-1"},
                {"code":"mb_matx","product_id":"s1-2"},{"code":"ram_ddr4","quantity":2,"spec":"32GB DDR5","product_id":"s1-3"},
                {"code":"gpu_240","product_id":"s1-4"}]}
                """;
        if (missingMini) proposal = proposal.replace("mb_matx", "mb_itx").replace("24000000", "36000000").replace("32GB DDR5", "32GB DDR4");
        reply(toolCall("s","search_products","{\"query\":\"parts\"}"),toolCall("p","propose_build",proposal),answer("Xong."),answer("Xong."),answer("Xong."));
        return service.reply(new ChatRequest(List.of(new Message("user",user)),null,null,"advice"));
    }

    @Test
    void bonThanhRamThiDoiSangBoBonKheVaGiu64Gb() {
        var res = ramSlotsResponse(true);
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).anySatisfy(line -> {
            assertThat(line.category()).isEqualTo("motherboard");
            assertThat(line.productName()).contains("GAMING PLUS");
        });
        assertThat(res.build().items()).anySatisfy(line -> {
            assertThat(line.category()).isEqualTo("ram");
            assertThat(line.quantity()).isEqualTo(4);
        });
    }

    @Test
    void gpuDuVramNhungSaiHangVanPhaiSua() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true,List.of(),List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280","gpu","Card"));
        when(search.enabled()).thenReturn(true);
        var bad = new ProductSearch.Product("VGA ASUS Dual RX 6700 XT 12GB","A",7_990_000L,"https://a.vn/rx",null);
        var good = new ProductSearch.Product("VGA MSI RTX 3060 Ventus 2X 12GB","A",9_200_000L,"https://a.vn/rtx",null);
        when(search.search(any())).thenReturn(List.of(bad,good));
        for(var p:List.of(bad,good))when(search.offer(p.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(p.title(),"A",p.priceVnd(),null,p.url(),null,true,36,java.time.Instant.now()));
        when(search.directLink(any(),any(),any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s","search_products","{\"query\":\"parts\"}"),toolCall("p","propose_build","""
                {"tier":"balanced","summary":"AI","budget_vnd":38000000,"items":[{"code":"gpu_280","product_id":"s1-1"}]}
                """),answer("Xong."),answer("Xong."));
        var res = service.reply(new ChatRequest(List.of(new Message("user","PC Stable Diffusion 38 triệu, GPU NVIDIA ít nhất 12GB VRAM")),null,null,"advice"));
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).anySatisfy(line -> assertThat(line.productName()).contains("RTX 3060"));
    }

    @Test
    void khongTimDuocBoBonKheThiKhongHienBonThanhTrenHaiKhe() {
        assertThat(ramSlotsResponse(false).build()).isNull();
    }

    private ChatResponse ramSlotsResponse(boolean replacement) {
        return ramSlotsResponse(replacement, false);
    }

    @Test
    void yeuCau64GbThiMuaDuHaiKit32GbDuaTrenHangThat() {
        var res = ramSlotsResponse(true, true);
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).anySatisfy(line -> {
            assertThat(line.category()).isEqualTo("ram");
            assertThat(line.productName()).contains("2x16GB");
            assertThat(line.quantity()).isEqualTo(4);
            assertThat(line.priceMaxVnd()).isEqualTo(9_398_000L);
        });
    }

    @Test
    void h610mEHaiKhePhaiDoiBoKhiMuaBonThanhTuHaiKit() {
        var res = ramSlotsResponse(true, true, "Mainboard MSI PRO H610M-E DDR4 | LGA 1700, mATX, 2 khe RAM");
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).anySatisfy(line -> {
            assertThat(line.category()).isEqualTo("motherboard");
            assertThat(line.productName()).contains("GAMING PLUS");
        });
        assertThat(res.build().items()).anySatisfy(line -> {
            assertThat(line.category()).isEqualTo("ram");
            assertThat(line.quantity()).isEqualTo(4);
            assertThat(line.priceMaxVnd()).isEqualTo(9_398_000L);
        });
    }

    @Test
    void h610mEKhongCoBoThayThiAnPhuongAnBonThanh() {
        assertThat(ramSlotsResponse(false, true, "Mainboard MSI PRO H610M-E DDR4").build()).isNull();
    }

    private ChatResponse ramSlotsResponse(boolean replacement, boolean misleadingKit) {
        return ramSlotsResponse(replacement, misleadingKit, "Mainboard MSI PRO B760M-E DDR4");
    }

    private ChatResponse ramSlotsResponse(boolean replacement, boolean misleadingKit, String boardTitle) {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true,List.of(),List.of()));
        for(String[] p:List.of(new String[]{"mb_matx","motherboard"},new String[]{"ram_ddr4","ram"})) {
            when(components.findByCode(p[0])).thenReturn(part(p[0],p[1],p[1]));
        }
        when(search.enabled()).thenReturn(true);
        var mb = new ProductSearch.Product(boardTitle,"A",2_390_000L,"https://a.vn/mb",null);
        var ram = new ProductSearch.Product(misleadingKit ? "RAM Lexar Thor 32GB (2x16GB) DDR4" : "RAM Kingston 16GB DDR4",
                "A",misleadingKit ? 4_699_000L : 2_000_000L,"https://a.vn/ram",null);
        var four = new ProductSearch.Product("Mainboard GIGABYTE B760M GAMING PLUS WIFI DDR4","A",3_290_000L,"https://a.vn/four",null);
        var products = replacement ? List.of(mb,ram,four) : List.of(mb,ram);
        when(search.search(any())).thenReturn(products);
        for(var p:products)when(search.offer(p.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(p.title(),"A",p.priceVnd(),null,p.url(),null,true,36,java.time.Instant.now()));
        when(search.directLink(any(),any(),any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s","search_products","{\"query\":\"parts\"}"),toolCall("p","propose_build","""
                {"tier":"balanced","summary":"RAM","budget_vnd":36000000,"items":[{"code":"mb_matx","product_id":"s1-1"},
                {"code":"ram_ddr4","quantity":4,"spec":"%s","product_id":"s1-2"}]}
                """.formatted(misleadingKit ? "2x16GB DDR4" : "64GB DDR4")),answer("Xong."),answer("Xong."),answer("Xong."));
        return service.reply(new ChatRequest(List.of(new Message("user","máy RAM 64GB 36 triệu")),null,null,"advice"));
    }

    @Test
    void mayVanPhongVuotTranDoiVoMatxDeCoBanTrongNganSach() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true,List.of(),List.of()));
        for(String[] p:List.of(new String[]{"case_standard_tower","case"},new String[]{"case_compact_tower","case"},
                new String[]{"cpu_std","cpu"},new String[]{"mb_matx","motherboard"},new String[]{"ram_ddr4","ram"},
                new String[]{"ssd_m2_2280","ssd"},new String[]{"psu_atx","psu"})) {
            when(components.findByCode(p[0])).thenReturn(part(p[0],p[1],p[1]));
        }
        when(builds.findByCode("compact_tower")).thenReturn(new PcBuildResponse(1L,"compact_tower","Gọn","",BuildType.PRESET,null,
                List.of(new PcBuildResponse.Item(1,part("mb_matx","motherboard","Bo mATX")))));
        when(search.enabled()).thenReturn(true);
        var products=List.of(
                new ProductSearch.Product("Vỏ máy tính ATX Corsair FRAME 4000D","A",1_990_000L,"https://a.vn/big",null),
                new ProductSearch.Product("CPU Intel Core i3 12100 Box","A",3_390_000L,"https://a.vn/cpu",null),
                new ProductSearch.Product("Mainboard GIGABYTE H610M-H V3 DDR4","A",1_790_000L,"https://a.vn/mb",null),
                new ProductSearch.Product("RAM SSTC 8GB DDR4","A",2_190_000L,"https://a.vn/ram",null),
                new ProductSearch.Product("SSD SSTC M2 NVMe 256GB","A",1_590_000L,"https://a.vn/ssd",null),
                new ProductSearch.Product("Nguồn AIGO VK450 450W ATX","A",449_000L,"https://a.vn/psu",null),
                new ProductSearch.Product("Vỏ máy tính EDRA ECS1105 Micro-ATX","A",290_000L,"https://a.vn/small",null));
        when(search.search(any())).thenReturn(products);
        for(var p:products)when(search.offer(p.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                p.title(),"A",p.priceVnd(),null,p.url(),null,true,36,java.time.Instant.now()));
        when(search.directLink(any(),any(),any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s","search_products","{\"query\":\"parts\"}"),toolCall("p","propose_build","""
                {"tier":"balanced","summary":"Văn phòng","budget_vnd":11000000,"items":[
                {"code":"case_standard_tower","product_id":"s1-1"},{"code":"cpu_std","product_id":"s1-2"},
                {"code":"mb_matx","product_id":"s1-3"},{"code":"ram_ddr4","quantity":2,"spec":"16GB DDR4","product_id":"s1-4"},
                {"code":"ssd_m2_2280","spec":"256GB","product_id":"s1-5"},{"code":"psu_atx","product_id":"s1-6"}]}
                """),answer("Xong."),answer("Xong."),answer("Xong."));
        var res=service.reply(new ChatRequest(List.of(new Message("user","PC xem phim, Word 11 triệu, rẻ nhất, không cần card rời")),null,null,"advice"));
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).anySatisfy(line -> assertThat(line.code()).isEqualTo("case_compact_tower"));
        assertThat(res.build().items()).noneSatisfy(line -> assertThat(line.category()).isEqualTo("gpu"));
        assertThat(res.build().items().stream().mapToLong(Line::priceMaxVnd).sum()).isLessThanOrEqualTo(11_000_000L);
    }

    @Test
    void giaiThichLayDungDungLuongKitTuTheKhongDoanBios() {
        var cpu = new Line("cpu", "cpu", "CPU", 1, null, null, 4_000_000L, 4_000_000L, null, "https://a/cpu", "Intel Core i3 12100 Box", "A");
        var ram = new Line("ram", "ram", "RAM", 2, null, null, 3_000_000L, 3_000_000L, null, "https://a/ram", "RAM Corsair 2x16GB DDR4", "A");
        var card = new ChatResponse.BuildCard(List.of(cpu, ram), true, List.of(), List.of(), null, 15_000_000L, null, "balanced", 1);
        String out = ChatService.adviceExplanation(ChatService.needs(List.of("máy học online 15 triệu")), card);
        assertThat(out).contains("đồ họa tích hợp").contains("RAM 32GB").doesNotContain("64GB").doesNotContain("BIOS");
        assertThat(ChatService.safeReason("cpu", 1, "6 nhân, chơi game mượt hơn")).isNull();
        assertThat(ChatService.safeReason("motherboard", 1, "Có WiFi, bảo hành 36 tháng")).isNull();
        assertThat(ChatService.safeReason("ram", 1, "Đúng 16GB, chạy 2 kênh")).isNull();
    }

    @Test
    void giaDoiLucChotThiKiemLaiTranTien() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        var product = new ProductSearch.Product("SSD Kingston NV3 1TB", "A", 1_500_000L, "https://a.vn/ssd", null);
        var offer = new java.util.concurrent.atomic.AtomicReference<>(new com.smartcarepc.backend.service.price.Offer(
                product.title(), "A", 1_500_000L, null, product.url(), null, true, 36, java.time.Instant.now()));
        when(search.search(any())).thenReturn(List.of(product));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        when(search.offer(product.url())).thenAnswer(inv -> offer.get());
        when(search.refreshQuote(product.url(), product.title())).thenAnswer(inv -> {
            offer.set(new com.smartcarepc.backend.service.price.Offer(product.title(), "A", 2_500_000L, null, product.url(), null, true, 36, java.time.Instant.now()));
            return offer.get();
        });
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":2000000,"items":[{"code":"ssd_m2_2280","spec":"1TB","product_id":"s1-1"}]}
                """), answer("Xong."), answer("Xong."));
        var res = service.reply(new ChatRequest(List.of(new Message("user", "máy học SSD 1TB 2 triệu")), null, null, "advice"));
        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("2,5 triệu").contains("vượt mức tối đa 2 triệu");
    }

    @Test
    void chayCheckBuildRoiGuiKetQuaVaTraVeTheCauHinh() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(false,
                List.of(new Issue("case.psu", "Nguồn ATX quá to cho vỏ này.")), List.of()));
        when(builds.findByCode("mini_sff")).thenReturn(new PcBuildResponse(4L, "mini_sff", "Mini / SFF", "Hộp nhôm rất nhỏ.",
                BuildType.PRESET, null, List.of()));
        when(components.findByCode("case_mini_sff")).thenReturn(part("case_mini_sff", "case", "Vỏ Mini / SFF"));
        when(components.findByCode("psu_atx")).thenReturn(part("psu_atx", "psu", "Nguồn ATX"));
        reply(toolCall("t1", "check_build",
                        "{\"items\":[{\"code\":\"case_mini_sff\"},{\"code\":\"psu_atx\",\"quantity\":1}]}"),
                answer("<think>nguồn to quá</think>Nguồn này không vừa vỏ."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "lắp nguồn ATX vào vỏ mini được không?")), "mini_sff"));

        assertThat(res.reply()).isEqualTo("Nguồn này không vừa vỏ.");
        assertThat((String) messages(0).getFirst().get("content")).contains("đang xem máy \"Mini / SFF\"");
        var req = ArgumentCaptor.forClass(BuildCheckRequest.class);
        verify(compatibility).check(req.capture());
        assertThat(req.getValue().items()).extracting(BuildCheckRequest.Item::code).containsExactly("case_mini_sff", "psu_atx");

        // Lượt 2 gửi lại tin gọi công cụ của model và kết quả của nó
        var second = messages(1);
        assertThat(second).hasSize(4);
        assertThat(second.get(2)).containsKey("tool_calls");
        assertThat(second.get(3)).containsEntry("role", "tool").containsEntry("tool_call_id", "t1");
        assertThat((String) second.get(3).get("content")).contains("Nguồn ATX quá to");

        // Thẻ cấu hình cho khung chat
        assertThat(res.build().compatible()).isFalse();
        assertThat(res.build().items()).extracting(Line::name).containsExactly("Vỏ Mini / SFF", "Nguồn ATX");
        assertThat(res.build().errors()).extracting(Issue::rule).containsExactly("case.psu");
    }

    @Test
    void proposeBuildLayThongTinSanPhamTheoIdKetQuaTimKiem() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("case_compact_tower")).thenReturn(part("case_compact_tower", "case", "Vỏ Tower nhỏ gọn"));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RAM DDR4 16GB")).thenReturn(List.of(new ProductSearch.Product(
                "RAM 16GB (2x8GB) DDR4 3200", "Shop A", 1_050_000L, "https://www.google.com/search?ibp=oshop&q=ram", "https://img/ram.jpg")));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> {
            String link = inv.getArgument(2);
            return link.contains("google") ? "https://shop-a.vn/ram-16gb" : link;
        });
        // Id vỏ không có trong kết quả tìm kiếm: app tự tìm vỏ theo spec trợ lý ghi (mọi dòng phải có hàng kèm link)
        when(search.search("Vỏ mATX nhỏ")).thenReturn(List.of(new ProductSearch.Product(
                "Vỏ máy tính Xigmatek XA-22 Micro-ATX", "Shop B", 800_000L, "https://shop-b.vn/vo", null)));
        reply(toolCall("s1", "search_products", "{\"query\":\"RAM DDR4 16GB\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Học online, máy nhỏ gọn","budget_vnd":12000000,"items":[
                  {"code":"case_compact_tower","spec":"Vỏ mATX nhỏ","reason":"Vừa bàn học","price_min_vnd":700000,"price_max_vnd":1000000,"product_id":"s9-9"},
                  {"code":"ram_ddr4","quantity":2,"spec":"16 GB, 2 thanh 8 GB","reason":"Mở nhiều tab vẫn mượt","product_id":"s1-1","buy_qty":1}]}
                """), answer("Tổng khoảng 1,8 - 2,1 triệu."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học online 12 triệu")), null, null, "advice"));

        var card = res.build();
        assertThat(card.summary()).isEqualTo("Học online, máy nhỏ gọn");
        var ram = card.items().get(1);
        assertThat(ram.quantity()).isEqualTo(2);
        assertThat(ram.productName()).isEqualTo("RAM 16GB (2x8GB) DDR4 3200");
        assertThat(ram.shop()).isEqualTo("Shop A");
        assertThat(ram.priceMinVnd()).as("bộ kit: mua 1 sản phẩm").isEqualTo(1_050_000L);
        assertThat(ram.sourceUrl()).as("đã đổi sang link cửa hàng").isEqualTo("https://shop-a.vn/ram-16gb");
        assertThat(ram.imageUrl()).isEqualTo("https://img/ram.jpg");
        var vo = card.items().getFirst();
        assertThat(vo.productName()).as("id không có trong kết quả tìm kiếm: app tự tìm").isEqualTo("Vỏ máy tính Xigmatek XA-22 Micro-ATX");
        assertThat(vo.sourceUrl()).isEqualTo("https://shop-b.vn/vo");
        // Trợ lý chỉ thấy id, tên, cửa hàng, giá; không thấy link dài
        String toolResult = (String) messages(1).get(3).get("content");
        assertThat(toolResult).contains("s1-1").contains("1050000").doesNotContain("google.com");
        // Tổng do backend cộng: vỏ app tự tìm 800k + 1,05tr giá cửa hàng
        assertThat((String) messages(2).get(5).get("content")).contains("\"total_min_vnd\":1850000").contains("\"total_max_vnd\":1850000");
    }

    @Test
    void giaChiTuTrangDanhSachLaGiaThamKhao() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        var gpu = new ProductSearch.Product("VGA ASUS Dual RTX 4060 8GB", "Shop", 7_990_000L, "https://shop.vn/4060", null);
        when(search.search("VGA RTX 4060")).thenReturn(List.of(gpu));
        when(search.offer(gpu.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                gpu.title(), gpu.shop(), 9_000_000L, null, gpu.url(), null, true, 36, java.time.Instant.now()));
        when(search.pageChecked(gpu.url())).thenReturn(false);
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"VGA RTX 4060\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","summary":"Game","budget_vnd":20000000,"items":[{"code":"gpu_280","spec":"RTX 4060 8GB","product_id":"s1-1"}]}
                """), answer("Xong."));

        var response = service.reply(new ChatRequest(List.of(new Message("user", "chơi game 20 triệu")), null, null, "advice"));

        assertThat((String) messages(1).get(3).get("content")).contains("\"verified\":false");
        assertThat(response.build().items().getFirst().priceMaxVnd()).isEqualTo(7_990_000L);
        assertThat(response.build().items().getFirst().priceChecked()).isFalse();
    }

    @Test
    void giaTrangBanDuocUuTienDuGiaDanhSachSaiNhieu() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        var gpu = new ProductSearch.Product("VGA ASUS Dual RTX 4060 8GB", "Shop", 1_000_000L, "https://shop.vn/4060", null);
        when(search.search("VGA RTX 4060")).thenReturn(List.of(gpu));
        when(search.offer(gpu.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                gpu.title(), gpu.shop(), 8_000_000L, null, gpu.url(), null, true, 36, java.time.Instant.now()));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"VGA RTX 4060\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","summary":"Game","budget_vnd":20000000,"items":[{"code":"gpu_280","spec":"RTX 4060 8GB","product_id":"s1-1"}]}
                """), answer("Xong."));

        var response = service.reply(new ChatRequest(List.of(new Message("user", "chơi game 20 triệu")), null, null, "advice"));

        assertThat((String) messages(1).get(3).get("content")).contains("\"price_vnd\":8000000").contains("\"verified\":true");
        assertThat(response.build().items().getFirst().priceMaxVnd()).isEqualTo(8_000_000L);
        assertThat(response.build().items().getFirst().priceChecked()).isTrue();
    }

    @Test
    void trangDaDocNhungSaiBienTheKhongDuocGanCoGiaDaXacMinh() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        var gpu = new ProductSearch.Product("VGA ASUS Dual RTX 4060 8GB", "Shop", 7_990_000L, "https://shop.vn/4060", null);
        when(search.search("VGA RTX 4060")).thenReturn(List.of(gpu));
        when(search.offer(gpu.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "VGA ASUS Dual RTX 4060 Ti 8GB", gpu.shop(), 9_000_000L, null, gpu.url(), null, true, 36, java.time.Instant.now()));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"VGA RTX 4060\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","summary":"Game","budget_vnd":20000000,"items":[{"code":"gpu_280","spec":"RTX 4060 8GB","product_id":"s1-1"}]}
                """), answer("Xong."));

        var response = service.reply(new ChatRequest(List.of(new Message("user", "chơi game 20 triệu")), null, null, "advice"));

        assertThat(response.build().items().getFirst().priceMaxVnd()).isEqualTo(7_990_000L);
        assertThat(response.build().items().getFirst().priceChecked()).isFalse();
    }

    @Test
    void monChuaCoSanPhamThiAppTuTimVaGan() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("low_profile_47")).thenReturn(part("low_profile_47", "cooler", "Tản khí thấp 47 mm"));
        when(components.findByCode("tool_kit")).thenReturn(part("tool_kit", "tools", "Bộ đồ nghề"));
        when(search.enabled()).thenReturn(true);
        // Tìm theo thông số ra 3 món: phụ kiện rẻ bất thường, món hợp khoảng giá, món đắt; chọn món rẻ nhất trong khoảng
        when(search.search("Tản khí thấp 47 mm, socket AM4")).thenReturn(List.of(
                new ProductSearch.Product("Keo tản nhiệt", "Shop X", 40_000L, "https://x/keo", null),
                new ProductSearch.Product("Tản ID-Cooling IS-40X", "Shop B", 450_000L, "https://b/is40", "https://img/is40.jpg"),
                new ProductSearch.Product("Tản Noctua L9a", "Shop C", 1_300_000L, "https://c/l9a", null)));
        // Thông số không ra gì thì tìm theo tên loại linh kiện
        when(search.search("Bộ đồ nghề")).thenReturn(List.of(new ProductSearch.Product("Bộ tua vít 24 in 1", "Shop D", 150_000L, "https://d/tuavit", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("p1", "propose_build", """
                {"summary":"Văn phòng nhỏ","items":[
                  {"code":"low_profile_47","spec":"Tản khí thấp 47 mm, socket AM4","reason":"Vừa vỏ Mini","price_min_vnd":350000,"price_max_vnd":600000},
                  {"code":"tool_kit","spec":"Tua vít đầu từ","reason":"Để tự lắp"}]}
                """), answer("Xong."));

        var card = service.reply(new ChatRequest(List.of(new Message("user", "máy văn phòng nhỏ, thêm bộ đồ nghề")), null, null, "advice")).build();

        assertThat(card.items().get(0).productName()).isEqualTo("Tản ID-Cooling IS-40X");
        assertThat(card.items().get(0).sourceUrl()).isEqualTo("https://b/is40");
        assertThat(card.items().get(0).priceMinVnd()).isEqualTo(450_000L);
        assertThat(card.items().get(1).productName()).isEqualTo("Bộ tua vít 24 in 1");
        // Trợ lý được báo món nào app đã tự gắn, để không nói "chưa tìm được hàng"
        String toolResult = (String) messages(1).get(3).get("content");
        assertThat(toolResult).contains("auto_filled").contains("Tản ID-Cooling IS-40X").contains("\"total_min_vnd\":600000");
    }

    @Test
    void baPhuongAnTraVeOptionsVaLayPhuHopLamTheChinh() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        String item = "{\"code\":\"ram_ddr4\",\"spec\":\"16 GB\",\"reason\":\"Đủ dùng\",\"price_min_vnd\":%d,\"price_max_vnd\":%d}";
        var calls = new ArrayList<Map<String, Object>>();
        for (var t : List.of(new Object[] { "cheap", 800_000 }, new Object[] { "balanced", 1_000_000 }, new Object[] { "quality", 1_500_000 })) {
            calls.add(Map.of("id", "p-" + t[0], "type", "function", "function", Map.of("name", "propose_build",
                    "arguments", "{\"tier\":\"" + t[0] + "\",\"summary\":\"Học\",\"items\":[" + item.formatted(t[1], t[1]) + "]}")));
        }
        reply(Map.of("choices", List.of(Map.of("finish_reason", "tool_calls",
                "message", Map.of("role", "assistant", "content", "", "tool_calls", calls)))), answer("Ba phương án."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học 12 triệu")), null, null, "advice"));

        assertThat(res.options()).extracting(o -> o.tier()).containsExactly("cheap", "balanced", "quality");
        assertThat(res.options()).extracting(o -> o.items().getFirst().priceMinVnd()).containsExactly(800_000L, 1_000_000L, 1_500_000L);
        assertThat(res.build().tier()).isEqualTo("balanced");
    }

    @Test
    void motPhuongAnThiKhongCoOptions() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        reply(toolCall("p1", "propose_build", """
                {"summary":"Học","items":[{"code":"ram_ddr4","spec":"16 GB","reason":"Đủ dùng"}]}
                """), answer("Một phương án."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice"));

        assertThat(res.options()).isNull();
        assertThat(res.build().tier()).isEqualTo("balanced");
    }

    @Test
    void monThayTheCungNhomDungLoaiVaKhoangGia() {
        var chosen = new ProductSearch.Product("RAM Kingston 16GB (2x8GB) 3200", "Phong Vũ", 1_000_000L, "https://phongvu.vn/a", null);
        var group = List.of(chosen,
                new ProductSearch.Product("RAM Corsair 16GB (2x8GB) 3200", "Hacom", 1_050_000L, "https://hacom.vn/b", null),
                new ProductSearch.Product("RAM Gskill 16GB (2x8GB) 3200", "GEARVN", 900_000L, "https://gearvn.com/c", null),
                new ProductSearch.Product("RAM Kingston 8GB 3200", "X", 980_000L, "https://x.vn/d", null),
                new ProductSearch.Product("RAM 16GB 2x8GB like new", "Y", 1_000_000L, "https://y.vn/e", null),
                new ProductSearch.Product("RAM TeamGroup 16GB (2x8GB)", "Z", 2_000_000L, "https://z.vn/f", null));
        // Bộ 2 thanh: chỉ đổi sang bộ khác; bỏ hàng like new và món đắt gấp đôi; gần giá nhất trước
        assertThat(ChatService.candidates(chosen, group, true, "quality")).extracting(ProductSearch.Product::title)
                .containsExactly("RAM Corsair 16GB (2x8GB) 3200", "RAM Gskill 16GB (2x8GB) 3200");
        // Phương án Rẻ: không lấy món đắt hơn 15%; Chất lượng: không lấy món rẻ hơn 15%
        assertThat(ChatService.candidates(chosen, group, true, "cheap")).extracting(ProductSearch.Product::title)
                .containsExactly("RAM Corsair 16GB (2x8GB) 3200", "RAM Gskill 16GB (2x8GB) 3200");
        // Không phải bộ: lấy cả thanh lẻ, nhưng cửa hàng biết tên miền (Hacom, GEARVN) đứng trước shop lạ dù giá gần hơn
        assertThat(ChatService.candidates(chosen, group, false, "quality")).extracting(ProductSearch.Product::title)
                .containsExactly("RAM Corsair 16GB (2x8GB) 3200", "RAM Gskill 16GB (2x8GB) 3200", "RAM Kingston 8GB 3200");
        var cheapOnly = List.of(chosen, new ProductSearch.Product("RAM X 16GB (2x8GB)", "S", 800_000L, "https://s.vn/x", null));
        assertThat(ChatService.candidates(chosen, cheapOnly, true, "quality")).isEmpty();
    }

    @Test
    void moiPhuongAnChiMotBuild() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD 1TB")).thenReturn(List.of(
                new ProductSearch.Product("SSD A 1TB", "Shop A", 1_500_000L, "https://a.vn/1", null),
                new ProductSearch.Product("SSD B 2TB", "Shop B", 1_850_000L, "https://b.vn/1", null),
                new ProductSearch.Product("SSD C 1TB", "Shop C", 1_100_000L, "https://c.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        // Ba tier ba sản phẩm (Chất lượng phải đắt hơn và khác Phù hợp, Rẻ rẻ hơn rõ rệt)
        var calls = new ArrayList<Map<String, Object>>();
        for (String[] t : List.of(new String[] { "cheap", "s1-1" }, new String[] { "balanced", "s1-1" }, new String[] { "quality", "s1-2" })) {
            String capacity = "quality".equals(t[0]) ? "2 TB" : "1 TB";
            String item = "{\"code\":\"ssd_m2_2280\",\"spec\":\"" + capacity + "\",\"reason\":\"Đủ chỗ\",\"product_id\":\"" + t[1] + "\"}";
            calls.add(Map.of("id", "p-" + t[0], "type", "function", "function", Map.of("name", "propose_build",
                    "arguments", "{\"tier\":\"" + t[0] + "\",\"summary\":\"Học\",\"items\":[" + item + "]}")));
        }
        reply(toolCall("s", "search_products", "{\"query\":\"SSD 1TB\"}"),
                Map.of("choices", List.of(Map.of("finish_reason", "tool_calls", "message", Map.of("role", "assistant", "content", "", "tool_calls", calls)))),
                answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học 12 triệu")), null, null, "advice"));

        // Mỗi phương án (Rẻ, Phù hợp, Chất lượng) đúng một build: trả lời nhanh hơn, không ghép option tham khảo
        assertThat(res.options()).hasSize(3);
        assertThat(res.options()).extracting(ChatResponse.BuildCard::tier).containsExactly("cheap", "balanced", "quality");
        assertThat(res.options()).extracting(ChatResponse.BuildCard::variant).containsOnly(1);
        // Phương án Rẻ do code chọn hàng: cùng 1TB nhưng rẻ hơn
        assertThat(res.options()).extracting(o -> o.items().getFirst().productName()).containsExactly("SSD C 1TB", "SSD A 1TB", "SSD B 2TB");
        assertThat(res.build().variant()).isEqualTo(1);
        assertThat(res.build().tier()).isEqualTo("balanced");
    }

    @Test
    void monKhongCoLinkCuaHangThiDoiSangMonCungNhomCoLink() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RTX 3060")).thenReturn(List.of(
                new ProductSearch.Product("RTX 3060 12GB Shop Nho", "Tân Thành", 7_000_000L, "https://www.google.com/x", null),
                new ProductSearch.Product("RTX 3060 12GB Phong Vu", "Phong Vũ", 7_200_000L, "https://www.google.com/y", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> "Phong Vũ".equals(inv.getArgument(1)) ? "https://phongvu.vn/rtx3060" : null);
        reply(toolCall("s", "search_products", "{\"query\":\"RTX 3060\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"gpu_280","spec":"RTX 3060","reason":"Chơi game","product_id":"s1-1"}]}
                """), answer("Xong."));

        var line = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice")).build().items().getFirst();

        assertThat(line.productName()).isEqualTo("RTX 3060 12GB Phong Vu");
        assertThat(line.sourceUrl()).isEqualTo("https://phongvu.vn/rtx3060");
        assertThat(line.priceMinVnd()).isEqualTo(7_200_000L);
    }

    @Test
    void linkChiLaTrangChuThiDoiSangMonCoTrangSanPham() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RTX 3060")).thenReturn(List.of(
                new ProductSearch.Product("RTX 3060 12GB Shop Nho", "hotgear.vn", 7_000_000L, "https://www.google.com/x", null),
                new ProductSearch.Product("RTX 3060 12GB Phong Vu", "Phong Vũ", 7_200_000L, "https://www.google.com/y", null)));
        // Shop nhỏ tìm không ra trang sản phẩm nên chỉ có trang chủ
        when(search.directLink(any(), any(), any())).thenAnswer(inv ->
                "Phong Vũ".equals(inv.getArgument(1)) ? "https://phongvu.vn/rtx3060" : "https://hotgear.vn/");
        reply(toolCall("s", "search_products", "{\"query\":\"RTX 3060\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"gpu_280","spec":"RTX 3060","reason":"Chơi game","product_id":"s1-1"}]}
                """), answer("Xong."));

        var line = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice")).build().items().getFirst();

        assertThat(line.productName()).isEqualTo("RTX 3060 12GB Phong Vu");
        assertThat(line.sourceUrl()).isEqualTo("https://phongvu.vn/rtx3060");
    }

    @Test
    void phuongAnReKhongBaoGioVuotNganSach() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        // Ngân sách 1,5 triệu nhưng trợ lý chọn món 1,6 triệu cho phương án Rẻ: code đổi sang món rẻ nhất cùng dung lượng
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("SSD A 512GB", "A", 1_600_000L, "https://a.vn/1", null),
                new ProductSearch.Product("SSD B 512GB", "B", 1_400_000L, "https://b.vn/1", null),
                new ProductSearch.Product("SSD C 512GB", "C", 900_000L, "https://c.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"),
                toolCall("p1", "propose_build", """
                {"tier":"cheap","summary":"Học","budget_vnd":1500000,"items":[{"code":"ssd_m2_2280","spec":"SSD","reason":"Đủ","product_id":"s1-1"}]}
                """), toolCall("p2", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":1500000,"items":[{"code":"ssd_m2_2280","spec":"SSD","reason":"Đủ","product_id":"s1-2"}]}
                """), answer("Xong."), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice"));

        // Rẻ tối đa 90% ngân sách (1,35 triệu): code đổi sang SSD C
        var cheap = res.options().stream().filter(o -> "cheap".equals(o.tier())).toList();
        assertThat(cheap).extracting(o -> o.items().getFirst().productName()).containsExactly("SSD C 512GB");
        assertThat(cheap).extracting(ChatResponse.BuildCard::variant).containsExactly(1);
        assertThat(cheap).allSatisfy(o -> assertThat(o.items().getFirst().priceMaxVnd()).isLessThanOrEqualTo(1_350_000L));
        // Phù hợp không vượt ngân sách
        var balanced = res.options().stream().filter(o -> "balanced".equals(o.tier())).toList();
        assertThat(balanced.getFirst().items().getFirst().priceMaxVnd()).isLessThanOrEqualTo(1_500_000L);
    }

    @Test
    void doiMonLayLinkKhongDayPhuongAnQuaNganSach() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        // Món trợ lý chọn không có link; món giá gần nhất (B) làm vượt ngân sách nên phải lấy món rẻ hơn (C)
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("SSD A 512GB", "A", 950_000L, "https://www.google.com/a", null),
                new ProductSearch.Product("SSD B 512GB", "B", 1_020_000L, "https://b.vn/1", null),
                new ProductSearch.Product("SSD C 512GB", "C", 870_000L, "https://c.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> "SSD A 512GB".equals(inv.getArgument(0)) ? null : inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"),
                toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":1000000,"items":[{"code":"ssd_m2_2280","spec":"SSD","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice"));

        assertThat(res.build().items().getFirst().productName()).isEqualTo("SSD C 512GB");
        assertThat(res.build().items().getFirst().sourceUrl()).isEqualTo("https://c.vn/1");
    }

    @Test
    void aiLoiOVongCuoiVanGuiPhuongAnDaDung() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD")).thenReturn(List.of(new ProductSearch.Product("SSD A 512GB", "A", 1_000_000L, "https://a.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        when(client.complete(any())).thenReturn(toolCall("s", "search_products", "{\"query\":\"SSD\"}"),
                toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","items":[{"code":"ssd_m2_2280","spec":"SSD","reason":"Đủ","product_id":"s1-1"}]}
                """)).thenThrow(new AiUnavailableException(HttpStatus.BAD_GATEWAY, "AI lỗi"));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice"));

        assertThat(res.reply()).startsWith(ChatService.BUILDS_READY);
        assertThat(res.build().items().getFirst().productName()).isEqualTo("SSD A 512GB");
    }

    @Test
    void aiLoiNgayVongDauThiBaoLoi() {
        when(client.complete(any())).thenThrow(new AiUnavailableException(HttpStatus.BAD_GATEWAY, "AI lỗi"));
        assertThatThrownBy(() -> service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice")))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void serperHetLuotThiTamTatTimKiem() {
        assertThat(ProductSearch.accountProblem(400, "{\"message\":\"Not enough credits\",\"statusCode\":400}")).isTrue();
        assertThat(ProductSearch.accountProblem(403, "")).isTrue();
        assertThat(ProductSearch.accountProblem(400, "{\"message\":\"bad query\"}")).isFalse();
    }

    @Test
    void boDdr5MaRamDdr4ThiBaoLoiVaBatTroLyChonLai() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("mb_atx")).thenReturn(part("mb_atx", "motherboard", "Bo mạch chủ ATX"));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        when(search.enabled()).thenReturn(true);
        when(search.search("B760")).thenReturn(List.of(
                new ProductSearch.Product("Mainboard MSI B760 GAMING PLUS WIFI DDR5", "Hacom", 4_300_000L, "https://hacom.vn/b760", null)));
        when(search.search("RAM DDR4")).thenReturn(List.of(
                new ProductSearch.Product("RAM Corsair Vengeance LPX 16GB DDR4 3200", "FPT", 1_600_000L, "https://fptshop.com.vn/ram", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s1", "search_products", "{\"query\":\"B760\"}"),
                toolCall("s2", "search_products", "{\"query\":\"RAM DDR4\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"mb_atx","spec":"B760","reason":"Bền","product_id":"s1-1"},
                {"code":"ram_ddr4","spec":"16 GB DDR4","reason":"Đủ","product_id":"s2-1"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice"));

        // Sản phẩm thật lắp không được (dù danh mục đều DDR4): không gửi thẻ, câu trả lời nói rõ
        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("Phương án Phù hợp: chưa tìm được cấu hình lắp chung được");
        // Trợ lý được báo lỗi để chọn lại, và thấy thông số khi tìm
        String toolResults = sent.getLast().get("messages").toString();
        assertThat(toolResults).contains("product_errors").contains("chipset B760, DDR5");
    }

    @Test
    void giaTrenTheLaGiaDocTuTrangCuaHang() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD")).thenReturn(List.of(new ProductSearch.Product("SSD Kingston NV2 1TB", "A", 1_400_000L, "https://a.vn/ssd", null)));
        when(search.search("Card")).thenReturn(List.of(new ProductSearch.Product("Card RTX 4060 8GB", "B", 8_000_000L, "https://b.vn/gpu", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        var at = java.time.Instant.parse("2026-10-20T10:00:00Z");
        // Trang SSD bán giá khác Google Shopping: thẻ dùng giá trang
        when(search.offer("https://a.vn/ssd")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "SSD Kingston NV2 1TB", "A", 1_550_000L, null, "https://a.vn/ssd", null, true, 36, at));
        // Trang card ghi giá sai đơn vị (x100): bỏ, giữ giá cũ và ghi là chưa kiểm tra
        when(search.offer("https://b.vn/gpu")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "Card RTX 4060 8GB", "B", 800_000_000L, null, "https://b.vn/gpu", null, true, null, at));
        reply(toolCall("s1", "search_products", "{\"query\":\"SSD\"}"), toolCall("s2", "search_products", "{\"query\":\"Card\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"ssd_m2_2280","spec":"SSD 1TB","reason":"Nhanh","product_id":"s1-1"},
                {"code":"gpu_280","spec":"RTX 4060","reason":"Chơi game","product_id":"s2-1"}]}
                """), answer("Xong."));

        var items = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice")).build().items();

        var ssd = items.stream().filter(i -> i.category().equals("ssd")).findFirst().orElseThrow();
        assertThat(ssd.priceMinVnd()).isEqualTo(1_550_000L);
        assertThat(ssd.priceChecked()).isTrue();
        var gpu = items.stream().filter(i -> i.category().equals("gpu")).findFirst().orElseThrow();
        assertThat(gpu.priceMinVnd()).isEqualTo(8_000_000L);
    }

    @Test
    void chiChotMonConHangVaTuTimThemKhiNhomKhongCoLink() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(components.findByCode("case_gaming")).thenReturn(part("case_gaming", "case", "Vỏ"));
        when(search.enabled()).thenReturn(true);
        var at = java.time.Instant.parse("2026-10-20T10:00:00Z");
        // SSD: món trợ lý chọn đã hết hàng, món cùng nhóm còn hàng -> đổi
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("SSD A 1TB", "A", 1_500_000L, "https://a.vn/ssd-a", null),
                new ProductSearch.Product("SSD B 1TB", "B", 1_520_000L, "https://b.vn/ssd-b", null)));
        when(search.offer("https://a.vn/ssd-a")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "SSD A 1TB", "A", 1_500_000L, null, "https://a.vn/ssd-a", null, false, null, at));
        when(search.offer("https://b.vn/ssd-b")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "SSD B 1TB", "B", 1_520_000L, null, "https://b.vn/ssd-b", null, true, null, at));
        // Vỏ: cả nhóm không có trang bán -> tìm thêm theo thông số và lấy món có link
        when(search.search("Vỏ kính")).thenReturn(List.of(new ProductSearch.Product("Vỏ Jonsbo D32", "X", 1_300_000L, "https://www.google.com/x", null)));
        when(search.search("Vỏ mid tower mặt kính")).thenReturn(List.of(
                new ProductSearch.Product("Vỏ Montech Air 100", "C", 1_350_000L, "https://c.vn/vo", null)));
        when(search.offer("https://c.vn/vo")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "Vỏ Montech Air 100", "C", 1_350_000L, null, "https://c.vn/vo", null, true, null, at));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> {
            String url = inv.getArgument(2);
            return url.contains("google") ? null : url;
        });
        reply(toolCall("s1", "search_products", "{\"query\":\"SSD\"}"), toolCall("s2", "search_products", "{\"query\":\"Vỏ kính\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"ssd_m2_2280","spec":"SSD 1TB","reason":"Nhanh","product_id":"s1-1"},
                {"code":"case_gaming","spec":"Vỏ mid tower mặt kính","reason":"Đẹp","product_id":"s2-1"}]}
                """), answer("Xong."));

        var items = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice")).build().items();

        var ssd = items.stream().filter(i -> i.category().equals("ssd")).findFirst().orElseThrow();
        assertThat(ssd.productName()).isEqualTo("SSD B 1TB");
        assertThat(ssd.inStock()).isTrue();
        var pcCase = items.stream().filter(i -> i.category().equals("case")).findFirst().orElseThrow();
        assertThat(pcCase.productName()).isEqualTo("Vỏ Montech Air 100");
        assertThat(pcCase.sourceUrl()).isEqualTo("https://c.vn/vo");
    }

    @Test
    void tierReVaChatLuongLayThongSoLyDoTuPhuHopKhiBoTrong() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        String balanced = "{\"code\":\"ssd_m2_2280\",\"spec\":\"1 TB NVMe\",\"reason\":\"Mở game nhanh\"}";
        String cheap = "{\"code\":\"ssd_m2_2280\"}";
        var calls = new ArrayList<Map<String, Object>>();
        calls.add(Map.of("id", "p-c", "type", "function", "function", Map.of("name", "propose_build",
                "arguments", "{\"tier\":\"cheap\",\"summary\":\"Game\",\"items\":[" + cheap + "]}")));
        calls.add(Map.of("id", "p-b", "type", "function", "function", Map.of("name", "propose_build",
                "arguments", "{\"tier\":\"balanced\",\"summary\":\"Game\",\"items\":[" + balanced + "]}")));
        reply(Map.of("choices", List.of(Map.of("finish_reason", "tool_calls", "message", Map.of("role", "assistant", "content", "", "tool_calls", calls)))),
                answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice"));

        var cheapLine = res.options().stream().filter(o -> "cheap".equals(o.tier())).findFirst().orElseThrow().items().getFirst();
        assertThat(cheapLine.spec()).isEqualTo("1 TB NVMe");
        assertThat(cheapLine.reason()).isEqualTo("Mở game nhanh");
    }

    @Test
    void sauHaiVongTimThiPhaiChot() {
        when(search.enabled()).thenReturn(true);
        when(search.search(any())).thenReturn(List.of());
        reply(toolCall("s1", "search_products", "{\"query\":\"SSD\"}"), toolCall("s2", "search_products", "{\"query\":\"RAM\"}"),
                toolCall("s3", "search_products", "{\"query\":\"Card\"}"), answer("Xong."));

        service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice"));

        // Lần tìm thứ 3 (trợ lý vẫn gọi dù công cụ đã bị bỏ) không chạy
        org.mockito.Mockito.verify(search, org.mockito.Mockito.never()).search("Card");
        assertThat(sent.get(3).get("messages").toString()).contains("Đã hết lượt tìm");
        // Vòng 3 (sau 2 vòng tìm) không còn công cụ search_products
        assertThat(sent.get(1).get("tools").toString()).contains("name=search_products");
        assertThat(sent.get(2).get("tools").toString()).doesNotContain("name=search_products");
    }

    @Test
    void sauBaVongDeXuatThiChiConVietCauTraLoi() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        String propose = """
                {"tier":"balanced","summary":"Học","items":[{"code":"ssd_m2_2280","spec":"SSD","reason":"Đủ"}]}
                """;
        reply(toolCall("p1", "propose_build", propose), toolCall("p2", "propose_build", propose),
                toolCall("p3", "propose_build", propose), toolCall("p4", "propose_build", propose), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice"));

        assertThat(res.reply()).startsWith("Xong.");
        // Vòng 4: không cho gọi công cụ nữa; lời gọi thứ 4 bị từ chối
        assertThat(sent.get(3).get("tool_choice")).isEqualTo("none");
        assertThat(sent.get(4).get("messages").toString()).contains("Đã chốt đủ phương án");
    }

    @Test
    void lanSuaChiGuiVaiMonThiGiuCacMonCon() {
        // Ca thật: lần sửa trợ lý chỉ gửi món đã đổi, phương án mất nguồn
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(components.findByCode("psu_atx")).thenReturn(part("psu_atx", "psu", "Nguồn"));
        reply(toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","items":[{"code":"ssd_m2_2280","spec":"SSD 512GB","reason":"Đủ"},
                {"code":"psu_atx","spec":"450W","reason":"Đủ"}]}
                """), toolCall("p2", "propose_build", """
                {"tier":"balanced","summary":"Học","items":[{"code":"ssd_m2_2280","spec":"SSD 256GB","reason":"Rẻ hơn"}]}
                """), answer("Xong."));

        var items = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice")).build().items();

        assertThat(items).extracting(Line::category).containsExactlyInAnyOrder("ssd", "psu");
        assertThat(items).filteredOn(i -> i.category().equals("ssd")).extracting(Line::spec).containsExactly("SSD 256GB");
    }

    @Test
    void phuongAnVuotTranThiCodeDoiHangCungThongSoReHon() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("SSD Samsung 990 EVO 1TB", "A", 2_500_000L, "https://a.vn/1", null),
                new ProductSearch.Product("SSD Kingston NV3 1TB", "B", 1_600_000L, "https://b.vn/1", null),
                new ProductSearch.Product("SSD Kingston NV3 512GB", "C", 900_000L, "https://c.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":2000000,"items":[{"code":"ssd_m2_2280","spec":"SSD 1TB","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var line = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice")).build().items().getFirst();

        // Vượt ngân sách 2 triệu: đổi sang SSD cùng 1TB rẻ hơn, không hạ xuống 512GB
        assertThat(line.productName()).isEqualTo("SSD Kingston NV3 1TB");
    }

    @Test
    void appTuTimBoMachKhopSocketCpu() {
        // Ca thật: dòng bo gắn nhầm cái vỏ, app tự tìm ra bo AM4 cho CPU LGA1700
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("cpu_std")).thenReturn(part("cpu_std", "cpu", "CPU"));
        when(components.findByCode("mb_atx")).thenReturn(part("mb_atx", "motherboard", "Bo mạch chủ ATX"));
        when(search.enabled()).thenReturn(true);
        when(search.search("i5")).thenReturn(List.of(
                new ProductSearch.Product("Bộ vi xử lý Intel Core i5-12400F", "GEARVN", 3_380_000L, "https://gearvn.com/products/i5", null)));
        when(search.search("vỏ")).thenReturn(List.of(
                new ProductSearch.Product("Vỏ Máy Tính Xigmatek XA-22 (ATX)", "GEARVN", 350_000L, "https://gearvn.com/products/xa22", null)));
        when(search.search(org.mockito.ArgumentMatchers.argThat(q -> q != null && (q.contains("ATX, DDR4") || q.contains("mainboard")))))
                .thenReturn(List.of(
                        new ProductSearch.Product("Bo Mạch Chủ Gigabyte A520M-K V2", "GEARVN", 1_590_000L, "https://gearvn.com/products/a520", null),
                        new ProductSearch.Product("Bo mạch chủ MSI PRO B760-P DDR4 ATX", "GEARVN", 2_790_000L, "https://gearvn.com/products/b760", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s1", "search_products", "{\"query\":\"i5\"}"), toolCall("s2", "search_products", "{\"query\":\"vỏ\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"cpu_std","spec":"i5-12400F","reason":"Game","product_id":"s1-1"},
                {"code":"mb_atx","spec":"ATX, DDR4, 4 khe RAM","reason":"Đủ","product_id":"s2-1"}]}
                """), answer("Xong."));

        var items = service.reply(new ChatRequest(List.of(new Message("user", "tìm bo phù hợp cho CPU này")), null, null, "advice")).build().items();

        assertThat(items).filteredOn(i -> i.category().equals("motherboard")).extracting(Line::productName)
                .containsExactly("Bo mạch chủ MSI PRO B760-P DDR4 ATX");
    }

    @Test
    void cpuCoDoHoaTichHopThiKhongBatBuocCardRoi() {
        var gpuError = new Issue("required.gpu", "CPU này không có sẵn đồ họa, cần thêm card đồ họa");
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(false, List.of(gpuError), List.of()));
        when(components.findByCode("cpu_std")).thenReturn(part("cpu_std", "cpu", "CPU"));
        when(search.enabled()).thenReturn(true);
        when(search.search("i3")).thenReturn(List.of(
                new ProductSearch.Product("Bộ vi xử lý Intel Core i3-12100", "A", 2_500_000L, "https://a.vn/i3", null),
                new ProductSearch.Product("Bộ vi xử lý Intel Core i3-12100F", "B", 2_000_000L, "https://b.vn/i3f", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"i3\"}"),
                toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","items":[{"code":"cpu_std","spec":"i3","reason":"Đủ","product_id":"s1-1"}]}
                """), toolCall("p2", "propose_build", """
                {"tier":"cheap","summary":"Học","items":[{"code":"cpu_std","spec":"i3","reason":"Đủ","product_id":"s1-2"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice"));

        // i3-12100 có đồ họa tích hợp: lắp được, kèm lưu ý; i3-12100F không có: thiếu card nên phương án Rẻ không
        // hiện (luật cứng: không gửi cấu hình lắp không được), câu trả lời nói rõ
        var balanced = res.build();
        assertThat(balanced.tier()).isEqualTo("balanced");
        assertThat(balanced.compatible()).isTrue();
        assertThat(balanced.warnings()).anySatisfy(i -> assertThat(i.rule()).isEqualTo("cpu.igpu"));
        assertThat(res.options()).as("chỉ còn một phương án").isNull();
        assertThat(res.reply()).contains("Phương án Rẻ: chưa tìm được cấu hình lắp chung được");
    }

    @Test
    void hetHangThiTimDungSanPhamOCuaHangKhac() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        var at = java.time.Instant.parse("2026-10-21T10:00:00Z");
        var pick = new ProductSearch.Product("Card Gigabyte RTX 4060 WINDFORCE OC 8G", "GEARVN", 7_900_000L, "https://gearvn.com/products/4060", null);
        when(search.search("RTX 4060")).thenReturn(List.of(pick,
                new ProductSearch.Product("Card MSI RTX 4060 Ventus 2X 8G", "GEARVN", 8_000_000L, "https://gearvn.com/products/msi-4060", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        // GEARVN hết hàng card đã chọn; An Phát còn đúng card đó; card MSI khác cũng còn hàng nhưng là sản phẩm khác
        when(search.offer("https://gearvn.com/products/4060")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                pick.title(), "GEARVN", 7_900_000L, null, pick.url(), null, false, null, at));
        when(search.offer("https://gearvn.com/products/msi-4060")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "Card MSI RTX 4060 Ventus 2X 8G", "GEARVN", 8_000_000L, null, "https://gearvn.com/products/msi-4060", null, true, null, at));
        var anphat = new ProductSearch.Product("VGA Gigabyte RTX 4060 WINDFORCE OC 8GB", "An Phát", 7_950_000L, "https://www.anphatpc.com.vn/gigabyte-4060.html", null);
        when(search.sameModelElsewhere(any())).thenReturn(List.of(anphat));
        when(search.offer(anphat.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                anphat.title(), "An Phát", 7_950_000L, null, anphat.url(), null, true, 36, at));
        reply(toolCall("s", "search_products", "{\"query\":\"RTX 4060\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"gpu_280","spec":"RTX 4060","reason":"Chơi game","product_id":"s1-1"}]}
                """), answer("Xong."));

        var line = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice")).build().items().getFirst();

        assertThat(line.productName()).isEqualTo("VGA Gigabyte RTX 4060 WINDFORCE OC 8GB");
        assertThat(line.shop()).isEqualTo("An Phát");
        assertThat(line.inStock()).isTrue();
    }

    @Test
    void sanPhamSaiLoaiBiBoVaTimLai() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("tower_air_155")).thenReturn(part("tower_air_155", "cooler", "Tản khí dạng tháp"));
        when(search.enabled()).thenReturn(true);
        // Trợ lý gắn nhầm bộ quạt case vào dòng tản nhiệt
        when(search.search("quạt")).thenReturn(List.of(
                new ProductSearch.Product("Bộ 3 quạt Cooler Master SickleFlow 120 ARGB", "GEARVN", 690_000L, "https://gearvn.com/products/fan", null)));
        when(search.search("Tản khí tháp")).thenReturn(List.of(
                new ProductSearch.Product("Tản nhiệt khí Cooler Master Hyper 212", "GEARVN", 590_000L, "https://gearvn.com/products/hyper", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"quạt\"}"),
                toolCall("p1", "propose_build", """
                {"summary":"Game","items":[{"code":"tower_air_155","spec":"Tản khí tháp","reason":"Mát","product_id":"s1-1"}]}
                """), answer("Xong."));

        var line = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice")).build().items().getFirst();

        assertThat(line.productName()).isEqualTo("Tản nhiệt khí Cooler Master Hyper 212");
    }

    @Test
    void tranGiaTheoPhuongAn() {
        // Rẻ: tối đa 90% ngân sách; Phù hợp: đúng ngân sách; Chất lượng: + 3 triệu
        assertThat(ChatService.tierCap(25_000_000L, "cheap")).isEqualTo(22_500_000L);
        assertThat(ChatService.tierCap(25_000_000L, "balanced")).isEqualTo(25_000_000L);
        assertThat(ChatService.tierCap(25_000_000L, "quality")).isEqualTo(28_000_000L);
        assertThat(ChatService.tierCap(null, "quality")).isNull();
    }

    @Test
    void tenNganVaNhanBietTrangChu() {
        assertThat(ProductSearch.shortTitle("CPU Intel Core i5-12400F (Up to 4.4GHz, 6 nhân 12 luồng) - Chính hãng"))
                .isEqualTo("CPU Intel Core i5-12400F");
        assertThat(ProductSearch.shortTitle("Card màn hình MSI GeForce RTX 4060 Ventus 2X Black 8G OC"))
                .isEqualTo("MSI GeForce RTX 4060 Ventus 2X Black 8G");
        // v7: tên dài mất mã model 4060 khi cắt 8 từ
        assertThat(ProductSearch.shortTitle("Card màn hình/ VGA Asus Dual GeForce RTX 4060 OC Edition 8GB GDDR6")).contains("4060");
        assertThat(ProductSearch.shortTitle("(Hot) RAM")).isEqualTo("(Hot) RAM");
        assertThat(ProductSearch.homePage("https://hotgear.vn/")).isTrue();
        assertThat(ProductSearch.homePage("https://hotgear.vn")).isTrue();
        assertThat(ProductSearch.homePage("https://phongvu.vn/rtx3060")).isFalse();
    }

    @Test
    void phuongAnPhuHopKhongVuotNganSach() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        // Ngân sách 1 triệu, trợ lý chọn món 1 triệu: món 1,2 triệu (gần giá nhất) làm vượt ngân sách nên phải dùng món rẻ hơn
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("SSD A 512GB", "A", 1_000_000L, "https://a.vn/1", null),
                new ProductSearch.Product("SSD Dat 512GB", "B", 1_200_000L, "https://b.vn/1", null),
                new ProductSearch.Product("SSD Re 512GB", "C", 850_000L, "https://c.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"),
                toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":1000000,"items":[{"code":"ssd_m2_2280","spec":"SSD","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice"));

        // Build trợ lý chọn nằm trong ngân sách: giữ nguyên, không thêm build tham khảo
        assertThat(res.options()).isNull();
        assertThat(res.build().items().getFirst().productName()).isEqualTo("SSD A 512GB");
    }

    @Test
    void nhanTenCuaHangRaTenMien() {
        assertThat(ProductSearch.domainOf("Phong Vũ")).isEqualTo("phongvu.vn");
        assertThat(ProductSearch.domainOf("Nguyễn Vũ Store")).isEqualTo("nguyenvu.store");
        assertThat(ProductSearch.domainOf("Fptshop.com.vn")).isEqualTo("fptshop.com.vn");
        assertThat(ProductSearch.domainOf("GEARVN")).isEqualTo("gearvn.com");
        assertThat(ProductSearch.domainOf("Cửa hàng lạ")).isNull();
        assertThat(ProductSearch.isGoogle("https://www.google.com/search?ibp=oshop&q=ram")).isTrue();
        assertThat(ProductSearch.isGoogle("https://phongvu.vn/ram-16gb")).isFalse();
    }

    @Test
    void chonSanPhamTheoKhoangGia() {
        var junk = new ProductSearch.Product("Phụ kiện", "S", 30_000L, "https://a", null);
        var mid = new ProductSearch.Product("Hợp", "S", 500_000L, "https://b", null);
        var noPrice = new ProductSearch.Product("Không giá", "S", null, "https://c", null);
        assertThat(ChatService.choose(List.of(junk, mid), 400_000L, 600_000L)).isEqualTo(mid);
        assertThat(ChatService.choose(List.of(noPrice, junk), null, null)).as("không có ước tính: món có giá đầu tiên").isEqualTo(junk);
        assertThat(ChatService.choose(List.of(noPrice), 400_000L, null)).as("không món nào có giá").isEqualTo(noPrice);
        assertThat(ChatService.choose(List.of(), 1L, 2L)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void chuaCoKhoaTimKiemThiKhongDuaCongCuTimKiem() {
        reply(answer("Ok."));

        service.reply(new ChatRequest(List.of(new Message("user", "?")), null, null, "advice"));

        var tools = (List<Map<String, Object>>) sent.getFirst().get("tools");
        assertThat(tools).extracting(t -> ((Map<String, Object>) t.get("function")).get("name")).doesNotContain("search_products");
        assertThat((String) messages(0).getFirst().get("content")).contains("tìm kiếm sản phẩm đang tắt");
    }

    @Test
    void modelVanGoiTimKiemKhiTatThiNhanLoiNhac() {
        reply(toolCall("s1", "search_products", "{\"query\":\"SSD\"}"), answer("Ok."));

        service.reply(new ChatRequest(List.of(new Message("user", "?")), null, null, "advice"));

        assertThat((String) messages(1).get(3).get("content")).contains("Chưa bật tìm kiếm");
    }

    @Test
    void docGiaTienDong() {
        assertThat(ProductSearch.vnd("1.290.000 ₫")).isEqualTo(1_290_000L);
        assertThat(ProductSearch.vnd("₫1,290,000")).isEqualTo(1_290_000L);
        assertThat(ProductSearch.vnd("1290000.00")).isEqualTo(1_290_000L);
        assertThat(ProductSearch.vnd("$129")).as("không phải tiền đồng").isNull();
    }

    @Test
    void congCuLoiThiBaoLaiChoModelThayViNemLoi() {
        reply(toolCall("t1", "khong_ton_tai", "{}"), answer("Ok."));

        service.reply(new ChatRequest(List.of(new Message("user", "?")), null));

        assertThat((String) messages(1).get(3).get("content")).startsWith("Lỗi:");
    }

    @Test
    void thamSoKhongPhaiJsonCungKhongLamHongCuocChat() {
        reply(toolCall("t1", "check_build", "{items: hỏng"), answer("Ok."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "?")), null));

        assertThat(res.reply()).isEqualTo("Ok.");
        assertThat((String) messages(1).get(3).get("content")).startsWith("Lỗi:");
    }

    @Test
    void ghiBuocDangLapVaoSystemPrompt() {
        reply(answer("Ok."));

        service.reply(new ChatRequest(List.of(new Message("user", "CPU không nằm phẳng")), null, "Bước 3/10: Đặt CPU vào socket"));

        assertThat((String) messages(0).getFirst().get("content")).contains("hiện ở: Bước 3/10: Đặt CPU vào socket");
    }

    @Test
    void trangTuVanDungLuatTuVan() {
        // Có ngân sách mà trả lời không chốt gì: app nhắc làm luôn một lần (cần câu trả lời thứ hai)
        reply(answer("Ok."), answer("Ok."));

        service.reply(new ChatRequest(List.of(new Message("user", "mình học online, có 12 triệu")), null, null, "advice"));

        String system = (String) messages(0).getFirst().get("content");
        assertThat(system).contains("trợ lý tư vấn build PC").doesNotContain("Gỡ rối khi lắp");
    }

    @Test
    void boLoiChaoMoDauCuaTroLy() {
        reply(answer("Ok."));

        service.reply(new ChatRequest(List.of(new Message("assistant", "Chào bạn, mình giúp gì được?"),
                new Message("user", "máy nóng quá")), null));

        assertThat(messages(0)).hasSize(2);
        assertThat(messages(0).get(1)).containsEntry("role", "user");
    }

    @Test
    void tinCuoiPhaiLaCauHoi() {
        assertThatThrownBy(() -> service.reply(new ChatRequest(List.of(
                new Message("user", "a"), new Message("assistant", "b")), null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---- Hồi quy từ lỗi thật vòng đo v4 (review Sol 6.1 + Astra, 21/10/2026) ---------------

    @Test
    void ramMotThanhMaSpecHaiThanhThiMuaDuHaiThanh() {
        // v4-game-1 balanced: spec "2x8GB DDR4 3200", quantity 1, hàng mua là Kingmax 1x8GB -> thẻ ghi 2x8GB nhưng chỉ có 8GB
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RAM")).thenReturn(List.of(
                new ProductSearch.Product("Ram Kingmax Blade X 1x8GB DDR4 Bus 3200Mhz", "A", 990_000L, "https://a.vn/ram", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"RAM\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Game","items":[{"code":"ram_ddr4","quantity":1,"spec":"2x8GB DDR4 3200","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var ram = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice")).build().items().getFirst();

        assertThat(ram.quantity()).as("2 thanh trong máy").isEqualTo(2);
        assertThat(ram.priceMaxVnd()).as("mua 2 thanh lẻ").isEqualTo(1_980_000L);
        assertThat(ram.spec()).startsWith("2x8GB DDR4");
    }

    @Test
    void kitHaiThanhThayThanhLeThiTinhMotBo() {
        // v4-student-1 cheap: spec "1x16GB", quantity 1, hàng mua là kit 2x8GB
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RAM")).thenReturn(List.of(
                new ProductSearch.Product("Gigabyte AORUS RGB RAM 16 GB 2 x 8 GB DDR4 3200 MHz", "A", 1_400_000L, "https://a.vn/kit", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"RAM\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","items":[{"code":"ram_ddr4","quantity":1,"spec":"1x16GB DDR4","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var ram = service.reply(new ChatRequest(List.of(new Message("user", "học")), null, null, "advice")).build().items().getFirst();

        assertThat(ram.quantity()).isEqualTo(2);
        assertThat(ram.priceMaxVnd()).as("một bộ kit").isEqualTo(1_400_000L);
        assertThat(ram.spec()).startsWith("2x8GB DDR4");
    }

    @Test
    void phuongAnVuotTranKhongHienVaCauTraLoiNoiRo() {
        // v4-video-1 quality: 47,35 triệu > trần 43 triệu vẫn được gửi. Giờ: không hiện, câu trả lời nói rõ
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        when(search.search("card")).thenReturn(List.of(
                new ProductSearch.Product("Card MSI RTX 4060 Ventus 2X 8G", "A", 30_000_000L, "https://a.vn/4060", null),
                new ProductSearch.Product("Card ASUS RTX 4090 ROG Strix 24G", "B", 50_000_000L, "https://b.vn/4090", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"card\"}"),
                toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Video","budget_vnd":40000000,"items":[{"code":"gpu_280","spec":"RTX 4060","reason":"Đủ","product_id":"s1-1"}]}
                """), toolCall("p2", "propose_build", """
                {"tier":"quality","summary":"Video","budget_vnd":40000000,"items":[{"code":"gpu_280","spec":"RTX 4090","reason":"Mạnh","product_id":"s1-2"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "dựng video 40 triệu")), null, null, "advice"));

        assertThat(res.options()).as("chỉ còn balanced").isNull();
        assertThat(res.build().tier()).isEqualTo("balanced");
        assertThat(res.reply()).contains("Phương án Chất lượng: chưa tìm được").contains("43 triệu");
    }

    @Test
    void nganSachKiemTranLayTheoLoiNguoiDung() {
        assertThat(ChatService.amounts(List.of("dựng video tầm 40 triệu, card khoảng 10 triệu"))).first().isEqualTo(40_000_000L);
        assertThat(ChatService.amounts(List.of("có sẵn màn hình 3 triệu, ngân sách 20tr"))).first().isEqualTo(20_000_000L);
        assertThat(ChatService.amounts(List.of("budget 25.000.000"))).containsExactly(25_000_000L);
        assertThat(ChatService.amounts(List.of("học online"))).isEmpty();
        assertThat(ChatService.amounts(List.of("Máy game 1 triệu, RTX 4090 24GB"))).containsExactly(1_000_000L);
        assertThat(ChatService.amounts(List.of("ngân sách 0,5 triệu"))).containsExactly(500_000L);
        // Trợ lý ghi đúng khoản người dùng nói: dùng; ghi khác hay quên: lấy lời người dùng; người dùng chưa nói: số trợ lý
        assertThat(ChatService.budgetOf(List.of(40_000_000L), 40_000_000L)).isEqualTo(40_000_000L);
        assertThat(ChatService.budgetOf(List.of(40_000_000L), 50_000_000L)).isEqualTo(40_000_000L);
        assertThat(ChatService.budgetOf(List.of(40_000_000L), null)).isEqualTo(40_000_000L);
        assertThat(ChatService.budgetOf(List.of(), 30_000_000L)).isEqualTo(30_000_000L);
    }

    @Test
    void troLyGhiSaiNganSachThiVanKiemTheoLoiNguoiDung() {
        // Người dùng nói 12 triệu, trợ lý ghi budget_vnd 20 triệu: trần Phù hợp là 12 triệu
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        when(search.search("card")).thenReturn(List.of(
                new ProductSearch.Product("Card MSI RTX 4060 Ventus 2X 8G", "A", 15_000_000L, "https://a.vn/4060", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"card\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Game","budget_vnd":20000000,"items":[{"code":"gpu_280","spec":"RTX 4060","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "chơi game, có 12 triệu")), null, null, "advice"));

        assertThat(res.build()).as("15 triệu > trần 12 triệu: không hiện").isNull();
        assertThat((String) messages(2).get(5).get("content")).contains("over_budget");
        // Đủ link, lắp được mà vượt trần, không có phương án nào khác hiện: nói thật là thiếu tiền và cần khoảng bao nhiêu
        assertThat(res.reply()).contains("Phương án Phù hợp: với giá hiện tại, cấu hình đã thử cho phương án này").contains("vượt mức tối đa 12 triệu")
                .contains("Ngân sách hiện tại chưa đủ").contains("nới lên khoảng 15 triệu");
    }

    @Test
    void ketQuaProposeBuildCoDanhSachCuoiCung() {
        // Lời tư vấn và thẻ phải cùng một bản: trợ lý được xem final_items (tên, giá sau khi đổi link / đọc giá trang)
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD")).thenReturn(List.of(new ProductSearch.Product("SSD Kingston NV3 1TB", "B", 1_600_000L, "https://b.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        when(search.offer("https://b.vn/1")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "SSD Kingston NV3 1TB", "B", 1_700_000L, null, "https://b.vn/1", null, true, null, null));
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":12000000,"items":[{"code":"ssd_m2_2280","spec":"SSD 1TB","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học 12 triệu")), null, null, "advice"));

        String toolResult = (String) messages(2).get(5).get("content");
        assertThat(toolResult).contains("final_items").contains("SSD Kingston NV3 1TB").contains("\"total_max_vnd\":1700000");
        assertThat(res.build().items().getFirst().priceMaxVnd()).as("thẻ dùng đúng giá trợ lý đã thấy").isEqualTo(1_700_000L);
    }

    @Test
    void nguonKhongHaChuan80Plus() {
        assertThat(ChatService.sameSpec("psu", "Nguồn FSP Hydro G Pro 750W 80 Plus Gold", "Nguồn FSP HV Pro 750W 80 Plus Bronze")).isFalse();
        assertThat(ChatService.sameSpec("psu", "Nguồn FSP 750W 80 Plus Bronze", "Nguồn MSI MAG A750GL 750W 80 Plus Gold")).isTrue();
        assertThat(ChatService.sameSpec("psu", "Nguồn FSP 750W 80 Plus Gold", "Nguồn Xigmatek 750W")).as("không ghi chuẩn: không chặn").isTrue();
        assertThat(ChatService.psuRating("Nguồn Corsair RM850e 850W 80 Plus Gold")).isEqualTo(3);
    }

    @Test
    void khopMoHinh3dToiThieu() {
        // v4-video-1 quality: bo B650M (Micro-ATX) gắn mã mb_atx; card 3 quạt không vào máy mẫu 240 mm
        assertThat(ChatService.fitsCode("mb_atx", "Mainboard ASUS TUF GAMING B650M-PLUS WIFI")).isFalse();
        assertThat(ChatService.fitsCode("mb_atx", "Mainboard MSI PRO B650-P WIFI ATX")).isTrue();
        assertThat(ChatService.fitsCode("gpu_240", "Card MSI RTX 4070 Gaming X Trio 12G")).isFalse();
        assertThat(ChatService.fitsCode("gpu_240", "Card ASUS Dual RTX 4060 EVO OC 8GB")).isTrue();
        assertThat(ChatService.gpuFans("Card Gigabyte RTX 4060 WINDFORCE 2X OC 8G")).isEqualTo(2);
        assertThat(ChatService.gpuFans("Card Gigabyte RTX 4070 WINDFORCE 3X OC")).isEqualTo(3);
    }

    // ---- Hồi quy vòng đo v5 (review Sol 6.1 + Astra, 21/10/2026) ---------------

    @Test
    void tuChonHangKhongLayMonDauBatKeGia() {
        var godlike = new ProductSearch.Product("Mainboard MSI MEG X870E GODLIKE DDR5", "A", 29_900_000L, "https://a.vn/1", null);
        var b650 = new ProductSearch.Product("Mainboard MSI PRO B650-P WIFI ATX", "B", 4_200_000L, "https://b.vn/1", null);
        var b650m = new ProductSearch.Product("Mainboard ASUS B650M-A", "C", 3_500_000L, "https://c.vn/1", null);
        assertThat(ChatService.choose(List.of(godlike, b650), null, null)).as("không ước tính: món rẻ nhất").isEqualTo(b650);
        assertThat(ChatService.choose(List.of(godlike, b650, b650m), 3_500_000L, 3_500_000L)).isEqualTo(b650m);
        assertThat(ChatService.choose(List.of(godlike, b650), 3_500_000L, 3_500_000L)).as("trong khoảng tới +15%").isEqualTo(b650);
    }

    @Test
    void boMicroAtxThiDongBoTheoCoHangThat() {
        // v5: trợ lý chọn B550M cho dòng mb_atx, app bỏ bo rồi tự tìm ra bo 29,9 triệu. Giờ: mã theo cỡ hàng thật
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("mb_atx")).thenReturn(part("mb_atx", "motherboard", "Bo mạch chủ ATX"));
        when(components.findByCode("mb_matx")).thenReturn(part("mb_matx", "motherboard", "Bo mạch chủ Micro-ATX"));
        when(search.enabled()).thenReturn(true);
        when(search.search("bo")).thenReturn(List.of(
                new ProductSearch.Product("Bo mạch chủ ASUS PRIME B550M-A", "A", 2_300_000L, "https://a.vn/b550m", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"bo\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Game","items":[{"code":"mb_atx","spec":"B550","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var line = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice")).build().items().getFirst();

        assertThat(line.code()).isEqualTo("mb_matx");
        assertThat(line.productName()).isEqualTo("Bo mạch chủ ASUS PRIME B550M-A");
    }

    @Test
    void ramChiNoiDungLuongThiMotHayHaiThanhDeuDuocKemGhiChu() {
        assertThat(ChatService.ramLayout(List.of("máy chơi game 16GB RAM, 25 triệu"))).isNull();
        assertThat(ChatService.ramLayout(List.of("RAM 2x8GB nhé"))).isEqualTo("2x8GB");
        assertThat(ChatService.ramLayout(List.of("mình muốn 1 thanh 16GB"))).isEqualTo("1x16GB");
        assertThat(ChatService.ramLayout(List.of("16GB (2 thanh)"))).isEqualTo("2x8GB");
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RAM")).thenReturn(List.of(
                new ProductSearch.Product("RAM Kingston Fury Beast 16GB DDR4 3200", "A", 1_500_000L, "https://a.vn/ram16", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"RAM\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","items":[{"code":"ram_ddr4","quantity":2,"spec":"2x8GB DDR4","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var card = service.reply(new ChatRequest(List.of(new Message("user", "học online, RAM 16GB")), null, null, "advice")).build();

        var ram = card.items().getFirst();
        assertThat(ram.quantity()).as("1 thanh 16GB: cùng dung lượng").isEqualTo(1);
        assertThat(ram.spec()).startsWith("1x16GB");
        assertThat(card.warnings()).noneSatisfy(i -> assertThat(i.rule()).isEqualTo("RAM_LAYOUT"));
        assertThat(card.recommendations()).anySatisfy(i -> assertThat(i.message()).contains("1 thanh RAM"));
    }

    @Test
    void nguoiDungNoiRoSoThanhMaHangKhacThiBaoRo() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RAM")).thenReturn(List.of(
                new ProductSearch.Product("RAM Kingston Fury Beast 16GB DDR4 3200", "A", 1_500_000L, "https://a.vn/ram16", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"RAM\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Game","items":[{"code":"ram_ddr4","quantity":2,"spec":"2x8GB DDR4","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var card = service.reply(new ChatRequest(List.of(new Message("user", "game, RAM 2x8GB")), null, null, "advice")).build();

        // Không có thanh 8GB nào trong kết quả: dòng RAM không có hàng đúng lời người dùng nên phương án không hiện
        assertThat(card).isNull();
    }

    @Test
    void nguoiDungNoiRoSoThanhThiChonDungCoThanh() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RAM")).thenReturn(List.of(
                new ProductSearch.Product("RAM Kingston Fury Beast 16GB DDR4 3200", "A", 1_500_000L, "https://a.vn/ram16", null),
                new ProductSearch.Product("RAM Kingston Fury Beast 8GB DDR4 3200", "A", 800_000L, "https://a.vn/ram8", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"RAM\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Game","items":[{"code":"ram_ddr4","quantity":2,"spec":"16GB DDR4","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var ram = service.reply(new ChatRequest(List.of(new Message("user", "game, RAM 2x8GB")), null, null, "advice")).build()
                .items().getFirst();

        assertThat(ram.productName()).isEqualTo("RAM Kingston Fury Beast 8GB DDR4 3200");
        assertThat(ram.quantity()).isEqualTo(2);
        assertThat(ram.priceMaxVnd()).isEqualTo(1_600_000L);
    }

    // ---- Hồi quy vòng đo v6 (review Sol 6.1, 21/10/2026) ---------------

    @Test
    void mucToiThieuTheoNhuCau() {
        assertThat(ChatService.needs(List.of("Mình là sinh viên, có khoảng 12 triệu, cần máy nhỏ gọn để học online và làm Word, Excel.")))
                .isEqualTo(new ChatService.Needs(8, 256, null, true, false));
        assertThat(ChatService.needs(List.of("Mình chơi game online và thỉnh thoảng livestream, có khoảng 25 triệu")))
                .isEqualTo(new ChatService.Needs(16, 512, 8));
        assertThat(ChatService.needs(List.of("học online, cần RAM 16GB và SSD 1TB"))).isEqualTo(new ChatService.Needs(16, 1000, null, true, false));
        assertThat(ChatService.needs(List.of("chơi game 25 triệu, thích máy có kính")).glass()).isTrue();
        assertThat(ChatService.needs(List.of("xin chào"))).isNull();
    }

    @Test
    void ramMayChuKhongChoMayBan() {
        assertThat(ChatService.serverRam("Bộ Nhớ Máy Chủ Kingston KSM32ED8/32HC 32GB DDR4 3200 ECC")).isTrue();
        assertThat(ChatService.serverRam("RAM Kingston Fury Beast 16GB DDR4 3200")).isFalse();
    }

    @Test
    void vuotTranThiHaRamXuongMucToiThieuTheoNhuCau() {
        // v6 học sinh 12tr: RAM 16GB 3,39tr làm vượt trần; máy học Word dùng 8GB được
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("cpu_std")).thenReturn(part("cpu_std", "cpu", "CPU"));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        when(search.enabled()).thenReturn(true);
        when(search.search("hàng")).thenReturn(List.of(
                new ProductSearch.Product("CPU Intel Core i3-12100 Tray", "A", 3_000_000L, "https://a.vn/cpu", null),
                new ProductSearch.Product("Ram SSTC 16GB DDR4 3200MHz", "A", 3_390_000L, "https://a.vn/ram16", null)));
        // Món hạ phải xác nhận còn hàng trên trang (MVP)
        when(search.offer("https://b.vn/ram8")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "Ram Kingston Fury Beast 8GB DDR4 3200", "B", 1_200_000L, null, "https://b.vn/ram8", null, true, 36, java.time.Instant.now()));
        when(search.search("RAM DDR4 8GB")).thenReturn(List.of(
                new ProductSearch.Product("Ram Kingston Fury Beast 8GB DDR4 3200", "B", 1_200_000L, "https://b.vn/ram8", null),
                new ProductSearch.Product("Bộ Nhớ Máy Chủ Kingston KSM 8GB DDR4 ECC", "B", 900_000L, "https://b.vn/ecc8", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"hàng\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":5000000,"items":[{"code":"cpu_std","spec":"i3-12100","reason":"Đủ","product_id":"s1-1"},
                {"code":"ram_ddr4","quantity":1,"spec":"16GB DDR4","reason":"Đủ","product_id":"s1-2"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học online, Word, Excel, có 5 triệu")), null, null, "advice"));

        var ram = res.build().items().stream().filter(l -> l.category().equals("ram")).findFirst().orElseThrow();
        assertThat(ram.productName()).as("8GB máy bàn, không lấy RAM máy chủ dù rẻ hơn").isEqualTo("Ram Kingston Fury Beast 8GB DDR4 3200");
        assertThat(res.build().recommendations()).anySatisfy(i -> assertThat(i.rule()).isEqualTo("DOWNGRADE"));
        assertThat((String) messages(2).get(5).get("content")).contains("downgrades").contains("RAM 16GB -> 8GB");
    }

    @Test
    void nguoiDungNoiRoDungLuongThiKhongHa() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        when(search.enabled()).thenReturn(true);
        when(search.search("hàng")).thenReturn(List.of(new ProductSearch.Product("Ram SSTC 16GB DDR4 3200MHz", "A", 3_390_000L, "https://a.vn/ram16", null)));
        when(search.search("RAM DDR4 8GB")).thenReturn(List.of(
                new ProductSearch.Product("Ram Kingston Fury Beast 8GB DDR4 3200", "B", 1_200_000L, "https://b.vn/ram8", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"hàng\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":3000000,"items":[{"code":"ram_ddr4","quantity":1,"spec":"16GB DDR4","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học online, cần RAM 16GB, có 3 triệu")), null, null, "advice"));

        assertThat(res.build()).as("không hạ dưới 16GB người dùng yêu cầu: vượt trần thì không hiện").isNull();
    }

    @Test
    void vuotTranThiDungQuatKemCpuBox() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("cpu_std")).thenReturn(part("cpu_std", "cpu", "CPU"));
        when(components.findByCode("tower_air_155")).thenReturn(part("tower_air_155", "cooler", "Tản khí"));
        when(search.enabled()).thenReturn(true);
        when(search.search("hàng")).thenReturn(List.of(
                new ProductSearch.Product("CPU Intel Core i3 12100 Box", "A", 3_590_000L, "https://a.vn/cpu", null),
                new ProductSearch.Product("Tản nhiệt khí Jonsbo CR-1200E", "A", 250_000L, "https://a.vn/tan", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"hàng\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":3700000,"items":[{"code":"cpu_std","spec":"i3","reason":"Đủ","product_id":"s1-1"},
                {"code":"tower_air_155","spec":"Tản khí","reason":"Mát","product_id":"s1-2"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học online, 3,7 triệu")), null, null, "advice"));

        assertThat(res.build().items()).extracting(Line::code).containsExactly("cpu_std");
    }

    // ---- Hồi quy vòng đo v7 (review Sol 6.1, 21/10/2026) ---------------

    @Test
    void tuKhoaLoiChoDongTrong() {
        assertThat(ChatService.coreQuery("gpu", "RTX 4060 8GB, 2 quạt")).isEqualTo("VGA RTX 4060");
        assertThat(ChatService.coreQuery("gpu", "RTX 4060 Ti 16GB")).isEqualTo("VGA RTX 4060 TI");
        assertThat(ChatService.coreQuery("motherboard", "Mainboard b550 ATX DDR4")).isEqualTo("mainboard B550 DDR4");
        assertThat(ChatService.coreQuery("motherboard", "B760M, DDR5")).isEqualTo("mainboard B760 DDR5");
        assertThat(ChatService.coreQuery("ssd", "SSD 512GB")).isNull();
    }

    @Test
    void monHetHangKhongTimDuocMonThayThiKhongHien() {
        // v7 game-2 quality: bo MSI B550 GAMING PLUS hết hàng vẫn được hiện vì tìm món thay thất bại
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("mb_atx")).thenReturn(part("mb_atx", "motherboard", "Bo mạch chủ ATX"));
        when(search.enabled()).thenReturn(true);
        var board = new ProductSearch.Product("Bo mạch chủ MSI MPG B550 GAMING PLUS DDR4", "A", 3_000_000L, "https://a.vn/b550", null);
        when(search.search("bo")).thenReturn(List.of(board));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        when(search.offer(board.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                board.title(), "A", 3_000_000L, null, board.url(), null, false, null, null));
        reply(toolCall("s", "search_products", "{\"query\":\"bo\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Game","items":[{"code":"mb_atx","spec":"B550 ATX","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "game")), null, null, "advice"));

        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("Phương án Phù hợp: chưa tìm được");
    }

    // ---- Hồi quy vòng đo v9 (review Sol 6.1, 21/10/2026) ---------------

    @Test
    void ramHaiThanhHaXuongMotThanhCungSanPham() {
        // v9 học sinh: RAM 2 x 16GB (6,78 triệu) cho máy học Word không hạ được vì so giá từng sản phẩm, không so tiền cả dòng
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM DDR4"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RAM")).thenReturn(List.of(new ProductSearch.Product("Ram SSTC 16GB DDR4 3200MHz", "A", 3_390_000L, "https://a.vn/ram16", null)));
        when(search.offer("https://a.vn/ram16")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "Ram SSTC 16GB DDR4 3200MHz", "A", 3_390_000L, null, "https://a.vn/ram16", null, true, 36, java.time.Instant.now()));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"RAM\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":4000000,"items":[{"code":"ram_ddr4","quantity":2,"spec":"2x16GB DDR4","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var ram = service.reply(new ChatRequest(List.of(new Message("user", "học online Word Excel, có 4 triệu")), null, null, "advice"))
                .build().items().getFirst();

        assertThat(ram.quantity()).isEqualTo(1);
        assertThat(ram.priceMaxVnd()).isEqualTo(3_390_000L);
    }

    @Test
    void phuongAnKhongHienThiBaoTroLyNgay() {
        // v9: trợ lý giới thiệu phương án đã bị ẩn vì dòng thiếu link (không được báo)
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        when(search.search("card")).thenReturn(List.of(new ProductSearch.Product(
                "Card màn hình ASUS Dual GeForce RTX 4060 V2 OC Edition 8GB", "A", 7_990_000L, "https://a.vn/4060", null)));
        reply(toolCall("s", "search_products", "{\"query\":\"card\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Game","budget_vnd":25000000,"items":[{"code":"gpu_280","spec":"RTX 4060","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        service.reply(new ChatRequest(List.of(new Message("user", "game 25 triệu")), null, null, "advice"));

        assertThat((String) messages(2).get(5).get("content")).contains("not_shown").contains("gpu_280");
    }

    // ---- Hồi quy câu game (review Sol 6.1, 21/10/2026) ---------------

    @Test
    void ssdMotTbHaXuong512GbTuKetQuaTroLyDaTim() {
        // v10c game: SSD 1TB 4,79 triệu không lần nào hạ xuống 512GB (lowerTier không xét nhóm kết quả của trợ lý)
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("Ổ Cứng SSD WD Green SN3000 1TB M.2 NVMe", "A", 4_790_000L, "https://a.vn/1tb", null),
                new ProductSearch.Product("Ổ Cứng SSD WD Green SN3000 500GB M.2 NVMe", "A", 2_390_000L, "https://a.vn/500", null)));
        when(search.offer("https://a.vn/500")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                "Ổ Cứng SSD WD Green SN3000 500GB M.2 NVMe", "A", 2_390_000L, null, "https://a.vn/500", null, true, 36, java.time.Instant.now()));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Game","budget_vnd":3000000,"items":[{"code":"ssd_m2_2280","spec":"SSD 1TB","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var ssd = service.reply(new ChatRequest(List.of(new Message("user", "chơi game, có 3 triệu")), null, null, "advice"))
                .build().items().getFirst();

        assertThat(ssd.productName()).isEqualTo("Ổ Cứng SSD WD Green SN3000 500GB M.2 NVMe");
    }

    @Test
    void cardCungChipCungVramMoiLaTuongDuong() {
        var now = new ProductSearch.Product("Card ASUS Dual GeForce RTX 4060 8GB", "A", 9_200_000L, "https://a.vn/1", null);
        var msi = new ProductSearch.Product("Card MSI GeForce RTX 4060 Ventus 2X 8G", "B", 7_790_000L, "https://b.vn/1", null);
        var ti = new ProductSearch.Product("Card MSI GeForce RTX 4060 Ti Ventus 2X 8G", "B", 8_990_000L, "https://b.vn/2", null);
        var r3060 = new ProductSearch.Product("Card Gigabyte GeForce RTX 3060 12GB", "C", 7_000_000L, "https://c.vn/1", null);
        assertThat(ChatService.equivalents("gpu", now, List.of(msi, ti, r3060), false, false, false)).containsExactly(msi);
        assertThat(ChatService.vram("Card MSI GeForce RTX 4060 Ventus 2X 8G")).isEqualTo(8);
    }

    @Test
    void cardDoiSangChipManhHonReHonKhiDuVram() {
        // v11 game: Ninja RTX 3060 12GB 9,2 triệu trong mọi lần vượt trần, RTX 4060 8GB 7,79 triệu mạnh hơn mà rẻ hơn
        var now = new ProductSearch.Product("Ninja GeForce RTX 3060 12GB GDDR6", "A", 9_200_000L, "https://a.vn/3060", null);
        var r4060 = new ProductSearch.Product("Card MSI GeForce RTX 4060 Ventus 2X 8G", "B", 7_790_000L, "https://b.vn/4060", null);
        var r3050 = new ProductSearch.Product("Card MSI GeForce RTX 3050 Ventus 2X 6G", "B", 5_000_000L, "https://b.vn/3050", null);
        var r4070 = new ProductSearch.Product("Card Asus RTX 4070 Dual 12G", "B", 8_000_000L, "https://b.vn/4070", null);
        // Game Full HD (VRAM tối thiểu 8GB): được lên 4060; không xuống 3050; không nhảy quá 15 hạng (4070)
        assertThat(ChatService.equivalents("gpu", now, List.of(r4060, r3050, r4070), false, false, false, 8)).containsExactly(r4060);
        // Đồ họa / không cho đổi chip (vramMin null): chỉ cùng chip
        assertThat(ChatService.equivalents("gpu", now, List.of(r4060), false, false, false, null)).isEmpty();
    }

    @Test
    void mayHocCpuDatThiHaXuongCpuPhoThongCungSocket() {
        // v12 học sinh: trợ lý chọn CPU 5,69 triệu cho máy học Word, phương án vượt trần
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("cpu_std")).thenReturn(part("cpu_std", "cpu", "CPU"));
        when(search.enabled()).thenReturn(true);
        when(search.search("CPU")).thenReturn(List.of(
                new ProductSearch.Product("CPU Intel Core i5-13400 Box", "A", 5_690_000L, "https://a.vn/i5", null)));
        when(search.search("CPU Intel Core i3 12100")).thenReturn(List.of(
                new ProductSearch.Product("CPU Intel Core i3-12100F Box", "B", 2_000_000L, "https://b.vn/i3f", null),
                new ProductSearch.Product("CPU Intel Core i3-12100 Box", "B", 2_900_000L, "https://b.vn/i3", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"CPU\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":3000000,"items":[{"code":"cpu_std","spec":"i5-13400","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));

        var cpu = service.reply(new ChatRequest(List.of(new Message("user", "học online, Word, Excel, có 3 triệu")), null, null, "advice"))
                .build().items().getFirst();

        assertThat(cpu.productName()).as("bản có đồ họa tích hợp (máy không card rời), không lấy bản F").isEqualTo("CPU Intel Core i3-12100 Box");
    }

    @Test
    void doiNguonReHonKhongHaChuan80Plus() {
        // v16: bước đổi hàng rẻ hơn cùng thông số hạ nguồn Bronze xuống White
        var now = new ProductSearch.Product("Nguồn máy tính MSI MAG A650BN - 80 Plus Bronze (650W)", "A", 1_190_000L, "https://a.vn/psu", null);
        var white = new ProductSearch.Product("Nguồn máy tính FSP VIC WD 650W - 80 Plus White (650W)", "B", 990_000L, "https://b.vn/w", null);
        var bronze = new ProductSearch.Product("Nguồn máy tính AIGO GB650 - 650W (80 Plus Bronze)", "B", 950_000L, "https://b.vn/b", null);
        assertThat(ChatService.equivalents("psu", now, List.of(white, bronze), false, false, false)).containsExactly(bronze);
    }

    @Test
    void chatLuongKhongHonPhuHopThiNhacChonLaiRoiAnBanTrung() {
        // v17: sau khi đổi vỏ nhỏ gọn, Chất lượng bằng y Phù hợp (cùng sản phẩm, cùng tổng)
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD")).thenReturn(List.of(new ProductSearch.Product("SSD A 1TB", "A", 1_500_000L, "https://a.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        String item = "{\"code\":\"ssd_m2_2280\",\"spec\":\"1 TB\",\"reason\":\"Đủ\",\"product_id\":\"s1-1\"}";
        var calls = new ArrayList<Map<String, Object>>();
        for (String t : List.of("balanced", "quality")) {
            calls.add(Map.of("id", "p-" + t, "type", "function", "function", Map.of("name", "propose_build",
                    "arguments", "{\"tier\":\"" + t + "\",\"summary\":\"Học\",\"budget_vnd\":2000000,\"items\":[" + item + "]}")));
        }
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"),
                Map.of("choices", List.of(Map.of("finish_reason", "tool_calls", "message", Map.of("role", "assistant", "content", "", "tool_calls", calls)))),
                answer("Xong."), answer("Vẫn vậy."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học 2 triệu")), null, null, "advice"));

        // Lần trả lời đầu bị chặn: trợ lý được nhắc chọn lại tier Chất lượng (tốt hơn Phù hợp)
        assertThat(sent.get(3).get("messages").toString()).contains("Chất lượng").contains("không tốt hơn Phù hợp");
        // Trợ lý không sửa: bản trùng không hiện, chỉ còn Phù hợp, câu trả lời nói rõ
        assertThat(res.options()).isNull();
        assertThat(res.build().tier()).isEqualTo("balanced");
        assertThat(res.reply()).contains("Phương án Chất lượng trùng cấu hình với Phù hợp");
    }

    @Test
    void nguoiDungMuonMiniItxThiGiuVoMini() {
        assertThat(ChatService.needs(List.of("Mình muốn máy thật nhỏ gọn kiểu mini ITX để đặt cạnh TV, chơi game nhẹ, khoảng 20 triệu."))
                .miniItx()).isTrue();
        assertThat(ChatService.needs(List.of("máy nhỏ gọn để học, 12 triệu")).miniItx()).as("nhỏ gọn là Micro-ATX, không phải mini").isFalse();
        // Trợ lý chọn vỏ nhỏ gọn cho câu "mini ITX": app đổi về vỏ Mini, bỏ vỏ mATX để tìm vỏ ITX
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("case_compact_tower")).thenReturn(part("case_compact_tower", "case", "Vỏ nhỏ gọn"));
        when(components.findByCode("case_mini_sff")).thenReturn(part("case_mini_sff", "case", "Vỏ Mini / SFF"));
        when(search.enabled()).thenReturn(true);
        when(search.search("vỏ")).thenReturn(List.of(new ProductSearch.Product("Vỏ máy tính EDRA ECS1105 Micro-ATX", "A", 290_000L, "https://a.vn/v", null)));
        when(search.search("vỏ máy tính mini ITX")).thenReturn(List.of(new ProductSearch.Product("Vỏ máy tính Jonsbo A4 Mini-ITX", "B", 2_500_000L, "https://b.vn/itx", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"vỏ\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Mini","budget_vnd":20000000,"items":[{"code":"case_compact_tower","spec":"vỏ nhỏ","reason":"Gọn","product_id":"s1-1"}]}
                """), answer("Xong."));

        var card = service.reply(new ChatRequest(List.of(new Message("user", "máy mini ITX chơi game nhẹ 20 triệu")), null, null, "advice")).build();

        assertThat(card.items().getFirst().code()).isEqualTo("case_mini_sff");
        assertThat(card.items().getFirst().productName()).isEqualTo("Vỏ máy tính Jonsbo A4 Mini-ITX");
    }

    @Test
    void chatLuongVanReHonPhuHopSauKhiNhacThiAnKemGhiChu() {
        // n1 ai-2: Chất lượng 29,4tr rẻ hơn Phù hợp 30,3tr dù đã nhắc một lần
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("SSD A 1TB", "A", 1_500_000L, "https://a.vn/1", null),
                new ProductSearch.Product("SSD B 1TB", "B", 1_400_000L, "https://b.vn/1", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        var calls = new ArrayList<Map<String, Object>>();
        for (String[] t : List.of(new String[] { "balanced", "s1-1" }, new String[] { "quality", "s1-2" })) {
            calls.add(Map.of("id", "p-" + t[0], "type", "function", "function", Map.of("name", "propose_build",
                    "arguments", "{\"tier\":\"" + t[0] + "\",\"summary\":\"Học\",\"budget_vnd\":2000000,\"items\":[{\"code\":\"ssd_m2_2280\",\"spec\":\"1 TB\",\"reason\":\"Đủ\",\"product_id\":\"" + t[1] + "\"}]}")));
        }
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"),
                Map.of("choices", List.of(Map.of("finish_reason", "tool_calls", "message", Map.of("role", "assistant", "content", "", "tool_calls", calls)))),
                answer("Xong."), answer("Vẫn vậy."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học 2 triệu")), null, null, "advice"));

        assertThat(res.options()).isNull();
        assertThat(res.reply()).contains("Phương án Chất lượng chưa tìm được cấu hình nâng cấp rõ ràng hơn Phù hợp");
    }

    @Test
    void dongGioiThieuPhuongAnLayTuTheVaBoLoiLechThe() {
        // v19 / n2: trợ lý tả phương án đã bị ẩn và ghi tổng cũ; giờ app dựng dòng từng phương án từ thẻ
        var line = new Line("ssd_m2_2280", "ssd", "SSD", 1, "1TB", null, 1_500_000L, 1_500_000L, null, "https://a.vn/1",
                "SSD Kingston NV3 1TB", "A", true, null, null, List.of(), true);
        var line2 = new Line("ssd_m2_2280", "ssd", "SSD", 1, "1TB", null, 2_390_000L, 2_390_000L, null, "https://b.vn/1",
                "SSD Samsung 990 EVO 1TB", "B", true, null, null, List.of(), true);
        var b = new ChatResponse.BuildCard(List.of(line), true, List.of(), List.of(), null, 2_000_000L, null, "balanced", 1, List.of(), List.of(), null);
        var q = new ChatResponse.BuildCard(List.of(line2), true, List.of(), List.of(), null, 2_000_000L, null, "quality", 1, List.of(), List.of(), null);
        String ai = "Mình hiểu bạn cần máy học, ngân sách 2 triệu.\n\n- **Phù hợp cho bạn - 25,0 triệu:** vỏ kính, Ryzen 5 5600\n- **Chất lượng - 40 triệu**\n\nNhớ xả tĩnh điện trước khi lắp."
                + "\n\nNâng những món còn lại:\n- RAM bảo hành 36 tháng dù thẻ chưa có dữ liệu.\n- Vỏ tốt hơn bản Phù hợp."
                + "\nPhương án \"Phù hợp\" dùng bo 4 khe dù chưa kiểm tra."
                + "\n\nPhương án Rẻ: chưa tìm được cấu hình lắp chung được, có link mua trong mức tối đa 1,8 triệu, nên app không hiển thị.";

        String out = ChatService.withTierRows(ai, List.of(b, q), List.of("Phương án Rẻ: chưa tìm được cấu hình lắp chung được, nên app không hiển thị."));

        assertThat(out).startsWith("Mình hiểu bạn cần máy học, ngân sách 2 triệu.").contains("Nhớ xả tĩnh điện")
                .doesNotContain("25,0 triệu").doesNotContain("Ryzen 5 5600")
                .doesNotContain("dùng bo 4 khe dù chưa kiểm tra")
                .doesNotContain("RAM bảo hành 36 tháng dù thẻ chưa có dữ liệu")
                .contains("**Phù hợp: 1,50 triệu** - gợi ý chính")
                .contains("**Chất lượng: 2,39 triệu** - tham khảo, vượt ngân sách 0,39 triệu - khác Phù hợp: SSD Samsung 990 EVO 1TB")
                .contains("Phương án Rẻ: chưa tìm được");
    }

    @Test
    void cauTraLoiUuTienKhacBietCardVaNoiRoGiaThamKhao() {
        var base = new ChatResponse.BuildCard(List.of(
                new Line("case", "case", "Vỏ", 1, null, null, 500_000L, 500_000L, null, "https://a.vn/case", "Vỏ A", "A"),
                new Line("cpu", "cpu", "CPU", 1, null, null, 3_000_000L, 3_000_000L, null, "https://a.vn/cpu", "Intel Core i5 12400F", "A"),
                new Line("gpu", "gpu", "Card", 1, null, null, 8_000_000L, 8_000_000L, null, "https://a.vn/gpu", "NVIDIA RTX 4060 8GB", "A")),
                true, List.of(), List.of(), null, 15_000_000L, null, "balanced", 1, List.of(),
                List.of(new CompatibilityResponse.Issue("DOWNGRADE", "6GB VRAM dưới mức khuyến nghị 8GB cho game.")));
        var cheap = new ChatResponse.BuildCard(List.of(
                new Line("case", "case", "Vỏ", 1, null, null, 400_000L, 400_000L, null, "https://a.vn/case2", "Vỏ B", "A"),
                new Line("cpu", "cpu", "CPU", 1, null, null, 2_500_000L, 2_500_000L, null, "https://a.vn/cpu2", "Intel Core i3 12100F", "A"),
                new Line("gpu", "gpu", "Card", 1, null, null, 6_000_000L, 6_000_000L, null, "https://a.vn/gpu2", "NVIDIA RTX 3050 6GB", "A")),
                true, List.of(), List.of(), null, 15_000_000L, null, "cheap", 1);

        String out = ChatService.withTierRows("Mình hiểu bạn chơi game Full HD.\nBạn muốn mình thử ngân sách cao hơn?", List.of(cheap, base));

        assertThat(out).contains("gợi ý chính: NVIDIA RTX 4060 8GB; Intel Core i5 12400F")
                .contains("khác Phù hợp: NVIDIA RTX 3050 6GB; Intel Core i3 12100F; Vỏ B")
                .contains("giá tham khảo chưa xác minh trên trang bán")
                .contains("Lưu ý phương án chính: 6GB VRAM dưới mức khuyến nghị 8GB cho game.")
                .doesNotContain("Bạn muốn mình thử ngân sách cao hơn?");
    }

    @Test
    void khongHienLyDoRamMauThuanVoiSoThanh() {
        assertThat(ChatService.safeReason("ram", 1, "Chạy 2 thanh, mở nhiều tab mượt")).isNull();
        assertThat(ChatService.safeReason("ram", 2, "Đủ 32GB cho dữ liệu")).isEqualTo("Đủ 32GB cho dữ liệu");
    }

    @Test
    void khongCoTheThiKhongDuocNoiDaChotPhuongAn() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD")).thenReturn(List.of(new ProductSearch.Product("SSD A 1TB", "A", 25_000_000L, "https://a.vn/ssd", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":20000000,"items":[{"code":"ssd_m2_2280","spec":"1TB","product_id":"s1-1"}]}
                """), answer("Cả ba phương án đều lắp ăn khớp, hãy chọn phương án Rẻ."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học 20 triệu")), null, null, "advice"));

        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("chưa chốt được cấu hình").contains("Phương án Phù hợp:")
                .doesNotContain("Cả ba phương án").doesNotContain("hãy chọn phương án Rẻ");
    }

    @Test
    void banReVuotTranRiengNhungVuaNganSachThiDungLamPhuHop() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("SSD A 1TB", "A", 19_000_000L, "https://a.vn/a", null),
                new ProductSearch.Product("SSD B 1TB", "B", 25_000_000L, "https://b.vn/b", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"SSD\"}"), toolCall("p1", "propose_build", """
                {"tier":"cheap","summary":"Học","budget_vnd":20000000,"items":[{"code":"ssd_m2_2280","spec":"1TB","product_id":"s1-1"}]}
                """), toolCall("p2", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":20000000,"items":[{"code":"ssd_m2_2280","spec":"1TB","product_id":"s1-2"}]}
                """), answer("Xong."));

        var res = service.reply(new ChatRequest(List.of(new Message("user", "học 20 triệu")), null, null, "advice"));

        assertThat(res.build()).isNotNull();
        assertThat(res.build().tier()).isEqualTo("balanced");
        assertThat(res.build().items().getFirst().priceMaxVnd()).isEqualTo(19_000_000L);
        assertThat(res.reply()).contains("Phương án Rẻ:").contains("App chưa tìm được");
    }

    @Test
    void giuDungVramNguoiDungYeuCauVaUuTienTinMoi() {
        assertThat(ChatService.needs(List.of("chơi game cần card NVIDIA 12GB")).vramMin()).isEqualTo(12);
        assertThat(ChatService.needs(List.of("chơi game cần RTX 4060 Ti 16GB")).vramMin()).isEqualTo(16);
        assertThat(ChatService.needs(List.of("RX 7600 8GB chơi game, RAM 32GB")).vramMin()).isEqualTo(8);
        assertThat(ChatService.needs(List.of("đổi sang card 16GB", "card 12GB chơi game")).vramMin()).isEqualTo(16);
        assertThat(ChatService.needs(List.of("chơi game nhẹ card 6GB")).vramMin()).isEqualTo(6);
        assertThat(ChatService.needs(List.of("học AI card 6GB")).vramMin()).isEqualTo(12);
        assertThat(ChatService.sameSpec("cpu", "Intel Core i3 12100 Box", "Intel Core i3 12100")).isFalse();
        assertThat(ChatService.cpuUpgrade("Intel Core i3 12100 Box", "Intel Core i5 12400")).isFalse();
        assertThat(ChatService.needs(List.of("chỉ học online, không chơi game")).office()).isTrue();
        assertThat(ChatService.needs(List.of("chỉ học online, không chơi game")).vramMin()).isNull();
        assertThat(ChatService.needs(List.of("không chơi game nữa, chỉ học online", "chơi game 20 triệu")).office()).isTrue();
        assertThat(ChatService.requestedGpu(List.of("cần RTX 4060 Ti 16GB"))).isEqualTo("rtx4060ti");
        assertThat(ChatService.requestedGpu(List.of("đổi card sang AMD", "cần RTX 4060"))).isNull();
        assertThat(ChatService.requestedGpu(List.of("so RTX 4060 với RTX 4070"))).isNull();
        assertThat(ChatService.entryCpu("CPU AMD Athlon 3000G")).isTrue();
        assertThat(ChatService.entryCpu("CPU Intel Core i3 12100F")).isFalse();
        assertThat(ChatService.adviceIntro(ChatService.needs(List.of("học online không chơi game")), 20_550_000L))
                .isEqualTo("Bạn cần máy học tập / văn phòng, ngân sách 20,55 triệu.");
        var changed = List.of("chỉ học online, không cần card rời", "chơi game RTX 4060 8GB 30 triệu");
        assertThat(ChatService.noDiscreteGpu(changed)).isTrue();
        assertThat(ChatService.noDiscreteGpu(List.of("KHÔNG CẦN CARD ĐỒ HOẠ RỜI"))).isTrue();
        assertThat(ChatService.requestedGpu(changed)).isNull();
        assertThat(ChatService.needs(changed).office()).isTrue();
        assertThat(ChatService.needs(changed).vramMin()).isNull();
    }

    @Test
    void gameYeuCau12GbDuocChonCardCuHonDeVuaTien() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        var old = new ProductSearch.Product("VGA ASUS RTX 4070 Super 12GB", "A", 20_000_000L, "https://a.vn/gpu", null);
        var fit = new ProductSearch.Product("VGA ASUS Dual RTX 3060 12GB", "B", 10_000_000L, "https://b.vn/gpu", null);
        when(search.search("GPU")).thenReturn(List.of(old));
        when(search.search("VGA RTX 3060")).thenReturn(List.of(fit));
        when(search.offer(fit.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                fit.title(), fit.shop(), fit.priceVnd(), null, fit.url(), null, true, 36, java.time.Instant.now()));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"GPU\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","summary":"Game","budget_vnd":15000000,"items":[{"code":"gpu_280","spec":"RTX 4070 Super 12GB","product_id":"s1-1"}]}
                """), answer("Xong."));
        var res = service.reply(new ChatRequest(List.of(new Message("user", "chơi game 15 triệu card NVIDIA tối thiểu 12GB")), null, null, "advice"));
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items().getFirst().productName()).contains("RTX 3060 12GB");
        assertThat(res.reply()).contains("hiệu năng GPU thấp hơn");
    }

    @Test
    void mayAiDoiCardMoi8GbSangCardCu12GbDuNhuCau() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        var small = new ProductSearch.Product("VGA ASUS RTX 5060 Ti 8GB", "A", 20_000_000L, "https://a.vn/gpu", null);
        var large = new ProductSearch.Product("VGA ASUS Dual RTX 3060 12GB", "B", 12_000_000L, "https://b.vn/gpu", null);
        when(search.offer(large.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                large.title(), large.shop(), large.priceVnd(), null, large.url(), null, true, 36, java.time.Instant.now()));
        when(search.search("GPU")).thenReturn(List.of(small));
        when(search.search("VGA RTX 3060")).thenReturn(List.of(large));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"GPU\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","summary":"AI","budget_vnd":35000000,"items":[{"code":"gpu_280","spec":"RTX 5060 Ti 8GB","product_id":"s1-1"}]}
                """), answer("Xong."));
        var res = service.reply(new ChatRequest(List.of(new Message("user", "học AI 35 triệu")), null, null, "advice"));
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items().getFirst().productName()).contains("RTX 3060 12GB");
    }

    @Test
    void khongTimDuocCardDuVramThiAnThayViHienCardThieu() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        var gpu = new ProductSearch.Product("VGA ASUS RTX 5060 Ti 8GB", "A", 10_000_000L, "https://a.vn/gpu", null);
        when(search.search("GPU")).thenReturn(List.of(gpu));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"GPU\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","summary":"AI","budget_vnd":35000000,"items":[{"code":"gpu_280","spec":"RTX 5060 Ti 8GB","product_id":"s1-1"}]}
                """), answer("Cả ba đều đủ VRAM."));
        var res = service.reply(new ChatRequest(List.of(new Message("user", "học AI 35 triệu")), null, null, "advice"));
        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("card chưa đủ 12GB VRAM").doesNotContain("đều đủ VRAM");
    }

    @Test
    void thieuRamNguoiDungYeuCauThiKhongHienDuGiaVanVuaTran() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RAM")).thenReturn(List.of(new ProductSearch.Product("RAM Corsair 16GB DDR4", "A", 3_000_000L, "https://a.vn/ram", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"RAM\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":30000000,"items":[{"code":"ram_ddr4","quantity":1,"spec":"1x16GB DDR4","product_id":"s1-1"}]}
                """), answer("Đủ RAM rồi."));
        var res = service.reply(new ChatRequest(List.of(new Message("user", "máy học RAM 32GB 30 triệu")), null, null, "advice"));
        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("RAM chưa đủ 32GB").doesNotContain("Đủ RAM rồi");
    }

    @Test
    void chiConBanVuotNganSachThiNoiRoKhoanVuot() {
        var line = new Line("ssd", "ssd", "SSD", 1, "1TB", null, 2_500_000L, 2_500_000L, null, "https://a.vn/ssd", "SSD A 1TB", "A");
        var card = new ChatResponse.BuildCard(List.of(line), true, List.of(), List.of(), null, 2_000_000L, null, "quality", 1);
        assertThat(ChatService.withTierRows("Mình hiểu bạn cần máy học.", List.of(card)))
                .contains("tham khảo, vượt ngân sách 0,5 triệu").doesNotContain("gợi ý chính");
    }

    @Test
    void khongGiuSoTienTietKiemChoThayDoiChuaKiemTra() {
        var line = new Line("ssd", "ssd", "SSD", 1, "1TB", null, 1_000_000L, 1_000_000L, null, "https://a.vn/ssd", "SSD A 1TB", "A");
        var card = new ChatResponse.BuildCard(List.of(line), true, List.of(), List.of(), null, 2_000_000L, null, "balanced", 1);
        String out = ChatService.withTierRows("Mình hiểu bạn cần máy học, ngân sách 2 triệu.\n\n"
                + "Bỏ bộ quạt thêm sẽ bớt khoảng 690.000đ vì vỏ có quạt sẵn dù chưa kiểm tra.", List.of(card));
        assertThat(out).startsWith("Mình hiểu bạn cần máy học, ngân sách 2 triệu.")
                .doesNotContain("690.000đ").doesNotContain("vỏ có quạt sẵn");
    }

    @Test
    void hoiLyDoPhuongAnBiAnThiDungSoDaTinhKhongGoiAi() {
        String note = "Phương án Rẻ: cấu hình đã thử 18,2 triệu vượt mức 18 triệu. App không hiển thị phương án này.";
        var res = service.reply(new ChatRequest(List.of(
                new Message("user", "chơi game 20 triệu"),
                new Message("assistant", "Các thẻ ở dưới.\n\n" + note),
                new Message("user", "Vì sao không có phương án Rẻ?")), null, null, "advice"));
        assertThat(res.reply()).contains(note).doesNotContain("21 triệu");
        assertThat(res.build()).isNull();
        org.mockito.Mockito.verifyNoInteractions(client);
    }

    @Test
    void aiChotAmdThiKhongAmThamDoiSangNvidia() {
        var res = service.reply(new ChatRequest(List.of(new Message("user", "học AI 35 triệu phải dùng card AMD")), null, null, "advice"));
        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("Bạn đã chốt card AMD").contains("chưa kiểm chứng");
        org.mockito.Mockito.verifyNoInteractions(client);
    }

    @Test
    void chuaNoiMucDichThiHoiLaiKhongTuGanGameVaHoc() {
        var res = service.reply(new ChatRequest(List.of(new Message("user", "Mình có ngân sách 15 triệu để mua máy tính.")), null, null, "advice"));
        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("dùng máy chủ yếu làm gì");
        org.mockito.Mockito.verifyNoInteractions(client);
    }

    @Test
    void dungLuongRamSatSsdKhongBiDocThanhDungLuongDia() {
        assertThat(ChatService.needs(List.of("Văn phòng21 triệu, RAM32GB SSD1TB, không card rời.")).ssdMin()).isEqualTo(1000);
        assertThat(ChatService.needs(List.of("Văn phòng21 triệu, 1TB SSD và RAM32GB.")).ssdMin()).isEqualTo(1000);
        assertThat(ChatService.needs(List.of("RAM32GB SSD512GB", "SSD1TB RAM16GB")).ssdMin()).isEqualTo(512);
    }

    @Test
    void chatLuongKhongHaGpuDeLaySsdLonHon() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ssd_m2_2280")).thenReturn(part("ssd_m2_2280", "ssd", "SSD"));
        when(components.findByCode("gpu_240")).thenReturn(part("gpu_240", "gpu", "GPU"));
        when(search.enabled()).thenReturn(true);
        when(search.search("SSD")).thenReturn(List.of(
                new ProductSearch.Product("SSD A 1TB", "A", 3_000_000L, "https://a.vn/1", null),
                new ProductSearch.Product("SSD B 2TB", "B", 6_000_000L, "https://b.vn/2", null)));
        when(search.search("GPU")).thenReturn(List.of(
                new ProductSearch.Product("MSI RTX 4060 8GB", "A", 9_000_000L, "https://a.vn/gpu", null),
                new ProductSearch.Product("Ninja RTX 3060 12GB", "B", 10_000_000L, "https://b.vn/gpu", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        var calls = new ArrayList<Map<String, Object>>();
        for (String[] t : List.of(new String[] { "balanced", "s1-1", "s2-1" }, new String[] { "quality", "s1-2", "s2-2" })) {
            calls.add(Map.of("id", "p-" + t[0], "type", "function", "function", Map.of("name", "propose_build",
                    "arguments", "{\"tier\":\"" + t[0] + "\",\"summary\":\"Học\",\"budget_vnd\":30000000,\"items\":["
                            + "{\"code\":\"ssd_m2_2280\",\"spec\":\"" + ("quality".equals(t[0]) ? "2" : "1") + "TB\",\"product_id\":\"" + t[1] + "\"},"
                            + "{\"code\":\"gpu_240\",\"product_id\":\"" + t[2] + "\"}]}")));
        }
        reply(toolCall("s1", "search_products", "{\"query\":\"SSD\"}"),
                toolCall("s2", "search_products", "{\"query\":\"GPU\"}"),
                Map.of("choices", List.of(Map.of("finish_reason", "tool_calls", "message", Map.of("role", "assistant", "content", "", "tool_calls", calls)))),
                answer("Xong."), answer("Vẫn vậy."));
        var res = service.reply(new ChatRequest(List.of(new Message("user", "học 30 triệu")), null, null, "advice"));
        assertThat(res.build().tier()).isEqualTo("balanced");
        assertThat(res.options()).isNull();
        assertThat(res.reply()).contains("Phương án Chất lượng chưa tìm được cấu hình nâng cấp rõ ràng hơn Phù hợp");
    }

    @Test
    void chatLuongCanNangCapThucSuKhongChiDoiCuaHangVaNguonToHon() {
        assertThat(ChatService.qualityUpgrade("gpu", "RTX 3050 Windforce 6GB", 1, "RTX 3050 Windforce V2 6GB", 1)).isFalse();
        assertThat(ChatService.qualityUpgrade("gpu", "RX 7600 8GB", 1, "RTX 3060 12GB", 1)).isFalse();
        assertThat(ChatService.qualityUpgrade("psu", "Thermalright SFX 650W Gold", 1, "Thermalright SFX 850W Gold", 1)).isFalse();
        assertThat(ChatService.qualityUpgrade("ram", "RAM SSTC 16GB DDR4", 2, "RAM Corsair 16GB DDR4", 2)).isFalse();
        assertThat(ChatService.qualityUpgrade("ram", "RAM SSTC 16GB DDR4", 1, "RAM SSTC 16GB DDR4", 2)).isTrue();
        assertThat(ChatService.qualityUpgrade("ssd", "SSD A 512GB", 1, "SSD B 1TB", 1)).isTrue();
    }

    @Test
    void cungCpuGpuRamThiUuTienBanReHonRoRetVaDaKiemGia() {
        java.util.function.BiFunction<String, String, Line> core = (kind, title) -> new Line(kind, kind, kind, 1,
                null, null, 3_000_000L, 3_000_000L, null, "https://a.vn/" + kind, title, "A", true, 36, null, List.of(), true);
        var cpu = core.apply("cpu", "Intel Core i5 12400F");
        var gpu = core.apply("gpu", "RTX 4060 8GB");
        var ram = core.apply("ram", "RAM Corsair 16GB DDR4");
        var small = core.apply("ssd", "SSD A 512GB");
        var large = new Line("ssd", "ssd", "SSD", 1, null, null, 8_000_000L, 8_000_000L, null,
                "https://a.vn/large", "SSD A 4TB", "A", true, 36, null, List.of(), true);
        var cheap = new ChatResponse.BuildCard(List.of(cpu, gpu, ram, small), true, List.of(), List.of(), null, 30_000_000L, null, "cheap", 1);
        var balanced = new ChatResponse.BuildCard(List.of(cpu, gpu, ram, large), true, List.of(), List.of(), null, 30_000_000L, null, "balanced", 1);
        assertThat(ChatService.recommendedCard(List.of(cheap, balanced))).isSameAs(cheap);
        assertThat(ChatService.withTierRows("Đã chọn cấu hình.", List.of(cheap, balanced)))
                .contains("Mình ưu tiên bản Rẻ").contains("**Rẻ: 12,00 triệu** - gợi ý chính");
        var faster = new ChatResponse.BuildCard(List.of(cpu, core.apply("gpu", "RTX 5060 Ti 8GB"), ram, large),
                true, List.of(), List.of(), null, 30_000_000L, null, "balanced", 1);
        assertThat(ChatService.recommendedCard(List.of(cheap, faster))).isSameAs(faster);
        assertThat(ChatService.recommendedCard(List.of(cheap, faster), true)).isSameAs(cheap);
        assertThat(ChatService.withTierRows("Đã chọn cấu hình.", List.of(cheap, faster), List.of(), true))
                .contains("bạn muốn tiết kiệm nhất").doesNotContain("cùng model CPU, chip GPU");
        assertThat(ChatService.prefersCheapest(List.of("cần máy rẻ nhất có thể"))).isTrue();
        assertThat(ChatService.prefersCheapest(List.of("không cần rẻ nhất nữa", "cần máy rẻ nhất"))).isFalse();
        assertThat(ChatService.prefersCheapest(List.of("Không cần card rời nữa, chỉ học online và Word. Giữ 30 triệu là mức tối đa, muốn rẻ nhất."))).isTrue();
    }

    @Test
    void dongSoSanhRamKitGhiSoBoVaThanhKhongNhanDoiDungLuong() {
        var base = new Line("ram", "ram", "RAM", 2, null, null, 5_980_000L, 5_980_000L, null,
                "https://a.vn/ram", "RAM SSTC 16GB DDR4", "A", true, 36, null, List.of(), true);
        var kit = new Line("ram", "ram", "RAM", 4, null, null, 9_398_000L, 9_398_000L, null,
                "https://a.vn/kit", "RAM Lexar Thor 32GB (2x16GB) DDR4", "A", true, 36, null, List.of(), true);
        var balanced = new ChatResponse.BuildCard(List.of(base), true, List.of(), List.of(), null, 30_000_000L, null, "balanced", 1);
        var quality = new ChatResponse.BuildCard(List.of(kit), true, List.of(), List.of(), null, 30_000_000L, null, "quality", 1);
        assertThat(ChatService.withTierRows("Đã chọn.", List.of(balanced, quality)))
                .contains("mua 2 bộ, 4 thanh").doesNotContain("DDR4 x4");
    }

    @Test
    void aiCungGpuVaBoNhoThiUuTienBanTietKiemGiaiThichCpuKhac() {
        java.util.function.BiFunction<String, String, Line> line = (kind, title) -> new Line(kind, kind, kind, 1,
                null, null, 3_000_000L, 3_000_000L, null, "https://a.vn/" + kind, title, "A", true, 36, null, List.of(), true);
        var gpu = line.apply("gpu", "NVIDIA RTX 3060 12GB");
        var ram = line.apply("ram", "RAM 32GB DDR4");
        var ssd = line.apply("ssd", "SSD 1TB");
        var cpu = line.apply("cpu", "CPU Intel Core i5 12400F");
        var costlyCpu = new Line("cpu", "cpu", "CPU", 1, null, null, 12_000_000L, 12_000_000L, null,
                "https://a.vn/7700x", "CPU AMD Ryzen 7 7700X", "A", true, 36, null, List.of(), true);
        var cheap = new ChatResponse.BuildCard(List.of(cpu, gpu, ram, ssd), true, List.of(), List.of(), null, 48_000_000L, null, "cheap", 1);
        var balanced = new ChatResponse.BuildCard(List.of(costlyCpu, gpu, ram, ssd), true, List.of(), List.of(), null, 48_000_000L, null, "balanced", 1);
        assertThat(ChatService.recommendedCard(List.of(cheap, balanced), false, false)).isSameAs(balanced);
        assertThat(ChatService.recommendedCard(List.of(cheap, balanced), false, true)).isSameAs(cheap);
        assertThat(ChatService.withTierRows("Máy học AI.", List.of(cheap, balanced), List.of(), false, true))
                .contains("AI trên GPU", "không giảm tổng RAM/SSD", "CPU, nền RAM và bảo hành", "**Rẻ: 12,00 triệu** - gợi ý chính")
                .doesNotContain("vì bạn muốn tiết kiệm nhất", "cùng model CPU");
        var small = new ChatResponse.BuildCard(List.of(cpu, gpu, ram, line.apply("ssd", "SSD512GB")), true, List.of(), List.of(), null, 48_000_000L, null, "cheap", 1);
        assertThat(ChatService.recommendedCard(List.of(small, balanced), false, true)).isSameAs(balanced);
        var faster = new ChatResponse.BuildCard(List.of(costlyCpu, line.apply("gpu", "RTX4070 SUPER12GB"), ram, ssd), true,
                List.of(), List.of(), null, 48_000_000L, null, "balanced", 1);
        assertThat(ChatService.recommendedCard(List.of(cheap, faster), false, true)).isSameAs(faster);
    }

    @Test
    void stableDiffusionLaMucDichRoRangKhongHoiLai() {
        reply(answer("Xong."), answer("Xong."));
        service.reply(new ChatRequest(List.of(new Message("user", "Cần bộ PC Stable Diffusion cục bộ, 38 triệu, RAM 32GB, GPU NVIDIA 12GB VRAM, SSD 1TB.")), null, null, "advice"));
        verify(client, org.mockito.Mockito.atLeastOnce()).complete(any());
    }

    @Test
    void khongGioiThieuAthlonKemCardGameDuLinhKienLapDuoc() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("cpu_std")).thenReturn(part("cpu_std", "cpu", "CPU"));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        when(search.search("parts")).thenReturn(List.of(
                new ProductSearch.Product("CPU AMD Athlon 3000G", "A", 1_000_000L, "https://a.vn/cpu", null),
                new ProductSearch.Product("VGA ASUS Radeon RX 7600 8GB", "A", 7_000_000L, "https://a.vn/gpu", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"parts\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","summary":"Game","budget_vnd":20000000,"items":[
                {"code":"cpu_std","spec":"Athlon 3000G","product_id":"s1-1"},
                {"code":"gpu_280","spec":"RX 7600 8GB","product_id":"s1-2"}]}
                """), answer("Đây là cấu hình game chất lượng."));
        var res = service.reply(new ChatRequest(List.of(new Message("user", "chơi game 20 triệu card AMD")), null, null, "advice"));
        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("CPU phổ thông quá thấp").doesNotContain("Đây là cấu hình game chất lượng");
    }

    @Test
    void yeuCauMoiKhongCardRoiKhongBiModelRtxCuGhiDe() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("cpu_std")).thenReturn(part("cpu_std", "cpu", "CPU"));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        when(search.search("parts")).thenReturn(List.of(
                new ProductSearch.Product("CPU Intel Core i5 12400", "A", 4_000_000L, "https://a.vn/cpu", null),
                new ProductSearch.Product("VGA ASUS RTX 4060 8GB", "A", 8_000_000L, "https://a.vn/gpu", null)));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"parts\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","summary":"Học","budget_vnd":30000000,"items":[
                {"code":"cpu_std","spec":"Core i5 12400","product_id":"s1-1"},
                {"code":"gpu_280","spec":"RTX 4060 8GB","product_id":"s1-2"}]}
                """), answer("Vẫn giữ card RTX như yêu cầu cũ."));
        var res = service.reply(new ChatRequest(List.of(new Message("user", "chơi game RTX 4060 8GB 30 triệu"),
                new Message("assistant", "Mình đã chọn RTX 4060."),
                new Message("user", "chỉ học online, không cần card rời")), null, null, "advice"));
        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("người dùng yêu cầu không dùng card rời").doesNotContain("Vẫn giữ card RTX");
    }

    // ---- Dựng phản hồi giả theo chuẩn OpenAI chat/completions ---------------

    private static Map<String, Object> answer(String text) {
        return Map.of("choices", List.of(Map.of("finish_reason", "stop",
                "message", Map.of("role", "assistant", "content", text))));
    }

    private static Map<String, Object> toolCall(String id, String name, String arguments) {
        return Map.of("choices", List.of(Map.of("finish_reason", "tool_calls",
                "message", Map.of("role", "assistant", "content", "",
                        "tool_calls", List.of(Map.of("id", id, "type", "function",
                                "function", Map.of("name", name, "arguments", arguments)))))));
    }

    private static ComponentResponse part(String code, String category, String name) {
        return new ComponentResponse(1L, code, category, name, null, null, null, null, Map.of(), List.of());
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

    @Test
    void phuongAnReChonHangReNhatCungThongSo() {
        var ram8 = product("Ram Kingmax Blade X 1x8GB DDR4 Bus 3200Mhz", 1_990_000L);
        var group = List.of(
                product("RAM Klevv 8GB DDR4 Udimm 3200 CL22", 1_250_000L),
                product("Ram Kingston 4GB DDR4 3200", 600_000L), // dung lượng thấp hơn: không lấy
                product("Ram Laptop Crucial 8GB DDR4 3200", 700_000L), // RAM laptop: không lấy
                product("RAM Corsair 8GB DDR5 5600", 900_000L), // khác loại DDR: không lấy
                product("Ram 8GB DDR4 cũ bóc máy", 400_000L), // hàng cũ: không lấy
                product("Bo mạch chủ GIGABYTE B760M GAMING PLUS WIFI DDR4", 800_000L)); // sai loại: không lấy
        assertThat(ChatService.cheaperEquivalent("ram", ram8, group, false).title()).isEqualTo("RAM Klevv 8GB DDR4 Udimm 3200 CL22");

        // SSD: 512GB thay được bằng 1TB rẻ hơn, không thay bằng 256GB
        var ssd = product("Ổ Cứng SSD SSTC Oceanic Whitetip E130 512GB M.2 NVMe", 2_390_000L);
        assertThat(ChatService.cheaperEquivalent("ssd", ssd, List.of(product("SSD Kingston NV3 256GB NVMe", 800_000L),
                product("SSD Lexar NM620 1TB NVMe", 2_100_000L)), false).title()).isEqualTo("SSD Lexar NM620 1TB NVMe");

        // Nguồn: không lấy công suất thấp hơn
        var psu = product("Nguồn Deepcool PF550 - 80 Plus (550W)", 990_000L);
        assertThat(ChatService.cheaperEquivalent("psu", psu, List.of(product("Nguồn Jetek 450W", 450_000L)), false)).isNull();
        // Ca thật: nguồn SFX của vỏ Mini-ITX không được đổi sang nguồn ATX dù cùng 450W
        var sfx = product("Nguồn Thermaltake Toughpower SFX 450W Gold", 2_450_000L);
        assertThat(ChatService.cheaperEquivalent("psu", sfx, List.of(product("Nguồn máy tính/ PSU Aigo VK450 450W", 449_000L),
                product("Nguồn Cooler Master V550 SFX Gold 550W", 2_000_000L)), false).title()).isEqualTo("Nguồn Cooler Master V550 SFX Gold 550W");

        // CPU: chỉ cùng mã ở cửa hàng rẻ hơn
        var cpu = product("CPU Intel Core i3 12100 Box (Socket 1700/ 4 Cores/ 8 Threads)", 4_199_000L);
        assertThat(ChatService.cheaperEquivalent("cpu", cpu, List.of(product("Bộ vi xử lý Intel Core i3-12100F", 2_000_000L),
                product("Bộ vi xử lý/ CPU Intel Core i3 12100", 2_890_000L)), false).title()).isEqualTo("Bộ vi xử lý/ CPU Intel Core i3 12100");
        var cooler = product("Tản nhiệt Cooler Master Hyper 612 Apex", 1_590_000L);
        // Tản cùng kiểu (tháp) rẻ hơn thì được đổi model khác
        assertThat(ChatService.cheaperEquivalent("cooler", cooler, List.of(product("Tản nhiệt khí Jonsbo CR-1200E", 190_000L)), false).title())
                .isEqualTo("Tản nhiệt khí Jonsbo CR-1200E");

        // Dòng mua bộ kit thì món thay cũng phải là kit
        var kit = product("RAM Corsair Vengeance 16GB (2x8GB) DDR4 3200", 2_500_000L);
        assertThat(ChatService.cheaperEquivalent("ram", kit, List.of(product("RAM Klevv 16GB DDR4 3200", 1_500_000L),
                product("RAM TeamGroup 16GB (2x8GB) DDR4 3200", 2_000_000L)), true).title()).isEqualTo("RAM TeamGroup 16GB (2x8GB) DDR4 3200");
    }

    @Test
    void docDungLuongVaCongSuatTuTen() {
        assertThat(ChatService.amount("ram", "Ram SSTC 16GB DDR4 3200MHz (U3200A-C22-16GB)")).isEqualTo(16);
        assertThat(ChatService.amount("ram", "RAM Corsair 32GB (2x16GB) DDR5")).isEqualTo(32);
        assertThat(ChatService.amount("ssd", "SSD WD Green SN3000 1TB M.2 NVMe")).isEqualTo(1000);
        assertThat(ChatService.amount("ssd", "SSD Kingston 512GB")).isEqualTo(512);
        assertThat(ChatService.amount("psu", "Nguồn máy tính Jetek Revolve B550 (550W) 80 plus Bronze")).isEqualTo(550);
        assertThat(ChatService.amount("psu", "Nguồn máy tính KENOO ATX")).isNull();
        // Đổi món lấy link: không nhận món sai loại
        assertThat(ChatService.sameKind("Ram Kingmax 8GB DDR4", "Bo mạch chủ GIGABYTE B760M DDR4")).isFalse();
        assertThat(ChatService.sameKind("Ram Kingmax 8GB DDR4", "Ram Klevv 8GB DDR4")).isTrue();
        assertThat(ChatService.cpuModel("CPU Intel Core i3 12100 Box (Socket 1700/ 4 Cores/ 8 Threads)")).isEqualTo("i3-12100");
        assertThat(ChatService.cpuModel("Bộ vi xử lý Intel Core i3-12100F")).isEqualTo("i3-12100f");
        assertThat(ChatService.cpuModel("CPU Intel Core Ultra 5 225F")).isEqualTo("ultra5-225f");
        assertThat(ChatService.cpuModel("CPU AMD Ryzen 7 7800X3D")).isEqualTo("ryzen7-7800x3d");
    }

    @Test
    void doiMonLayLinkGiuDungThongSo() {
        // Ca thật: dòng RAM 8GB bị thay bằng bộ 32GB, CPU i3-12100 có thể thành bản F
        var ram = product("Ram Kingmax Blade X 1x8GB DDR4 Bus 3200Mhz", 1_000_000L);
        var group = List.of(product("Silicon Power XPOWER Zenith RGB RAM 32GB DDR4", 900_000L),
                product("RAM Klevv 8GB DDR4 3200", 950_000L));
        assertThat(ChatService.candidates("ram", ram, group, false, "cheap")).extracting(ProductSearch.Product::title)
                .containsExactly("RAM Klevv 8GB DDR4 3200");
        assertThat(ChatService.sameSpec("cpu", "CPU Intel Core i3 12100 Box", "Bộ vi xử lý Intel Core i3-12100F")).isFalse();
        assertThat(ChatService.sameSpec("psu", "Nguồn 550W", "Nguồn 450W")).isFalse();
        assertThat(ChatService.sameSpec("ssd", "SSD 512GB", "SSD 512GB NVMe Gen4")).isTrue();
    }

    @Test
    void monThayCuoiCungVanDungThongSo() {
        assertThat(ChatService.sameSpec("gpu", "Card MSI RTX 4060 Ventus 2X 8G", "Card ASUS Dual GeForce RTX 4060 OC 8GB")).isTrue();
        assertThat(ChatService.sameSpec("gpu", "Card MSI RTX 4060 Ventus 2X 8G", "Card ASUS Dual RTX 4060 Ti 8GB")).isFalse();
        assertThat(ChatService.sameSpec("gpu", "Card Powercolor RX 6600 8GB", "Card Sapphire Pulse RX 6600 XT")).isFalse();
        assertThat(ChatService.sameSpec("motherboard", "Mainboard MSI PRO H610M-E DDR4", "Mainboard ASUS PRIME H610M-K D4 mATX")).isTrue();
        assertThat(ChatService.sameSpec("motherboard", "Mainboard ASUS PRIME H610M-E D4 Micro-ATX", "Mainboard ASUS PRIME H610I-PLUS D4 Mini-ITX")).isFalse();
        assertThat(ChatService.sameSpec("case", "Vỏ máy tính EDRA ECS1105 Micro-ATX", "Vỏ Xigmatek Gaming X II ATX Mid Tower")).isTrue();
        assertThat(ChatService.sameSpec("case", "Vỏ máy tính EDRA ECS1105 Micro-ATX", "Case NXPC NX-01 Mini ITX")).isFalse();
        assertThat(ChatService.sameSpec("cooler", "Tản nhiệt khí Jonsbo CR-1200E", "Tản nhiệt AIO Corsair 240")).isFalse();
        assertThat(ChatService.sameSpec("cooler", "Tản nước AIO DeepCool LE520 240mm", "Tản nhiệt nước Corsair H100 240")).isTrue();
        assertThat(ChatService.boardSize("Mainboard Gigabyte B760M D DDR4 (Intel B760/ Socket 1700/ M-ATX/ 2 khe)")).isEqualTo(2);
    }

    @Test
    void appTuChonChiLayHangDungLoaiVaGiaHopLy() {
        // Ca thật: dòng bo mạch tự tìm ra RAM; dòng tản tự tìm ra cái pát 30 nghìn
        assertThat(ChatService.ofKind("motherboard", "Ram Corsair Vengeance LPX 16GB 3200MHz DDR4")).isFalse();
        assertThat(ChatService.ofKind("motherboard", "Mainboard MSI PRO H610M-E DDR4")).isTrue();
        assertThat(ChatService.ofKind("gpu", "Cạc màn hình Inno3D GEFORCE RTX 4060 Ti TWIN X2 16GB (GDDR6/ 128 bit)")).isTrue();
        assertThat(ChatService.ofKind("gpu", "Màn hình ASUS 24 inch")).isFalse();
        assertThat(ChatService.plausible("cooler", 30_000L)).isFalse();
        assertThat(ChatService.plausible("cooler", 190_000L)).isTrue();
        assertThat(ChatService.plausible("cpu", 2_890_000L)).isTrue();
    }

    @Test
    void docDungLuongTrongSpecVaMaSsd() {
        // Ca thật: spec "1TB" nhưng chọn Kingston NV2 bản SNV2S/4000G (4TB, 5,79 triệu)
        assertThat(ChatService.amount("ssd", "Kingston NV2 M.2 2280 PCIe 4.0 x4 NVMe SSD SNV2S/4000G")).isEqualTo(4000);
        assertThat(ChatService.specAmount("ssd", "SSD NVMe 1TB")).isEqualTo(1000);
        assertThat(ChatService.specAmount("ram", "8 GB DDR4 3200")).isEqualTo(8);
        // Spec RAM nói nhiều thanh: không chắc là tổng hay mỗi thanh
        assertThat(ChatService.specAmount("ram", "16 GB, 2 thanh, DDR4 3200")).isNull();
        assertThat(ChatService.specAmount("ram", "2x8GB DDR4")).isNull();
    }

    @Test
    void cpuCanLinkThiDuocLayBanCungSocketDoiCaoHon() {
        // Ca thật: Ryzen 7 7700 không cửa hàng nào có trang xác minh được, bản 7700X thì có
        assertThat(ChatService.cpuUpgrade("CPU AMD Ryzen 7 7700", "CPU AMD Ryzen 7 7700X / AM5")).isTrue();
        assertThat(ChatService.cpuUpgrade("Bộ vi xử lý Intel Core i3-12100", "Bộ vi xử lý Intel Core i5-12400")).isTrue();
        // Không thành bản F (mất đồ họa tích hợp), không sang socket khác, không đời thấp hơn
        assertThat(ChatService.cpuUpgrade("Bộ vi xử lý Intel Core i3-12100", "Bộ vi xử lý Intel Core i5-12400F")).isFalse();
        assertThat(ChatService.cpuUpgrade("CPU AMD Ryzen 7 7700", "CPU AMD Ryzen 7 5700X")).isFalse();
        assertThat(ChatService.cpuUpgrade("CPU AMD Ryzen 7 7700", "CPU AMD Ryzen 5 7600")).isFalse();
    }

    @Test
    void sanPhamPhaiKhopMaDanhMuc() {
        // Ca thật: dòng bo Mini-ITX là bo A520M (Micro-ATX); dòng tản khí thấp của vỏ Mini là tản tháp Hyper 612
        assertThat(ChatService.fitsCode("mb_itx", "Bo Mạch Chủ Gigabyte A520M-K V2")).isFalse();
        assertThat(ChatService.fitsCode("mb_itx", "Mainboard ASUS ROG STRIX B550-I GAMING")).isTrue();
        assertThat(ChatService.fitsCode("low_profile_47", "Tản nhiệt Cooler Master Hyper 612 Apex")).isFalse();
        assertThat(ChatService.fitsCode("low_profile_47", "Tản nhiệt ID-COOLING IS-40X V3 low profile")).isTrue();
        assertThat(ChatService.fitsCode("aio_240", "Tản nhiệt khí Jonsbo CR-1200E")).isFalse();
        assertThat(ChatService.fitsCode("psu_sfx", "Nguồn máy tính Jetek J400 (400W)")).isFalse();
        assertThat(ChatService.fitsCode("case_mini_sff", "Vỏ Xigmatek Gaming X II ATX")).isFalse();
        // Vỏ rẻ hơn: cùng cỡ; kính chỉ bắt buộc khi người dùng thích kính (need_glass, kiểm ở glassOk), không theo tên vỏ cũ
        var glassCase = product("Vỏ máy tính Montech AIR 903 ATX kính cường lực", 2_000_000L);
        assertThat(ChatService.cheaperEquivalent("case", glassCase, List.of(product("Vỏ Jetek EM4 ATX hông kín", 330_000L),
                product("Vỏ Xigmatek Gaming X II ATX mặt kính", 790_000L)), false).title()).isEqualTo("Vỏ Jetek EM4 ATX hông kín");
        assertThat(ChatService.glassOk(Map.of("case_gaming", Map.of("need_glass", true)), "case_gaming", product("Vỏ Jetek EM4 ATX hông kín", 330_000L))).isFalse();
        assertThat(ChatService.glassOk(Map.of("case_gaming", Map.of("need_glass", true)), "case_gaming", product("Vỏ Xigmatek Gaming X II ATX mặt kính", 790_000L))).isTrue();
        assertThat(ChatService.glassOk(Map.of(), "case_gaming", product("Vỏ Jetek EM4 ATX hông kín", 330_000L))).isTrue();
        var tower = product("Tản nhiệt Cooler Master Hyper 612 Apex", 1_590_000L);
        assertThat(ChatService.cheaperEquivalent("cooler", tower, List.of(product("Tản nhiệt khí Jonsbo CR-1200E", 190_000L),
                product("Tản nhiệt ID-COOLING IS-40X low profile", 300_000L)), false).title()).isEqualTo("Tản nhiệt khí Jonsbo CR-1200E");
    }

    @Test
    void phuongAnReDuocNoiBoVaNguon() {
        var b760 = product("Bo mạch chủ MSI PRO B760M-E DDR4", 2_390_000L);
        var group = List.of(product("Mainboard Colorful H610M-D V20A DDR4", 1_550_000L),
                product("Mainboard ASUS PRIME H610M-E D5", 1_400_000L), // DDR5: không lấy
                product("Mainboard ASUS H610I-PLUS D4", 1_300_000L)); // Mini-ITX: không lấy
        assertThat(ChatService.equivalents("motherboard", b760, group, false, true, true)).extracting(ProductSearch.Product::title)
                .containsExactly("Mainboard Colorful H610M-D V20A DDR4");
        // Phương án khác: bo chỉ đổi đúng model
        assertThat(ChatService.equivalents("motherboard", b760, group, false, false, true)).isEmpty();
        // Máy không card rời: nguồn 400W thay được 550W ở phương án Rẻ
        var psu = product("Nguồn Deepcool PF550 (550W)", 990_000L);
        assertThat(ChatService.equivalents("psu", psu, List.of(product("Nguồn Jetek J400 (400W)", 590_000L)), false, true, true))
                .hasSize(1);
        assertThat(ChatService.equivalents("psu", psu, List.of(product("Nguồn Jetek J400 (400W)", 590_000L)), false, true, false))
                .isEmpty();
    }

    @Test
    void giaVoLyBiLoai() {
        // Ca thật: nguồn 725.000đ đọc thành 725.000.000đ; RAM DDR5 ở dòng RAM DDR4
        assertThat(ChatService.plausible("psu", 725_000_000L)).isFalse();
        // Mã ram_ddr4 là mô hình 3D chung cho mọi thanh RAM: DDR5 vẫn hợp lệ (loại DDR so với bo ở ProductRules)
        assertThat(ChatService.fitsCode("ram_ddr4", "RAM Corsair Dominator Titanium 32GB (2x16GB) DDR5 6000")).isTrue();
    }

    @Test
    void cpuBoxIntelKemQuatTan() {
        assertThat(ChatService.stockCooler("CPU Intel Core i3 12100 Box (Socket 1700/ 4 Cores)")).isTrue();
        assertThat(ChatService.stockCooler("CPU Intel Core i5-14600K Box")).isFalse();
        assertThat(ChatService.stockCooler("CPU Intel Core i3 12100 Tray")).isFalse();
        assertThat(ChatService.stockCooler("CPU AMD Ryzen 5 5600 Box")).isFalse();
    }

    @Test
    void ssdKhongGhiDungLuongKhongCoiLaCungThongSo() {
        // Ca thật: "Kingston KC3000 2 2280 NVMe PCIe Gen4" là bản 4TB 9,45 triệu
        assertThat(ChatService.sameSpec("ssd", "SSD Kingston NV3 1TB", "Ổ cứng SSD Kingston KC3000 2 2280 NVMe PCIe Gen4")).isFalse();
        assertThat(ChatService.sameSpec("ssd", "SSD Kingston NV3 1TB", "SSD Lexar NM620 1TB")).isTrue();
    }

    @Test
    void docDungLuongMoiThanhRamTuSpec() {
        // Ca thật: spec "16 GB, 2 thanh" mà chọn 2 thanh 16GB (32GB, 6,78 triệu)
        assertThat(ChatService.ramWant("16 GB, 2 thanh, DDR4 3200", 2, false)).isEqualTo(8);
        assertThat(ChatService.ramWant("2x8GB DDR4", 2, false)).isEqualTo(8);
        assertThat(ChatService.ramWant("8GB x 2 DDR4", 2, false)).isEqualTo(8);
        assertThat(ChatService.ramWant("16 GB (2x8GB)", 2, true)).isEqualTo(16);
        assertThat(ChatService.ramWant("8 GB DDR4 3200", 1, false)).isEqualTo(8);
        assertThat(ChatService.ramWant("32 GB DDR5", 2, false)).isEqualTo(16);
    }

    @Test
    void voMiniPhaiGhiItxVaKhongNhanDeTanLaptop() {
        // Ca thật: dòng vỏ Mini là Xigmatek QUANTUM 4AF; dòng tản thấp là đế tản laptop X-Slim II
        assertThat(ChatService.fitsCode("case_mini_sff", "Vỏ máy tính Xigmatek QUANTUM 4AF")).isFalse();
        assertThat(ChatService.fitsCode("case_mini_sff", "Vỏ máy tính Jonsbo A4 Mini-ITX")).isTrue();
        assertThat(ChatService.fitsCode("low_profile_47", "Đế Tản Nhiệt Cooler Master X-Slim II")).isFalse();
        assertThat(ChatService.fitsCode("tower_air_155", "Đế tản nhiệt laptop Deepcool N80")).isFalse();
        assertThat(ChatService.specAmount("psu", "650W, 80 Plus, bảo hành 5 năm")).isEqualTo(650);
    }

    @Test
    void kitGskillGhiSoThanhTrongSkuVanMuaMotBoVaHienHaiThanh() {
        String title = "RAM G.SKILL Trident Z5 32 GB-DDR5-6000 MHz (F5-6000U4040E16GX2-TZ5K)";
        assertThat(ChatService.kitSticks(title)).isEqualTo(2);
        assertThat(ChatService.kitSticks(title.replace("16GX2", "16GA2"))).isEqualTo(2);
        assertThat(ChatService.kitSticks("RAM G.SKILL Ripjaws S5 16GB DDR5")).isNull();
        assertThat(ChatService.kitSticks("VGA RTX 4060 X2 8GB")).isNull();
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RAM")).thenReturn(List.of(new ProductSearch.Product(title,"A",4_199_000L,"https://a.vn/gskill",null)));
        when(search.offer("https://a.vn/gskill")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                title,"A",4_199_000L,null,"https://a.vn/gskill",null,true,36,java.time.Instant.now()));
        when(search.directLink(any(),any(),any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s","search_products","{\"query\":\"RAM\"}"),toolCall("p","propose_build","""
                {"tier":"balanced","budget_vnd":10000000,"items":[{"code":"ram_ddr4","quantity":1,"spec":"32GB DDR5","product_id":"s1-1"}]}
                """),answer("Xong."));
        var ram = service.reply(new ChatRequest(List.of(new Message("user","RAM 32GB, ngân sách 10 triệu")),null,null,"advice"))
                .build().items().getFirst();
        assertThat(ram.quantity()).isEqualTo(2);
        assertThat(ram.spec()).contains("2x16GB");
        assertThat(ram.priceMaxVnd()).isEqualTo(4_199_000L);
    }

    @Test
    void giaKhongDoiVanPhaiLaySkuKitTuTrangSanPham() {
        String generic = "RAM Kingston Fury 32 GB-DDR4-3200 MHz";
        String actual = generic + " (KF432C16BB1AK2/32)";
        assertThat(ChatService.kitSticks(actual)).isEqualTo(2);
        assertThat(ChatService.kitSticks("RAM Kingston 32GB KF432C16BB/32")).isNull();
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("ram_ddr4")).thenReturn(part("ram_ddr4", "ram", "RAM"));
        when(search.enabled()).thenReturn(true);
        when(search.search("RAM")).thenReturn(List.of(new ProductSearch.Product(generic,"A",4_390_000L,"https://a.vn/kingston",null)));
        when(search.offer("https://a.vn/kingston")).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                actual,"A",4_390_000L,null,"https://a.vn/kingston",null,true,36,java.time.Instant.now()));
        when(search.directLink(any(),any(),any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s","search_products","{\"query\":\"RAM\"}"),toolCall("p","propose_build","""
                {"tier":"balanced","budget_vnd":10000000,"items":[{"code":"ram_ddr4","quantity":1,"spec":"32GB DDR4","product_id":"s1-1"}]}
                """),answer("Xong."));
        var ram = service.reply(new ChatRequest(List.of(new Message("user","RAM 32GB, ngân sách 10 triệu")),null,null,"advice"))
                .build().items().getFirst();
        assertThat(ram.productName()).contains("KF432C16BB1AK2/32");
        assertThat(ram.quantity()).isEqualTo(2);
        assertThat(ram.spec()).contains("2x16GB");
        assertThat(ram.priceMaxVnd()).isEqualTo(4_390_000L);
    }

    @Test
    void kitRamQuenBuyQtyThiHieuLaMotBo() {
        // Ca thật: kit 2x16GB với quantity 2 bị tính 2 bộ (64GB, 14 triệu)
        assertThat(ChatService.kitSticks("Ram Corsair Vengeance RGB 32GB (2x16GB) 6000 DDR5")).isEqualTo(2);
        assertThat(ChatService.kitSticks("Ram SSTC 16GB DDR4 3200MHz")).isNull();
    }

    @Test
    void mayHocAiCanCardNvidia12GbVaKhongPhaiMayVanPhong() {
        var n = ChatService.needs(List.of("Mình học AI, cần chạy mô hình deep learning nhỏ trên GPU, ngân sách khoảng 35 triệu."));
        assertThat(n.cuda()).isTrue();
        assertThat(n.allowedVendor()).isEqualTo("nvidia");
        assertThat(n.office()).as("chữ \"học\" không làm thành máy văn phòng").isFalse();
        assertThat(n.vramMin()).isEqualTo(12);
        assertThat(n.ramMin()).isEqualTo(16);
        assertThat(n.ssdMin()).isEqualTo(512);
        assertThat(ChatService.needs(List.of("máy học online, ai trong nhà cũng dùng được, 12 triệu")).cuda())
                .as("\"ai\" viết thường là đại từ").isFalse();
        assertThat(ChatService.cudaOk(Map.of("gpu_280", Map.of("gpu_vendor", "nvidia")), "gpu_280",
                new ProductSearch.Product("Card ASUS Dual Radeon RX 7700 XT 12GB", "s", 12_000_000L, "https://a/b", null))).isFalse();
        assertThat(ChatService.cudaOk(Map.of("gpu_280", Map.of("gpu_vendor", "nvidia")), "gpu_280",
                new ProductSearch.Product("Card ASUS Dual GeForce RTX 4070 12GB", "s", 15_000_000L, "https://a/b", null))).isTrue();
    }

    @Test
    void tuKhoaCpuTheoMaGon() {
        assertThat(ChatService.cpuQuery("i3-12100f")).isEqualTo("CPU Intel Core i3 12100F");
        assertThat(ChatService.cpuQuery("ryzen5-7500f")).isEqualTo("CPU AMD Ryzen 5 7500F");
        assertThat(ChatService.GAME_CPUS.get("LGA1700")).contains(ChatService.cpuModel("Bộ vi xử lý Intel Core i3 12100F / 3.3GHz"));
    }

    @Test
    void hangCardNguoiDungDoi() {
        assertThat(ChatService.gpuVendor(List.of("Đổi sang card AMD giúp mình", "Mình chơi game 25 triệu, CPU Intel"))).isEqualTo("amd");
        assertThat(ChatService.gpuVendor(List.of("Thôi hãng nào cũng được", "Đổi sang card AMD"))).isNull();
        assertThat(ChatService.gpuVendor(List.of("Mình muốn dùng CPU AMD Ryzen, chơi game 25 triệu"))).as("CPU AMD không phải card AMD").isNull();
        assertThat(ChatService.gpuVendor(List.of("cần RTX 4060 cho game"))).isEqualTo("nvidia");
        assertThat(ChatService.gpuVendor(List.of("cần máy chơi game 20 triệu có card NVIDA"))).as("viết sai NVIDIA").isEqualTo("nvidia");
        assertThat(ChatService.gpuVendor(List.of("muốn card nvdia"))).isEqualTo("nvidia");
        assertThat(ChatService.needs(List.of("cần máy chơi game 20 triệu có card NVIDA")).allowedVendor()).isEqualTo("nvidia");
        assertThat(ChatService.needs(List.of("cần máy chơi game 20 triệu có card NVIDA")).vramMin()).isEqualTo(8);
        assertThat(ChatService.needs(List.of("cần máy chơi game 20 triệu có card NVIDIA 8GB")).explicitGpuVram()).isTrue();
        assertThat(ChatService.needs(List.of("cần máy chơi game 20 triệu có card NVIDA")).explicitGpuVram()).isFalse();
        assertThat(ChatService.needs(List.of("cần card NVIDA và RAM 8GB để chơi game")).explicitGpuVram()).isFalse();
        assertThat(ChatService.gpuVendor(List.of("RTX 4060 hay RX 7600 tốt hơn?"))).as("so sánh hai hãng").isNull();
        var n = ChatService.needs(List.of("Đổi sang card AMD giúp mình", "Mình chơi game và livestream, 25 triệu, thích kính"));
        assertThat(n.allowedVendor()).isEqualTo("amd");
        assertThat(ChatService.needs(List.of("chơi game 25 triệu")).allowedVendor()).as("không nói hãng thì đổi khác hãng được").isNull();
        assertThat(ChatService.needs(List.of("Đổi card đồ họa sang AMD giúp mình nhé, vẫn trong ngân sách đó.",
                "Mình chơi game online và thỉnh thoảng livestream, có khoảng 25 triệu, thích máy có kính.")).vramMin())
                .as("card đồ họa là linh kiện, không phải làm đồ họa").isEqualTo(8);
        assertThat(ChatService.needs(List.of("mình học thiết kế đồ họa, 30 triệu")).vramMin()).isEqualTo(6);
        assertThat(ChatService.needs(List.of("Mình muốn máy thật nhỏ gọn kiểu mini ITX để đặt cạnh TV, chơi game nhẹ, khoảng 25 triệu.")).vramMin())
                .as("game nhẹ: card 6GB đủ").isEqualTo(6);
    }

    @Test
    void gameDoHoaCaoVanLaGameVaGiuSanVram() {
        var needs = ChatService.needs(List.of("Chơi game đồ họa cao ở Full HD, 33 triệu, RAM 32GB."));
        assertThat(needs.vramMin()).isEqualTo(8);
        assertThat(ChatService.adviceIntro(needs, 33_000_000L)).contains("máy chơi game");
        assertThat(ChatService.needs(List.of("Game Valorant và dựng video, 30 triệu")).vramMin()).isEqualTo(6);
        assertThat(ChatService.needs(List.of("làm đồ họa Photoshop 30 triệu")).vramMin()).isEqualTo(6);
    }

    @Test
    void thietKePhanBietTacVuVaKhongNhanNhamTenCard() {
        for (String text : List.of("Học thiết kế đồ họa Photoshop Illustrator20 triệu", "Chỉnh ảnh Lightroom20 triệu", "Dùng Illustrator20 triệu")) {
            var n = ChatService.needs(List.of(text));
            assertThat(n.creator()).as(text).isTrue();
            assertThat(n.office()).isFalse();
            assertThat(n.vramMin()).isEqualTo(6);
            assertThat(ChatService.adviceIntro(n,20_000_000L)).contains("thiết kế đồ họa").doesNotContain("máy chơi game");
        }
        for (String text : List.of("Dựng3D Blender Cycles30 triệu", "Render3D30 triệu", "Dựng video4K Premiere30 triệu", "After Effects30 triệu"))
            assertThat(ChatService.needs(List.of(text)).vramMin()).as(text).isEqualTo(8);
        assertThat(ChatService.needs(List.of("Chơi game đồ hoạ cao30 triệu")).creator()).isFalse();
        assertThat(ChatService.needs(List.of("Đổi card đồ hoạ sang AMD", "Chơi game30 triệu")).creator()).isFalse();
        assertThat(ChatService.adviceIntro(ChatService.needs(List.of("Chơi Valorant và dựng video30 triệu")),30_000_000L))
                .contains("chơi game và thiết kế");
    }

    @Test
    void doiNhuCauThietKeGiuDungYeuCauMoiVeGpu() {
        var creator=ChatService.needs(List.of("Giờ chuyển sang Blender render3D30 triệu", "Máy văn phòng không cần card rời20 triệu"));
        assertThat(creator.creator()).isTrue();
        assertThat(creator.vramMin()).isEqualTo(8);
        var integrated=ChatService.needs(List.of("Photoshop2D nhẹ20 triệu, không cần card rời"));
        assertThat(integrated.creator()).isTrue();
        assertThat(integrated.vramMin()).isNull();
        assertThat(ChatService.needs(List.of("Chỉ học online Word, không cần card rời", "Blender render3D30 triệu")).creator()).isFalse();
        assertThat(ChatService.needs(List.of("RAM lên32GB, giữ ngân sách", "Dùng Illustrator20 triệu")).creator()).isTrue();
    }

    @Test
    void giaiThichThietKeKhongThanhVanPhongHoacHuaFps() {
        var gpu=new Line("gpu_240","gpu","Card",1,null,null,5_000_000L,5_000_000L,null,"https://a/gpu","RTX 3050 6GB","A");
        var card=new ChatResponse.BuildCard(List.of(gpu),true,List.of(),List.of(),null,20_000_000L,null,"balanced",1);
        assertThat(ChatService.adviceExplanation(ChatService.needs(List.of("Photoshop20 triệu")),card))
                .contains("tăng tốc GPU","không phải mọi phần mềm").doesNotContain("FPS","nhu cầu học và văn phòng");
        var cpu=new Line("cpu_std","cpu","CPU",1,null,null,4_000_000L,4_000_000L,null,"https://a/cpu","Intel Core i5 12400","A");
        var noGpu=new ChatResponse.BuildCard(List.of(cpu),true,List.of(),List.of(),null,20_000_000L,null,"balanced",1);
        assertThat(ChatService.adviceExplanation(ChatService.needs(List.of("Photoshop20 triệu không cần card rời")),noGpu))
                .contains("Theo yêu cầu không mua card rời","phần mềm, phiên bản và driver").doesNotContain("nhu cầu học và văn phòng");
    }

    @Test
    void aiBoCardThietKeThiKhongDuocHienCauHinhIgpuDuLapDuoc() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true,List.of(),List.of()));
        when(components.findByCode("cpu_std")).thenReturn(part("cpu_std","cpu","CPU"));
        when(search.enabled()).thenReturn(true);
        var cpu=new ProductSearch.Product("CPU Intel Core i5 12400","A",4_000_000L,"https://a.vn/cpu",null);
        when(search.search("parts")).thenReturn(List.of(cpu));
        when(search.directLink(any(),any(),any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s","search_products","{\"query\":\"parts\"}"),toolCall("p","propose_build","""
                {"tier":"balanced","summary":"Thiết kế","budget_vnd":20000000,"items":[
                {"code":"cpu_std","spec":"Core i5 12400","product_id":"s1-1"}]}
                """),answer("Dùng iGPU đủ mọi thiết kế."));
        var res=service.reply(new ChatRequest(List.of(new Message("user","PC Illustrator20 triệu")),null,null,"advice"));
        assertThat(res.build()).isNull();
        assertThat(res.reply()).contains("thiếu card đồ họa rời theo nhu cầu").doesNotContain("đủ mọi thiết kế");
    }

    @Test
    void khongDoiCardDungVramSangTenKhongDocDuocVram() {
        var current = product("VGA ASUS RTX 3060 12GB", 12_690_000L);
        var unclear = product("VGA ASUS Dual Geforce RTX 3060 O8G White", 7_490_000L);
        assertThat(ChatService.equivalents("gpu", current, List.of(unclear), false, true, false, 8)).isEmpty();
        assertThat(ChatService.sameSpec("gpu", current.title(), unclear.title())).isFalse();
        assertThat(ChatService.cudaOk(Map.of("gpu_240", Map.of("vram_min", 8)), "gpu_240", unclear)).isFalse();
        assertThat(ChatService.fitsCode("case_compact_tower", "Vỏ Corsair FRAME 4000D Modular Mid-Tower White")).isFalse();
        assertThat(ChatService.fitsCode("case_compact_tower", "Vỏ Micro-ATX Mid-Tower")).isTrue();
    }

    @Test
    void giuDungModelKhiAiChonSaiVaTenShopCoThuongHieu() {
        var res = lockedGpuResponse(true);
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).anySatisfy(l -> assertThat(l.productName()).isEqualTo("VGA ASUS Dual GeForce RTX™ 4070 12GB"));
        assertThat(res.build().items()).noneSatisfy(l -> assertThat(l.category()).isEqualTo("tools"));
        assertThat(ChatService.gpuChip("ASUS RTX® 4070 Ti SUPER 16GB")).isEqualTo("rtx4070tisuper");
    }

    @Test
    void modelBiKhoaHetHangThiKhongTuDoiSangSuper() {
        assertThat(lockedGpuResponse(false).build()).isNull();
    }

    private ChatResponse lockedGpuResponse(boolean available) {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(components.findByCode("tool_kit")).thenReturn(part("tool_kit", "tools", "Bộ đồ nghề lắp máy"));
        when(search.enabled()).thenReturn(true);
        var wrong = new ProductSearch.Product("VGA ASUS RTX 4070 SUPER 12GB", "A", 19_000_000L, "https://a.vn/super", null);
        var exact = new ProductSearch.Product("VGA ASUS Dual GeForce RTX™ 4070 12GB", "A", 15_590_000L, "https://a.vn/exact", null);
        when(search.search(any())).thenReturn(available ? List.of(wrong, exact) : List.of(wrong));
        when(search.engineSearch(any())).thenReturn(List.of());
        for (var p : List.of(wrong, exact)) when(search.offer(p.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                p.title(), "A", p.priceVnd(), null, p.url(), null, true, 36, java.time.Instant.now()));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"VGA\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","budget_vnd":48000000,"items":[
                {"code":"gpu_280","spec":"RTX 4070 SUPER 12GB","product_id":"s1-1"},{"code":"tool_kit"}]}
                """), answer("Xong."), answer("Xong."));
        return service.reply(new ChatRequest(List.of(new Message("user", "PC chơi game 48 triệu, bắt buộc đúng RTX 4070 12GB, không đổi model card")), null, null, "advice"));
    }

    @Test
    void aiCoTheDoiCardChuaCoHangHieuNangTheoSanVramVaNganSach() {
        var result = unknownGpuBudgetResponse("Stable Diffusion local, 20 triệu, NVIDIA tối thiểu 16GB VRAM");
        assertThat(result.build()).isNotNull();
        assertThat(result.build().items().getFirst().productName()).contains("RTX 4060 Ti").contains("16GB");
        assertThat(result.build().recommendations()).anySatisfy(i -> assertThat(i.message()).contains("chưa có benchmark").doesNotContain("hiệu năng GPU thấp hơn"));
    }

    @Test
    void aiKhoaModelThiKhongDoiCardDuChuaCoTrongBangHang() {
        assertThat(unknownGpuBudgetResponse("Stable Diffusion, 20 triệu, bắt buộc đúng RTX 4080 SUPER 16GB, không đổi model").build()).isNull();
    }

    @Test
    void gameKhongTuDoanXepHangCuaChipChuaBiet() {
        assertThat(unknownGpuBudgetResponse("Chơi game 20 triệu, NVIDIA ít nhất 16GB VRAM").build()).isNull();
    }

    private ChatResponse unknownGpuBudgetResponse(String question) {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        var expensive = new ProductSearch.Product("Card GIGABYTE AORUS GeForce RTX 4080 SUPER XTREME ICE 16G", "A", 39_990_000L, "https://a.vn/4080", null);
        var affordable = new ProductSearch.Product("Cạc màn hình Inno3D GEFORCE RTX 4060 Ti TWIN X2 16GB", "A", 11_399_000L, "https://a.vn/4060ti", null);
        when(search.search(any())).thenReturn(List.of(expensive, affordable));
        when(search.engineSearch(any())).thenReturn(List.of());
        for (var p : List.of(expensive, affordable)) when(search.offer(p.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                p.title(), "A", p.priceVnd(), null, p.url(), null, true, 36, java.time.Instant.now()));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"GPU\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","budget_vnd":20000000,"items":[{"code":"gpu_280","spec":"RTX 4080 SUPER 16GB","product_id":"s1-1"}]}
                """), answer("Xong."), answer("Xong."));
        return service.reply(new ChatRequest(List.of(new Message("user", question)), null, null, "advice"));
    }

    @Test
    void cardNvidia6GbChiDungKhiGameVuotTranVaKhongDoi8Gb() {
        var build = nvidiaBudgetResponse("chơi game 7 triệu, cần card NVIDA").build();
        var card = build.items().getFirst();

        assertThat(card.productName()).isEqualTo("VGA MSI GeForce RTX 3050 6GB");
        assertThat(card.priceMaxVnd()).isEqualTo(6_000_000L);
        assertThat(build.recommendations()).anySatisfy(w -> assertThat(w.message()).contains("6GB VRAM").contains("8GB"));
    }

    @Test
    void cardNvidiaGhiRo8GbThiKhongHaXuong6Gb() {
        assertThat(nvidiaBudgetResponse("chơi game 7 triệu, cần card NVIDA 8GB").build()).isNull();
    }

    @Test
    void giamCpuVanGiuDu8GbThiKhongHaVramSom() {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("cpu_std")).thenReturn(part("cpu_std", "cpu", "CPU"));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        var cpu = new ProductSearch.Product("CPU Intel Core i5 12400F", "A", 4_000_000L, "https://a.vn/cpu", null);
        var small = new ProductSearch.Product("CPU Intel Core i3 13100F", "A", 2_900_000L, "https://a.vn/i3", null);
        var gpu = new ProductSearch.Product("VGA ASUS RTX 4060 8GB", "A", 9_000_000L, "https://a.vn/gpu", null);
        var six = new ProductSearch.Product("VGA MSI RTX 3050 6GB", "A", 6_000_000L, "https://a.vn/six", null);
        when(search.search(any())).thenReturn(List.of(cpu, small, gpu, six));
        for (var p : List.of(cpu, small, gpu, six)) when(search.offer(p.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                p.title(), "A", p.priceVnd(), null, p.url(), null, true, 36, java.time.Instant.now()));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"parts\"}"), toolCall("p", "propose_build", """
                {"tier":"balanced","summary":"Game","budget_vnd":12000000,"items":[
                {"code":"cpu_std","product_id":"s1-1"},{"code":"gpu_280","product_id":"s1-3"}]}
                """), answer("Xong."), answer("Xong."));
        var res = service.reply(new ChatRequest(List.of(new Message("user", "chơi game 12 triệu, dùng card NVIDIA")), null, null, "advice"));
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).anySatisfy(l -> {
            assertThat(l.category()).isEqualTo("gpu"); assertThat(l.productName()).contains("RTX 4060 8GB");
        });
    }

    @Test
    void cpuKhacSocketThiSuaTheoBoKhiNguoiDungChuaChotCpu() {
        var res = mixedSocketResponse("chơi game 32 triệu");
        assertThat(res.build()).isNotNull();
        assertThat(res.build().items()).anySatisfy(line -> {
            assertThat(line.category()).isEqualTo("cpu"); assertThat(line.productName()).contains("7500F");
        });
    }

    @Test
    void doiNenTangKhongDuocTuDoiCpuNguoiDungDaChiDinh() {
        assertThat(mixedSocketResponse("chơi game 32 triệu, phải dùng CPU AMD Ryzen 5 5600X").build()).isNull();
    }

    private ChatResponse mixedSocketResponse(String text) {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        for (String[] p : List.of(new String[]{"cpu_std","cpu"}, new String[]{"mb_itx","motherboard"},
                new String[]{"ram_ddr4","ram"}, new String[]{"gpu_240","gpu"})) {
            when(components.findByCode(p[0])).thenReturn(part(p[0],p[1],p[1]));
        }
        when(search.enabled()).thenReturn(true);
        var products = List.of(new ProductSearch.Product("CPU AMD Ryzen 5 5600X", "A", 4_690_000L, "https://a.vn/am4", null),
                new ProductSearch.Product("Mainboard JGINYUE B650I NIGHT DEVIL DDR5", "A", 3_290_000L, "https://a.vn/mb", null),
                new ProductSearch.Product("RAM Kingmax 16GB DDR5", "A", 6_490_000L, "https://a.vn/ram", null),
                new ProductSearch.Product("VGA MSI RTX 4060 8GB", "A", 8_990_000L, "https://a.vn/gpu", null),
                new ProductSearch.Product("CPU AMD Ryzen 5 7500F", "A", 4_599_000L, "https://a.vn/am5", null));
        when(search.search(any())).thenReturn(products);
        for(var p:products) when(search.offer(p.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                p.title(),"A",p.priceVnd(),null,p.url(),null,true,36,java.time.Instant.now()));
        when(search.directLink(any(),any(),any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s","search_products","{\"query\":\"parts\"}"),toolCall("p","propose_build","""
                {"tier":"balanced","summary":"Game","budget_vnd":32000000,"items":[
                {"code":"cpu_std","product_id":"s1-1"},{"code":"mb_itx","product_id":"s1-2"},
                {"code":"ram_ddr4","spec":"16GB DDR5","product_id":"s1-3"},{"code":"gpu_240","product_id":"s1-4"}]}
                """),answer("Xong."),answer("Xong."),answer("Xong."));
        return service.reply(new ChatRequest(List.of(new Message("user",text)),null,null,"advice"));
    }

    private ChatResponse nvidiaBudgetResponse(String userText) {
        when(compatibility.check(any())).thenReturn(new CompatibilityResponse(true, List.of(), List.of()));
        when(components.findByCode("gpu_280")).thenReturn(part("gpu_280", "gpu", "Card"));
        when(search.enabled()).thenReturn(true);
        var r4060 = new ProductSearch.Product("Card ASUS Dual GeForce RTX 4060 8GB", "A", 9_000_000L, "https://a.vn/4060", null);
        var r3050 = new ProductSearch.Product("VGA MSI GeForce RTX 3050 6GB", "B", 6_000_000L, "https://b.vn/3050", null);
        when(search.search("card")).thenReturn(List.of(r4060));
        when(search.search("VGA RTX 3050")).thenReturn(List.of(r3050));
        when(search.offer(r4060.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                r4060.title(), "A", r4060.priceVnd(), null, r4060.url(), null, true, 36, java.time.Instant.now()));
        when(search.offer(r3050.url())).thenReturn(new com.smartcarepc.backend.service.price.Offer(
                r3050.title(), "B", r3050.priceVnd(), null, r3050.url(), null, true, 36, java.time.Instant.now()));
        when(search.directLink(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
        reply(toolCall("s", "search_products", "{\"query\":\"card\"}"), toolCall("p1", "propose_build", """
                {"tier":"balanced","summary":"Game","budget_vnd":7000000,"items":[{"code":"gpu_280","spec":"RTX 4060 8GB","reason":"Đủ","product_id":"s1-1"}]}
                """), answer("Xong."));
        return service.reply(new ChatRequest(List.of(new Message("user", userText)), null, null, "advice"));
    }

    @Test
    void chipCardYeuHonVaTuKhoaTheoChip() {
        assertThat(ChatService.GPU_RANK).containsKey("rtx5070");
        assertThat(ChatService.gpuDowngrades("rtx4060", 5)).containsExactly("rx6650xt", "rx7600", "rtx3060");
        assertThat(ChatService.gpuDowngrades("rtx5070", 15).getFirst()).isEqualTo("rtx4070super");
        assertThat(ChatService.gpuDowngrades("xyz", 5)).isEmpty();
        assertThat(ChatService.chipQuery("rtx4060ti")).isEqualTo("VGA RTX 4060 TI");
        assertThat(ChatService.chipQuery("rx6600")).isEqualTo("VGA RX 6600");
        assertThat(ChatService.vendorOf("rtx4060")).isEqualTo("nvidia");
        assertThat(ChatService.vendorOf("rx7600")).isEqualTo("amd");
    }

    @Test
    void voMiniItxGhiNguonAtxVanLaVoItx() {
        assertThat(ChatService.fitsCode("case_mini_sff", "Vỏ case Cooler Master NR200P Mini-ITX, hỗ trợ nguồn SFX/ATX")).isTrue();
        assertThat(ChatService.fitsCode("case_mini_sff", "Case Lian Li Q58 Mini ITX (PSU ATX)")).isTrue();
        assertThat(ChatService.fitsCode("case_mini_sff", "Vỏ Xigmatek Gaming X II ATX")).isFalse();
        assertThat(ChatService.fitsCode("case_mini_sff", "Vỏ case mATX Mini-ITX VSP")).isFalse();
        assertThat(ChatService.fitsCode("case_gaming", "Vỏ case Montech AIR 903 ATX")).isTrue();
        assertThat(ChatService.fitsCode("case_mini_sff", "Vỏ Case Mini Silverstone Sugo 13 Black SST-SG13B-Q")).as("tên mẫu ITX").isTrue();
        assertThat(ChatService.fitsCode("case_compact_tower", "Vỏ Case Mini Silverstone Sugo 13 Black SST-SG13B-Q")).isFalse();
        assertThat(ChatService.fitsCode("mb_itx", "Bo mạch chủ ASRock B760M-ITX/D4")).isTrue();
        assertThat(ChatService.sameSpec("case", "Vỏ máy tính Jetek Mini I8-22B (ITX)", "Vỏ Case Mini Silverstone Sugo 13 Black SST-SG13B-Q"))
                .as("vỏ ITX thay bằng vỏ ITX nhận theo tên mẫu").isTrue();
        assertThat(ChatService.fitsCode("psu_sfx", "Nguồn Corsair SF600 600W 80 Plus Platinum")).as("model SFX không ghi chữ SFX").isTrue();
        assertThat(ChatService.fitsCode("psu_sfx", "Nguồn SilverStone SX500-G 500W Gold")).isTrue();
        assertThat(ChatService.fitsCode("psu_sfx", "Nguồn Cooler Master V850 SFX-L Gold")).as("SFX-L dài hơn mẫu 3D").isFalse();
        assertThat(ChatService.fitsCode("psu_sfx", "Nguồn FSP HV PRO 650W 80 Plus Bronze")).isFalse();
    }

    @Test
    void nhanRaNganSachTrongCauNguoiDung() {
        assertThat(ChatService.hasBudget("ngân sách khoảng 40 triệu")).isTrue();
        assertThat(ChatService.hasBudget("có 25tr, thích máy có kính")).isTrue();
        assertThat(ChatService.hasBudget("tầm 12.000.000 đồng")).isTrue();
        assertThat(ChatService.hasBudget("mình học thiết kế, cần máy êm")).isFalse();
    }

    @Test
    void daCoNganSachVanChoHoiThemWorkloadTruocKhiDungMay() {
        reply(answer("Bạn dựng video ở độ phân giải nào?"));
        var res = service.reply(new ChatRequest(List.of(new Message("user", "Mình dựng video, ngân sách khoảng 40 triệu")), null, null, "advice"));
        assertThat(res.reply()).isEqualTo("Bạn dựng video ở độ phân giải nào?");
        assertThat(res.build()).isNull();
        verify(client,org.mockito.Mockito.times(1)).complete(any());
    }

    @Test
    void hoi2d3dHoacGameKhongBiEpThanhBaoGia() {
        for (String q : List.of("Ngân sách20 triệu: bạn chủ yếu làm2D hay dựng3D/render?", "Bạn chơi game nhẹ/esports hay game nặng/AAA?")) {
            org.mockito.Mockito.reset(client);
            reply(answer(q));
            var res=service.reply(new ChatRequest(List.of(new Message("user","Cần máy thiết kế và chơi game20 triệu")),null,null,"advice"));
            assertThat(res.reply()).isEqualTo(q);
            assertThat(res.build()).isNull();
            verify(client,org.mockito.Mockito.times(1)).complete(any());
        }
        assertThat(ChatService.workloadQuestion("Bạn sẽ dùng máy chủ yếu làm gì: học / văn phòng, chơi game, đồ họa hay AI?")).isFalse();
    }

    @Test
    void traLoiNganGiuNganSachRamVaDungHoSo() {
        var graphic=List.of(new Message("user","PC thiết kế30 triệu RAM32GB SSD1TB"),
                new Message("assistant","Bạn làm2D hay3D/render, dùng phần mềm nào?"),new Message("user","3D"));
        var texts=ChatService.adviceUserTexts(graphic);
        assertThat(ChatService.needs(texts).creator()).isTrue();
        assertThat(ChatService.needs(texts).vramMin()).isEqualTo(8);
        assertThat(ChatService.needs(texts).ramMin()).isEqualTo(32);
        assertThat(ChatService.needs(texts).ssdMin()).isEqualTo(1000);
        assertThat(ChatService.amounts(texts)).contains(30_000_000L);
        var twoD=ChatService.adviceUserTexts(List.of(graphic.get(0),graphic.get(1),new Message("user","Không làm3D, chỉ2D")));
        assertThat(ChatService.needs(twoD).vramMin()).isEqualTo(6);
        var light=ChatService.adviceUserTexts(List.of(new Message("user","PC game25 triệu, trước chơi game nặng"),
                new Message("assistant","Bạn chơi game nhẹ hay game nặng?"),new Message("user","Giờ chỉ nhẹ thôi")));
        assertThat(ChatService.needs(light).vramMin()).isEqualTo(6);
        assertThat(ChatService.amounts(light)).contains(25_000_000L);
    }

    @Test
    void hoiLaiCungContextSauTraLoiThiNhacDungThongTinDaCo() {
        reply(answer("Bạn làm2D hay3D?"),answer("Mình dùng thông tin bạn vừa bổ sung."));
        var res=service.reply(new ChatRequest(List.of(new Message("user","Thiết kế20 triệu"),
                new Message("assistant","Bạn làm2D hay3D/render?"),new Message("user","2D")),null,null,"advice"));
        assertThat(res.reply()).isEqualTo("Mình dùng thông tin bạn vừa bổ sung.");
        assertThat(messages(1).toString()).contains("làm luôn ba phương án");
    }

    @Test
    void khachMuonTuVanLuonThiKhongHoiVongLap() {
        reply(answer("Bạn chơi game nhẹ hay game nặng?"),answer("Mình sẽ nêu giả định."));
        var res=service.reply(new ChatRequest(List.of(new Message("user","PC chơi game25 triệu, tư vấn luôn, không hỏi thêm")),null,null,"advice"));
        assertThat(res.reply()).isEqualTo("Mình sẽ nêu giả định.");
        assertThat(messages(1).toString()).contains("làm luôn ba phương án");
    }

    @Test
    void doiTuGameSangThietKeVanDuocHoiContextMoi() {
        reply(answer("Bạn thiết kế2D hay dựng3D/render?"));
        var res=service.reply(new ChatRequest(List.of(new Message("user","Máy game25 triệu"),
                new Message("assistant","Bạn chơi game nhẹ hay game nặng?"),new Message("user","Nhẹ"),
                new Message("assistant","Đã ghi nhận game nhẹ."),new Message("user","Đổi sang máy thiết kế30 triệu")),null,null,"advice"));
        assertThat(res.reply()).contains("2D","3D");
        verify(client,org.mockito.Mockito.times(1)).complete(any());
    }

    private static ProductSearch.Product product(String title, long price) {
        return new ProductSearch.Product(title, "GEARVN", price, "https://gearvn.com/products/" + Math.abs(title.hashCode()), null);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> messages(int call) {
        return (List<Map<String, Object>>) sent.get(call).get("messages");
    }
}
