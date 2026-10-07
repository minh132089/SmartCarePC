package com.smartcarepc.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Danh sách linh kiện người dùng đang chọn trong PC Builder, vd:
 * { "items": [ { "code": "case_mini_sff" }, { "code": "ram_ddr4", "quantity": 2 } ] }
 */
public record BuildCheckRequest(@NotEmpty(message = "Chưa chọn linh kiện nào") List<@Valid Item> items) {

    public record Item(
            @NotBlank(message = "Thiếu mã linh kiện") String code,
            @Min(value = 1, message = "Số lượng phải từ 1 trở lên") Integer quantity) {

        /** Không gửi quantity thì tính là 1 */
        public int qty() {
            return quantity == null ? 1 : quantity;
        }
    }
}
