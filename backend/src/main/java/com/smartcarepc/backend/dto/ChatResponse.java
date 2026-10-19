package com.smartcarepc.backend.dto;

import java.util.List;

import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;

/**
 * Câu trả lời của trợ lý. reply có thể chứa gạch đầu dòng (- ) và chữ đậm (**...**).
 * build: cấu hình trợ lý vừa kiểm tra trong lượt này (null nếu không có), để khung chat vẽ thành thẻ.
 * Khi trợ lý chốt cấu hình bằng propose_build, thẻ có thêm nhu cầu đã hiểu, ngân sách, và từng linh kiện có
 * thông số nên mua, lý do chọn, giá (VND, cho cả số lượng của dòng đó), và sản phẩm thật trợ lý tìm được
 * bằng search_products: tên (productName), cửa hàng (shop), ảnh (imageUrl), trang mua (sourceUrl).
 * Link luôn là trang của cửa hàng (không qua Google), lấy từ kết quả tìm kiếm thật; không có thì null.
 * modelNote: ghi chú khi số thanh RAM hoặc số quạt khác máy mẫu 3D của vỏ đã chọn (null nếu giống).
 * options: ba phương án Rẻ / Phù hợp cho bạn / Chất lượng (tier cheap / balanced / quality) khi trợ lý đưa đủ;
 * build khi đó là phương án balanced. Mỗi tier có tới 3 build (variant 1 = trợ lý chọn; 2 và 3 = cùng linh kiện,
 * đổi sang sản phẩm khác giá gần để tham khảo).
 */
public record ChatResponse(String reply, BuildCard build, List<BuildCard> options) {

    public ChatResponse(String reply) {
        this(reply, null, null);
    }

    public ChatResponse(String reply, BuildCard build) {
        this(reply, build, null);
    }

    public record BuildCard(List<Line> items, boolean compatible, List<Issue> errors, List<Issue> warnings,
            String summary, Long budgetVnd, String modelNote, String tier, int variant) {
    }

    public record Line(String code, String category, String name, int quantity,
            String spec, String reason, Long priceMinVnd, Long priceMaxVnd, String imageUrl, String sourceUrl,
            String productName, String shop) {
    }
}
