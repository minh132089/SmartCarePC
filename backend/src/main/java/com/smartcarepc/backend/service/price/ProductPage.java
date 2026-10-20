package com.smartcarepc.backend.service.price;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Đọc trang chi tiết sản phẩm của cửa hàng: tên, giá, còn hàng từ JSON-LD (schema.org Product, cửa hàng nào cũng có
 * để lên Google); bảo hành từ chữ trên trang ("Bảo hành: 36 tháng").
 */
final class ProductPage {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern WARRANTY = Pattern.compile(
            "bảo\\s*hành[^0-9]{0,25}?(\\d{1,2})\\s*(tháng|năm)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private ProductPage() {
    }

    /** Offer đọc từ html của url, null nếu trang không phải trang một sản phẩm có giá */
    static Offer parse(String html, String url, Instant at) {
        if (html == null) return null;
        Document doc = Jsoup.parse(html, url);
        for (var script : doc.select("script[type=application/ld+json]")) {
            for (JsonNode product : products(script.data())) {
                JsonNode offers = product.path("offers");
                JsonNode offer = offers.isArray() ? offers.path(0) : offers;
                String name = text(product, "name");
                if (name == null) continue;
                // Trang có nhiều phiên bản / mức giá (vd nguồn 550W và 650W chung một trang, AggregateOffer giá thấp - cao):
                // không biết giá nào đúng bản đã chọn, nên coi như chưa đọc được giá
                java.util.Set<Long> prices = new java.util.TreeSet<>();
                for (JsonNode o : offers.isArray() ? offers : List.of(offers)) {
                    Long p = Prices.parse(text(o, "price") != null ? text(o, "price") : text(o, "lowPrice"));
                    if (p != null && p >= 10_000) prices.add(p);
                    Long high = Prices.parse(text(o, "highPrice"));
                    if (high != null && high >= 10_000) prices.add(high);
                }
                if (prices.isEmpty()) continue;
                Long price = prices.size() == 1 ? prices.iterator().next() : null;
                String availability = text(offer, "availability");
                Boolean inStock = availability == null ? null
                        : availability.contains("InStock") || availability.contains("LimitedAvailability")
                                ? Boolean.TRUE
                                : availability.matches(".*(OutOfStock|SoldOut|Discontinued).*") ? Boolean.FALSE : null;
                String image = product.path("image").isArray() ? text(product.path("image"), 0) : text(product, "image");
                if (image == null) image = doc.select("meta[property=og:image]").attr("content");
                return new Offer(org.jsoup.parser.Parser.unescapeEntities(name.strip(), false), Shops.name(url), price, null, url,
                        image == null || image.isBlank() ? null : image, inStock, warranty(doc.body() == null ? "" : doc.body().text()), at);
            }
        }
        return null;
    }

    /**
     * Như parse, nhưng nhận cả trang không có JSON-LD: tên lấy từ og:title / h1, giá từ meta product:price:amount hoặc
     * itemprop=price (có thể không có). Chỉ nhận khi trang có dấu hiệu là trang sản phẩm (og:type product, có giá, hay
     * JSON-LD Product); null nếu không phải.
     */
    static Offer parseLoose(String html, String url, Instant at) {
        Offer strict = parse(html, url, at);
        if (strict != null || html == null) return strict;
        Document doc = Jsoup.parse(html, url);
        String name = doc.select("meta[property=og:title]").attr("content");
        if (name.isBlank() && doc.selectFirst("h1") != null) name = doc.selectFirst("h1").text();
        if (name.isBlank()) return null;
        String raw = doc.select("meta[property=product:price:amount], meta[property=og:price:amount], meta[itemprop=price]").attr("content");
        if (raw.isBlank() && doc.selectFirst("[itemprop=price][content]") != null) raw = doc.selectFirst("[itemprop=price][content]").attr("content");
        Long price = Prices.parse(raw);
        boolean product = price != null || doc.select("meta[property=og:type]").attr("content").toLowerCase().contains("product");
        if (!product) return null;
        String image = doc.select("meta[property=og:image]").attr("content");
        return new Offer(name.strip(), Shops.name(url), price != null && price >= 10_000 ? price : null, null, url,
                image.isBlank() ? null : image, null, warranty(doc.body() == null ? "" : doc.body().text()), at);
    }

    /** Số tháng bảo hành ghi trên trang ("Bảo hành 3 năm" -> 36), null nếu không thấy */
    static Integer warranty(String text) {
        Matcher m = WARRANTY.matcher(text);
        if (!m.find()) return null;
        int n = Integer.parseInt(m.group(1));
        int months = m.group(2).toLowerCase().startsWith("n") ? n * 12 : n;
        return months > 0 && months <= 120 ? months : null;
    }

    /** Các nút @type Product trong một khối JSON-LD (kể cả nằm trong @graph hay mảng) */
    private static List<JsonNode> products(String json) {
        List<JsonNode> out = new ArrayList<>();
        try {
            collect(JSON.readTree(json), out);
        } catch (RuntimeException e) {
            // JSON-LD hỏng (hay gặp: xuống dòng trong chuỗi): bỏ qua khối này
        }
        return out;
    }

    private static void collect(JsonNode node, List<JsonNode> out) {
        if (node == null) return;
        if (node.isArray()) {
            node.forEach(n -> collect(n, out));
        } else if (node.isObject()) {
            JsonNode type = node.path("@type");
            if (type.asString("").equals("Product") || type.isArray() && type.toString().contains("\"Product\"")) out.add(node);
            collect(node.get("@graph"), out);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.isObject() ? text(v, "url") : v.asString();
    }

    private static String text(JsonNode node, int index) {
        JsonNode v = node.path(index);
        return v.isMissingNode() || v.isNull() ? null : v.isObject() ? text(v, "url") : v.asString();
    }
}
