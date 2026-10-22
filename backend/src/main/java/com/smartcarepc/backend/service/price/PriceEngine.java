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
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Engine so giá: tìm cùng lúc ở trang tìm kiếm của cửa hàng (ShopSearch) và qua SearXNG, đọc trang sản phẩm để biết
 * giá mới nhất, còn hàng, bảo hành (ProductPage), rồi khớp đúng model với từ khoá, bỏ hàng cũ / sai loại và xếp:
 * khớp nhất, còn hàng, rẻ nhất. Mọi lần gọi web đi qua PoliteFetcher (robots.txt, giãn cách, tự giới thiệu đúng tên).
 */
@Slf4j
@Component
public class PriceEngine {

    /**
     * Kết quả giữ 2 giờ: câu hỏi lặp lại trả lời ngay, không gọi lại cửa hàng và máy tìm kiếm (gọi dồn dập là bị chặn).
     * Thẻ cấu hình ghi rõ giờ kiểm tra giá.
     */
    static final Duration CACHE = Duration.ofHours(2);
    /** Chờ tối đa cho một từ khoá; nguồn nào chậm hơn thì bỏ, dùng các nguồn đã xong */
    static final Duration DEADLINE = Duration.ofSeconds(8);
    static final int MAX = 8;
    /** Số trang sản phẩm đọc thêm mỗi từ khoá (lấy còn hàng, bảo hành) */
    static final int PAGES = 3;

    private static final Pattern JUNK = Pattern.compile(
            "like ?new|\\bcũ\\b|đã qua sử dụng|\\b2nd\\b|trưng bày|thanh lý|\\btray\\b|bóc máy",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
    /** Tên bắt đầu bằng các loại này mà từ khoá không nhắc tới thì là hàng khác loại (laptop, PC lắp sẵn, màn hình) */
    private static final List<String> OTHER_KINDS = List.of("laptop", "pc ", "may tinh", "bo may", "bo pc", "combo", "man hinh", "may bo",
            "macbook", "imac", "mac mini", "mac studio");
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
            // "Tản ID-Cooling IS-40X": tên bắt đầu bằng "Tản" cũng là tản nhiệt
            Map.entry("cooler", Pattern.compile("\\btan nhiet\\b|\\btan nuoc\\b|\\btan khi\\b|\\bcooler\\b|\\baio\\b|^tan\\b")),
            Map.entry("case", Pattern.compile("^vo\\b|\\bvo may tinh\\b|\\bvo case\\b|^case\\b")),
            Map.entry("mainboard", Pattern.compile("\\bbo mach chu\\b|\\bmainboard\\b|^main\\b")),
            Map.entry("gpu", Pattern.compile("\\bcard man hinh\\b|\\bcard do hoa\\b|\\bvga\\b")),
            Map.entry("psu", Pattern.compile("\\bnguon\\b|\\bpsu\\b")),
            Map.entry("ssd", Pattern.compile("\\bo cung\\b|\\bssd\\b")),
            // "bộ nhớ cache / đệm" trong tên CPU không phải RAM
            Map.entry("ram", Pattern.compile("^ram\\b|\\bbo nho\\b(?! (cache|dem|l[23]))|^ram ")),
            Map.entry("cpu", Pattern.compile("\\bcpu\\b|\\bvi xu ly\\b|\\bbo xu ly\\b|\\bprocessor\\b")),
            // Quạt case: "Bộ 3 quạt ...", "Fan case ...". Tản / vỏ / nguồn có chữ "quạt" thì chữ loại kia đứng trước nên vẫn đúng loại
            Map.entry("fans", Pattern.compile("\\bquat\\b|\\bfan\\b")));
    /** Giá cao nhất hợp lý của một linh kiện lẻ: cao hơn là trang ghi / đọc nhầm đơn vị (725.000đ thành 725.000.000đ) */
    static final long MAX_PRICE = 150_000_000L;

    /** Hậu tố làm card / CPU thành sản phẩm khác hẳn (đắt hơn): trang có mà tên mong đợi không có thì là trang khác */
    private static final Set<String> TIER_VARIANTS = Set.of("ti", "super", "xt", "xtx", "gre");
        /** Chữ phân biệt phiên bản: phải trùng nhau mới là cùng model (RTX 4060 khác 4060 Ti, B760M khác B760M WiFi) */
    private static final Set<String> VARIANTS = Set.of("ti", "super", "xt", "xtx", "gre", "oc", "pro", "plus", "max", "wifi", "ii", "v2", "v3");
    /** Chữ chung chung, không giúp phân biệt model */
    private static final Set<String> FILLER = Set.of("card", "man", "hinh", "vga", "do", "hoa", "chinh", "hang", "may", "tinh",
            "bo", "nho", "trong", "o", "cung", "nguon", "vo", "case", "tan", "nhiet", "mainboard", "mach", "chu", "cpu",
            "xu", "ly", "vi", "ram", "ssd", "psu", "edition", "phien", "ban", "gia", "re", "new", "moi", "full", "box");

    private final PoliteFetcher fetcher;
    private final Searxng searxng;
    private final boolean enabled;
    /** Luồng ảo: mỗi lượt gọi web một luồng, không xếp hàng chờ nhau (xem service/Io) */
    private final ExecutorService pool = com.smartcarepc.backend.service.Io.POOL;
    private record Cached(Instant at, List<Offer> offers, boolean incomplete) { }
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    /** Mọi offer đã thấy theo link, để thẻ cấu hình hiện được còn hàng, bảo hành, lúc kiểm tra */
    private final Map<String, Offer> byUrl = new ConcurrentHashMap<>();

    public PriceEngine(PoliteFetcher fetcher, Searxng searxng, @Value("${app.search.engine:true}") boolean enabled) {
        this.fetcher = fetcher;
        this.searxng = searxng;
        this.enabled = enabled;
    }

    /**
     * File lưu bộ nhớ đệm giá (kết quả tìm + giá / còn hàng đã đọc trên trang) để khởi động lại backend không phải tìm lại từ đầu:
     * mọi lượt tìm đều đi từ IP máy chủ, tìm lại cả nghìn từ khoá sau mỗi lần khởi động làm máy tìm kiếm / cửa hàng chặn.
     * Để trống thì không lưu (test tự tạo PriceEngine).
     */
    @Value("${app.price.cache-file:data/price-cache.json}")
    private String cacheFile;
    private final java.util.concurrent.ScheduledExecutorService saver = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "price-cache-saver");
        t.setDaemon(true);
        return t;
    });

    private record Snapshot(Map<String, Cached> searches, Map<String, Offer> offers, Map<String, Long> pageRead) {
    }

    private static final tools.jackson.databind.json.JsonMapper JSON = tools.jackson.databind.json.JsonMapper.builder().build();

    @jakarta.annotation.PostConstruct
    void loadCache() {
        if (cacheFile == null || cacheFile.isBlank()) return;
        var file = java.nio.file.Path.of(cacheFile);
        if (java.nio.file.Files.exists(file)) {
            try {
                var snap = JSON.readValue(file.toFile(), Snapshot.class);
                Instant oldest = Instant.now().minus(CACHE);
                if (snap.searches() != null) snap.searches().forEach((k, v) -> { if (v.at().isAfter(oldest) && !v.offers().isEmpty()) cache.put(k, v); });
                long verifiedSince = System.currentTimeMillis() - VERIFIED_TTL.toMillis();
                if (snap.pageRead() != null) snap.pageRead().forEach((u, at) -> { if (at > verifiedSince) pageRead.put(u, at); });
                if (snap.offers() != null) byUrl.putAll(snap.offers());
                log.info("Nạp bộ nhớ đệm giá: {} từ khoá, {} link, {} trang đã đọc", cache.size(), byUrl.size(), pageRead.size());
            } catch (RuntimeException e) {
                log.warn("Không đọc được bộ nhớ đệm giá {}: {}", file, e.getMessage());
            }
        }
        saver.scheduleWithFixedDelay(this::saveCache, 5, 5, TimeUnit.MINUTES);
    }

    /** Tắt backend: dừng lịch lưu và chờ lượt đang chạy xong, rồi lưu lần cuối (không để hai lượt ghi cùng lúc) */
    @jakarta.annotation.PreDestroy
    void saveOnShutdown() {
        saver.shutdown();
        try {
            saver.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        saveCache();
    }

    /** Ghi file tuần tự (synchronized): lịch lưu và lần lưu khi tắt không ghi đè file tạm của nhau */
    synchronized void saveCache() {
        if (cacheFile == null || cacheFile.isBlank()) return;
        try {
            prune();
            Map<String, Cached> keep = new LinkedHashMap<>();
            cache.forEach((k, v) -> { if (!v.offers().isEmpty()) keep.put(k, v); });
            var file = java.nio.file.Path.of(cacheFile);
            if (file.getParent() != null) java.nio.file.Files.createDirectories(file.getParent());
            var tmp = file.resolveSibling(file.getFileName() + ".tmp");
            JSON.writeValue(tmp.toFile(), new Snapshot(keep, new LinkedHashMap<>(byUrl), new LinkedHashMap<>(pageRead)));
            java.nio.file.Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            log.warn("Không lưu được bộ nhớ đệm giá: {}", e.getMessage());
        }
    }

    /** Bỏ kết quả hết hạn; còn quá MAX_CACHED từ khoá thì bỏ những từ khoá cũ nhất (không xoá sạch như trước) */
    private void prune() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(e -> e.getValue().at().isBefore(now.minus(e.getValue().offers().isEmpty()
                ? (e.getValue().incomplete() ? FAILED_CACHE : EMPTY_CACHE) : CACHE)));
        if (cache.size() > MAX_CACHED) {
            cache.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.comparing(Cached::at)))
                    .limit(cache.size() - MAX_CACHED * 4L / 5).map(Map.Entry::getKey).toList().forEach(cache::remove);
        }
        if (byUrl.size() > 8000) {
            byUrl.entrySet().stream().sorted(Comparator.comparing((Map.Entry<String, Offer> e) ->
                    e.getValue().checkedAt() == null ? Instant.EPOCH : e.getValue().checkedAt()))
                    .limit(byUrl.size() - 6000).map(Map.Entry::getKey).toList().forEach(byUrl::remove);
        }
    }

    static final int MAX_CACHED = 4000;

    /**
     * Khoá bộ nhớ đệm: chữ thường, bỏ dấu câu / ngoặc, gộp khoảng trắng ("RTX 4060 8GB, 2 quạt" và "rtx 4060 8gb 2 quạt" dùng
     * chung kết quả)
     */
    static String cacheKey(String query) {
        return Normalizer.normalize(query, Normalizer.Form.NFC).toLowerCase().replaceAll("[,;:()\\[\\]{}|/+\"'“”]+", " ")
                .replaceAll("\\s+", " ").strip();
    }

    public boolean enabled() {
        return enabled;
    }

    /** Offer đã thấy của link (null nếu chưa thấy) */
    public Offer offer(String url) {
        return url == null ? null : byUrl.get(url);
    }

    /**
     * Xác minh link trước khi đưa cho người dùng: phải là trang một sản phẩm, tên trên trang khớp sản phẩm mong đợi
     * (cùng mã model, cùng dung lượng: SSD 1TB khác 4TB). Trả về thông tin đọc từ trang (giá trên trang là giá thật),
     * null nếu không xác minh được. Kết quả được nhớ theo link, thẻ cấu hình dùng giá này thay giá Google Shopping.
     */
    public Offer readPage(String url, String expectTitle) {
        if (url == null || !Shops.productLike(url) || blocked(url) || blocked(url + "|" + expectTitle)) return null;
        // Trang máy nguyên bộ không phải trang của một linh kiện (trừ khi đang tìm đúng một bộ)
        if (BUNDLE_PATH.matcher(url.toLowerCase().replaceFirst("[?#].*$", "")).find() && !BUNDLE.matcher(expectTitle.toLowerCase()).find()) {
            return null;
        }
        var known = byUrl.get(url);
        // Đã đọc chính trang sản phẩm: dùng lại. Chỉ mới thấy trên trang tìm kiếm / danh mục của cửa hàng (giá, còn hàng
        // ở đó có thể cũ hoặc của bản khác): mở trang sản phẩm để xác minh, không coi kết quả danh sách là đã kiểm trang
        if (known != null && pageChecked(url)) {
            return samePage(expectTitle, known.title()) && !Boolean.FALSE.equals(known.inStock()) && !usedGoods(known.title(), url)
                    ? known : null;
        }
        var fetched = unreadable(url) ? null : fetcher.page(url, Duration.ofSeconds(8));
        if (fetched != null) {
            boolean product = Shops.productLike(fetched.finalUrl());
            Offer page = product ? ProductPage.parseLoose(fetched.html(), url, Instant.now()) : null;
            if (page == null && product) {
                // Trang sản phẩm tải được mà không đọc ra tên / giá (cửa hàng vẽ trang bằng JavaScript...): CHƯA XÁC MINH, không
                // phải bị loại. Thử lại sau UNREADABLE_TTL; trong lúc đó dùng tạm kết quả danh sách (pageChecked vẫn false)
                log.debug("readPage {}: không đọc được nội dung trang sản phẩm", url);
                unreadableUntil.put(url, System.currentTimeMillis() + UNREADABLE_TTL.toMillis());
            } else {
                String why = page == null ? "chuyển sang trang không phải sản phẩm (ngừng bán / danh mục)"
                        : Boolean.FALSE.equals(page.inStock()) ? "hết hàng"
                        : usedGoods(page.title(), fetched.finalUrl()) ? "hàng cũ / thanh lý"
                        : page.priceVnd() != null && page.priceVnd() > MAX_PRICE ? "giá đọc sai đơn vị"
                        : BUNDLE_PATH.matcher(fetched.finalUrl().toLowerCase()).find() && !BUNDLE.matcher(expectTitle.toLowerCase()).find()
                                ? "trang máy nguyên bộ" : null;
                if (why != null) {
                    // Nhớ các trang bị loại để không tải lại (cùng sản phẩm hay xuất hiện ở nhiều option), có hạn REJECT_TTL.
                    // Trang hết hàng: lưu luôn bằng chứng hết hàng (offer của trang) để kết quả danh sách cũ không che mất
                    log.debug("readPage {}: loại vì {}", url, why);
                    remember(url);
                    if (page != null && Boolean.FALSE.equals(page.inStock())) {
                        byUrl.put(url, page);
                        pageRead.put(clean(url), System.currentTimeMillis());
                    }
                    return null;
                }
                if (byUrl.size() > 8000) prune();
                if (pageRead.size() > 5000) pageRead.clear();
                pageRead.put(clean(url), System.currentTimeMillis());
                byUrl.put(url, page);
                if (!samePage(expectTitle, page.title())) {
                    log.debug("readPage {}: sai biến thể (trang \"{}\", cần \"{}\")", url, page.title(), expectTitle);
                    remember(url + "|" + expectTitle);
                    return null;
                }
                return page;
            }
        }
        // Tải không được (chậm, robots.txt) hay không đọc được trang: còn kết quả danh sách thì tạm dùng làm link mua, giá
        // và còn hàng vẫn là CHƯA kiểm trang (pageChecked false, thẻ ghi "xem lại giá"); lần sau thử lại
        return known != null && samePage(expectTitle, known.title()) && !Boolean.FALSE.equals(known.inStock())
                && !usedGoods(known.title(), url) ? known : null;
    }

    /** Trang sản phẩm tải được nhưng không đọc được nội dung: thử lại sau từng này */
    static final Duration UNREADABLE_TTL = Duration.ofMinutes(10);
    /** Trang bị loại (hết hàng, ngừng bán, sai biến thể, hàng cũ): không tải lại trong từng này */
    static final Duration REJECT_TTL = Duration.ofMinutes(30);
    /** Giá / tồn kho đọc trên trang sản phẩm chỉ coi là còn đúng trong từng này */
    static final Duration VERIFIED_TTL = Duration.ofHours(2);
    private final Map<String, Long> unreadableUntil = new ConcurrentHashMap<>();

    private boolean unreadable(String url) {
        Long until = unreadableUntil.get(url);
        return until != null && until > System.currentTimeMillis();
    }

    /** Link đã đọc thẳng trang sản phẩm (không chỉ thấy trên trang tìm kiếm / danh mục), theo link đã làm gọn -> lúc đọc */
    private final Map<String, Long> pageRead = new ConcurrentHashMap<>();

    /** Giá và tình trạng hàng của link được đọc trên chính trang sản phẩm, trong VERIFIED_TTL gần đây */
    public boolean pageChecked(String url) {
        Long at = url == null ? null : pageRead.get(clean(url));
        return at != null && System.currentTimeMillis() - at < VERIFIED_TTL.toMillis();
    }

    /** HTML một trang qua PoliteFetcher (UA trung thực, tôn trọng robots.txt, giãn cách theo cửa hàng); null nếu không tải được */
    public String html(String url, Duration maxWait) {
        return fetcher.get(url, maxWait);
    }

    /** Đường dẫn của trang hàng cũ: "/2nd-nguon-...", "/like-new-...", "/da-qua-su-dung" */
    private static final Pattern USED_PATH = Pattern.compile("(?i)[/-](2nd|like-?new|qua-su-dung|thanh-ly|trung-bay|hang-cu|second-?hand)([/-]|$)");

    /** Hàng cũ / 2nd / thanh lý / trưng bày, theo tên trên trang hoặc đường dẫn */
    static boolean usedGoods(String title, String url) {
        return title != null && JUNK.matcher(title).find() || url != null && USED_PATH.matcher(url.replaceFirst("[?#].*$", "")).find();
    }

    /** Link (hoặc link|tên mong đợi) đã bị loại khi xác minh, giữ tới khi quá nhiều thì xoá */
    private final Map<String, Long> rejected = new ConcurrentHashMap<>();

    private void remember(String key) {
        if (rejected.size() > 5000) rejected.clear();
        rejected.put(key, System.currentTimeMillis() + REJECT_TTL.toMillis());
    }

    private boolean blocked(String key) {
        Long until = rejected.get(key);
        return until != null && until > System.currentTimeMillis();
    }

    private static final Pattern CAPACITY = Pattern.compile("(\\d+)\\s?(gb|tb)\\b");

    /**
     * Tên trên trang có đúng là sản phẩm mong đợi: cùng loại linh kiện, cùng dung lượng lớn nhất (GB / TB), và có phần
     * lớn mã model của tên mong đợi (vd SNV2S, 1000G, 12400F). Tên mong đợi không có mã model thì cần trùng nửa số chữ.
     */
    public static boolean samePage(String expect, String page) {
        if (expect == null || page == null) return false;
        String e = norm(expect), p = norm(page), squashed = p.replace(" ", "");
        // Trang bán cả bộ (bộ PC, combo, "CPU + mainboard") không phải trang của một linh kiện
        if (BUNDLE.matcher(page.toLowerCase()).find() && !BUNDLE.matcher(expect.toLowerCase()).find()) return false;
        String ek = kind(e), pk = kind(p);
        if (ek != null && pk != null && !ek.equals(pk)) return false;
        Long ec = maxCapacity(e), pc = maxCapacity(p);
        if (ec != null && pc != null && !ec.equals(pc)) return false;
        // Phiên bản khác của cùng mã: trang "RTX 4060 Ti" không phải trang của "RTX 4060", "i3-12100F" không phải "i3-12100"
        Set<String> et = new HashSet<>(Arrays.asList(e.split(" "))), pt = new HashSet<>(Arrays.asList(p.split(" ")));
        for (String v : TIER_VARIANTS) if (pt.contains(v) && !et.contains(v)) return false;
        List<String> expectCodes = Arrays.stream(e.split(" ")).filter(w -> w.matches(".*\\d.*")).toList();
        if (noIgpuVariant(expectCodes, p)) return false;
        // Mã có số (h51e, 16gb, ddr4, 3200mhz, snv2s, 12400f) nói lên đúng sản phẩm; chữ mô tả ("vỏ máy cao cấp kính")
        // mỗi cửa hàng viết một kiểu nên không dùng để so
        List<String> codes = Arrays.stream(e.split(" ")).filter(w -> w.length() >= 2 && w.matches(".*\\d.*")).distinct().toList();
        List<String> models = codes.stream().filter(PriceEngine::isModel).toList();
        if (!models.isEmpty() && models.stream().filter(squashed::contains).count() * 10 < models.size() * 6) return false;
        if (!codes.isEmpty()) return codes.stream().filter(squashed::contains).count() * 2 >= codes.size();
        // Không có mã: cần trùng chữ đầu (thường là hãng) và một nửa số chữ còn lại
        List<String> words = Arrays.stream(e.split(" ")).filter(w -> w.length() > 1 && !STOP.contains(w)).distinct().toList();
        Set<String> pw = new HashSet<>(Arrays.asList(p.split(" ")));
        return !words.isEmpty() && words.stream().filter(pw::contains).count() * 2 >= words.size();
    }

    /** Dấu hiệu trang bán cả bộ: "Bộ PC ...", "Combo ...", "PC Gaming ...", "CPU ... + Mainboard ..." */
    private static final Pattern BUNDLE = Pattern.compile(
            "(?iuU)\\bbộ\\s+pc\\b|\\bcombo\\b|\\bpc\\s+gaming\\b|\\bbộ\\s+máy\\b|\\+\\s*(mainboard|main|bo mạch|ram|vga|card|cpu|ssd|nguồn)\\b"
            // Máy nguyên bộ: "PC Văn Phòng i3 12100", "Máy tính bộ ...", tên bắt đầu bằng "PC"
            + "|^\\s*pc\\b|\\bpc\\s+(văn\\s+phòng|đồ\\s+họa|học\\s+tập|giá\\s+rẻ|streaming|livestream)\\b|\\bmáy\\s+(tính\\s+)?bộ\\b");
    /** Link trang máy nguyên bộ (vd maytinhtsc.com/pc-van-phong-i3-12100): tên Google Shopping có khi ghi như linh kiện lẻ */
    private static final Pattern BUNDLE_PATH = Pattern.compile(
            "/(bo-)?pc-|/may-tinh-bo|/may-bo-|/combo-|/bo-may-tinh");

    /** Dung lượng lớn nhất ghi trong tên, theo GB (1TB = 1000GB); null nếu không ghi */
    static Long maxCapacity(String normalized) {
        var m = CAPACITY.matcher(normalized);
        Long max = null;
        while (m.find()) {
            long v = Long.parseLong(m.group(1)) * (m.group(2).equals("tb") ? 1000 : 1);
            if (max == null || v > max) max = v;
        }
        return max;
    }

    /** Sản phẩm đang bán khớp từ khoá, tốt nhất trước (tối đa MAX) */
    public List<Offer> search(String query) {
        // "RTX 3050 hoặc GTX 1660": tìm từng model rồi gộp, vì không sản phẩm nào có cả hai mã
        var parts = alternatives(query);
        if (parts.size() > 1) {
            Map<String, Offer> merged = new LinkedHashMap<>();
            parts.stream().map(part -> CompletableFuture.supplyAsync(() -> search(part), pool)).toList()
                    .forEach(f -> join(f).forEach(o -> merged.putIfAbsent(clean(o.url()), o)));
            return merged.values().stream()
                    .sorted(Comparator.comparingInt((Offer o) -> o.inStock() == null ? 1 : o.inStock() ? 0 : 2)
                            .thenComparingLong(Offer::priceVnd))
                    .limit(MAX).toList();
        }
        String key = cacheKey(query);
        var hit = cache.get(key);
        if (hit != null && hit.at().isAfter(Instant.now().minus(hit.offers().isEmpty() ? (hit.incomplete() ? FAILED_CACHE : EMPTY_CACHE) : CACHE))) {
            return hit.offers();
        }
        // Cùng từ khoá đang được tìm (ba phương án chốt song song hay tìm trùng, vd "tản nhiệt khí CPU" 19 lần trong một câu):
        // chờ chung một lượt, không bắn thêm request vào cửa hàng (ca thật v10: quá tải, 272/401 lượt hết giờ, trả rỗng)
        var mine = new CompletableFuture<List<Offer>>();
        var running = inFlight.putIfAbsent(key, mine);
        if (running != null) {
            try {
                // Lượt đang chạy có thể phải chờ tới lượt (SLOT_WAIT) rồi mới tìm (tới vài lần DEADLINE)
                return running.get(SLOT_WAIT.toMillis() + DEADLINE.toMillis() * 3, TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException | java.util.concurrent.ExecutionException e) {
                return List.of();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return List.of();
            }
        }
        List<Offer> found = List.of();
        boolean slot = false;
        try {
            // Tối đa SEARCH_SLOTS lượt tìm mới chạy cùng lúc: mỗi lượt mở trang tìm kiếm của mọi cửa hàng, chạy quá nhiều
            // thì hàng đợi giãn cách theo cửa hàng dồn lại, lượt nào cũng hết giờ (ca thật v13: 64/98 lượt rỗng ~8,3 giây)
            long w0 = System.nanoTime();
            slot = slots.tryAcquire(SLOT_WAIT.toMillis(), TimeUnit.MILLISECONDS);
            if (!slot) {
                log.info("So giá \"{}\": chờ lượt quá {} ms, bỏ qua", query, SLOT_WAIT.toMillis());
                return found;
            }
            long waited = (System.nanoTime() - w0) / 1_000_000;
            long t0 = System.nanoTime();
            var incomplete = new java.util.concurrent.atomic.AtomicBoolean();
            found = fresh(query, incomplete);
            if (waited > 1000) log.info("So giá \"{}\": chờ lượt {} ms", query, waited);
            log.info("So giá \"{}\": {} sản phẩm trong {} ms{}", query, found.size(), (System.nanoTime() - t0) / 1_000_000,
                    incomplete.get() ? " (có nguồn hết giờ / lỗi)" : "");
            if (cache.size() > MAX_CACHED) prune();
            // Kết quả rỗng chỉ nhớ EMPTY_CACHE (có thể là cửa hàng chậm nhất thời), có hàng nhớ CACHE. Rỗng vì nguồn hết giờ / lỗi
            // thì không nhớ: lần tìm sau thử lại thật (ca thật n18: dòng card trống mọi truy vấn trả rỗng trong cùng câu tư vấn)
            // Rỗng vì nguồn lỗi chỉ nhớ FAILED_CACHE: đủ để không gửi lại cùng từ khoá liên tục vào cửa hàng (ca thật n32: cùng một
            // từ khoá 9 lần trong vài phút, càng bị chặn), vẫn thử lại sớm
            cache.put(key, new Cached(Instant.now(), found, found.isEmpty() && incomplete.get()));
            return found;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return found;
        } finally {
            if (slot) slots.release();
            mine.complete(found);
            inFlight.remove(key, mine);
        }
    }

    /** Số lượt tìm mới chạy cùng lúc, và thời gian tối đa chờ tới lượt */
    static final int SEARCH_SLOTS = 4;
    static final Duration SLOT_WAIT = Duration.ofSeconds(25);
    private final java.util.concurrent.Semaphore slots = new java.util.concurrent.Semaphore(SEARCH_SLOTS, true);

    /** Từ khoá đang tìm -> kết quả sắp có (gộp các lượt tìm trùng chạy cùng lúc) */
    private final Map<String, CompletableFuture<List<Offer>>> inFlight = new ConcurrentHashMap<>();
    /** Kết quả rỗng chỉ nhớ ngắn: đủ để không tìm lại trong cùng một câu tư vấn, rồi thử lại */
    static final Duration EMPTY_CACHE = Duration.ofMinutes(2);
    /** Kết quả rỗng do nguồn hết giờ / lỗi: nhớ rất ngắn */
    static final Duration FAILED_CACHE = Duration.ofSeconds(30);

    private List<Offer> fresh(String query, java.util.concurrent.atomic.AtomicBoolean incomplete) {
        Instant at = Instant.now();
        // Ô tìm kiếm của cửa hàng tìm theo mọi chữ: bỏ chữ thừa ("giá rẻ", số đo) để không ra rỗng
        String shopQuery = shopQuery(query);
        var shops = Arrays.stream(ShopSearch.values())
                .map(s -> CompletableFuture.supplyAsync(() -> {
                    // An Phát yêu cầu cách 5 giây mỗi lần: chờ lâu hơn thì bỏ lần này, các cửa hàng khác vẫn có
                    String html = fetcher.get(s.url(shopQuery), s.maxWait);
                    // Tải trang tìm kiếm thất bại (hết giờ, bị chặn): không phải "không có hàng", không nhớ kết quả rỗng
                    if (html == null) incomplete.set(true);
                    return html == null ? List.<Offer>of() : s.parse(html, at);
                }, pool))
                .toList();
        var links = CompletableFuture.supplyAsync(() -> searxng.productUrls(query, 6), pool);

        Map<String, Offer> all = new LinkedHashMap<>();
        for (var f : shops) join(f, incomplete).forEach(o -> all.putIfAbsent(clean(o.url()), o));
        List<Offer> ranked = rank(query, all.values());

        // Đọc trang sản phẩm: link SearXNG chưa có trong kết quả cửa hàng, và vài món đầu để biết còn hàng / bảo hành
        List<String> pages = new ArrayList<>();
        for (String url : join(links, incomplete)) if (!all.containsKey(clean(url)) && pages.size() < PAGES) pages.add(url);
        for (var o : ranked) if (o.inStock() == null && pages.size() < PAGES * 2) pages.add(o.url());
        var read = pages.stream()
                .map(url -> CompletableFuture.supplyAsync(() -> ProductPage.parse(fetcher.get(url, Duration.ofSeconds(3)), url, Instant.now()), pool))
                .toList();
        Set<String> readNow = new HashSet<>();
        for (var f : read) {
            Offer page = joinOne(f);
            if (page == null) continue;
            all.merge(clean(page.url()), page, (shop, p) -> shop.merge(p));
            pageRead.put(clean(page.url()), System.currentTimeMillis());
            readNow.add(clean(page.url()));
        }
        if (byUrl.size() > 8000) prune();
        // Link đã đọc chính trang sản phẩm (còn hạn VERIFIED_TTL) mà lần này chỉ thấy trên trang tìm kiếm: giữ bản đọc trang
        // (giá trang, còn / hết hàng), không để kết quả danh sách ghi đè (ca thật v8: CPU GEARVN trang ghi InStock thành null)
        var result = rank(query, all.values()).stream().map(o -> {
            Offer old = byUrl.get(o.url());
            return old != null && pageChecked(o.url()) && !readNow.contains(clean(o.url())) ? old : o;
        }).toList();
        result.forEach(o -> byUrl.put(o.url(), o));
        return result;
    }

    private <T> List<T> join(CompletableFuture<List<T>> f) {
        return join(f, new java.util.concurrent.atomic.AtomicBoolean());
    }

    /** Như join, ghi lại incomplete khi nguồn hết giờ hoặc lỗi (kết quả rỗng khi đó không phải "không có hàng") */
    private <T> List<T> join(CompletableFuture<List<T>> f, java.util.concurrent.atomic.AtomicBoolean incomplete) {
        try {
            return f.get(DEADLINE.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            incomplete.set(true);
            return List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (java.util.concurrent.ExecutionException e) {
            incomplete.set(true);
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
                .filter(o -> o.priceVnd() != null && o.priceVnd() <= MAX_PRICE && o.title() != null && !JUNK.matcher(o.title()).find())
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
    public static double relevance(String query, String title) {
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
        int hitCodes = 0, models = 0, hitModels = 0;
        for (String c : codes) {
            boolean hit = squashed.contains(c);
            if (hit) hitCodes++;
            if (isModel(c)) {
                models++;
                if (hit) hitModels++;
            }
        }
        // Có mã model (3050, 12400f, b760m) thì phải trúng ít nhất một; thông số (155mm, 650w, lga1700) chỉ cộng điểm
        if (models > 0 && hitModels == 0) return 0;
        // CPU "i3 12100" khác "i3-12100F": đuôi F / KF là bản không có đồ họa tích hợp, phải mua thêm card rời
        if (noIgpuVariant(codes, t)) return 0;
        if (models == 0 && !codes.isEmpty() && hitCodes == 0 && !(qKind != null && qKind.equals(tKind))) return 0;
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

    /** Tên sản phẩm chỉ có bản đuôi F / KF của mã CPU trong từ khoá (từ khoá không ghi đuôi đó) */
    static boolean noIgpuVariant(List<String> codes, String normalizedTitle) {
        Set<String> words = new HashSet<>(Arrays.asList(normalizedTitle.split(" ")));
        return codes.stream().filter(PriceEngine::isModel).filter(c -> !words.contains(c))
                .anyMatch(c -> words.contains(c + "f") || words.contains(c + "kf"));
    }

    /** Thông số có đơn vị hoặc socket, không phải mã model */
    private static final Pattern SPEC = Pattern.compile(
            "\\d+(mm|cm|w|gb|tb|mb|mhz|ghz|hz|inch|in|v|a|rpm|k|p|bit|x|nm|l|m|mt|mts|cl\\d*)|(lga|am|ddr|pcie|gen|usb|cl|m)\\d+[a-z]*");

    /** Mã model: có số, dài từ 3 ký tự, không phải thông số (4060, 12400f, b760m, a650bn) */
    static boolean isModel(String token) {
        return token.matches(".*\\d{3,}.*") && !SPEC.matcher(token).matches();
    }

    /** "A hoặc B" / "A hay B" / "A / B": các model thay thế, mỗi cái một lần tìm (tối đa 3) */
    static List<String> alternatives(String query) {
        String[] parts = query.split("(?i)\\s+(hoặc|hay|or)\\s+");
        // "/" chỉ là "hoặc" khi mọi phía đều có số ("RTX 3050 / GTX 1660"); tên kiểu "Bộ nhớ/ RAM Kingston" thì không tách
        if (parts.length < 2) {
            String[] slash = query.split("\\s*/\\s*");
            parts = slash.length > 1 && java.util.Arrays.stream(slash).allMatch(x -> x.matches(".*\\d.*")) ? slash : parts;
        }
        if (parts.length < 2) return List.of(query);
        String head = kindPrefix(parts[0]);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < parts.length && out.size() < 3; i++) {
            String p = parts[i].strip();
            if (p.isEmpty()) continue;
            // "Card RTX 3050 hoặc GTX 1660": phần sau thiếu tên loại thì mượn của phần đầu
            out.add(i > 0 && kind(norm(p)) == null && head != null ? head + " " + p : p);
        }
        return out.size() < 2 ? List.of(query) : out;
    }

    /** Chữ chỉ loại linh kiện ở đầu từ khoá ("Card màn hình", "Tản nhiệt"), null nếu không có */
    private static String kindPrefix(String part) {
        String[] w = part.strip().split("\\s+");
        for (int n = Math.min(3, w.length); n >= 1; n--) {
            String head = String.join(" ", java.util.Arrays.copyOf(w, n));
            if (kind(norm(head)) != null) return head;
        }
        return null;
    }

    /** Cách gọi thường ngày -> chữ cửa hàng hay dùng trong tên sản phẩm (ô tìm kiếm của cửa hàng tìm đúng chữ) */
    private static final List<Map.Entry<Pattern, String>> SHOP_WORDS = List.of(
            Map.entry(Pattern.compile("(?iuU)\\btản\\s+nước\\b"), "tản nhiệt AIO"),
            Map.entry(Pattern.compile("(?iuU)\\btản\\s+khí\\b"), "tản nhiệt khí"),
            Map.entry(Pattern.compile("(?iuU)^\\s*case\\b"), "vỏ máy tính"),
            Map.entry(Pattern.compile("(?iuU)\\bvỏ\\s+case\\b"), "vỏ máy tính"),
            Map.entry(Pattern.compile("(?iuU)^\\s*main\\b"), "bo mạch chủ"),
            Map.entry(Pattern.compile("(?iuU)\\bnguồn\\b(?!\\s+máy)"), "nguồn máy tính"));

    /** Từ khoá gửi ô tìm kiếm của cửa hàng: đổi sang chữ cửa hàng dùng, bỏ chữ thừa và thông số, tối đa 5 chữ */
    static String shopQuery(String query) {
        for (var e : SHOP_WORDS) query = e.getKey().matcher(query).replaceAll(e.getValue());
        List<String> keep = new ArrayList<>();
        for (String w : query.replaceAll("(?i)giá (rẻ|tốt)|chính hãng|loại tốt|có đèn|đèn", " ").split("\\s+")) {
            String n = norm(w);
            if (n.isBlank() || STOP.contains(n)) continue;
            if (n.matches(".*\\d.*") && !isModel(n.replace(" ", "")) && !n.matches("(rtx|gtx|rx|i[3579]|ryzen|ddr[345]|\\d+gb|\\d+tb|\\d+w)")) continue;
            keep.add(w);
            if (keep.size() >= 5) break;
        }
        return keep.isEmpty() ? query : String.join(" ", keep);
    }

    /** Loại linh kiện khớp sớm nhất trong chuỗi đã bỏ dấu, null nếu không nhận ra */
    public static String kind(String normalized) {
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
    public static String norm(String s) {
        String n = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd');
        return n.replaceAll("[^a-z0-9]+", " ").strip();
    }

    /** Link bỏ phần ?... và #..., để cùng một trang không bị đếm hai lần */
    public static String clean(String url) {
        return url == null ? "" : url.replaceFirst("[?#].*$", "");
    }

}
