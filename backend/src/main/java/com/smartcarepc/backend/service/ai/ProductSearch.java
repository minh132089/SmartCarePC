package com.smartcarepc.backend.service.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
        // Trang cửa hàng chậm thì bỏ qua sớm: chờ tối đa 4 giây, thà thiếu ảnh còn hơn bắt người dùng chờ
        var pageFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NORMAL).build());
        pageFactory.setReadTimeout(Duration.ofSeconds(4));
        this.web = RestClient.builder().requestFactory(pageFactory)
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; PCSmartCare/1.0; +product-preview)")
                .defaultHeader("Accept-Language", "vi-VN,vi;q=0.9")
                .build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean enabled() {
        return !apiKey.isEmpty();
    }

    /** Kết quả đã tìm trong 30 phút gần đây, theo từ khoá: câu hỏi lặp lại trả lời nhanh và không tốn lượt Serper */
    private static final long CACHE_MS = 30 * 60 * 1000;
    private record Cached(long at, List<Product> products) {
    }
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    /** Tìm tối đa 6 sản phẩm, đã bỏ trùng link, ưu tiên món có cả giá và ảnh */
    public List<Product> search(String query) {
        if (!enabled()) throw new IllegalStateException("Chưa bật tìm kiếm sản phẩm (thiếu SERPER_API_KEY)");
        String key = query.strip().toLowerCase();
        var hit = cache.get(key);
        if (hit != null && System.currentTimeMillis() - hit.at() < CACHE_MS) return hit.products();
        var found = fresh(query);
        if (!found.isEmpty()) {
            if (cache.size() > 500) cache.clear();
            cache.put(key, new Cached(System.currentTimeMillis(), found));
        }
        return found;
    }

    private List<Product> fresh(String query) {
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
     * Tên cửa hàng máy tính hay gặp trong kết quả Google Shopping -> tên miền. Khoá là tên đã bỏ dấu, viết thường,
     * bỏ khoảng trắng và ký tự lạ (xem slug()).
     */
    private static final Map<String, String> SHOP_DOMAINS = Map.ofEntries(
            Map.entry("phongvu", "phongvu.vn"), Map.entry("hacom", "hacom.vn"), Map.entry("gearvn", "gearvn.com"),
            Map.entry("anphatpc", "anphatpc.com.vn"), Map.entry("anphat", "anphatpc.com.vn"),
            Map.entry("nguyenvustore", "nguyenvu.store"), Map.entry("nguyenvu", "nguyenvu.store"),
            Map.entry("fptshop", "fptshop.com.vn"), Map.entry("thegioididong", "thegioididong.com"),
            Map.entry("dienmayxanh", "dienmayxanh.com"), Map.entry("cellphones", "cellphones.com.vn"),
            Map.entry("memoryzone", "memoryzone.com.vn"), Map.entry("tinhocngoisao", "tinhocngoisao.com"),
            Map.entry("xgear", "xgear.net"), Map.entry("kccshop", "kccshop.vn"), Map.entry("pcmarket", "pcmarket.vn"),
            Map.entry("songphuong", "songphuong.vn"), Map.entry("hanoicomputer", "hanoicomputer.vn"),
            Map.entry("ankhang", "ankhang.vn"), Map.entry("ankhangpc", "ankhang.vn"), Map.entry("vitinhanphu", "vitinhanphu.com"),
            Map.entry("tandoanh", "tandoanh.vn"), Map.entry("pcx", "pcx.vn"), Map.entry("hoanghapc", "hoanghapc.vn"),
            Map.entry("shopee", "shopee.vn"), Map.entry("lazada", "lazada.vn"), Map.entry("tiki", "tiki.vn"));

    /**
     * Link trong mục Shopping là trang Google Shopping. Người dùng phải được đưa thẳng tới trang của cửa hàng, nên:
     * 1. biết tên miền cửa hàng (tên là tên miền, hoặc có trong SHOP_DOMAINS) thì tìm trang sản phẩm trên tên miền đó;
     * 2. không thì tìm "tên sản phẩm + tên cửa hàng", lấy kết quả đầu tiên có tên miền giống tên cửa hàng;
     * 3. vẫn không có thì trang chủ của cửa hàng (biết tên miền), hoặc trang sản phẩm đầu tiên ngoài Google.
     * Không bao giờ trả về link Google; không tìm được gì thì null (giao diện dùng nút tìm trên trang cửa hàng).
     */
    @SuppressWarnings("unchecked")
    public String directLink(String title, String shop, String link) {
        if (link != null && !isGoogle(link)) return link;
        if (!enabled() || title == null) return null;
        String domain = domainOf(shop);
        if (domain != null) {
            String hit = firstOn(post("/search", title + " site:" + domain), domain);
            return hit != null ? hit : "https://" + domain + "/";
        }
        var organic = (List<Map<String, Object>>) post("/search", title + (shop == null ? "" : " " + shop)).getOrDefault("organic", List.of());
        String key = shop == null ? null : slug(shop);
        String any = null;
        for (var i : organic) {
            String u = https(i.get("link"));
            if (u == null || isGoogle(u) || host(u) == null) continue;
            if (key != null && key.length() >= 4 && slug(host(u)).contains(key)) return u;
            if (any == null) any = u;
        }
        return any;
    }

    /** Tên miền của cửa hàng theo tên hiện trong kết quả Shopping, null nếu không biết */
    static String domainOf(String shop) {
        if (shop == null) return null;
        String s = shop.toLowerCase().strip().replaceFirst("^www\\.", "");
        if (s.matches("[a-z0-9.-]+\\.[a-z]{2,}")) return s;
        String key = slug(shop);
        if (SHOP_DOMAINS.containsKey(key)) return SHOP_DOMAINS.get(key);
        for (var e : SHOP_DOMAINS.entrySet()) if (key.startsWith(e.getKey()) && e.getKey().length() >= 4) return e.getValue();
        return null;
    }

    /** "Nguyễn Vũ Store" -> "nguyenvustore", "phongvu.vn" -> "phongvuvn" */
    static String slug(String s) {
        String n = java.text.Normalizer.normalize(s.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd');
        return n.replaceAll("[^a-z0-9]", "");
    }

    static boolean isGoogle(String url) {
        String h = host(url);
        return h == null || h.equals("google.com") || h.endsWith(".google.com") || h.startsWith("google.");
    }

    @SuppressWarnings("unchecked")
    private static String firstOn(Map<String, Object> res, String domain) {
        return ((List<Map<String, Object>>) res.getOrDefault("organic", List.of())).stream()
                .map(i -> https(i.get("link")))
                .filter(u -> u != null && host(u) != null && (host(u).equals(domain) || host(u).endsWith("." + domain)))
                .findFirst()
                .orElse(null);
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

    static String host(String url) {
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
