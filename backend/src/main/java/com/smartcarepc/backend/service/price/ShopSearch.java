package com.smartcarepc.backend.service.price;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    GEARVN("https://gearvn.com/search?q=", Duration.ofSeconds(4)) {
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
    ANPHAT("https://www.anphatpc.com.vn/tim?q=", Duration.ofSeconds(1)) {
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
    },

    /** Haravan: giá bán ở span.price của từng thẻ, không đọc giá gạch ngang hay JSON đơn vị xu. */
    HTCGEARS("https://htcgears.com/collections/vga", Duration.ofSeconds(3)) {
        @Override
        boolean supports(String query) {
            return query.toLowerCase(java.util.Locale.ROOT).matches(".*\\b(vga|gpu|rtx|gtx|rx|radeon|geforce)\\b.*");
        }

        @Override
        String url(String query) {
            return searchUrl; // /search bị robots.txt cấm; chỉ đọc danh mục được phép rồi lọc model tại engine.
        }

        @Override
        List<Offer> parse(String html, Instant at) {
            Document doc = Jsoup.parse(html, "https://htcgears.com/");
            List<Offer> out = new ArrayList<>();
            for (Element item : doc.select(".listProduct-filter .product-loop")) {
                Element name = item.selectFirst("h3 a[href*=/products/]");
                Element price = item.selectFirst(".proloop--price .price");
                if (name == null || price == null) continue;
                Long value = Prices.parse(price.text());
                if (value == null || value < 10_000) continue;
                Element img = item.selectFirst("img[data-src], img[src]");
                String image = img == null ? null : img.absUrl(img.hasAttr("data-src") ? "data-src" : "src");
                out.add(new Offer(name.text().strip(), "HTC Gears", value, null, name.absUrl("href"), image, null, null, at));
            }
            return out;
        }
    },

    /** TPassion cấm /search; đọc hai trang đầu danh mục SSD, rồi engine lọc model và kiểm trang mua. */
    TPASSION("https://tpassion.vn/collections/o-cung-ssd", Duration.ofSeconds(3)) {
        @Override
        boolean supports(String query) {
            return "ssd".equals(PriceEngine.kind(PriceEngine.norm(query)));
        }

        @Override
        List<String> urls(String query) {
            return List.of(searchUrl, searchUrl + "?page=2");
        }

        @Override
        List<Offer> parse(String html, Instant at) {
            Document doc = Jsoup.parse(html, "https://tpassion.vn/");
            List<Offer> out = new ArrayList<>();
            for (Element card : doc.select(".product-list form.variants[data-id]")) {
                Element name = card.selectFirst("h3.product-name a[href*=/products/]");
                Element price = card.selectFirst(".price-box .price");
                Long value = price == null ? null : Prices.parse(price.text());
                if (name == null || value == null || value < 10_000) continue;
                // Nguồn bổ sung cho ổ M.2 trong app: không suy ổ SATA 2.5 inch từ tên chỉ có dung lượng/tốc độ.
                if (!name.text().matches("(?iu).*\\b(?:m\\.?2|nvme)\\b.*")) continue;
                Element img = card.selectFirst("img.product-thumbnail__img--primary");
                out.add(new Offer(name.text().strip(), "TPassion", value, null, name.absUrl("href"),
                        img == null ? null : img.absUrl("src"), null, null, at));
            }
            return out;
        }
    };

    private static final Pattern MONEY = Pattern.compile("(\\d{1,3}(?:[.,]\\d{3}){1,3})\\s*(?:đ|₫|vnđ|vnd)", Pattern.CASE_INSENSITIVE);

    final String searchUrl;
    /**
     * Chờ tới lượt gọi tối đa bao lâu. An Phát yêu cầu cách 5 giây mỗi lần (robots.txt): nhiều món tìm cùng lúc thì
     * chỉ món tới lượt ngay mới đọc An Phát, món khác bỏ qua thay vì đứng chờ
     */
    final Duration maxWait;

    ShopSearch(String searchUrl, Duration maxWait) {
        this.searchUrl = searchUrl;
        this.maxWait = maxWait;
    }

    String url(String query) {
        return searchUrl + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    List<String> urls(String query) {
        return List.of(url(query));
    }

    boolean supports(String query) {
        return true;
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
