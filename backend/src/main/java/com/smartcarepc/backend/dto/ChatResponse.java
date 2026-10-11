package com.smartcarepc.backend.dto;

import java.util.List;

import com.smartcarepc.backend.dto.CompatibilityResponse.Issue;

/**
 * Câu trả lời của trợ lý. reply có thể chứa gạch đầu dòng (- ) và chữ đậm (**...**).
 * build: cấu hình trợ lý vừa kiểm tra trong lượt này (null nếu không có), để khung chat vẽ thành thẻ.
 * Khi trợ lý chốt cấu hình bằng propose_build, thẻ có thêm nhu cầu đã hiểu, ngân sách, và từng linh kiện có
 * thông số nên mua, lý do chọn, giá (VND, cho cả số lượng của dòng đó), và sản phẩm thật trợ lý tìm được
 * bằng search_products: tên (productName), cửa hàng (shop), ảnh (imageUrl), trang mua (sourceUrl).
 * Link chỉ nhận khi có trong kết quả tìm kiếm của lượt đó, nên không có link bịa; không có thì null.
 */
public record ChatResponse(String reply, BuildCard build) {

    public ChatResponse(String reply) {
        this(reply, null);
    }

    public record BuildCard(List<Line> items, boolean compatible, List<Issue> errors, List<Issue> warnings,
            String summary, Long budgetVnd) {
    }

    public record Line(String code, String category, String name, int quantity,
            String spec, String reason, Long priceMinVnd, Long priceMaxVnd, String imageUrl, String sourceUrl,
            String productName, String shop) {
    }
}
