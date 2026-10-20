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
                JsonNode offer = product.path("offers");
                if (offer.isArray()) offer = offer.path(0);
                Long price = Prices.parse(text(offer, "price") != null ? text(offer, "price") : text(offer, "lowPrice"));
                String name = text(product, "name");
                if (price == null || price < 10_000 || name == null) continue;
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
