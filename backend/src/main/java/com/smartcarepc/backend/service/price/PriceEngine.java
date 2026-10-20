package com.smartcarepc.backend.service.price;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Engine so giá: tìm cùng lúc ở trang tìm kiếm của cửa hàng (ShopSearch) và qua SearXNG, đọc trang sản phẩm để biết
 * giá mới nhất, còn hàng, bảo hành (ProductPage), rồi khớp đúng model với từ khoá, bỏ hàng cũ / sai loại và xếp:
 * khớp nhất, còn hàng, rẻ nhất. Mọi lần gọi web đi qua PoliteFetcher (robots.txt, giãn cách, tự giới thiệu đúng tên).
 */
@Slf4j
@Component
public class PriceEngine {

    /** Kết quả giữ 30 phút: câu hỏi lặp lại trả lời ngay, không gọi lại cửa hàng */
    static final Duration CACHE = Duration.ofMinutes(30);
    /** Chờ tối đa cho một từ khoá; nguồn nào chậm hơn thì bỏ, dùng các nguồn đã xong */
    static final Duration DEADLINE = Duration.ofSeconds(12);
    static final int MAX = 8;
    /** Số trang sản phẩm đọc thêm mỗi từ khoá (lấy còn hàng, bảo hành) */
    static final int PAGES = 3;

    private static final Pattern JUNK = Pattern.compile(
            "like ?new|\\bcũ\\b|đã qua sử dụng|\\b2nd\\b|trưng bày|thanh lý|\\btray\\b|bóc máy",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
    /** Tên bắt đầu bằng các loại này mà từ khoá không nhắc tới thì là hàng khác loại (laptop, PC lắp sẵn, màn hình) */
    private static final List<String> OTHER_KINDS = List.of("laptop", "pc ", "may tinh", "bo may", "man hinh", "may bo");
    /** Hàng cho laptop (RAM SO-DIMM, SSD laptop): không lắp vào máy bàn */
    private static final Pattern LAPTOP_PART = Pattern.compile("\\b(laptop|notebook|so ?dimm)\\b");
    /** Chữ không mang nghĩa khi so từ khoá với tên */
    private static final Set<String> STOP = Set.of("gia", "re", "moi", "chinh", "hang", "loai", "tot", "cho", "va", "co", "ban",
            "phien", "edition", "new", "full", "box", "chat", "luong", "cao");
    /**
     * Loại linh kiện theo chữ đầu tiên khớp trong tên / từ khoá (đã bỏ dấu). Từ khoá và tên cùng nêu loại mà khác loại
     * là sai hàng (tìm "tản nhiệt khí" mà ra vỏ máy).
     */
    private static final List<Map.Entry<String, Pattern>> KINDS = List.of(
            Map.entry("cooler", Pattern.compile("\\btan nhiet\\b|\\bcooler\\b|\\baio\\b")),
            Map.entry("case", Pattern.compile("^vo\\b|\\bvo may tinh\\b|\\bvo case\\b|^case\\b")),
            Map.entry("mainboard", Pattern.compile("\\bbo mach chu\\b|\\bmainboard\\b|^main\\b")),
            Map.entry("gpu", Pattern.compile("\\bcard man hinh\\b|\\bcard do hoa\\b|\\bvga\\b")),
            Map.entry("psu", Pattern.compile("\\bnguon\\b|\\bpsu\\b")),
            Map.entry("ssd", Pattern.compile("\\bo cung\\b|\\bssd\\b")),
            Map.entry("ram", Pattern.compile("^ram\\b|\\bbo nho\\b|^ram ")),
            Map.entry("cpu", Pattern.compile("\\bcpu\\b|\\bvi xu ly\\b")));
    /** Chữ phân biệt phiên bản: phải trùng nhau mới là cùng model (RTX 4060 khác 4060 Ti, B760M khác B760M WiFi) */
    private static final Set<String> VARIANTS = Set.of("ti", "super", "xt", "xtx", "gre", "oc", "pro", "plus", "max", "wifi", "ii", "v2", "v3");
    /** Chữ chung chung, không giúp phân biệt model */
    private static final Set<String> FILLER = Set.of("card", "man", "hinh", "vga", "do", "hoa", "chinh", "hang", "may", "tinh",
            "bo", "nho", "trong", "o", "cung", "nguon", "vo", "case", "tan", "nhiet", "mainboard", "mach", "chu", "cpu",
            "xu", "ly", "vi", "ram", "ssd", "psu", "edition", "phien", "ban", "gia", "re", "new", "moi", "full", "box");

    private final PoliteFetcher fetcher;
    private final Searxng searxng;
    private final boolean enabled;
    private final ExecutorService pool = Executors.newFixedThreadPool(12);
    private record Cached(Instant at, List<Offer> offers) { }
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    /** Mọi offer đã thấy theo link, để thẻ cấu hình hiện được còn hàng, bảo hành, lúc kiểm tra */
    private final Map<String, Offer> byUrl = new ConcurrentHashMap<>();

    public PriceEngine(PoliteFetcher fetcher, Searxng searxng, @Value("${app.search.engine:true}") boolean enabled) {
        this.fetcher = fetcher;
        this.searxng = searxng;
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    /** Offer đã thấy của link (null nếu chưa thấy) */
    public Offer offer(String url) {
        return url == null ? null : byUrl.get(url);
    }

    /** Sản phẩm đang bán khớp từ khoá, tốt nhất trước (tối đa MAX) */
    public List<Offer> search(String query) {
        String key = query.strip().toLowerCase();
        var hit = cache.get(key);
        if (hit != null && hit.at().isAfter(Instant.now().minus(CACHE))) return hit.offers();
        long t0 = System.nanoTime();
        var found = fresh(query);
        log.info("So giá \"{}\": {} sản phẩm trong {} ms", query, found.size(), (System.nanoTime() - t0) / 1_000_000);
        if (!found.isEmpty()) {
            if (cache.size() > 500) cache.clear();
            cache.put(key, new Cached(Instant.now(), found));
        }
        return found;
    }

    private List<Offer> fresh(String query) {
        Instant at = Instant.now();
        var shops = Arrays.stream(ShopSearch.values())
                .map(s -> CompletableFuture.supplyAsync(() -> {
                    // An Phát yêu cầu cách 5 giây mỗi lần: chờ lâu hơn thì bỏ lần này, các cửa hàng khác vẫn có
                    String html = fetcher.get(s.url(query), Duration.ofSeconds(6));
                    return html == null ? List.<Offer>of() : s.parse(html, at);
                }, pool))
                .toList();
        var links = CompletableFuture.supplyAsync(() -> searxng.productUrls(query, 6), pool);

        Map<String, Offer> all = new LinkedHashMap<>();
        for (var f : shops) join(f).forEach(o -> all.putIfAbsent(clean(o.url()), o));
        List<Offer> ranked = rank(query, all.values());

        // Đọc trang sản phẩm: link SearXNG chưa có trong kết quả cửa hàng, và vài món đầu để biết còn hàng / bảo hành
        List<String> pages = new ArrayList<>();
        for (String url : join(links)) if (!all.containsKey(clean(url)) && pages.size() < PAGES) pages.add(url);
        for (var o : ranked) if (o.inStock() == null && pages.size() < PAGES * 2) pages.add(o.url());
        var read = pages.stream()
                .map(url -> CompletableFuture.supplyAsync(() -> ProductPage.parse(fetcher.get(url, Duration.ofSeconds(3)), url, Instant.now()), pool))
                .toList();
        for (var f : read) {
            Offer page = joinOne(f);
            if (page == null) continue;
            all.merge(clean(page.url()), page, (shop, p) -> shop.merge(p));
        }
        var result = rank(query, all.values());
        result.forEach(o -> byUrl.put(o.url(), o));
        if (byUrl.size() > 5000) byUrl.clear();
        return result;
    }

    private <T> List<T> join(CompletableFuture<List<T>> f) {
        try {
            return f.get(DEADLINE.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (java.util.concurrent.ExecutionException e) {
            log.debug("Nguồn giá lỗi: {}", e.getCause().getMessage());
            return List.of();
        }
    }

    private Offer joinOne(CompletableFuture<Offer> f) {
        try {
            return f.get(DEADLINE.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Lọc và xếp: bỏ hàng cũ, sai loại, không khớp model; khớp nhất trước, rồi còn hàng, rồi rẻ hơn. Còn đủ hàng
     * còn bán thì bỏ hẳn món hết hàng.
     */
    static List<Offer> rank(String query, java.util.Collection<Offer> offers) {
        var ok = offers.stream()
                .filter(o -> o.priceVnd() != null && o.title() != null && !JUNK.matcher(o.title()).find())
                .filter(o -> relevance(query, o.title()) > 0)
                .sorted(Comparator.comparingDouble((Offer o) -> -relevance(query, o.title()))
                        .thenComparingInt(o -> o.inStock() == null ? 1 : o.inStock() ? 0 : 2)
                        .thenComparingLong(Offer::priceVnd))
                .toList();
        long available = ok.stream().filter(o -> !Boolean.FALSE.equals(o.inStock())).count();
        return ok.stream().filter(o -> available < 3 || !Boolean.FALSE.equals(o.inStock())).limit(MAX).toList();
    }

    /**
     * Mức khớp 0..1 giữa từ khoá và tên sản phẩm; 0 là loại. Chữ có số (4060, i5, 12400f, b760m, 16gb, 650w) là mã
     * model: thiếu mã dài (từ 3 chữ số) là loại, thiếu quá nửa mã còn lại cũng loại. Chữ thường chỉ cộng điểm.
     */
    static double relevance(String query, String title) {
        String t = norm(title), squashed = t.replace(" ", "");
        String q = norm(query);
        for (String kind : OTHER_KINDS) if (t.startsWith(kind) && !q.contains(kind.strip())) return 0;
        if (LAPTOP_PART.matcher(t).find() && !LAPTOP_PART.matcher(q).find()) return 0;
        String qKind = kind(q), tKind = kind(t);
        if (qKind != null && tKind != null && !qKind.equals(tKind)) return 0;
        List<String> codes = new ArrayList<>(), words = new ArrayList<>();
        for (String w : q.split(" ")) {
            if (w.isBlank() || STOP.contains(w)) continue;
            (w.matches(".*\\d.*") ? codes : words).add(w);
        }
        int hitCodes = 0;
        for (String c : codes) {
            if (squashed.contains(c)) hitCodes++;
            else if (c.matches(".*\\d{3,}.*")) return 0;
        }
        if (!codes.isEmpty() && hitCodes * 2 < codes.size()) return 0;
        Set<String> titleWords = new HashSet<>(Arrays.asList(t.split(" ")));
        long hitWords = words.stream().filter(titleWords::contains).count();
        double wordScore = words.isEmpty() ? 1 : (double) hitWords / words.size();
        if (codes.isEmpty()) {
            // Từ khoá chung chung ("tản nhiệt khí CPU"): đúng loại là được, khớp nhiều chữ thì xếp trước
            if (qKind != null && qKind.equals(tKind)) return 0.5 + 0.5 * wordScore;
            return wordScore >= 0.5 ? wordScore : 0;
        }
        return 0.7 * hitCodes / codes.size() + 0.3 * wordScore;
    }

    /** Loại linh kiện khớp sớm nhất trong chuỗi đã bỏ dấu, null nếu không nhận ra */
    static String kind(String normalized) {
        String best = null;
        int at = Integer.MAX_VALUE;
        for (var k : KINDS) {
            var m = k.getValue().matcher(normalized);
            if (m.find() && m.start() < at) {
                at = m.start();
                best = k.getKey();
            }
        }
        return best;
    }

    /**
     * Cùng một model ở cửa hàng khác (để so giá): cùng mã model và phần lớn chữ trùng nhau. "MSI RTX 4060 Ventus 2X 8G
     * OC" ở GEARVN và "Card MSI GeForce RTX 4060 VENTUS 2X 8G OC" ở An Phát là một; bản Gaming X thì không.
     */
    public static boolean sameModel(String a, String b) {
        Set<String> x = tokens(a), y = tokens(b);
        Set<String> cx = x.stream().filter(w -> w.matches(".*\\d.*")).collect(Collectors.toSet());
        Set<String> cy = y.stream().filter(w -> w.matches(".*\\d.*")).collect(Collectors.toSet());
        if (cx.isEmpty() || !cx.equals(cy)) return false;
        Set<String> vx = new HashSet<>(x), vy = new HashSet<>(y);
        vx.retainAll(VARIANTS);
        vy.retainAll(VARIANTS);
        if (!vx.equals(vy)) return false;
        Set<String> inter = new HashSet<>(x);
        inter.retainAll(y);
        Set<String> union = new HashSet<>(x);
        union.addAll(y);
        return (double) inter.size() / union.size() >= 0.6;
    }

    private static Set<String> tokens(String s) {
        return Arrays.stream(norm(s).split(" "))
                .filter(w -> !w.isBlank() && !FILLER.contains(w) && !w.equals("geforce") && !w.equals("radeon") && !w.matches("gddr\\d"))
                .collect(Collectors.toSet());
    }

    /** Bỏ dấu, viết thường, chỉ giữ chữ và số */
    static String norm(String s) {
        String n = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd');
        return n.replaceAll("[^a-z0-9]+", " ").strip();
    }

    /** Link bỏ phần ?... và #..., để cùng một trang không bị đếm hai lần */
    public static String clean(String url) {
        return url == null ? "" : url.replaceFirst("[?#].*$", "");
    }

    @PreDestroy
    void stop() {
        pool.shutdownNow();
    }
}
