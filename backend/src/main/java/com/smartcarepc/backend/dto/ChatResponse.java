package com.smartcarepc.backend.dto;

import java.time.Instant;
import java.util.List;

import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;

/**
 * Câu trả lời của trợ lý. reply có thể chứa gạch đầu dòng (- ) và chữ đậm (**...**).
 * build: cấu hình trợ lý vừa kiểm tra trong lượt này (null nếu không có), để khung chat vẽ thành thẻ.
 * Khi trợ lý chốt cấu hình bằng propose_build, thẻ có thêm nhu cầu đã hiểu, ngân sách, và từng linh kiện có
 * thông số nên mua, lý do chọn, giá (VND, cho cả số lượng của dòng đó), và sản phẩm thật trợ lý tìm được
 * bằng search_products: tên (productName), cửa hàng (shop), ảnh (imageUrl), trang mua (sourceUrl).
 * Link luôn là trang của cửa hàng (không qua Google), lấy từ kết quả tìm kiếm thật; không có thì null.
 * Giá đọc từ web cửa hàng lúc checkedAt; compare liệt kê cùng model ở cửa hàng khác.
 * modelNote: ghi chú khi số thanh RAM hoặc số quạt khác máy mẫu 3D của vỏ đã chọn (null nếu giống).
 * options: những phương án Rẻ / Phù hợp cho bạn / Chất lượng đã qua kiểm tra (tier cheap / balanced / quality).
 * build là gợi ý chính: thường là balanced; có thể là cheap khi giữ cùng CPU, GPU và RAM với mức tiết kiệm rõ rệt.
 */
public record ChatResponse(String reply, BuildCard build, List<BuildCard> options) {

    public ChatResponse(String reply) {
        this(reply, null, null);
    }

    public ChatResponse(String reply, BuildCard build) {
        this(reply, build, null);
    }

    /**
     * Mức kiểm tra theo docs/pc_compatibility_rules.md: errors (ERROR, phải đổi linh kiện), warnings (WARNING),
     * unknowns (UNKNOWN: app chưa xác minh được, vd CPU Support List của bo; người dùng nên tự kiểm tra trước khi mua),
     * recommendations (RECOMMENDATION: lời khuyên, không bắt buộc).
     */
    public record BuildCard(List<Line> items, boolean compatible, List<Issue> errors, List<Issue> warnings,
            String summary, Long budgetVnd, String modelNote, String tier, int variant, List<Issue> unknowns,
            List<Issue> recommendations, Verdict verdict) {

        public BuildCard(List<Line> items, boolean compatible, List<Issue> errors, List<Issue> warnings, String summary,
                Long budgetVnd, String modelNote, String tier, int variant) {
            this(items, compatible, errors, warnings, summary, budgetVnd, modelNote, tier, variant, List.of(), List.of(), null);
        }

        public BuildCard(List<Line> items, boolean compatible, List<Issue> errors, List<Issue> warnings, String summary,
                Long budgetVnd, String modelNote, String tier, int variant, List<Issue> unknowns, List<Issue> recommendations) {
            this(items, compatible, errors, warnings, summary, budgetVnd, modelNote, tier, variant, unknowns, recommendations, null);
        }
    }

    /**
     * Trạng thái phương án theo docs/pc-build-verifier (mục 1.3, 7.2): READY / CONDITIONAL / HOLD / BLOCKED. technical:
     * lắp được, khởi động được; purchase: link, giá, còn hàng, ngân sách; status: mức xấu nhất của hai phần.
     * blockers: điểm đang chặn; conditions: việc cần làm trước khi mua. Chỉ có ở trang tư vấn (null ở khung lắp máy).
     */
    public record Verdict(String status, String technical, String purchase, List<String> blockers, List<String> conditions) {
    }

    /**
     * inStock / warrantyMonths / checkedAt: engine so giá đọc từ trang cửa hàng (null nếu không biết). compare: cùng
     * model ở cửa hàng khác, rẻ nhất trước (tối đa 3), để người dùng thấy bảng giá tốt nhất. priceChecked: giá đã đọc
     * lại trên chính trang của sourceUrl (false: giá tham khảo từ Google Shopping, chưa kiểm tra trên trang).
     */
    public record Line(String code, String category, String name, int quantity,
            String spec, String reason, Long priceMinVnd, Long priceMaxVnd, String imageUrl, String sourceUrl,
            String productName, String shop, Boolean inStock, Integer warrantyMonths, Instant checkedAt,
            List<ShopPrice> compare, Boolean priceChecked) {

        public Line(String code, String category, String name, int quantity, String spec, String reason, Long priceMinVnd,
                Long priceMaxVnd, String imageUrl, String sourceUrl, String productName, String shop) {
            this(code, category, name, quantity, spec, reason, priceMinVnd, priceMaxVnd, imageUrl, sourceUrl, productName, shop,
                    null, null, null, List.of(), null);
        }
    }

    /** Giá của cùng sản phẩm ở một cửa hàng khác */
    public record ShopPrice(String shop, Long priceVnd, String url, Boolean inStock, Instant checkedAt) {
    }
}
