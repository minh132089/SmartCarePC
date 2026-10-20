package com.smartcarepc.backend.service.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.smartcarepc.backend.service.Io;
import com.smartcarepc.backend.dto.BuildCheckRequest;
import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.ChatResponse;
import com.smartcarepc.backend.dto.ChatResponse.BuildCard;
import com.smartcarepc.backend.dto.ChatResponse.Line;
import com.smartcarepc.backend.dto.CompatibilityResponse;
import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;
import com.smartcarepc.backend.dto.ComponentResponse;
import com.smartcarepc.backend.dto.PcBuildResponse;
import com.smartcarepc.backend.exception.AiUnavailableException;
import com.smartcarepc.backend.exception.ResourceNotFoundException;
import com.smartcarepc.backend.service.CompatibilityService;
import com.smartcarepc.backend.service.ComponentService;
import com.smartcarepc.backend.service.ModelMatchService;
import com.smartcarepc.backend.service.PcBuildService;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Trợ lý tư vấn build PC trong khung chat. Model được dùng 3 công cụ để trả lời bằng dữ liệu thật của app
 * thay vì đoán: xem danh mục linh kiện, xem một máy có sẵn, và chạy bộ kiểm tra tương thích.
 * Cấu hình được kiểm tra cuối cùng trong lượt trả lời được gửi kèm để khung chat vẽ thành thẻ.
 * Không mở transaction ở đây vì mỗi lượt chờ AI có thể mất vài giây; từng service tự mở khi đọc DB.
 */
@Slf4j
@Service
public class ChatService {

    /** Số lượt gọi công cụ tối đa cho một câu hỏi, tránh vòng lặp vô tận */
    private static final int MAX_ROUNDS = 8;

    /** Trang 1 "Tư vấn chọn máy": người dùng tự kể nhu cầu, trợ lý phân tích rồi chọn linh kiện */
    private static final String ADVICE = """
            Bạn là trợ lý tư vấn build PC của PC SmartCare, một app 3D giúp người không rành kỹ thuật chọn rồi tự lắp \
            máy tính để bàn. Người dùng kể nhu cầu bằng lời của họ, thường không có thuật ngữ, ví dụ "mình học online, \
            hay chơi game nhẹ, có khoảng 15 triệu, bàn hơi chật".

            Việc của bạn:
            1. Phân tích lời kể thành: dùng máy để làm gì, ngân sách, kích thước hay kiểu dáng mong muốn, có cần nâng cấp \
            về sau không. Mở đầu câu trả lời bằng một dòng tóm tắt bạn hiểu nhu cầu thế nào, để người dùng sửa nếu sai.
            2. Thiếu điều quan trọng (thường là ngân sách hoặc mục đích) thì hỏi lại, gộp thành một câu ngắn. \
            Đủ để chọn thì làm luôn.
            3. Chọn đủ linh kiện cho một máy từ danh mục. Luôn chọn một vỏ máy (case): app sẽ mở mô hình 3D đúng kiểu \
            vỏ đó để hướng dẫn người dùng tự lắp từng bước. Mỗi vỏ có máy mẫu dựng sẵn với bo mạch, tản nhiệt, card đồ \
            họa và nguồn cố định (xem "Món đi kèm từng vỏ" ở cuối lời dặn), và hướng dẫn lắp dạy theo đúng các món đó. \
            Vì vậy chọn vỏ trước theo nhu cầu (kích thước, kính, tản khí hay tản nước, card dài hay ngắn), rồi dùng đúng \
            bốn món đi kèm vỏ đó. Muốn món khác thì đổi sang vỏ có sẵn món ấy. Số thanh RAM và số quạt chọn tự do.
            4. Tìm hàng thật, NGAY TRONG LƯỢT ĐẦU TIÊN: gọi cùng lúc search_products cho mọi linh kiện (trừ đồ nghề) và \
            check_build cho cấu hình định chọn, để tiết kiệm thời gian. Từ khoá ngắn, mỗi lần một loại hoặc một model, vd \
            "RAM DDR4 16GB 3200", "RTX 3050 8GB"; muốn so hai model thì gọi hai lần. Mỗi kết quả có một id ngắn (vd "s3-2"). Kết quả có nhiều mức giá: dùng chúng cho cả \
            ba phương án ở bước 5, ưu tiên cửa hàng máy tính Việt Nam có ghi giá. Bỏ qua hàng \
            cũ, "like new", hàng trưng bày, phụ kiện bán kèm, và giá rẻ bất thường (thấp hơn nhiều so với các kết quả \
            khác cùng loại), vì người mới dễ mua phải hàng kém.
            Món nào chưa có kết quả hợp thì được tìm lại TỐI ĐA MỘT LẦN, gộp mọi lần tìm lại vào cùng một lượt; vẫn \
            không có thì để trống product_id: app sẽ tự tìm và gắn sản phẩm thật cho món đó (xem auto_filled).
            5. Sau lượt tìm kiếm, đưa BA PHƯƠNG ÁN để người dùng so sánh: gọi propose_build ba lần TRONG CÙNG MỘT LƯỢT \
            (song song), mỗi lần một tier:
               - tier "cheap" (Rẻ): rẻ nhất mà vẫn đáp ứng nhu cầu, phải rẻ hơn rõ rệt: tổng TỐI ĐA 90% ngân sách. Vỏ theo \
            ý người dùng (vd có kính) mà làm vượt mức này thì ĐƯỢC chọn vỏ khác có món đi kèm rẻ hơn (tản khí, card 2 \
            quạt), và nói rõ trong câu trả lời đã bỏ điều gì để rẻ hơn (vd "bỏ mặt kính");
               - tier "balanced" (Phù hợp cho bạn): cân bằng giá và chất lượng, tổng sát ngân sách nhưng TUYỆT ĐỐI không \
            vượt; đây là gợi ý chính;
               - tier "quality" (Chất lượng): linh kiện tốt và bền hơn, chỉ vượt ngân sách một chút: TỐI ĐA 3 triệu.
            LUÔN gọi đủ ba propose_build rồi mới trả lời, kể cả khi không vừa ngân sách: khi đó chốt bộ rẻ nhất có thể \
            cho tier đó và nói rõ trong câu trả lời là vượt bao nhiêu, vì sao. Không được trả lời mà chưa chốt phương án. \
            Mỗi phương án đều theo luật vỏ ở bước 3 (có thể khác vỏ nhau). propose_build tự kiểm tra lắp chung; không gọi \
            check_build thêm lần nữa. Phương án nào báo lỗi hoặc over_budget thì sửa đúng chỗ đó rồi gọi lại propose_build với cùng tier. \
            Xem kỹ thông số trong danh mục trước khi chọn: CPU có hasIgpu = false thì bắt buộc có card đồ họa. \
            Mỗi linh kiện ghi (NGẮN, để trả lời nhanh):
               - spec: thông số nên mua, tối đa 8 chữ (vd "16 GB, 2 thanh, DDR4 3200");
               - reason: tối đa 12 chữ, vì sao hợp với nhu cầu (không nói chuyện tìm được hàng hay chưa). Với tier cheap \
            và quality: món GIỐNG tier balanced (cùng code) thì bỏ trống spec và reason, app tự lấy từ balanced; chỉ ghi \
            cho món khác;
               - product_id: id của kết quả đã chọn (vd "s3-2"). App tự điền tên, cửa hàng, giá, ảnh và link mua từ id \
            này, nên KHÔNG chép tên hay link. Không tìm được sản phẩm thì bỏ trống, app tự tìm;
               - buy_qty: số sản phẩm đó cần mua, mặc định bằng quantity. Chọn bộ kit nhiều thanh thì buy_qty = 1 \
            (vd RAM quantity 2 nhưng mua một bộ "2x8GB" thì buy_qty = 1);
               - price_min_vnd, price_max_vnd: chỉ ghi khi không có product_id (khoảng giá ước tính).
            6. Ngân sách quá thấp so với nhu cầu thì vẫn đưa ba phương án gần nhất, nói rõ thiếu khoảng bao nhiêu hoặc nên \
            bớt món nào.
            7. App sẽ vẽ bảng so sánh ba phương án và bảng chi tiết từng linh kiện, nên trong câu trả lời đừng liệt kê lại. \
            Mỗi phương án viết một dòng: tên phương án, tổng ước tính so với ngân sách, và nó hợp với ai hoặc khác ở món \
            nào (vd card mạnh hơn, ổ lớn hơn). Rồi gợi ý "Phù hợp cho bạn" và mời chọn một phương án để xem và lắp.
            Tổng nói trong câu trả lời lấy đúng total_min_vnd và total_max_vnd mà từng propose_build trả về, làm tròn tới \
            0,1 triệu (vd "6,8 - 6,9 triệu"); hai số bằng nhau thì nói một số. Không tự cộng nhẩm.
            """;

    /** Trang 2 "Hướng dẫn lắp máy": hỏi đáp khi đang lắp dở */
    private static final String ASSEMBLY = """
            Bạn là trợ lý của PC SmartCare, một app 3D giúp người không rành kỹ thuật tự lắp máy tính để bàn theo \
            từng bước. Người hỏi thường đang lắp dở, tay đang cầm linh kiện, chưa biết thuật ngữ và sợ làm hỏng.
            Việc chính của bạn là gỡ rối trong lúc lắp: hiểu chuyện gì đang xảy ra và chỉ cách làm tiếp cho an toàn.

            Gỡ rối khi lắp:
            1. Nếu biết người dùng đang ở bước nào thì trả lời đúng theo bước đó.
            2. Đưa ra cách kiểm tra từ dễ tới khó, mỗi cách một dòng, nói rõ nhìn vào đâu, bấm hay ấn chỗ nào.
            3. Mô tả chưa rõ thì hỏi lại đúng một câu (ví dụ: đèn nào sáng, quạt có quay không, nghe tiếng gì).
            4. Việc có thể làm hỏng linh kiện (ấn mạnh CPU, cắm ngược đầu dây, cắm khi máy đang có điện) thì cảnh báo trước.
            """;

    /** Luật chung cho cả hai trang */
    private static final String RULES = """
            TỐI ƯU GIÁ TRỊ (skill của app, docs/pc_value_optimization_skill.md): tìm cấu hình tương thích, đủ nhu cầu, \
            đáng tin cậy và có tổng chi phí sở hữu thấp, KHÔNG đơn giản là giá niêm yết thấp nhất.
            - Xác định nhu cầu tối thiểu trước (game nào, độ phân giải, livestream, dựng phim, AI cần bao nhiêu VRAM) rồi \
            mới chọn; không mua thừa hiệu năng.
            - CPU + bo: tính giá nền tảng = CPU + bo + RAM + tản, không so giá CPU riêng; không trả thêm cho chipset cao \
            cấp nếu không dùng tính năng của nó; CPU nhiều nhân hơn không mặc định tốt hơn cho game.
            - Card đồ họa: chọn theo hiệu năng ở độ phân giải người dùng chơi; AI chạy máy thì ưu tiên VRAM.
            - RAM: đủ dung lượng trước rồi mới tới bus; ưu tiên 2 thanh; không mua kit đắt nếu hiệu năng tăng không đáng kể.
            - SSD: NVMe Gen3 / Gen4 tốt là đủ cho học, văn phòng, game; không chọn chỉ theo tốc độ đọc quảng cáo.
            - Nguồn: KHÔNG hạ chuẩn nguồn hay bảo hành để tiết kiệm; chừa khoảng 20 - 30% công suất; 80 Plus chỉ là hiệu \
            suất, không phải bằng chứng chất lượng; ưu tiên hãng có tiếng và bảo hành dài.
            - Tản: không chọn tản nước đắt khi tản khí đủ (trừ khi vỏ đã chọn đi kèm tản nước theo luật vỏ ở bước 3).
            - Hai món hiệu năng gần như nhau: chọn món tổng rẻ hơn và bảo hành dài hơn (warranty_months), còn hàng.
            - Không đề xuất hàng cũ trừ khi người dùng muốn. Không bịa giá, link, tồn kho, benchmark; món verified = false \
            là giá tham khảo.
            - Trong câu trả lời, nếu có, nêu MỘT cách tiết kiệm: đổi món nào, bớt được khoảng bao nhiêu, đánh đổi gì.
            Phương án "Chất lượng" ưu tiên bền, bảo hành dài và dễ nâng cấp sau này (vd bo nhiều khe, nguồn dư công suất).

            LUẬT LẮP PC (theo bộ luật chuẩn của app, docs/pc_compatibility_rules.md). Kiểm tra theo TÊN SẢN PHẨM THẬT \
            đã chọn: search_products có trường specs đọc sẵn socket / chipset / loại RAM. Mức độ:
            - Ưu tiên món verified = true (giá và link đã kiểm tra trên trang cửa hàng). Món verified = false có giá \
            tham khảo, có thể không mua được: chỉ chọn khi không có món verified phù hợp.
            - CHỈ chốt sản phẩm CÒN HÀNG: không chọn món in_stock = false; có món in_stock = true thì ưu tiên. App tự đổi \
            món hết hàng / không có trang bán sang món cùng loại còn hàng, người dùng không phải tự tìm.
            - ERROR: lắp không được, BẮT BUỘC đổi (propose_build báo product_errors thì chọn lại sản phẩm).
            - WARNING: dùng được nhưng cần cập nhật / kiểm tra; nói ngắn với người dùng (propose_build báo product_warnings).
            - UNKNOWN: chưa đủ dữ liệu; KHÔNG khẳng định là tương thích, nhắc người dùng cần xác minh.
            Luật:
            - Socket CPU phải khớp socket bo (ERROR), nhưng cùng socket chưa đủ: CPU phải có trong CPU Support List của \
            đúng mã bo và BIOS đủ mới (UNKNOWN, nhắc người dùng hỏi cửa hàng). Ví dụ nền tảng (không phải bảng đầy đủ):
              LGA1700: bo H610 / B760 / Z790, Core đời 12 / 13 / 14. LGA1851: bo B860 / Z890, Core Ultra máy bàn đời 2, \
            không lắp CPU LGA1700. AM4: bo B450 / B550, nhiều Ryzen 3000 / 5000 (B450 với Ryzen 5000 có thể cần BIOS mới). \
            AM5: bo B650 / X870, Ryzen 7000 / 8000G / 9000.
            - RAM (ERROR): đúng loại DDR với bo và CPU, đúng dạng thanh máy bàn (không dùng RAM laptop SO-DIMM), không \
            vượt số khe. Bo LGA1700 có bản DDR4 và bản DDR5: xem đúng mã bo, không suy ra từ socket. Nên chọn kit 2 thanh \
            (RECOMMENDATION); tốc độ XMP / EXPO không được đảm bảo.
            - Máy học / văn phòng không chơi game nặng: chọn CPU CÓ đồ họa tích hợp (vd Intel Core i3 / i5 đời 12 - 14 \
            KHÔNG đuôi F, Ryzen bản G) và KHÔNG thêm card rời để tiết kiệm; app tự bỏ lỗi "thiếu card" khi CPU thật có \
            đồ họa tích hợp. Chơi game thì mới cần card rời.
            - Đồ họa: KHÔNG suy ra từ hậu tố. Intel đuôi F thường không có đồ họa tích hợp; Ryzen 7000 / 9000 thường có \
            đồ họa cơ bản nhưng có ngoại lệ (vd 7500F). Không chắc CPU có đồ họa thì chọn thêm card đồ họa.
            - Tản nhiệt: phải có gá đúng socket (ERROR); gá Intel và AMD khác nhau. Gá LGA1700 thường dùng cho LGA1851, gá \
            AM4 thường dùng cho AM5 nhưng vẫn cần xác nhận với từng tản (WARNING). Tản khí phải thấp hơn giới hạn vỏ, két \
            tản nước phải có chỗ gắn (ERROR). CPU có kèm tản hay không phải xem đúng bản (box / tray), không suy ra chỉ từ \
            đuôi K / KF / X (WARNING): không chắc thì chọn thêm tản. CPU Intel đuôi K muốn ép xung cần bo chipset Z (WARNING).
            - Nguồn: ước lượng sơ bộ khoảng 1,2 - 1,3 lần tổng điện CPU + card + phần khác, đối chiếu công suất khuyến \
            nghị của hãng card (chỉ là gợi ý, không phải điều kiện tuyệt đối). Phải đủ đầu cấp điện cho CPU (EPS), bo và \
            card đúng loại (PCIe 6+2, 12VHPWR / 12V-2x6) (ERROR). 80 Plus chỉ là hiệu suất, không bảo đảm chất lượng.
            - Kích thước (ERROR): vỏ hỗ trợ cỡ bo (Mini-ITX / Micro-ATX / ATX), đủ chỗ cho card, tản, két nước, nguồn.
            - Lưu trữ (ERROR): SSD M.2 phải đúng giao thức khe (NVMe hay SATA) và kích thước (2280...); PCIe đời mới vẫn \
            chạy trên khe đời cũ nhưng chậm hơn (WARNING).

            Cách trả lời:
            - Trả lời bằng ngôn ngữ người dùng đang viết (thường là tiếng Việt). Giọng thân thiện, xưng "mình", gọi "bạn".
            - Ngắn gọn, khoảng 150 chữ trở xuống, trừ khi người dùng xin giải thích kỹ. Không dùng bảng, không dùng tiêu đề.
              Được dùng gạch đầu dòng "- " và **chữ đậm** cho ý chính.
            - Gặp thuật ngữ thì giải thích luôn bằng một câu đời thường, ví dụ "RAM là bộ nhớ tạm, càng nhiều thì mở \
            được càng nhiều chương trình cùng lúc".
            - Giải thích vì sao bạn chọn như vậy, để người dùng tự kiểm tra được lời khuyên chứ không chỉ làm theo.

            Khi người dùng hỏi chọn hay đổi linh kiện:
            1. Cần biết ngân sách và máy dùng để làm gì (học, văn phòng, chơi game, dựng phim...). Thiếu thì hỏi lại, \
            gộp thành một câu hỏi ngắn. Có rồi thì làm luôn, đừng hỏi thêm.
            2. Chọn linh kiện từ danh mục của app (list_components). Một máy cần đủ: vỏ, bo mạch chủ, CPU, tản nhiệt, \
            RAM, SSD, nguồn; thêm card đồ họa khi chơi game hoặc làm đồ họa, hoặc khi CPU không có sẵn đồ họa.
            3. Luôn chạy check_build với cấu hình định đề xuất. Còn lỗi (errors) thì đổi linh kiện rồi chạy lại cho tới \
            khi lắp được. Cảnh báo (warnings) thì nói lại cho người dùng bằng lời dễ hiểu.
            4. App sẽ tự vẽ danh sách linh kiện thành một thẻ ngay dưới câu trả lời, nên đừng liệt kê lại cả danh sách. \
            Chỉ nói vài món quan trọng nhất và lý do chọn.
            5. Danh mục của app chỉ là bộ mẫu nhỏ. Nếu nhu cầu vượt quá danh mục thì nói rõ điều đó.
            - Danh mục đầy đủ đã có ở cuối lời dặn này, dùng luôn, chỉ gọi list_components khi cần xem lại.
            - Khoảng số (giá, ngân sách) viết bằng dấu gạch nối thường, vd "20,9 - 28,9 triệu", không dùng gạch dài.
            - Gọi linh kiện bằng tên tiếng Việt trong danh mục, không đọc mã code cho người dùng.
            - Tên hãng hay mẫu sản phẩm chỉ nêu khi lấy từ kết quả search_products; còn lại nói theo thông số (vd "RAM 16 GB DDR4").
            - Giá lấy từ cửa hàng là giá lúc tìm, có thể đổi; giá không có sản phẩm là ước tính. Nói rõ điều này. \
            Không bịa thông số mà danh mục để trống rồi nói như thông tin chắc chắn.
            - Danh mục của app là các kiểu linh kiện để kiểm tra lắp chung và dựng mô hình 3D. Đừng nhắc tên hay mã \
            trong danh mục với người dùng; người dùng chỉ cần thấy sản phẩm thật nên mua.

            An toàn:
            - Trước mọi việc mở máy, nhắc: tắt máy, rút dây điện, chạm tay vào phần kim loại của vỏ để xả tĩnh điện.
            - Có mùi khét, khói, tụ phồng, hoặc lỗi ở bộ nguồn thì khuyên mang ra thợ. Không hướng dẫn mở bộ nguồn.

            Phạm vi: chỉ trả lời về máy tính (chọn mua, lắp ráp, nâng cấp, bảo dưỡng, máy chậm / nóng / ồn). \
            Câu hỏi ngoài lề thì từ chối nhẹ nhàng và gợi ý một câu hỏi phù hợp.
            """;

    private static final List<Map<String, Object>> TOOLS = List.of(
            tool("list_components",
                    "Danh mục linh kiện của app kèm thông số. Lọc theo loại hoặc bỏ trống để lấy tất cả. "
                            + "Loại: case, motherboard, cpu, cooler, ram, gpu, ssd, psu, fans, tools.",
                    Map.of("type", "object",
                            "properties", Map.of("category", Map.of("type", "string", "description", "Mã loại linh kiện")))),
            tool("get_build",
                    "Xem toàn bộ linh kiện của một máy có sẵn trong app, theo mã máy.",
                    Map.of("type", "object",
                            "properties", Map.of("code", Map.of("type", "string", "description", "Mã máy, vd standard_tower")),
                            "required", List.of("code"))),
            tool("check_build",
                    "Kiểm tra các linh kiện có lắp chung được không. Trả về errors (không lắp được) "
                            + "và warnings (lắp được nhưng nên xem lại). Cần đủ vỏ, bo mạch, CPU, nguồn, RAM, SSD.",
                    Map.of("type", "object",
                            "properties", Map.of("items", Map.of("type", "array",
                                    "items", Map.of("type", "object",
                                            "properties", Map.of(
                                                    "code", Map.of("type", "string", "description", "Mã linh kiện"),
                                                    "quantity", Map.of("type", "integer", "minimum", 1)),
                                            "required", List.of("code")))),
                            "required", List.of("items"))),
            tool("propose_build",
                    "Chốt cấu hình đề xuất cho người dùng (gọi một lần, sau khi check_build không còn lỗi). "
                            + "App vẽ bảng chi tiết từ dữ liệu này. Trả về kết quả kiểm tra tương thích.",
                    Map.of("type", "object",
                            "properties", Map.of(
                                    "tier", Map.of("type", "string", "enum", List.of("cheap", "balanced", "quality"),
                                            "description", "Phương án: cheap = Rẻ, balanced = Phù hợp cho bạn, quality = Chất lượng"),
                                    "summary", Map.of("type", "string",
                                            "description", "Một câu tóm tắt nhu cầu đã hiểu, vd 'Học online và làm văn phòng, máy nhỏ gọn'"),
                                    "budget_vnd", Map.of("type", "integer", "description", "Ngân sách người dùng nói, đơn vị đồng; bỏ trống nếu chưa biết"),
                                    "items", Map.of("type", "array",
                                            "items", Map.of("type", "object",
                                                    "properties", Map.of(
                                                            "code", Map.of("type", "string", "description", "Mã linh kiện trong danh mục"),
                                                            "quantity", Map.of("type", "integer", "minimum", 1),
                                                            "spec", Map.of("type", "string", "description", "Thông số nên mua, tối đa 8 chữ"),
                                                            "reason", Map.of("type", "string", "description",
                                                                    "Vì sao chọn, tối đa 12 chữ; tier cheap / quality bỏ trống nếu giống balanced"),
                                                            "price_min_vnd", Map.of("type", "integer"),
                                                            "price_max_vnd", Map.of("type", "integer"),
                                                            "product_id", Map.of("type", "string", "description", "id kết quả search_products đã chọn, vd s3-2"),
                                                            "buy_qty", Map.of("type", "integer", "minimum", 1, "description", "Số sản phẩm cần mua, mặc định = quantity")),
                                                    "required", List.of("code")))),
                            "required", List.of("summary", "items"))),
            tool("search_products",
                    "Tìm sản phẩm thật đang bán ở cửa hàng máy tính Việt Nam (đọc thẳng web cửa hàng). Trả về tối đa 8 món: "
                            + "id, title, shop, price_vnd, in_stock và warranty_months nếu biết. Dùng id trong propose_build. "
                            + "Ưu tiên món in_stock = true.",
                    Map.of("type", "object",
                            "properties", Map.of("query", Map.of("type", "string",
                                    "description", "Từ khoá NGẮN 2-5 chữ, MỘT loại / model mỗi lần (không dùng 'hoặc'), "
                                            + "vd 'RTX 3050 8GB', 'SSD NVMe 512GB', 'tản nhiệt khí CPU'. Không thêm 'giá rẻ', số đo hay socket")),
                            "required", List.of("query"))));

    private final LlmClient client;
    private final ProductSearch search;
    private final ComponentService componentService;
    private final CompatibilityService compatibilityService;
    private final PcBuildService pcBuildService;
    private final ModelMatchService modelMatch;
    private final JsonMapper json;
    private final String model;
    private final int maxTokens;

    public ChatService(LlmClient client, ProductSearch search, ComponentService componentService,
            CompatibilityService compatibilityService, PcBuildService pcBuildService, ModelMatchService modelMatch, JsonMapper json,
            @Value("${app.ai.model:deepseek-v4-flash}") String model,
            @Value("${app.ai.max-tokens:2048}") int maxTokens) {
        this.client = client;
        this.search = search;
        this.componentService = componentService;
        this.compatibilityService = compatibilityService;
        this.pcBuildService = pcBuildService;
        this.modelMatch = modelMatch;
        this.json = json;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    /**
     * Cấu hình kiểm tra gần nhất trong một lượt trả lời. Đề xuất chốt bằng propose_build có thêm
     * thông tin từng dòng (details, theo mã linh kiện), nhu cầu đã hiểu và ngân sách.
     */
    private record Checked(BuildCheckRequest request, CompatibilityResponse result,
            Map<String, Map<String, Object>> details, String summary, Long budgetVnd,
            Map<String, ProductSearch.Product> picks, Map<String, CompletableFuture<String>> links,
            Map<String, List<ProductSearch.Product>> siblings) {

        Checked(BuildCheckRequest request, CompatibilityResponse result) {
            this(request, result, Map.of(), null, null, Map.of(), Map.of(), Map.of());
        }
    }

    /**
     * Trạng thái của một lượt trả lời: cấu hình kiểm tra gần nhất và mọi kết quả search_products theo id ngắn.
     * Trợ lý chỉ chọn bằng id, nên tên, giá, ảnh, link luôn lấy từ kết quả tìm kiếm thật, không thể bịa.
     * Công cụ trong cùng một lượt chạy song song nên dùng cấu trúc an toàn đa luồng.
     */
    private static final class Turn {
        /** Trang tư vấn: cấu hình phải khớp máy mẫu 3D của vỏ đã chọn (ModelMatchService) */
        final boolean advice;
        volatile Checked last;
        /** Ba phương án chốt bằng propose_build, theo tier (cheap / balanced / quality) */
        final Map<String, Checked> tiers = new ConcurrentHashMap<>();
        /** Phương án đã bị nhắc vượt ngân sách (chỉ nhắc một lần, để trợ lý không chọn lại mãi tới hết lượt) */
        final java.util.Set<String> overWarned = ConcurrentHashMap.newKeySet();
        /** Toàn bộ kết quả của từng lần search_products, theo nhóm id ("s3" cho s3-1 ... s3-6) */
        final Map<String, List<ProductSearch.Product>> groups = new ConcurrentHashMap<>();
        final Map<String, ProductSearch.Product> results = new ConcurrentHashMap<>();
        final AtomicInteger searches = new AtomicInteger();

        Turn(boolean advice) {
            this.advice = advice;
        }
    }

    @SuppressWarnings("unchecked")
    public ChatResponse reply(ChatRequest request) {
        return reply(request, stage -> { });
    }

    /** progress nhận câu báo bước đang làm (hiện cho người dùng trong lúc chờ, xem ChatJobs) */
    public ChatResponse reply(ChatRequest request, java.util.function.Consumer<String> progress) {
        progress.accept(STAGE_READ);
        List<Map<String, Object>> messages = new ArrayList<>();
        String step = request.guideStep() == null || request.guideStep().isBlank() ? ""
                : "\nNgười dùng đang làm theo hướng dẫn lắp, hiện ở: " + request.guideStep().strip() + ".\n";
        String intro = "advice".equals(request.mode()) ? ADVICE : ASSEMBLY;
        if (!search.enabled()) {
            intro += "\nLƯU Ý: tìm kiếm sản phẩm đang tắt. Bỏ qua bước tìm hàng thật, để trống phần sản phẩm, dùng giá ước tính "
                    + "và không nêu tên hãng hay mẫu sản phẩm.\n";
        }
        messages.add(Map.of("role", "system", "content", intro + "\n" + RULES + "\n" + context(request.buildCode()) + step));
        for (var m : request.messages()) {
            // Lời chào mở đầu của khung chat là tin "assistant" đứng đầu: bỏ để hội thoại bắt đầu bằng người dùng
            if (messages.size() == 1 && m.role().equals("assistant")) continue;
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }
        if (messages.size() == 1 || !messages.getLast().get("role").equals("user")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tin cuối cùng phải là câu hỏi của người dùng");
        }

        Turn turn = new Turn("advice".equals(request.mode()));
        // Chưa có khoá tìm kiếm: bỏ hẳn công cụ search_products để trợ lý không thử đi thử lại mất nhiều lượt
        var tools = search.enabled() ? TOOLS
                : TOOLS.stream().filter(t -> !"search_products".equals(((Map<?, ?>) t.get("function")).get("name"))).toList();
        int searchRounds = 0, proposeRounds = 0;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("messages", messages);
            // Đã tìm đủ MAX_SEARCH_ROUNDS vòng: bỏ công cụ tìm để trợ lý chốt với kết quả đã có (mỗi vòng tìm thêm tốn
            // 15 - 35 giây; món chưa ưng thì app tự đổi lúc dựng thẻ)
            var offered = searchRounds >= MAX_SEARCH_ROUNDS ? withoutSearch(tools) : tools;
            // Đã đề xuất đủ MAX_PROPOSE_ROUNDS vòng: chỉ còn viết câu trả lời (mỗi vòng đề xuất tốn ~50 giây; giá thật
            // quá cao so với ngân sách thì trợ lý đề xuất lại mãi). Bước dựng thẻ vẫn lọc option theo trần giá
            body.put("tools", offered);
            if (proposeRounds >= MAX_PROPOSE_ROUNDS) body.put("tool_choice", "none");

            long t0 = System.nanoTime();
            Map<String, Object> res;
            try {
                res = client.complete(body);
            } catch (AiUnavailableException e) {
                // AI lỗi ở vòng viết câu trả lời nhưng các phương án đã dựng xong: vẫn gửi phương án, không bỏ phí
                if (turn.tiers.isEmpty() && turn.last == null) throw e;
                log.warn("Chat vòng {}: AI lỗi sau khi đã có phương án, gửi phương án kèm câu dẫn có sẵn", round + 1);
                return respond(BUILDS_READY, turn);
            }
            long aiMs = (System.nanoTime() - t0) / 1_000_000;
            var choices = (List<Map<String, Object>>) res.getOrDefault("choices", List.of());
            if (choices.isEmpty()) break;
            var message = (Map<String, Object>) choices.getFirst().getOrDefault("message", Map.of());
            var calls = (List<Map<String, Object>>) message.get("tool_calls");
            if (calls == null || calls.isEmpty()) {
                log.info("Chat vòng {}: AI {} ms, trả lời xong", round + 1, aiMs);
                long tc = System.nanoTime();
                if (!turn.tiers.isEmpty() && search.enabled()) progress.accept(STAGE_LINKS);
                var done = respond(clean(message.get("content")), turn);
                log.info("Chat: dựng thẻ (đổi link cửa hàng) {} ms", (System.nanoTime() - tc) / 1_000_000);
                return done;
            }
            // Gửi lại nguyên tin của model (kể cả reasoning_content của model suy luận) rồi tới kết quả từng công cụ
            messages.add(message);
            // Chạy song song các công cụ của cùng một lượt (thường là nhiều search_products), rồi trả kết quả theo đúng thứ tự
            var names = calls.stream().map(c -> String.valueOf(((Map<?, ?>) c.get("function")).get("name"))).toList();
            // Trợ lý có khi vẫn gọi search_products sau khi công cụ đã bị bỏ (chép theo các lượt trước): không chạy
            boolean searchClosed = searchRounds >= MAX_SEARCH_ROUNDS;
            boolean proposeClosed = proposeRounds >= MAX_PROPOSE_ROUNDS;
            if (names.contains("propose_build") && !proposeClosed) proposeRounds++;
            if (names.contains("search_products") && !searchClosed) {
                searchRounds++;
                progress.accept(STAGE_SEARCH);
            } else if (names.contains("propose_build")) progress.accept(STAGE_CHECK);
            var results = calls.stream()
                    .map(call -> {
                        var fn = (Map<String, Object>) call.get("function");
                        if (proposeClosed && !turn.tiers.isEmpty()) {
                            return CompletableFuture.completedFuture(json.writeValueAsString(Map.of("error",
                                    "Đã chốt đủ phương án. Không gọi công cụ nữa, viết câu trả lời cho người dùng.")));
                        }
                        if (searchClosed && "search_products".equals(fn.get("name"))) {
                            return CompletableFuture.completedFuture(json.writeValueAsString(Map.of("error",
                                    "Đã hết lượt tìm. Chốt bằng propose_build với kết quả đã có; món chưa ưng thì app tự đổi.")));
                        }
                        return Io.async(() -> runTool(fn, turn));
                    })
                    .toList();
            long t1 = System.nanoTime();
            for (int i = 0; i < calls.size(); i++) {
                messages.add(Map.of("role", "tool", "tool_call_id", String.valueOf(calls.get(i).get("id")),
                        "content", results.get(i).join()));
            }
            progress.accept(!turn.tiers.isEmpty() ? STAGE_WRITE : names.contains("search_products") ? STAGE_PICK : STAGE_READ);
            log.info("Chat vòng {}: AI {} ms, {} công cụ {} chạy {} ms", round + 1, aiMs, calls.size(),
                    calls.stream().map(c -> String.valueOf(((Map<?, ?>) c.get("function")).get("name"))).toList(),
                    (System.nanoTime() - t1) / 1_000_000);
        }
        // Hết số vòng: có phương án rồi thì vẫn gửi (kèm câu dẫn), chưa có gì mới bảo người dùng hỏi cụ thể hơn
        return respond(turn.tiers.isEmpty() && turn.last == null
                ? "Câu này mình phải tra cứu nhiều quá. Bạn hỏi cụ thể hơn một chút nhé." : BUILDS_READY, turn);
    }

    /** Thứ tự hiện ba phương án trong bảng so sánh */
    private static final List<String> TIERS = List.of("cheap", "balanced", "quality");

    /**
     * Câu trả lời kèm thẻ: có nhiều phương án thì trả cả danh sách (options) và lấy "balanced" làm thẻ chính (build),
     * để các chỗ chỉ đọc một thẻ (lịch sử hội thoại, nút xem và lắp) vẫn chạy như cũ.
     */
    private ChatResponse respond(String reply, Turn turn) {
        Map<String, List<Checked>> chosen = new LinkedHashMap<>();
        for (String t : TIERS) {
            if (!turn.tiers.containsKey(t)) continue;
            // Mọi nút mua phải vào thẳng web cửa hàng. Link đã đổi song song lúc trợ lý viết câu trả lời; món nào vẫn
            // không có link thì thay bằng sản phẩm cùng nhóm có link (hiếm, vài giây)
            Checked base = turn.tiers.get(t);
            Long cap = turn.advice ? tierCap(base.budgetVnd(), t) : null;
            if (turn.advice && search.enabled()) base = withLinkedPicks(base, t, cap);
            List<Checked> all = new ArrayList<>();
            all.add(base);
            // Giá hiển thị là giá đọc trên chính trang cửa hàng của link (Google Shopping có thể cũ hoặc là bản khác)
            all.replaceAll(c -> turn.advice ? withPagePrices(c) : c);
            // Luật cứng (tierCap): đo trên thẻ cuối cùng (sau khi đổi món lấy link), cùng cách cộng với giao diện
            List<Checked> kept = options(all, cap, c -> productIssues(c.picks()).isEmpty());
            Checked low = null;
            if (kept.isEmpty() && search.enabled()) {
                // Trợ lý vẫn chọn vượt dù đã được báo: thử bộ rẻ nhất trong cùng các lần tìm kiếm
                low = cheapest(base, t, cap);
                if (total(low.request(), low.details(), low.picks()) <= cap) kept = List.of(low);
            }
            if (kept.isEmpty()) {
                // Giá thật làm mọi bộ vượt trần: vẫn gửi bộ rẻ nhất (thẻ ghi rõ vượt bao nhiêu)
                List<Checked> pool = new ArrayList<>(all);
                if (low != null) pool.add(low);
                pool.sort(Comparator.comparingLong(c -> total(c.request(), c.details(), c.picks())));
                kept = pool.stream().limit(1).toList();
                log.warn("Phương án {} vượt trần {} dù đã thử bộ rẻ nhất, gửi bộ rẻ nhất", t, cap);
            }
            chosen.put(t, kept);
        }
        // Trợ lý chỉ viết thông số / lý do cho món khác balanced (trả lời nhanh hơn): điền phần trống từ tier khác cùng món
        Map<String, Map<String, Object>> shared = new LinkedHashMap<>();
        for (String t : List.of("balanced", "cheap", "quality")) {
            var c = turn.tiers.get(t);
            if (c == null) continue;
            c.details().forEach((code, d) -> {
                var into = shared.computeIfAbsent(code, k -> new LinkedHashMap<>());
                if (text(d.get("spec")) != null) into.putIfAbsent("spec", d.get("spec"));
                if (text(d.get("reason")) != null) into.putIfAbsent("reason", d.get("reason"));
            });
        }
        chosen.replaceAll((t, list) -> list.stream().map(c -> withSharedText(c, shared)).toList());
        // Bảng giá tốt nhất: tìm trước (song song) cùng model ở cửa hàng khác cho mọi món sẽ hiện, rồi mới dựng thẻ
        if (turn.advice && search.enabled()) {
            Map<String, ProductSearch.Product> picks = new LinkedHashMap<>();
            chosen.values().forEach(list -> list.forEach(c -> c.picks().values().forEach(p -> picks.putIfAbsent(p.url(), p))));
            search.warmCompare(picks.values());
        }
        List<BuildCard> options = new ArrayList<>();
        chosen.forEach((t, kept) -> {
            int v = 1;
            for (var c : kept) options.add(card(c, turn.advice, t, v++));
        });
        BuildCard build = options.stream().filter(o -> "balanced".equals(o.tier()) && o.variant() == 1).findFirst()
                .orElseGet(() -> turn.last == null ? null : card(turn.last, turn.advice, null, 1));
        return new ChatResponse(reply, build, options.size() > 1 ? options : null);
    }

    /**
     * Build hiện cho phương án (mỗi phương án một build, để trả lời nhanh): build đầu tiên không vượt cap và có sản
     * phẩm thật lắp chung được (fits; không build nào lắp được thì bỏ điều kiện này). Không có build nào dưới cap thì rỗng.
     */
    static List<Checked> options(List<Checked> all, Long cap, java.util.function.Predicate<Checked> fits) {
        var unique = withinCap(all, null);
        if (unique.stream().anyMatch(fits)) unique = unique.stream().filter(fits).toList();
        return unique.stream().filter(c -> cap == null || total(c.request(), c.details(), c.picks()) <= cap).limit(1).toList();
    }

    /** Các build không vượt trần (cap null: không giới hạn), bỏ build trùng hệt nhau sau khi đổi món lấy link */
    private static List<Checked> withinCap(List<Checked> all, Long cap) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        return all.stream()
                .filter(c -> cap == null || total(c.request(), c.details(), c.picks()) <= cap)
                .filter(c -> seen.add(signature(c.picks())))
                .toList();
    }

    /** Mỗi món đổi sang sản phẩm rẻ nhất cùng nhóm tìm kiếm (hàng mới, đúng loại bộ / thanh lẻ), rồi lấy link */
    private Checked cheapest(Checked c, String tier, Long cap) {
        Map<String, ProductSearch.Product> picks = new LinkedHashMap<>(c.picks());
        Map<String, CompletableFuture<String>> links = new LinkedHashMap<>(c.links());
        for (var e : c.picks().entrySet()) {
            var p = e.getValue();
            if (p.priceVnd() == null) continue;
            candidates(p, c.siblings().getOrDefault(e.getKey(), List.of()), kit(c.request(), c.details(), e.getKey()), "cheap").stream()
                    .filter(a -> a.priceVnd() < p.priceVnd())
                    .min(Comparator.comparingLong(ProductSearch.Product::priceVnd))
                    .ifPresent(a -> {
                        picks.put(e.getKey(), a);
                        links.put(e.getKey(), Io.async(() -> search.directLink(a.title(), a.shop(), a.url())));
                    });
        }
        if (!productIssues(picks).isEmpty() && productIssues(c.picks()).isEmpty()) return withLinkedPicks(c, tier, cap);
        return withLinkedPicks(new Checked(c.request(), c.result(), c.details(), c.summary(), c.budgetVnd(), picks, links, c.siblings()), tier, cap);
    }

    /** Dòng mua theo bộ (buy_qty nhỏ hơn số lượng, vd RAM 2x8GB là 1 sản phẩm cho 2 thanh) */
    private static boolean kit(BuildCheckRequest req, Map<String, Map<String, Object>> details, String code) {
        int qty = req.items().stream().filter(i -> i.code().equals(code)).mapToInt(BuildCheckRequest.Item::qty).sum();
        return details.getOrDefault(code, Map.of()).get("buy_qty") instanceof Number b && b.intValue() < qty;
    }

    /** Các bước báo cho người dùng trong lúc chờ (trang tư vấn mất cả phút) */
    static final String STAGE_READ = "Đang đọc nhu cầu của bạn…";
    static final String STAGE_SEARCH = "Đang tìm giá ở các cửa hàng…";
    static final String STAGE_PICK = "Đang chọn linh kiện cho ba phương án Rẻ, Phù hợp, Chất lượng…";
    static final String STAGE_CHECK = "Đang kiểm tra các linh kiện lắp chung được không…";
    static final String STAGE_WRITE = "Đã có phương án, đang viết lời giải thích…";
    static final String STAGE_LINKS = "Sắp xong, đang so giá cùng sản phẩm ở các cửa hàng khác…";

    /** Câu dẫn khi AI lỗi ở vòng cuối nhưng phương án đã có */
    static final String BUILDS_READY = "Mình đã chọn xong các phương án bên dưới. Bạn bấm vào từng option để xem chi tiết "
            + "linh kiện, giá và link mua nhé. Phần giải thích chi tiết đang tạm lỗi, bạn có thể hỏi thêm ở ô bên dưới.";

    /** Hàng không nên đưa người mới tham khảo */
    private static final java.util.regex.Pattern JUNK =
            java.util.regex.Pattern.compile("like ?new|cũ|đã qua sử dụng|2nd|trưng bày|thanh lý|tray|bóc máy", java.util.regex.Pattern.CASE_INSENSITIVE);
    /** "2x8GB", "2 x 16GB", "kit": bộ nhiều thanh */
    private static final java.util.regex.Pattern KIT =
            java.util.regex.Pattern.compile("\\d\\s*x\\s*\\d+\\s*gb|\\bkit\\b", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * Món thay thế tốt nhất (tối đa 4 lần thử): còn hàng và giá đã kiểm tra, rồi giá đã kiểm tra, rồi có trang sản
     * phẩm. now: link món đang chọn; món đang chọn ngang bằng thì trả về null (giữ nguyên).
     */
    private ProductSearch.Product bestLinked(List<ProductSearch.Product> candidates, String now) {
        int nowRank = rank(now);
        // Thử song song (mỗi lần thử có thể phải tìm và mở trang cửa hàng), rồi lấy món hạng cao nhất, gần giá nhất
        var tries = candidates.stream().limit(4)
                .map(c -> Io.async(() -> {
                    String l = search.directLink(c.title(), c.shop(), c.url());
                    return l == null ? null : new ProductSearch.Product(c.title(), c.shop(), c.priceVnd(), l, c.imageUrl());
                }))
                .toList();
        ProductSearch.Product best = null;
        int bestRank = nowRank;
        for (var t : tries) {
            var found = t.join();
            int r = found == null ? 0 : rank(found.url());
            if (r > bestRank) {
                best = found;
                bestRank = r;
            }
        }
        return best;
    }

    /** 3: còn hàng + giá đã kiểm tra; 2: giá đã kiểm tra; 1: trang sản phẩm; 0: không có link / trang chủ / hết hàng */
    private int rank(String url) {
        if (url == null || ProductSearch.homePage(url) || soldOut(url)) return 0;
        if (!priceChecked(url)) return 1;
        return inStock(url) ? 3 : 2;
    }

    /** Trang cửa hàng của link báo hết hàng */
    private boolean soldOut(String url) {
        var page = url == null ? null : search.offer(url);
        return page != null && Boolean.FALSE.equals(page.inStock());
    }

    /** Trang cửa hàng của link xác nhận còn hàng */
    private boolean inStock(String url) {
        var page = url == null ? null : search.offer(url);
        return page != null && Boolean.TRUE.equals(page.inStock());
    }

    /** Link đã đọc được giá trên chính trang cửa hàng */
    private boolean priceChecked(String url) {
        var page = url == null ? null : search.offer(url);
        return page != null && page.priceVnd() != null;
    }

    /** Điền spec / reason còn trống của từng món từ tier khác có cùng món */
    private static Checked withSharedText(Checked c, Map<String, Map<String, Object>> shared) {
        Map<String, Map<String, Object>> details = new LinkedHashMap<>();
        c.details().forEach((code, d) -> {
            var filled = new LinkedHashMap<>(d);
            var from = shared.getOrDefault(code, Map.of());
            if (text(filled.get("spec")) == null && from.get("spec") != null) filled.put("spec", from.get("spec"));
            if (text(filled.get("reason")) == null && from.get("reason") != null) filled.put("reason", from.get("reason"));
            details.put(code, filled);
        });
        return new Checked(c.request(), c.result(), details, c.summary(), c.budgetVnd(), c.picks(), c.links(), c.siblings());
    }

    /** Thay giá Google Shopping bằng giá đọc trên trang cửa hàng của link đã xác minh (nếu đọc được) */
    private Checked withPagePrices(Checked c) {
        Map<String, ProductSearch.Product> picks = new LinkedHashMap<>(c.picks());
        c.picks().forEach((code, p) -> {
            var link = c.links().get(code);
            String url = link == null ? null : link.join();
            var page = url == null ? null : search.offer(url);
            if (page == null || page.priceVnd() == null || page.priceVnd().equals(p.priceVnd())) return;
            // Lệch quá 5 lần là lỗi đơn vị trên trang (vd giá ghi theo đồng x 100), không dùng
            if (p.priceVnd() != null && (page.priceVnd() > p.priceVnd() * 5 || page.priceVnd() * 5 < p.priceVnd())) return;
            if (p.priceVnd() != null && Math.abs(page.priceVnd() - p.priceVnd()) * 10 > p.priceVnd() * 2) {
                log.info("Giá trang khác Google Shopping: {} {} -> {} ({})", p.title(), p.priceVnd(), page.priceVnd(), url);
            }
            picks.put(code, new ProductSearch.Product(p.title(), p.shop(), page.priceVnd(), p.url(),
                    p.imageUrl() != null ? p.imageUrl() : page.imageUrl()));
        });
        return new Checked(c.request(), c.result(), c.details(), c.summary(), c.budgetVnd(), picks, c.links(), c.siblings());
    }

    /**
     * Bỏ lỗi "thiếu card đồ họa" khi CPU thật đã chọn có đồ họa tích hợp theo mã sản phẩm (ProductRules.hasIgpu);
     * còn lại giữ nguyên kết quả kiểm tra.
     */
    private CompatibilityResponse withoutFalseGpuError(CompatibilityResponse r, Map<String, ProductSearch.Product> picks) {
        if (r.errors().stream().noneMatch(i -> i.rule().equals("required.gpu"))) return r;
        String cpu = productTitles(picks).get("cpu");
        if (!Boolean.TRUE.equals(ProductRules.hasIgpu(cpu))) return r;
        var errors = r.errors().stream().filter(i -> !i.rule().equals("required.gpu")).toList();
        var warnings = new ArrayList<>(r.warnings());
        warnings.add(new Issue("cpu.igpu", "Không có card rời: máy dùng đồ họa tích hợp của CPU \"" + cpu
                + "\". Đủ cho học, văn phòng, xem phim; chơi game nặng thì cần thêm card."));
        return new CompatibilityResponse(errors.isEmpty(), errors, warnings);
    }

    /** Lỗi lắp chung giữa các sản phẩm thật đã chọn (theo tên sản phẩm, xem ProductRules) */
    private List<String> productIssues(Map<String, ProductSearch.Product> picks) {
        return ProductRules.issues(productTitles(picks));
    }

    /** Mọi kết quả kiểm tra (ERROR / WARNING / UNKNOWN) cho các sản phẩm thật đã chọn */
    private List<ProductRules.Finding> productFindings(Map<String, ProductSearch.Product> picks) {
        return ProductRules.findings(productTitles(picks));
    }

    /** Loại linh kiện -> tên sản phẩm thật đã chọn */
    private Map<String, String> productTitles(Map<String, ProductSearch.Product> picks) {
        Map<String, String> titles = new LinkedHashMap<>();
        picks.forEach((code, p) -> {
            try {
                var part = componentService.findByCode(code);
                if (part != null && p != null && p.title() != null) titles.putIfAbsent(part.category(), p.title());
            } catch (ResourceNotFoundException e) {
                // mã lạ: kiểm tra tương thích đã báo
            }
        });
        return titles;
    }

    /** Số vòng trợ lý được gọi search_products trước khi phải chốt */
    static final int MAX_SEARCH_ROUNDS = 2;
    /** Số vòng propose_build (lần đầu + một lần sửa) trước khi phải viết câu trả lời */
    static final int MAX_PROPOSE_ROUNDS = 2;

    private static List<Map<String, Object>> withoutSearch(List<Map<String, Object>> tools) {
        return tools.stream().filter(t -> !"search_products".equals(((Map<?, ?>) t.get("function")).get("name"))).toList();
    }

    /** Thời gian tối đa cho bước đổi món lấy link của một phương án */
    static final java.time.Duration LINK_BUDGET = java.time.Duration.ofSeconds(25);

    /** Rẻ phải rẻ hơn rõ rệt: tối đa 90% ngân sách */
    static final double CHEAP_SHARE = 0.9;
    /** Chất lượng chỉ vượt ngân sách một chút: tối đa từng này */
    static final long QUALITY_OVER = 3_000_000;

    /** Trần của phương án: Rẻ 90% ngân sách, Phù hợp đúng ngân sách, Chất lượng + 3 triệu */
    static Long tierCap(Long budgetVnd, String tier) {
        if (budgetVnd == null) return null;
        return switch (tier == null ? "" : tier) {
            case "cheap" -> Math.round(budgetVnd * CHEAP_SHARE);
            case "quality" -> budgetVnd + QUALITY_OVER;
            default -> budgetVnd;
        };
    }

    /**
     * Tổng giá của bộ sản phẩm (giá cửa hàng nhân số mua; dòng không có sản phẩm lấy giá ước tính cao), cùng cách giao
     * diện tính "dư / vượt" ngân sách
     */
    private static long total(BuildCheckRequest req, Map<String, Map<String, Object>> details, Map<String, ProductSearch.Product> picks) {
        long sum = 0;
        for (var i : req.items()) {
            Long[] r = price(details.getOrDefault(i.code(), Map.of()), picks.get(i.code()), i.qty());
            sum += r[1] != null ? r[1] : (r[0] != null ? r[0] : 0);
        }
        return sum;
    }

    /** Khoảng giá món thay thế so với món trợ lý chọn, theo phương án: Rẻ không đắt lên nhiều, Chất lượng không rẻ đi nhiều */
    private static final Map<String, double[]> PRICE_BAND = Map.of(
            "cheap", new double[] { 0.6, 1.15 }, "balanced", new double[] { 0.75, 1.3 }, "quality", new double[] { 0.85, 1.6 });

    /** Mọi sản phẩm thay thế hợp lệ cho p: cửa hàng biết tên miền trước, rồi giá gần nhất */
    static List<ProductSearch.Product> candidates(ProductSearch.Product p, List<ProductSearch.Product> group, boolean kit, String tier) {
        if (p == null || p.priceVnd() == null) return List.of();
        long price = p.priceVnd();
        double[] band = PRICE_BAND.getOrDefault(tier, PRICE_BAND.get("balanced"));
        return group.stream()
                .filter(s -> s.priceVnd() != null && s.title() != null && !s.title().equalsIgnoreCase(p.title()))
                .filter(s -> !JUNK.matcher(s.title()).find() && (!kit || KIT.matcher(s.title()).find()))
                .filter(s -> s.priceVnd() >= price * band[0] && s.priceVnd() <= price * band[1])
                // Cửa hàng biết tên miền trước (đổi được sang link cửa hàng, khỏi tốn lượt tìm), rồi giá gần nhất
                .sorted(Comparator.comparing((ProductSearch.Product s) -> !ProductSearch.knownShop(s.shop()))
                        .thenComparingLong(s -> Math.abs(s.priceVnd() - price)))
                .toList();
    }

    /**
     * Chờ đổi link cửa hàng cho mọi món (song song). Món nào không có link hoặc chỉ có trang chủ cửa hàng thì thử các
     * sản phẩm cùng nhóm tìm kiếm, giá gần nhất trong khoảng của phương án, tới khi có link (tối đa 4 lần mỗi món).
     */
    private Checked withLinkedPicks(Checked c, String tier, Long cap) {
        Map<String, ProductSearch.Product> picks = new LinkedHashMap<>(c.picks());
        Map<String, CompletableFuture<String>> links = new LinkedHashMap<>(c.links());
        linkedPicks(c.request(), c.details(), picks, links, c.siblings(), tier, cap);
        return new Checked(c.request(), c.result(), c.details(), c.summary(), c.budgetVnd(), picks, links, c.siblings());
    }

    private void linkedPicks(BuildCheckRequest req, Map<String, Map<String, Object>> details, Map<String, ProductSearch.Product> picks,
            Map<String, CompletableFuture<String>> links, Map<String, List<ProductSearch.Product>> siblings, String tier, Long cap) {
        Map<String, CompletableFuture<ProductSearch.Product>> fixes = new LinkedHashMap<>();
        // Món yếu: chưa có link, chỉ có trang chủ, hoặc link chưa đọc được giá trên trang (giá Google có thể sai).
        // Trang hết hàng đã bị loại từ bước xác minh link; trang không ghi tình trạng hàng thì giữ (đổi mãi rất chậm)
        var weak = links.entrySet().stream().filter(e -> {
            String l = e.getValue().join();
            return l == null || ProductSearch.homePage(l) || !priceChecked(l) || soldOut(l);
        }).map(Map.Entry::getKey).toList();
        // Phần còn dư tới trần chia đều cho các món phải đổi, để đổi món lấy link không làm phương án vượt trần
        long allowance = cap == null || weak.isEmpty() ? Long.MAX_VALUE : (cap - total(req, details, picks)) / weak.size();
        weak.forEach(code -> {
            String now = links.get(code).join();
            var pick = picks.get(code);
            int qty = req.items().stream().filter(i -> i.code().equals(code)).mapToInt(BuildCheckRequest.Item::qty).sum();
            Long[] was = price(details.getOrDefault(code, Map.of()), pick, qty);
            var candidates = candidates(pick, siblings.getOrDefault(code, List.of()), kit(req, details, code), tier).stream()
                    .filter(c -> was[1] == null || price(details.getOrDefault(code, Map.of()), c, qty)[1] - was[1] <= allowance)
                    .toList();
            String spec = text(details.getOrDefault(code, Map.of()).get("spec"));
            fixes.put(code, Io.async(() -> {
                // 1. Đúng sản phẩm đã chọn nhưng ở cửa hàng khác (hết hàng / không có trang bán ở cửa hàng này): giữ
                //    nguyên lựa chọn của trợ lý, chỉ đổi nơi mua
                var sameElsewhere = search.sameModelElsewhere(pick).stream()
                        .filter(c -> was[1] == null || price(details.getOrDefault(code, Map.of()), c, qty)[1] - was[1] <= allowance)
                        .toList();
                var same = bestLinked(sameElsewhere, now);
                if (same != null && rank(same.url()) == 3) return same;
                // 2. Sản phẩm khác cùng nhóm. Thứ tự ưu tiên: còn hàng + giá đã kiểm tra trên trang; giá đã kiểm tra;
                //    có trang sản phẩm. Món đang chọn không thua món thay thế thì giữ nguyên
                var best = bestLinked(candidates, same != null ? same.url() : now);
                if (best == null) best = same;
                if (best != null || now != null) return best;
                // Không món nào trong nhóm có trang bán: tự tìm thêm theo thông số (app đi tìm, không bắt người dùng tìm)
                String query = spec != null ? spec : ProductSearch.shortTitle(pick.title());
                java.util.function.Function<List<ProductSearch.Product>, List<ProductSearch.Product>> fit = found ->
                        candidates(pick, found, kit(req, details, code), tier).stream()
                                .filter(c -> was[1] == null || price(details.getOrDefault(code, Map.of()), c, qty)[1] - was[1] <= allowance)
                                .toList();
                var more = bestLinked(fit.apply(search.search(query)), null);
                // Google Shopping vẫn không cho link xác minh được: tìm thẳng trên web cửa hàng (link sẵn là trang sản phẩm)
                if (more == null) more = bestLinked(fit.apply(search.engineSearch(query)), null);
                if (more == null && spec != null) more = bestLinked(fit.apply(search.engineSearch(ProductSearch.shortTitle(pick.title()))), null);
                return more;
            }));
        });
        // Giới hạn thời gian: món tìm chưa xong thì giữ nguyên (thẻ ghi "chưa kiểm tra" / "chưa tìm được trang bán")
        long deadline = System.currentTimeMillis() + LINK_BUDGET.toMillis();
        fixes.forEach((code, f) -> {
            ProductSearch.Product p;
            try {
                p = f.get(Math.max(0, deadline - System.currentTimeMillis()), java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException e) {
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (p == null) return;
            var trial = new LinkedHashMap<>(picks);
            trial.put(code, p);
            if (!productIssues(trial).isEmpty() && productIssues(picks).isEmpty()) return;
            picks.put(code, p);
            links.put(code, CompletableFuture.completedFuture(p.url()));
        });
    }

    private static String signature(Map<String, ProductSearch.Product> picks) {
        return picks.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue().title()).sorted().collect(Collectors.joining("|"));
    }

    /** Danh mục linh kiện, máy người dùng đang xem và danh sách máy có sẵn, gắn vào cuối system prompt */
    private String context(String buildCode) {
        // Đưa sẵn danh mục (khoảng 25 món) để trợ lý khỏi mất một lượt gọi list_components, trả lời nhanh hơn
        StringBuilder sb = new StringBuilder("Danh mục linh kiện của app (JSON):\n")
                .append(json.writeValueAsString(componentService.findAll(null).stream().map(ChatService::brief).toList()))
                .append("\n\nCác máy có sẵn trong app (mã: tên, mô tả):\n");
        pcBuildService.findPresets().forEach(b ->
                sb.append("- ").append(b.code()).append(": ").append(b.name()).append(", ").append(b.tagline()).append('\n'));
        String fixed = modelMatch.presetParts();
        if (!fixed.isEmpty()) sb.append("\nMón đi kèm từng vỏ (mô hình 3D và hướng dẫn lắp dựng theo đúng các món này):\n").append(fixed);
        if (buildCode != null && !buildCode.isBlank()) {
            try {
                PcBuildResponse b = pcBuildService.findByCode(buildCode);
                sb.append("\nNgười dùng đang xem máy \"").append(b.name()).append("\" (mã ").append(b.code())
                        .append(") trên màn hình 3D. Linh kiện: ")
                        .append(b.items().stream()
                                .map(i -> i.quantity() + " × " + i.component().name() + " [" + i.component().code() + "]")
                                .collect(Collectors.joining(", ")))
                        .append(".\nKhi người dùng nói \"máy này\" là nói về máy đó.\n");
            } catch (ResourceNotFoundException ignored) {
                // Máy chưa có trong DB: trả lời chung, không kèm ngữ cảnh
            }
        }
        return sb.toString();
    }

    /** Chạy một lời gọi công cụ, trả về chuỗi JSON (hoặc lời báo lỗi) để model đọc */
    @SuppressWarnings("unchecked")
    private String runTool(Map<String, Object> function, Turn turn) {
        try {
            String name = String.valueOf(function.get("name"));
            Object rawArgs = function.get("arguments");
            Map<String, Object> input = rawArgs == null || rawArgs.toString().isBlank()
                    ? Map.of()
                    : json.readValue(rawArgs.toString(), Map.class);
            Object out = switch (name) {
                case "list_components" -> componentService.findAll(blankToNull(input.get("category"))).stream()
                        .map(ChatService::brief).toList();
                case "get_build" -> {
                    PcBuildResponse b = pcBuildService.findByCode(String.valueOf(input.get("code")));
                    yield Map.of("code", b.code(), "name", b.name(), "tagline", b.tagline(), "items", b.items().stream()
                            .map(i -> Map.of("quantity", i.quantity(), "component", brief(i.component()))).toList());
                }
                case "check_build" -> {
                    var req = checkRequest(input);
                    var result = matched(compatibilityService.check(req), req, turn);
                    // Đã chốt đề xuất thì giữ thẻ đề xuất, check_build sau đó chỉ để model tự thử
                    if (turn.last == null || turn.last.summary() == null) turn.last = new Checked(req, result);
                    yield result;
                }
                case "propose_build" -> {
                    var req = checkRequest(input);
                    var result = matched(compatibilityService.check(req), req, turn);
                    Map<String, Map<String, Object>> details = new LinkedHashMap<>();
                    ((List<Map<String, Object>>) input.getOrDefault("items", List.of())).stream()
                            .filter(i -> i.get("code") != null)
                            .forEach(i -> details.put(String.valueOf(i.get("code")), i));
                    // Sản phẩm đã chọn theo id; bắt đầu đổi link Google Shopping sang link cửa hàng ngay bây giờ,
                    // chạy song song trong lúc trợ lý viết câu trả lời
                    Map<String, ProductSearch.Product> picks = new LinkedHashMap<>();
                    Map<String, CompletableFuture<String>> links = new LinkedHashMap<>();
                    Map<String, List<ProductSearch.Product>> siblings = new ConcurrentHashMap<>();
                    details.forEach((code, d) -> {
                        String id = String.valueOf(d.get("product_id")).strip();
                        var pick = turn.results.get(id);
                        if (pick == null) return;
                        picks.put(code, pick);
                        var group = turn.groups.get(id.replaceFirst("-\\d+$", ""));
                        if (group != null) siblings.put(code, group);
                        links.put(code, Io.async(() -> search.directLink(pick.title(), pick.shop(), pick.url())));
                    });
                    // Món trợ lý chưa chọn được sản phẩm: app tự tìm, để dòng nào cũng có hàng thật kèm link mua
                    var auto = autoPick(req, details, picks, siblings);
                    auto.forEach((code, pick) -> {
                        picks.put(code, pick);
                        links.put(code, Io.async(() -> search.directLink(pick.title(), pick.shop(), pick.url())));
                    });
                    String tier = TIERS.contains(String.valueOf(input.get("tier"))) ? String.valueOf(input.get("tier")) : "balanced";
                    // CPU thật có đồ họa tích hợp (theo mã sản phẩm) thì không bắt buộc card rời: danh mục chỉ có một CPU
                    // chung chung ghi "không có đồ họa tích hợp" nên luôn báo thiếu card, đẩy máy văn phòng vượt ngân sách
                    result = withoutFalseGpuError(result, picks);
                    // Ba propose_build của cùng một lượt chạy song song: giữ thẻ trong biến riêng, không đọc lại turn.last
                    // Trợ lý có khi quên budget_vnd ở một tier: lấy ngân sách của tier khác trong cùng lượt
                    Long budget = input.get("budget_vnd") instanceof Number n ? n.longValue()
                            : turn.tiers.values().stream().map(Checked::budgetVnd).filter(java.util.Objects::nonNull).findFirst().orElse(null);
                    var checked = new Checked(req, result, details, String.valueOf(input.getOrDefault("summary", "")),
                            budget, picks, links, siblings);
                    turn.tiers.put(tier, checked);
                    turn.last = checked;
                    // Build 2 và 3 dựng ngay để việc đổi link cửa hàng chạy song song lúc trợ lý viết câu trả lời
                    // Tổng do backend cộng (giá cửa hàng nhân số lượng, hoặc giá ước tính), để trợ lý đọc đúng con số
                    long totalMin = 0, totalMax = 0;
                    for (var i : req.items()) {
                        Long[] range = price(details.getOrDefault(i.code(), Map.of()), picks.get(i.code()), i.qty());
                        totalMin += range[0] != null ? range[0] : (range[1] != null ? range[1] : 0);
                        totalMax += range[1] != null ? range[1] : (range[0] != null ? range[0] : 0);
                    }
                    Map<String, Object> saved = new LinkedHashMap<>();
                    saved.put("saved", true);
                    saved.put("compatibility", result);
                    var productErrors = productIssues(picks);
                    if (!productErrors.isEmpty()) {
                        log.info("propose_build {}: sản phẩm thật lắp không được: {}", tier, productErrors);
                        saved.put("product_errors", productErrors);
                    }
                    var productWarnings = productFindings(picks).stream()
                            .filter(f -> f.severity() == ProductRules.Severity.WARNING).map(ProductRules.Finding::message).toList();
                    if (!productWarnings.isEmpty()) saved.put("product_warnings", productWarnings);
                    saved.put("total_min_vnd", totalMin);
                    saved.put("total_max_vnd", totalMax);
                    saved.put("note", !result.compatible() ? "Cấu hình còn lỗi: đổi linh kiện rồi gọi lại propose_build."
                            : !productErrors.isEmpty() ? "Sản phẩm thật đã chọn lắp không được với nhau (xem product_errors): chọn lại "
                                    + "sản phẩm cho đúng socket / loại RAM rồi gọi lại propose_build với cùng tier."
                            : "Đã lưu đề xuất, app sẽ vẽ bảng chi tiết.");
                    // Phương án vượt trần của nó (Rẻ, Phù hợp: ngân sách; Chất lượng: +15%): báo để trợ lý chọn lại. Thẻ vượt
                    // trần không được gửi cho người dùng (respond)
                    Long cap = tierCap(checked.budgetVnd(), tier);
                    if (!result.compatible()) log.info("propose_build {}: chưa lắp chung được, trợ lý phải sửa", tier);
                    if (turn.advice && cap != null && totalMax > cap && !turn.overWarned.add(tier)) {
                        // Đã nhắc một lần mà vẫn vượt (thường do món đi kèm vỏ cố định): chấp nhận, chỉ cần nói rõ
                        log.info("propose_build {}: vẫn vượt trần ({} > {}), chấp nhận", tier, totalMax, cap);
                        saved.put("over_budget_note", "Vẫn vượt mức tối đa " + cap + " đồng. Không cần chọn lại nữa: "
                                + "nói rõ trong câu trả lời là phương án này vượt bao nhiêu và vì sao.");
                    } else if (turn.advice && cap != null && totalMax > cap) {
                        log.info("propose_build {}: tổng {} vượt trần {}, trợ lý phải chọn lại. Từng món: {}", tier, totalMax, cap,
                                req.items().stream().map(i -> i.code() + " x" + i.qty() + " = "
                                        + price(details.getOrDefault(i.code(), Map.of()), picks.get(i.code()), i.qty())[1]
                                        + (picks.get(i.code()) == null ? " (ước tính)" : " " + picks.get(i.code()).title())).toList());
                        saved.put("over_budget", "Tổng " + totalMax + " đồng vượt mức tối đa " + cap + " đồng của tier \"" + tier
                                + "\". Đổi vài món sang sản phẩm rẻ hơn (search_products) rồi gọi lại propose_build với cùng tier."
                                + ("cheap".equals(tier) ? " Món đi kèm vỏ đắt (tản nước, card 3 quạt) thì đổi sang vỏ khác rẻ hơn, "
                                        + "kể cả khi phải bỏ mặt kính, và nói rõ điều đó với người dùng." : ""));
                    }
                    if (!auto.isEmpty()) {
                        saved.put("auto_filled", auto.entrySet().stream().map(e -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("code", e.getKey());
                            m.put("product", e.getValue().title());
                            m.put("price_vnd", e.getValue().priceVnd());
                            return m;
                        }).toList());
                        saved.put("auto_note", "App đã tự gắn sản phẩm thật cho các món trong auto_filled. Tổng ở trên đã tính giá "
                                + "của chúng. Đừng nói là chưa tìm được hàng; nếu nhắc tới món đó thì dùng đúng tên và giá này.");
                    }
                    yield saved;
                }
                case "search_products" -> {
                    if (!search.enabled()) {
                        yield Map.of("error", "Chưa bật tìm kiếm sản phẩm. Dùng giá ước tính và để trống phần sản phẩm.");
                    }
                    var found = search.search(String.valueOf(input.get("query")));
                    int n = turn.searches.incrementAndGet();
                    turn.groups.put("s" + n, found);
                    List<Map<String, Object>> rows = new ArrayList<>();
                    for (int i = 0; i < found.size(); i++) {
                        String id = "s" + n + "-" + (i + 1);
                        var p = found.get(i);
                        turn.results.put(id, p);
                        // Không gửi link và ảnh cho trợ lý: nó chỉ cần chọn, link dài làm nó viết chậm
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", id);
                        m.put("title", p.title());
                        m.put("shop", p.shop());
                        m.put("price_vnd", p.priceVnd());
                        String specs = ProductRules.describeAny(p.title());
                        if (!specs.isEmpty()) m.put("specs", specs);
                        // Engine so giá đọc được trang sản phẩm thì báo còn hàng / bảo hành để trợ lý chọn món mua được
                        var offer = search.offer(p.url());
                        // verified: giá và link đọc thẳng trên trang cửa hàng; món không verified (Google Shopping) có
                        // thể không mua được hoặc giá đã khác
                        m.put("verified", offer != null && offer.priceVnd() != null);
                        if (offer != null && offer.inStock() != null) m.put("in_stock", offer.inStock());
                        if (offer != null && offer.warrantyMonths() != null) m.put("warranty_months", offer.warrantyMonths());
                        rows.add(m);
                    }
                    yield rows;
                }
                default -> throw new IllegalArgumentException("Không có công cụ " + name);
            };
            return json.writeValueAsString(out);
        } catch (RuntimeException e) {
            return "Lỗi: " + Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName());
        }
    }

    /** Trang tư vấn: thêm lỗi khi bo mạch, tản nhiệt, card hay nguồn khác máy mẫu 3D của vỏ đã chọn */
    private CompatibilityResponse matched(CompatibilityResponse result, BuildCheckRequest req, Turn turn) {
        return turn.advice ? ModelMatchService.merge(result, modelMatch.check(req).errors()) : result;
    }

    @SuppressWarnings("unchecked")
    private static BuildCheckRequest checkRequest(Map<String, Object> input) {
        return new BuildCheckRequest(((List<Map<String, Object>>) input.getOrDefault("items", List.of())).stream()
                .filter(i -> i.get("code") != null)
                .map(i -> new BuildCheckRequest.Item(String.valueOf(i.get("code")),
                        i.get("quantity") instanceof Number n ? n.intValue() : null))
                .toList());
    }

    /** Thẻ cấu hình cho khung chat: loại của từng linh kiện, sản phẩm đã chọn (nếu có), kèm kết quả kiểm tra */
    private BuildCard card(Checked original, boolean advice, String tier, int variant) {
        // Mọi thẻ (cả option 2, 3 và bộ rẻ nhất) đều dùng giá đọc trên trang cửa hàng của link
        Checked c = advice ? withPagePrices(original) : original;
        Map<String, Integer> qty = new LinkedHashMap<>();
        c.request().items().forEach(i -> qty.merge(i.code(), i.qty(), Integer::sum));
        List<Line> lines = new ArrayList<>();
        qty.forEach((code, n) -> {
            ComponentResponse part = componentService.findByCode(code);
            Map<String, Object> d = c.details().getOrDefault(code, Map.of());
            var pick = c.picks().get(code);
            Long[] range = price(d, pick, n);
            String link = pick == null ? null : c.links().get(code).join();
            // Bảng giá tốt nhất: còn hàng, bảo hành, lúc kiểm tra của món đã chọn, và cùng model ở cửa hàng khác
            var offer = pick == null ? null : link != null && search.offer(link) != null ? search.offer(link) : search.offer(pick.url());
            List<ChatResponse.ShopPrice> compare = !advice || pick == null ? List.of()
                    : search.compare(pick, c.siblings().getOrDefault(code, List.of())).stream()
                            .map(o -> new ChatResponse.ShopPrice(o.shop(), o.priceVnd(), o.url(), o.inStock(), o.checkedAt())).toList();
            lines.add(new Line(code, part.category(), part.name(), n, text(d.get("spec")), text(d.get("reason")),
                    range[0], range[1], pick == null ? null : pick.imageUrl(), link,
                    pick == null ? null : pick.title(), pick == null ? null : pick.shop(),
                    offer == null ? null : offer.inStock(), offer == null ? null : offer.warrantyMonths(),
                    offer == null ? null : offer.checkedAt(), compare, link == null ? null : priceChecked(link)));
        });
        var r = c.result();
        String note = advice ? modelMatch.check(c.request()).note() : null;
        // Kiểm tra trên sản phẩm thật theo docs/pc_compatibility_rules.md: ERROR làm thẻ báo đỏ (dù danh mục lắp được),
        // WARNING thêm vào cảnh báo, UNKNOWN là điểm app chưa xác minh được, RECOMMENDATION là lời khuyên
        var findings = productFindings(c.picks());
        List<Issue> errors = new ArrayList<>(forUser(r.errors()));
        List<Issue> warnings = new ArrayList<>(forUser(r.warnings()));
        List<Issue> unknowns = new ArrayList<>(), tips = new ArrayList<>();
        for (var f : findings) {
            var issue = new Issue(f.ruleId(), f.message());
            switch (f.severity()) {
                case ERROR -> errors.add(issue);
                case WARNING -> warnings.add(issue);
                case UNKNOWN -> unknowns.add(issue);
                case RECOMMENDATION -> tips.add(issue);
            }
        }
        // Mục 3: ưu tiên kit 2 thanh để chạy kênh đôi
        lines.stream().filter(l -> "ram".equals(l.category()) && l.quantity() == 1).findFirst().ifPresent(l -> tips.add(
                new Issue("RAM_DUAL_CHANNEL", "Nên dùng 2 thanh RAM (kit 2 thanh) để chạy kênh đôi, máy nhanh hơn rõ khi chơi game.")));
        boolean productOk = findings.stream().noneMatch(f -> f.severity() == ProductRules.Severity.ERROR);
        return new BuildCard(lines, r.compatible() && productOk, errors, warnings, c.summary(), c.budgetVnd(), note, tier, variant,
                unknowns, tips);
    }

    /** Lời nhắn cho trợ lý có kèm mã linh kiện trong ngoặc vuông; người dùng không cần thấy mã */
    private static List<Issue> forUser(List<Issue> issues) {
        return issues.stream().map(i -> new Issue(i.rule(), i.message().replaceAll(" \\[[a-z0-9_]+\\]", ""))).toList();
    }

    /**
     * Người dùng không rành kỹ thuật không tự tìm hàng theo thông số được, nên món nào trợ lý chưa chọn sản phẩm thì
     * app tự tìm: theo thông số trợ lý ghi, không ra thì theo tên loại linh kiện. Các món tìm song song.
     */
    private Map<String, ProductSearch.Product> autoPick(BuildCheckRequest req, Map<String, Map<String, Object>> details,
            Map<String, ProductSearch.Product> picks, Map<String, List<ProductSearch.Product>> siblings) {
        if (!search.enabled()) return Map.of();
        Map<String, CompletableFuture<ProductSearch.Product>> jobs = new LinkedHashMap<>();
        for (var i : req.items()) {
            if (picks.containsKey(i.code()) || jobs.containsKey(i.code())) continue;
            Map<String, Object> d = details.getOrDefault(i.code(), Map.of());
            String spec = text(d.get("spec"));
            String name;
            try {
                name = componentService.findByCode(i.code()).name();
            } catch (ResourceNotFoundException e) {
                continue; // mã lạ: kiểm tra tương thích đã báo lỗi
            }
            Long lo = vnd(d.get("price_min_vnd")), hi = vnd(d.get("price_max_vnd"));
            jobs.put(i.code(), Io.async(() -> findFor(spec, name, lo, hi, list -> siblings.put(i.code(), list))));
        }
        Map<String, ProductSearch.Product> out = new LinkedHashMap<>();
        jobs.forEach((code, job) -> {
            var p = job.join();
            if (p != null) out.put(code, p);
        });
        return out;
    }

    private ProductSearch.Product findFor(String spec, String name, Long lo, Long hi,
            java.util.function.Consumer<List<ProductSearch.Product>> found) {
        for (String query : Stream.of(spec, name).filter(Objects::nonNull).distinct().toList()) {
            try {
                var list = search.search(query);
                var p = choose(list, lo, hi);
                if (p != null) {
                    found.accept(list);
                    return p;
                }
            } catch (RuntimeException e) {
                log.warn("Tự tìm sản phẩm cho \"{}\" lỗi: {}", query, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Chọn một kết quả: món rẻ nhất có giá trong khoảng trợ lý ước tính (nới từ một nửa giá thấp tới gấp đôi giá cao, để
     * tránh phụ kiện rẻ bất thường); không có ước tính hay không món nào lọt khoảng thì lấy món có giá xếp đầu (kết quả
     * tìm kiếm đã xếp món liên quan nhất lên trước); không món nào có giá thì lấy kết quả đầu tiên.
     */
    static ProductSearch.Product choose(List<ProductSearch.Product> found, Long lo, Long hi) {
        if (found.isEmpty()) return null;
        var priced = found.stream().filter(p -> p.priceVnd() != null).toList();
        if (lo != null || hi != null) {
            long min = (lo != null ? lo : hi) / 2;
            long max = (hi != null ? hi : lo) * 2;
            var inRange = priced.stream().filter(p -> p.priceVnd() >= min && p.priceVnd() <= max)
                    .min(Comparator.comparing(ProductSearch.Product::priceVnd));
            if (inRange.isPresent()) return inRange.get();
        }
        return priced.isEmpty() ? found.getFirst() : priced.getFirst();
    }

    /** [giá thấp, giá cao] của một dòng: giá cửa hàng nhân số lượng nếu có sản phẩm, còn lại là giá ước tính */
    private static Long[] price(Map<String, Object> d, ProductSearch.Product pick, int qty) {
        if (pick != null && pick.priceVnd() != null) {
            // buy_qty: số sản phẩm phải mua (bộ kit 2 thanh RAM là 1 sản phẩm dù dòng có quantity 2)
            int buy = d.get("buy_qty") instanceof Number b && b.intValue() > 0 ? b.intValue() : qty;
            long v = pick.priceVnd() * buy;
            return new Long[] { v, v };
        }
        return new Long[] { vnd(d.get("price_min_vnd")), vnd(d.get("price_max_vnd")) };
    }

    private static String text(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString().strip();
    }

    /** Giá âm hay không phải số thì bỏ, để giao diện không cộng nhầm */
    private static Long vnd(Object o) {
        return o instanceof Number n && n.longValue() >= 0 ? n.longValue() : null;
    }

    /** Bản rút gọn của linh kiện cho AI: bỏ đường dẫn model / ảnh và các thông số còn trống */
    private static Map<String, Object> brief(ComponentResponse c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", c.code());
        m.put("category", c.category());
        m.put("name", c.name());
        if (c.description() != null) m.put("description", c.description());
        if (c.price() != null) m.put("price", c.price());
        Map<String, Object> specs = new LinkedHashMap<>();
        c.specs().forEach((k, v) -> {
            if (v != null) specs.put(k, v);
        });
        m.put("specs", specs);
        return m;
    }

    private static Map<String, Object> tool(String name, String description, Map<String, Object> parameters) {
        return Map.of("type", "function",
                "function", Map.of("name", name, "description", description, "parameters", parameters));
    }

    private static String blankToNull(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString();
    }

    /** Bỏ phần suy nghĩ <think>...</think> mà vài model suy luận để lẫn trong câu trả lời */
    private static String clean(Object content) {
        String t = content == null ? "" : content.toString().replaceAll("(?s)<think>.*?</think>", "").strip();
        return t.isEmpty() ? "Xin lỗi, mình chưa trả lời được câu này. Bạn thử hỏi theo cách khác nhé." : t;
    }
}
