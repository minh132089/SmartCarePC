package com.smartcarepc.backend.service.price;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * SearXNG chạy trên máy (Docker, xem searxng/ ở thư mục gốc): tìm trang sản phẩm ở các cửa hàng trong Shops. Chỉ
 * trả về link; giá đọc sau từ chính trang sản phẩm. SearXNG tắt thì trả rỗng, engine vẫn chạy với các cửa hàng còn lại.
 */
@Slf4j
@Component
class Searxng {

    /** Trang danh mục, bài viết, tìm kiếm: không phải trang một sản phẩm */
    private static final Pattern NOT_PRODUCT = Pattern.compile(
            "/(collections|blogs?|tin-tuc|tin-cong-nghe|news|c|danh-muc|category|tag|tim|search|pages|huong-dan|review)(/|$|\\?)"
                    + "|[?&](q|s|query|page)=",
            Pattern.CASE_INSENSITIVE);

    /** Mọi máy tìm kiếm đều chặn (tìm quá dồn dập): nghỉ một lúc thay vì gọi tiếp */
    static final Duration PAUSE = Duration.ofMinutes(5);
    private volatile java.time.Instant pausedUntil = java.time.Instant.EPOCH;
    private final String base;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final JsonMapper json = JsonMapper.builder().build();

    Searxng(@Value("${app.search.searxng-url:http://localhost:8888}") String base) {
        this.base = base == null ? "" : base.replaceAll("/+$", "");
    }

    /** Link trang sản phẩm ở cửa hàng đã biết cho từ khoá, nhiều nhất max link, theo thứ tự SearXNG xếp */
    List<String> productUrls(String query, int max) {
        if (base.isEmpty() || java.time.Instant.now().isBefore(pausedUntil)) return List.of();
        try {
            var req = HttpRequest.newBuilder(URI.create(base + "/search?format=json&language=vi-VN&q="
                    + URLEncoder.encode(query + " giá", StandardCharsets.UTF_8))).timeout(Duration.ofSeconds(6)).GET().build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return List.of();
            JsonNode root = json.readTree(res.body());
            if (root.path("results").isEmpty() && !root.path("unresponsive_engines").isEmpty()) {
                log.warn("SearXNG: mọi máy tìm kiếm đang chặn ({}), nghỉ {} phút", root.path("unresponsive_engines"), PAUSE.toMinutes());
                pausedUntil = java.time.Instant.now().plus(PAUSE);
                return List.of();
            }
            return pick(root, max);
        } catch (java.io.IOException | RuntimeException e) {
            log.debug("SearXNG không trả lời: {}", e.getMessage());
            return List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    static List<String> pick(JsonNode root, int max) {
        Set<String> out = new LinkedHashSet<>();
        for (JsonNode r : root.path("results")) {
            String url = r.path("url").asString("").replaceFirst("#.*$", "");
            if (!url.startsWith("https://") || !Shops.known(url)) continue;
            try {
                URI u = URI.create(url);
                String path = u.getRawPath() + (u.getRawQuery() == null ? "" : "?" + u.getRawQuery());
                if (path.length() <= 1 || NOT_PRODUCT.matcher(path).find()) continue;
            } catch (IllegalArgumentException e) {
                continue;
            }
            out.add(url);
            if (out.size() >= max) break;
        }
        return new ArrayList<>(out);
    }
}
