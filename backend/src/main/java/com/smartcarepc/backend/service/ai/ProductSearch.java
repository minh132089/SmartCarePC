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

import com.smartcarepc.backend.service.price.Offer;
import com.smartcarepc.backend.service.price.PriceEngine;

import lombok.extern.slf4j.Slf4j;

/**
 * Tìm sản phẩm thật đang bán ở cửa hàng Việt Nam. Nguồn chính là engine so giá tự chạy (service/price: đọc thẳng web
 * cửa hàng và SearXNG, không tốn lượt dịch vụ nào). Serper.dev (Google Shopping, khoá SERPER_API_KEY) là nguồn phụ khi
 * engine tìm được ít.
 */
@Slf4j
@Component
public class ProductSearch {

    /** Một sản phẩm tìm được. Giá theo đồng, null nếu trang không ghi giá. */
    public record Product(String title, String shop, Long priceVnd, String url, String imageUrl) {
    }

    private static final int MAX = 8;
    private static final Pattern OG_IMAGE = Pattern.compile(
            "<meta[^>]+(?:property|name)=[\"']og:image[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern OG_IMAGE_REV = Pattern.compile(
            "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]*(?:property|name)=[\"']og:image[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE = Pattern.compile(
            "(?:\"price\"\\s*:\\s*\"?|product:price:amount[\"'][^>]*content=[\"'])([0-9][0-9.,]{3,})", Pattern.CASE_INSENSITIVE);

    private final RestClient serper;
    private final RestClient web;
    private final String apiKey;
    private final PriceEngine engine;

    public ProductSearch(@Value("${app.search.serper-key:}") String apiKey, PriceEngine engine) {
        this.engine = engine;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build());
        // Ba phương án và build tham khảo tìm link cùng lúc nên Serper có lúc trả chậm; 8 giây làm đứt giữa chừng
        factory.setReadTimeout(Duration.ofSeconds(15));
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
        return engine.enabled() || serperReady();
    }

    /** Serper có khoá và không bị tạm tắt (hết lượt) */
    private boolean serperReady() {
        return !apiKey.isEmpty() && java.time.Instant.now().isAfter(pausedUntil);
    }

    /** Thông tin engine đọc được về link (còn hàng, bảo hành, lúc kiểm tra), null nếu link không do engine tìm */
    public Offer offer(String url) {
        return engine.offer(url);
    }

    /** Kết quả tìm đúng tên model của từng món đã chọn (warmCompare), theo link món đó */
    private final Map<String, List<Offer>> sameModel = new ConcurrentHashMap<>();

    /**
     * Tìm trước, song song, đúng tên model của các món đã chọn ở mọi cửa hàng (engine), để compare() có giá cùng
     * sản phẩm ở cửa hàng khác. Chờ tối đa 20 giây; món tìm chưa xong thì chỉ so trong nhóm tìm kiếm cũ.
     */
    public void warmCompare(java.util.Collection<Product> picks) {
        if (!engine.enabled()) return;
        var jobs = picks.stream()
                .filter(p -> p.title() != null && p.url() != null && !sameModel.containsKey(p.url()))
                .map(p -> CompletableFuture.runAsync(() -> sameModel.put(p.url(), engine.search(shortTitle(p.title())))))
                .toArray(CompletableFuture[]::new);
        try {
            CompletableFuture.allOf(jobs).get(20, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException e) {
            log.debug("Tìm cùng model chưa xong hết: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (sameModel.size() > 2000) sameModel.clear();
    }

    /**
     * Cùng model ở cửa hàng khác (để so giá): trong nhóm tìm kiếm của món và trong lần tìm đúng tên model
     * (warmCompare), rẻ nhất trước, mỗi cửa hàng một giá, tối đa 3. Không tính chính cửa hàng của món đó.
     */
    public List<Offer> compare(Product pick, List<Product> group) {
        if (pick == null || pick.title() == null) return List.of();
        String self = PriceEngine.clean(pick.url());
        Map<String, Offer> best = new LinkedHashMap<>();
        List<Offer> pool = new ArrayList<>();
        group.forEach(p -> {
            var o = engine.offer(p.url());
            pool.add(o != null ? o : new Offer(p.title(), p.shop(), p.priceVnd(), null, p.url(), p.imageUrl(), null, null, null));
        });
        pool.addAll(sameModel.getOrDefault(pick.url(), List.of()));
        pool.stream()
                .filter(o -> o.title() != null && o.priceVnd() != null && !PriceEngine.clean(o.url()).equals(self))
                .filter(o -> PriceEngine.sameModel(pick.title(), o.title()))
                .filter(o -> o.shop() != null && !o.shop().equalsIgnoreCase(pick.shop()))
                .sorted(java.util.Comparator.comparingLong(Offer::priceVnd))
                .forEach(o -> best.putIfAbsent(o.shop(), o));
        return best.values().stream().limit(3).toList();
    }

    /** Serper hết lượt / khoá bị từ chối: tạm coi như tắt tìm kiếm, để trợ lý trả lời bằng giá ước tính thay vì gọi mãi */
    private volatile java.time.Instant pausedUntil = java.time.Instant.EPOCH;
    static final Duration PAUSE = Duration.ofMinutes(10);

    /** Lỗi do tài khoản (hết lượt, khoá sai), thử lại cũng vô ích */
    static boolean accountProblem(int status, String body) {
        return status == 401 || status == 402 || status == 403 || body != null && body.toLowerCase().contains("credits");
    }

    /** Kết quả đã tìm trong 30 phút gần đây, theo từ khoá: câu hỏi lặp lại trả lời nhanh và không tốn lượt Serper */
    private static final long CACHE_MS = 30 * 60 * 1000;
    private record Cached(long at, List<Product> products) {
    }
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    /** Tìm tối đa 6 sản phẩm, đã bỏ trùng link, ưu tiên món có cả giá và ảnh */
    public List<Product> search(String query) {
        if (!enabled()) throw new IllegalStateException("Chưa bật tìm kiếm sản phẩm");
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
        List<Product> out = new ArrayList<>();
        if (engine.enabled()) {
            engine.search(query).forEach(o -> out.add(new Product(o.title(), o.shop(), o.priceVnd(), o.url(), o.imageUrl())));
        }
        // Engine tìm được ít (món hiếm, cửa hàng đổi giao diện): thêm kết quả Google Shopping nếu Serper còn lượt
        if (out.size() < 3 && serperReady()) {
            out.addAll(shopping(query));
            if (out.size() < 3) out.addAll(organic(query + " giá"));
        }
        Map<String, Product> unique = new LinkedHashMap<>();
        out.stream().filter(p -> p.url() != null).forEach(p -> unique.putIfAbsent(p.url(), p));
        // Kết quả engine đã xếp (khớp model, còn hàng, rẻ) nên giữ nguyên thứ tự; chỉ đẩy món thiếu giá xuống cuối
        return unique.values().stream()
                .sorted((a, b) -> Integer.compare(score(b) >= 2 ? 1 : 0, score(a) >= 2 ? 1 : 0))
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
            Map.entry("tinhocanhphat", "tinhocanhphat.vn"),
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
    /** Link cửa hàng đã đổi, theo tên + cửa hàng: build 2 / 3 và các phương án hay trùng sản phẩm, khỏi tìm lại */
    private final Map<String, String> linkCache = new ConcurrentHashMap<>();

    public String directLink(String title, String shop, String link) {
        if (link != null && !isGoogle(link)) return link;
        if (!enabled() || title == null) return null;
        String key = title + "|" + shop;
        String hit = linkCache.get(key);
        if (hit != null) return hit;
        String found = resolve(title, shop);
        if (found != null) {
            if (linkCache.size() > 2000) linkCache.clear();
            linkCache.put(key, found);
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private String resolve(String title, String shop) {
        String domain = domainOf(shop);
        if (domain != null) {
            String hit = firstOn(post("/search", title + " site:" + domain), domain);
            // Tên trong Shopping hay dài, kèm mã và quà tặng nên trang cửa hàng không khớp: thử lại với vài chữ đầu
            String brief = shortTitle(title);
            if (hit == null && !brief.equals(title)) hit = firstOn(post("/search", brief + " site:" + domain), domain);
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

    /** "CPU Intel Core i5-12400F (Up to 4.4GHz, 6 nhân 12 luồng) - Chính hãng" -> "CPU Intel Core i5-12400F" (tối đa 8 chữ) */
    static String shortTitle(String title) {
        String t = title.replaceAll("[(\\[|].*$", "").replaceAll("\\s+-\\s+.*$", "").strip();
        if (t.isEmpty()) return title;
        String[] w = t.split("\\s+");
        return w.length <= 8 ? t : String.join(" ", java.util.Arrays.copyOf(w, 8));
    }

    /** Link chỉ là trang chủ cửa hàng (tìm không ra trang sản phẩm): người dùng còn phải tự tìm lại */
    public static boolean homePage(String url) {
        if (url == null) return false;
        try {
            String path = java.net.URI.create(url).getPath();
            return path == null || path.isEmpty() || path.equals("/");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Cửa hàng biết tên miền: đổi sang link trang cửa hàng gần như chắc chắn được */
    public static boolean knownShop(String shop) {
        return domainOf(shop) != null;
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

    /** Serper cho tối đa 5 lượt mỗi giây; giữ 4 cho chắc. Mốc thời gian các lượt gần đây, dùng chung mọi luồng */
    private static final int PER_SECOND = 4;
    private final java.util.ArrayDeque<Long> recent = new java.util.ArrayDeque<>();

    /** Chờ tới lượt: trong 1 giây gần nhất chưa quá PER_SECOND lượt (ba phương án và build tham khảo tìm link song song) */
    private void throttle() {
        while (true) {
            long wait;
            synchronized (recent) {
                long now = System.currentTimeMillis();
                while (!recent.isEmpty() && now - recent.peekFirst() >= 1000) recent.pollFirst();
                if (recent.size() < PER_SECOND) {
                    recent.addLast(now);
                    return;
                }
                wait = 1000 - (now - recent.peekFirst()) + 5;
            }
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, String query) {
        for (int attempt = 0; attempt < 3; attempt++) {
            throttle();
            try {
                Map<String, Object> res = serper.post().uri(path)
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("q", query, "gl", "vn", "hl", "vi", "num", 10))
                        .retrieve()
                        .body(Map.class);
                return res == null ? Map.of() : res;
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                log.info("Serper {} quá giới hạn, thử lại", path); // vòng sau throttle() tự chờ
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                if (accountProblem(e.getStatusCode().value(), e.getResponseBodyAsString())) {
                    if (java.time.Instant.now().isAfter(pausedUntil)) {
                        log.warn("Serper từ chối ({}): {}. Tạm tắt tìm sản phẩm {} phút, nạp thêm lượt hoặc đổi SERPER_API_KEY",
                                e.getStatusCode().value(), e.getResponseBodyAsString(), PAUSE.toMinutes());
                    }
                    pausedUntil = java.time.Instant.now().plus(PAUSE);
                } else {
                    log.warn("Serper {} lỗi: {}", path, e.getMessage());
                }
                return Map.of();
            } catch (RestClientException e) {
                // Mạng chập chờn / trả chậm bị cắt: thử lại; lỗi khác (khoá sai, 4xx) thì thôi
                boolean transientError = e instanceof org.springframework.web.client.ResourceAccessException
                        || e.getMessage() != null && e.getMessage().startsWith("Error while extracting response");
                log.warn("Serper {} lỗi{}: {}", path, transientError ? ", thử lại" : "", e.getMessage());
                if (!transientError) return Map.of();
            }
        }
        return Map.of();
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
