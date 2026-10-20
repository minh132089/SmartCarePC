package com.smartcarepc.backend.service.price;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Gọi web cửa hàng một cách lịch sự: tự giới thiệu đúng tên (không giả trình duyệt), làm theo robots.txt (Disallow,
 * Crawl-delay) và giãn cách các lần gọi cùng một trang. Phải chờ lâu hơn maxWait thì bỏ qua lần gọi đó.
 */
@Slf4j
@Component
public class PoliteFetcher {

    static final String USER_AGENT = "SmartCarePC/1.0 (+https://smartcarepc.pages.dev; student project price checker)";
    /** Khoảng cách tối thiểu giữa hai lần gọi cùng một trang khi robots.txt không quy định */
    static final Duration DEFAULT_GAP = Duration.ofMillis(500);
    static final Duration ROBOTS_TTL = Duration.ofHours(6);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final Map<String, Robots> robots = new ConcurrentHashMap<>();
    /** Thời điểm sớm nhất được gọi tiếp mỗi trang (mili giây) */
    private final Map<String, Long> nextSlot = new ConcurrentHashMap<>();

    /** HTML của url, hoặc null nếu robots.txt cấm, phải chờ quá maxWait, hay trang lỗi */
    public String get(String url, Duration maxWait) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (uri.getHost() == null) return null;
        String origin = uri.getScheme() + "://" + uri.getHost();
        Robots rules = robotsOf(origin);
        String path = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        if (!rules.allowed(path)) {
            log.debug("robots.txt cấm {}", url);
            return null;
        }
        if (!waitTurn(uri.getHost(), rules.delay(), maxWait)) return null;
        return fetch(uri, Duration.ofSeconds(6));
    }

    private String fetch(URI uri, Duration timeout) {
        try {
            var req = HttpRequest.newBuilder(uri).timeout(timeout)
                    .header("User-Agent", USER_AGENT).header("Accept-Language", "vi-VN,vi;q=0.9").GET().build();
            var res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200 ? res.body() : null;
        } catch (java.io.IOException e) {
            log.debug("Không đọc được {}: {}", uri, e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** Giữ chỗ lượt gọi kế tiếp của trang; chờ tới lượt nếu không quá maxWait */
    private boolean waitTurn(String host, Duration gap, Duration maxWait) {
        long now = System.currentTimeMillis();
        long[] slot = new long[1];
        nextSlot.compute(host, (h, next) -> {
            long at = next == null || next < now ? now : next;
            slot[0] = at;
            // Không giữ chỗ khi phải chờ quá lâu, để lượt sau không bị đẩy lùi vô ích
            return at - now > maxWait.toMillis() ? next : at + gap.toMillis();
        });
        long wait = slot[0] - now;
        if (wait > maxWait.toMillis()) return false;
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    private Robots robotsOf(String origin) {
        var r = robots.get(origin);
        if (r != null && System.currentTimeMillis() - r.at() < ROBOTS_TTL.toMillis()) return r;
        String txt = fetch(URI.create(origin + "/robots.txt"), Duration.ofSeconds(4));
        r = Robots.parse(txt == null ? "" : txt, "smartcarepc");
        robots.put(origin, r);
        return r;
    }

    /** Luật robots.txt áp dụng cho engine: nhóm "smartcarepc" nếu có, không thì nhóm "*" */
    record Robots(List<Rule> rules, Duration delay, long at) {

        record Rule(boolean allow, Pattern pattern, int length) { }

        boolean allowed(String path) {
            Rule best = null;
            for (var r : rules) {
                if (r.pattern().matcher(path).lookingAt() && (best == null || r.length() > best.length()
                        || r.length() == best.length() && r.allow())) {
                    best = r;
                }
            }
            return best == null || best.allow();
        }

        static Robots parse(String txt, String agent) {
            List<Rule> mine = new ArrayList<>(), any = new ArrayList<>();
            Duration myDelay = null, anyDelay = null;
            List<String> group = new ArrayList<>();
            boolean inRules = false;
            for (String raw : txt.split("\\R")) {
                String line = raw.replaceFirst("#.*", "").strip();
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String key = line.substring(0, colon).strip().toLowerCase(), value = line.substring(colon + 1).strip();
                if (key.equals("user-agent")) {
                    if (inRules) group.clear();
                    inRules = false;
                    group.add(value.toLowerCase());
                    continue;
                }
                inRules = true;
                boolean forMe = group.stream().anyMatch(agent::contains), forAny = group.contains("*");
                if (!forMe && !forAny) continue;
                if (key.equals("crawl-delay")) {
                    try {
                        var d = Duration.ofMillis((long) (Double.parseDouble(value) * 1000));
                        if (forMe) myDelay = d;
                        if (forAny) anyDelay = d;
                    } catch (NumberFormatException ignored) {
                        // bỏ qua giá trị lạ
                    }
                } else if ((key.equals("disallow") || key.equals("allow")) && !value.isEmpty()) {
                    var rule = new Rule(key.equals("allow"), toPattern(value), value.length());
                    if (forMe) mine.add(rule);
                    if (forAny) any.add(rule);
                }
            }
            boolean useMine = !mine.isEmpty() || myDelay != null;
            Duration delay = useMine ? myDelay : anyDelay;
            return new Robots(useMine ? mine : any, delay == null ? DEFAULT_GAP : delay, System.currentTimeMillis());
        }

        /** "/search?*" -> regex khớp từ đầu đường dẫn; "*" là chuỗi bất kỳ, "$" là hết đường dẫn */
        static Pattern toPattern(String rule) {
            StringBuilder re = new StringBuilder();
            for (char ch : rule.toCharArray()) {
                if (ch == '*') re.append(".*");
                else if (ch == '$') re.append("$");
                else re.append(Pattern.quote(String.valueOf(ch)));
            }
            return Pattern.compile(re.toString());
        }
    }
}
