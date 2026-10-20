package com.smartcarepc.backend.service.price;

import java.net.URI;
import java.util.Map;

/** Các cửa hàng máy tính Việt Nam engine đọc giá: tên miền -> tên hiện cho người dùng */
public final class Shops {

    private Shops() {
    }

    /**
     * Chỉ cửa hàng linh kiện / máy tính có trang sản phẩm đọc được (giá nằm sẵn trong HTML). Không gồm sàn TMĐT
     * (Shopee, Lazada, Tiki): trang dựng bằng JavaScript và nhiều người bán lẫn lộn.
     */
    static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("gearvn.com", "GEARVN"), Map.entry("anphatpc.com.vn", "An Phát"), Map.entry("phongvu.vn", "Phong Vũ"),
            Map.entry("hacom.vn", "Hacom"), Map.entry("nguyenvu.store", "Nguyễn Vũ Store"), Map.entry("fptshop.com.vn", "FPT Shop"),
            Map.entry("cellphones.com.vn", "CellphoneS"), Map.entry("memoryzone.com.vn", "Memoryzone"),
            Map.entry("tinhocngoisao.com", "Tin học Ngôi Sao"), Map.entry("kccshop.vn", "KCC Shop"), Map.entry("hoanghapc.vn", "Hoàng Hà PC"),
            Map.entry("ankhang.vn", "An Khang"), Map.entry("hanoicomputer.vn", "Hà Nội Computer"), Map.entry("songphuong.vn", "Song Phương"),
            Map.entry("tandoanh.vn", "Tân Doanh"), Map.entry("pcmarket.vn", "PC Market"), Map.entry("xgear.net", "Xgear"),
            Map.entry("tncstore.vn", "TNC Store"), Map.entry("ttgshop.vn", "TTG Shop"), Map.entry("vitinhanphu.com", "Vi Tính An Phú"),
            Map.entry("tinhocanhphat.vn", "Tin học Anh Phát"), Map.entry("thegioididong.com", "Thế Giới Di Động"));

    /** Tên miền đã bỏ "www." của url, null nếu url hỏng */
    static String domain(String url) {
        try {
            String h = URI.create(url).getHost();
            return h == null ? null : h.toLowerCase().replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Url thuộc một cửa hàng trong danh sách */
    static boolean known(String url) {
        String d = domain(url);
        return d != null && NAMES.containsKey(d);
    }

    static String name(String url) {
        String d = domain(url);
        return d == null ? null : NAMES.getOrDefault(d, d);
    }
}
