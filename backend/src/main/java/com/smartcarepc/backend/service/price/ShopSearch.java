package com.smartcarepc.backend.service.price;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Đọc trang tìm kiếm của cửa hàng có kết quả nằm sẵn trong HTML. Mỗi cửa hàng một cách đọc riêng vì giao diện khác
 * nhau; cửa hàng đổi giao diện thì sửa đúng hàm của cửa hàng đó (test dùng HTML đã lưu ở src/test/resources/shops).
 */
enum ShopSearch {

    /** GEARVN: thẻ a.product-card, tên ở ảnh / thẻ p, giá gốc và giá bán là hai số tiền trong thẻ (giá bán thấp hơn) */
    GEARVN("https://gearvn.com/search?q=") {
        @Override
        List<Offer> parse(String html, Instant at) {
            Document doc = Jsoup.parse(html, "https://gearvn.com/");
            List<Offer> out = new ArrayList<>();
            for (Element card : doc.select("a.product-card[href*=/products/]")) {
                Element img = card.selectFirst("img[alt]");
                Element p = card.selectFirst("[data-slot=product-card-content] p");
                String name = p != null && !p.text().isBlank() ? p.text() : img == null ? null : img.attr("alt");
                List<Long> prices = prices(card.text());
                if (name == null || prices.isEmpty()) continue;
                long price = prices.stream().min(Comparator.naturalOrder()).orElseThrow();
                Long old = prices.stream().max(Comparator.naturalOrder()).filter(o -> o > price).orElse(null);
                out.add(new Offer(name.strip(), "GEARVN", price, old, card.absUrl("href"),
                        img == null ? null : img.absUrl("src"), null, null, at));
            }
            return out;
        }
    },

    /** An Phát: .p-item, tên .p-name, giá bán .price-container .p-price, giá gốc del.p-old-price. robots.txt: cách 5 giây */
    ANPHAT("https://www.anphatpc.com.vn/tim?q=") {
        @Override
        List<Offer> parse(String html, Instant at) {
            Document doc = Jsoup.parse(html, "https://www.anphatpc.com.vn/");
            List<Offer> out = new ArrayList<>();
            for (Element item : doc.select(".p-item[data-id]")) {
                Element name = item.selectFirst("a.p-name");
                Element price = item.selectFirst(".price-container .p-price");
                if (name == null || price == null) continue;
                Long p = Prices.parse(price.text());
                if (p == null || p < 10_000) continue;
                Element old = item.selectFirst("del.p-old-price");
                Element img = item.selectFirst("img[data-src], img[src]");
                String image = img == null ? null : img.hasAttr("data-src") ? img.absUrl("data-src") : img.absUrl("src");
                out.add(new Offer(name.text().strip(), "An Phát", p, old == null ? null : Prices.parse(old.text()),
                        name.absUrl("href"), image, null, null, at));
            }
            return out;
        }
    };

    private static final Pattern MONEY = Pattern.compile("(\\d{1,3}(?:[.,]\\d{3}){1,3})\\s*(?:đ|₫|vnđ|vnd)", Pattern.CASE_INSENSITIVE);

    final String searchUrl;

    ShopSearch(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    String url(String query) {
        return searchUrl + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    abstract List<Offer> parse(String html, Instant at);

    /** Các số tiền trong một đoạn chữ, bỏ số nhỏ (vd "giảm 500.000đ" vẫn giữ, lọc ở nơi gọi theo giá lớn nhất) */
    static List<Long> prices(String text) {
        List<Long> out = new ArrayList<>();
        Matcher m = MONEY.matcher(text);
        while (m.find()) {
            Long v = Prices.parse(m.group(1));
            if (v != null && v >= 10_000) out.add(v);
        }
        // Số tiền nhỏ hơn 40% số lớn nhất là tiền giảm / quà, không phải giá
        long max = out.stream().max(Comparator.naturalOrder()).orElse(0L);
        out.removeIf(v -> v < max * 0.4);
        return out;
    }
}
