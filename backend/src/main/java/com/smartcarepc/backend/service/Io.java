package com.smartcarepc.backend.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Chạy việc chờ mạng (tìm kiếm, mở trang cửa hàng, gọi Serper) trên luồng ảo: tạo bao nhiêu cũng được, không chiếm
 * nhóm luồng chung của Java (chỉ bằng số nhân CPU). Trước đây hàng chục lượt tìm cùng lúc lúc dựng thẻ xếp hàng chờ
 * nhau tới hết giờ và trả về rỗng.
 */
public final class Io {

    public static final ExecutorService POOL = Executors.newVirtualThreadPerTaskExecutor();

    private Io() {
    }

    public static <T> CompletableFuture<T> async(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, POOL);
    }

    public static CompletableFuture<Void> run(Runnable task) {
        return CompletableFuture.runAsync(task, POOL);
    }
}
