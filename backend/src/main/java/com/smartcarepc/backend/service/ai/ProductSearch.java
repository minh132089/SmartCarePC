package com.smartcarepc.backend.service.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;

/**
 * Tìm sản phẩm thật đang bán ở cửa hàng Việt Nam qua Serper.dev (kết quả Google).
 * Ưu tiên mục Shopping vì có sẵn giá và ảnh; ít kết quả thì tìm Google thường rồi mở trang sản phẩm
 * để đọc ảnh (og:image) và giá (JSON-LD / meta). Khoá đặt bằng biến môi trường SERPER_API_KEY.
 */
@Slf4j
@Component
public class ProductSearch {

    /** Một sản phẩm tìm được. Giá theo đồng, null nếu trang không ghi giá. */
    public record Product(String title, String shop, Long priceVnd, String url, String imageUrl) {
    }

    private static final int MAX = 6;
    private static final Pattern OG_IMAGE = Pattern.compile(
            "<meta[^>]+(?:property|name)=[\"']og:image[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE_REV = Pattern.compile(
            "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]*(?:property|name)=[\"']og:image[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE = Pattern.compile(
            "(?:\"price\"\\s*:\\s*\"?|product:price:amount[\"'][^>]*content=[\"'])([0-9][0-9.,]{3,})", Pattern.CASE_INSENSITIVE);

    private final RestClient serper;
    private final RestClient web;
    private final String apiKey;

    public ProductSearch(@Value("${app.search.serper-key:}") String apiKey) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build());
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.serper = RestClient.builder().baseUrl("https://google.serper.dev").requestFactory(factory).build();
        this.web = RestClient.builder().requestFactory(factory)
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; PCSmartCare/1.0; +product-preview)")
                .defaultHeader("Accept-Language", "vi-VN,vi;q=0.9")
                .build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean enabled() {
        return !apiKey.isEmpty();
    }

    /** Tìm tối đa 6 sản phẩm, đã bỏ trùng link, ưu tiên món có cả giá và ảnh */
    public List<Product> search(String query) {
        if (!enabled()) throw new IllegalStateException("Chưa bật tìm kiếm sản phẩm (thiếu SERPER_API_KEY)");
        List<Product> out = new ArrayList<>(shopping(query));
        if (out.size() < 3) out.addAll(organic(query + " giá"));
        Map<String, Product> unique = new LinkedHashMap<>();
        out.stream().filter(p -> p.url() != null).forEach(p -> unique.putIfAbsent(p.url(), p));
        return unique.values().stream()
                .sorted((a, b) -> Integer.compare(score(b), score(a)))
                .limit(MAX)
                .toList();
    }

    private static int score(Product p) {
        return (p.priceVnd() != null ? 2 : 0) + (p.imageUrl() != null ? 1 : 0);
    }

    /**
     * Link trong mục Shopping là trang Google Shopping. Với sản phẩm đã chọn, tìm thêm một lần để lấy link trang
     * sản phẩm trên đúng cửa hàng đó (khi tên cửa hàng là tên miền, vd "Fptshop.com.vn"). Không chắc thì giữ link cũ,
     * vì từ trang Google Shopping người dùng vẫn bấm sang cửa hàng được.
     */
    @SuppressWarnings("unchecked")
    public String directLink(String title, String shop, String link) {
        if (!enabled() || link == null || title == null || !link.contains("google.com/")) return link;
        String domain = shop == null ? null : shop.toLowerCase().strip().replaceFirst("^www\\.", "");
        if (domain == null || !domain.matches("[a-z0-9.-]+\\.[a-z]{2,}")) return link;
        Map<String, Object> res = post("/search", title + " site:" + domain);
        return ((List<Map<String, Object>>) res.getOrDefault("organic", List.of())).stream()
                .map(i -> https(i.get("link")))
                .filter(u -> u != null && host(u) != null && (host(u).equals(domain) || host(u).endsWith("." + domain)))
                .findFirst()
                .orElse(link);
    }

    @SuppressWarnings("unchecked")
    private List<Product> shopping(String query) {
        Map<String, Object> res = post("/shopping", query);
        var items = (List<Map<String, Object>>) res.getOrDefault("shopping", List.of());
        return items.stream()
                .map(i -> new Product(str(i.get("title")), str(i.get("source")), vnd(i.get("price")),
                        https(i.get("link")), https(first(i.get("imageUrl"), i.get("image"), i.get("thumbnail")))))
                .filter(p -> p.title() != null && p.url() != null)
                .toList();
    }

    /** Google thường: lấy link sản phẩm rồi mở song song từng trang để đọc ảnh và giá */
    @SuppressWarnings("unchecked")
    private List<Product> organic(String query) {
        Map<String, Object> res = post("/search", query);
        var items = ((List<Map<String, Object>>) res.getOrDefault("organic", List.of())).stream()
                .filter(i -> https(i.get("link")) != null)
                .limit(5)
                .toList();
        var futures = items.stream().map(i -> CompletableFuture.supplyAsync(() -> {
            String url = https(i.get("link"));
            String[] meta = pageMeta(url);
            return new Product(str(i.get("title")), host(url), vnd(meta[1]), url, https(meta[0]));
        })).toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, String query) {
        try {
            Map<String, Object> res = serper.post().uri(path)
                    .header("X-API-KEY", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("q", query, "gl", "vn", "hl", "vi", "num", 10))
                    .retrieve()
                    .body(Map.class);
            return res == null ? Map.of() : res;
        } catch (RestClientException e) {
            log.warn("Serper {} lỗi: {}", path, e.getMessage());
            return Map.of();
        }
    }

    /** [ảnh og:image, giá] đọc từ HTML trang sản phẩm; lỗi thì trả rỗng */
    private String[] pageMeta(String url) {
        try {
            String html = web.get().uri(URI.create(url)).retrieve().body(String.class);
            if (html == null) return new String[2];
            String head = html.length() > 400_000 ? html.substring(0, 400_000) : html;
            return new String[] { find(OG_IMAGE, head, OG_IMAGE_REV), find(PRICE, head, null) };
        } catch (RuntimeException e) {
            return new String[2];
        }
    }

    private static String find(Pattern p, String s, Pattern alt) {
        Matcher m = p.matcher(s);
        if (m.find()) return m.group(1);
        if (alt != null) {
            Matcher a = alt.matcher(s);
            if (a.find()) return a.group(1);
        }
        return null;
    }

    /** "1.290.000 ₫", "₫1,290,000", "1290000.00" -> 1290000; số quá nhỏ (không phải tiền đồng) thì bỏ */
    static Long vnd(Object o) {
        if (o == null) return null;
        String s = o.toString().replaceAll("\\.00$|,00$", "").replaceAll("[^0-9]", "");
        if (s.isEmpty() || s.length() > 11) return null;
        long v = Long.parseLong(s);
        return v >= 10_000 ? v : null;
    }

    static String https(Object o) {
        String s = str(o);
        if (s != null && s.startsWith("//")) s = "https:" + s;
        return s != null && s.startsWith("https://") && s.length() <= 2000 && !s.contains(" ") ? s : null;
    }

    private static String host(String url) {
        try {
            return URI.create(url).getHost().replaceFirst("^www\\.", "");
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Object first(Object... os) {
        for (Object o : os) if (o != null && !o.toString().isBlank()) return o;
        return null;
    }

    private static String str(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString().strip();
    }
}
