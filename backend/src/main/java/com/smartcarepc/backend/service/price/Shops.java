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
            Map.entry("phucanh.vn", "Phúc Anh Computer"),
            Map.entry("nguyencongpc.vn", "Nguyễn Công PC"),
            Map.entry("anduycom.vn", "AnDuy Computer"),
            Map.entry("sp-one.vn", "SP-One"),
            Map.entry("trianhtech.vn", "Trí An Tech"),
            Map.entry("hotgear.vn", "HotGear"),
            Map.entry("khoavang.vn", "Khoa Vàng"),
            Map.entry("vtcom.com.vn", "VTCom"),
            Map.entry("thanhcongcomputer.vn", "Thành Công Computer"),
            Map.entry("hugotech.vn", "Hugo Tech"),
            Map.entry("dinhvangcomputer.vn", "Đinh Vàng Computer"),
            Map.entry("tinviettien.vn", "Tín Việt Tiến"),
            Map.entry("mygear.com.vn", "MyGear"),
            Map.entry("kenhtinhoc.vn", "Kênh Tin Học"),
            Map.entry("tpassion.vn", "TPassion"),
            Map.entry("hoavinhcomputer.vn", "Hoa Vinh Computer"),
            Map.entry("ynstore.vn", "YN Store"),
            Map.entry("phucnguyendanang.com", "Phúc Nguyên"),
            Map.entry("sicomp.vn", "SiComp"),
            Map.entry("wbpc.vn", "WBPC"),
            Map.entry("trivietpc.vn", "Trí Việt PC"),
            Map.entry("hocostore.vn", "Hòco Store"),
            Map.entry("vietphatcomputer.com", "Việt Phát Computer"),
            Map.entry("ggstore.com.vn", "GG Store"),
            Map.entry("skycomputer.vn", "Sky Computer"),
            Map.entry("mega.com.vn", "Mega"),
            Map.entry("giatin.com.vn", "Gia Tín"),
            Map.entry("thanhnhancomputer.vn", "Thanh Nhân Computer"),
            Map.entry("gearvn.com", "GEARVN"), Map.entry("anphatpc.com.vn", "An Phát"), Map.entry("phongvu.vn", "Phong Vũ"),
            Map.entry("hacom.vn", "Hacom"), Map.entry("nguyenvu.store", "Nguyễn Vũ Store"), Map.entry("fptshop.com.vn", "FPT Shop"),
            Map.entry("cellphones.com.vn", "CellphoneS"), Map.entry("memoryzone.com.vn", "Memoryzone"),
            Map.entry("tinhocngoisao.com", "Tin học Ngôi Sao"), Map.entry("kccshop.vn", "KCC Shop"), Map.entry("hoanghapc.vn", "Hoàng Hà PC"),
            Map.entry("ankhang.vn", "An Khang"), Map.entry("songphuong.vn", "Song Phương"),
            Map.entry("tandoanh.vn", "Tân Doanh"), Map.entry("pcmarket.vn", "PC Market"), Map.entry("xgear.net", "Xgear"),
            Map.entry("tncstore.vn", "TNC Store"), Map.entry("ttgshop.vn", "TTG Shop"),
            Map.entry("tinhocanhphat.vn", "Tin học Anh Phát"));

    /** Trang danh mục, bài viết, tìm kiếm, chính sách...: không phải trang một sản phẩm */
    private static final java.util.regex.Pattern NOT_PRODUCT = java.util.regex.Pattern.compile(
            "/(collections?|blogs?|tin-tuc|tin-cong-nghe|news|c|danh-muc|category|categories|tag|tags|tim|tim-kiem|search|pages|"
                    + "huong-dan|review|chinh-sach|lien-he|gioi-thieu|tuyen-dung|khuyen-mai|build-pc|buildpc|brand|brands|"
                    + "thuong-hieu|sitemap|account|cart|gio-hang)(/|$|\\?|\\.html)"
                    // Trang chính sách / tin / hướng dẫn hay có đuôi: /chinh-sach-kiem-hang.html, /tin-tuc-cong-nghe
                    + "|/(chinh-sach|tin-tuc|huong-dan|gioi-thieu|lien-he|tuyen-dung|khuyen-mai)-"
                    + "|/(products?|san-pham)/?$|[?&](q|s|query|keyword|page)=",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    /** Link có dạng trang một sản phẩm (không phải trang chủ, danh mục, tìm kiếm, tin tức, chính sách) */
    public static boolean productLike(String url) {
        try {
            URI u = URI.create(url);
            String path = (u.getRawPath() == null ? "" : u.getRawPath()) + (u.getRawQuery() == null ? "" : "?" + u.getRawQuery());
            return u.getHost() != null && path.length() > 1 && !NOT_PRODUCT.matcher(path).find();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

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
