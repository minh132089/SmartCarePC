package com.smartcarepc.backend.support;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** English display labels for built-in records saved before the language change. */
public final class EnglishCopy {
    private static final Properties CATALOG = load();

    private EnglishCopy() { }

    public static String text(String value) {
        return value == null ? null : CATALOG.getProperty(value, value);
    }

    /** Translate store descriptors for display, keeping model identifiers and source data intact. */
    public static String productName(String value) {
        if (value == null) return null;
        String[][] labels = {
                {"card màn hình|card đồ họa", "Graphics card"},
                {"bo mạch chủ|bo mạch chính", "Motherboard"},
                {"bộ vi xử lý|bộ xử lý", "Processor"},
                {"bộ nhớ trong|bộ nhớ", "Memory"},
                {"ổ cứng", "Drive"},
                {"nguồn máy tính|bộ nguồn|nguồn", "Power supply"},
                {"vỏ máy tính|vỏ case|vỏ máy|vỏ", "Case"},
                {"tản nhiệt nước|tản nước", "Liquid cooler"},
                {"tản nhiệt khí|tản khí", "Air cooler"},
                {"tản nhiệt", "Cooler"},
                {"kính cường lực", "Tempered glass"},
                {"chính hãng", "Official"},
                {"màu đen", "Black"}, {"màu trắng", "White"},
                {"đen", "Black"}, {"trắng", "White"},
                {"nhân", "cores"}, {"luồng", "threads"},
                {"quạt", "Fan"}
        };
        for (var label : labels) value = value.replaceAll("(?iuU)\\b(?:" + label[0] + ")\\b", label[1]);
        return value;
    }

    private static Properties load() {
        try (var stream = EnglishCopy.class.getResourceAsStream("/english-catalog.properties")) {
            if (stream == null) throw new IllegalStateException("English catalog labels are missing");
            var labels = new Properties();
            labels.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return labels;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
