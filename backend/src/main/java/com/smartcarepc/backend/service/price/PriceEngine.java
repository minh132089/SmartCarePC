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

    /**
     * Xác minh link trước khi đưa cho người dùng: phải là trang một sản phẩm, tên trên trang khớp sản phẩm mong đợi
     * (cùng mã model, cùng dung lượng: SSD 1TB khác 4TB). Trả về thông tin đọc từ trang (giá trên trang là giá thật),
     * null nếu không xác minh được. Kết quả được nhớ theo link, thẻ cấu hình dùng giá này thay giá Google Shopping.
     */
    public Offer readPage(String url, String expectTitle) {
        if (url == null || !Shops.productLike(url) || rejected.contains(url) || rejected.contains(url + "|" + expectTitle)) return null;
        // Trang máy nguyên bộ không phải trang của một linh kiện (trừ khi đang tìm đúng một bộ)
        if (BUNDLE_PATH.matcher(url.toLowerCase().replaceFirst("[?#].*$", "")).find() && !BUNDLE.matcher(expectTitle.toLowerCase()).find()) {
            return null;
        }
        var known = byUrl.get(url);
        if (known != null) {
            return samePage(expectTitle, known.title()) && !Boolean.FALSE.equals(known.inStock()) && !usedGoods(known.title(), url)
                    ? known : null;
        }
        var fetched = fetcher.page(url, Duration.ofSeconds(8));
        if (fetched == null) return null; // tải không được (chậm, robots.txt): lần sau thử lại
        // Sản phẩm ngừng bán hay bị web chuyển sang trang danh mục / trang chủ: không phải trang sản phẩm nữa
        Offer page = Shops.productLike(fetched.finalUrl()) ? ProductPage.parseLoose(fetched.html(), url, Instant.now()) : null;
        // Trang báo hết hàng, hoặc là hàng cũ / 2nd / thanh lý (tên Google Shopping có khi không ghi): không nhận.
        // Nhớ các trang bị loại để không tải lại (cùng sản phẩm hay xuất hiện ở nhiều option)
        if (page == null || Boolean.FALSE.equals(page.inStock()) || usedGoods(page.title(), fetched.finalUrl())
                || BUNDLE_PATH.matcher(fetched.finalUrl().toLowerCase()).find() && !BUNDLE.matcher(expectTitle.toLowerCase()).find()) {
            remember(url);
            return null;
        }
        if (!samePage(expectTitle, page.title())) {
            byUrl.put(url, page);
            remember(url + "|" + expectTitle);
            return null;
        }
        if (byUrl.size() > 5000) byUrl.clear();
        byUrl.put(url, page);
        return page;
    }

    /** Đường dẫn của trang hàng cũ: "/2nd-nguon-...", "/like-new-...", "/da-qua-su-dung" */
    private static final Pattern USED_PATH = Pattern.compile("(?i)[/-](2nd|like-?new|qua-su-dung|thanh-ly|trung-bay|hang-cu|second-?hand)([/-]|$)");

    /** Hàng cũ / 2nd / thanh lý / trưng bày, theo tên trên trang hoặc đường dẫn */
    static boolean usedGoods(String title, String url) {
        return title != null && JUNK.matcher(title).find() || url != null && USED_PATH.matcher(url.replaceFirst("[?#].*$", "")).find();
    }

    /** Link (hoặc link|tên mong đợi) đã bị loại khi xác minh, giữ tới khi quá nhiều thì xoá */
    private final Set<String> rejected = ConcurrentHashMap.newKeySet();

    private void remember(String key) {
        if (rejected.size() > 5000) rejected.clear();
        rejected.add(key);
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
        // Ô tìm kiếm của cửa hàng tìm theo mọi chữ: bỏ chữ thừa ("giá rẻ", số đo) để không ra rỗng
        String shopQuery = shopQuery(query);
        var shops = Arrays.stream(ShopSearch.values())
                .map(s -> CompletableFuture.supplyAsync(() -> {
                    // An Phát yêu cầu cách 5 giây mỗi lần: chờ lâu hơn thì bỏ lần này, các cửa hàng khác vẫn có
                    String html = fetcher.get(s.url(shopQuery), s.maxWait);
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
