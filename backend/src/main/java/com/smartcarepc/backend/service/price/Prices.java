package com.smartcarepc.backend.service.price;

/** Đọc số tiền viết kiểu Việt Nam */
final class Prices {

    private Prices() {
    }

    /** "8.990.000đ", "8,990,000 ₫", "7990000", "7990000.00" -> 7990000; null nếu không đọc được */
    static Long parse(String s) {
        if (s == null) return null;
        String t = s.replaceAll("[^0-9.,]", "");
        if (t.isEmpty()) return null;
        // Phần lẻ kiểu "7990000.00" / "7990000,5": bỏ phần sau dấu cuối nếu nó không đủ 3 chữ số
        int last = Math.max(t.lastIndexOf('.'), t.lastIndexOf(','));
        if (last >= 0 && t.length() - last - 1 != 3) t = t.substring(0, last);
        t = t.replaceAll("[.,]", "");
        if (t.isEmpty() || t.length() > 12) return null;
        long v = Long.parseLong(t);
        return v > 0 ? v : null;
    }
}
