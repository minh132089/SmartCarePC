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
    // Each task gets a new virtual thread, so nested price lookups inherit the request's deadline.
    private static final InheritableThreadLocal<Long> DEADLINE = new InheritableThreadLocal<>();

    public static <T> T within(java.time.Duration budget, Supplier<T> task) {
        Long previous = DEADLINE.get();
        long end = System.nanoTime() + budget.toNanos();
        DEADLINE.set(previous == null ? end : Math.min(previous, end));
        try {
            return task.get();
        } finally {
            if (previous == null) DEADLINE.remove();
            else DEADLINE.set(previous);
        }
    }

    public static long remainingNanos(long maximum) {
        Long end = DEADLINE.get();
        return end == null ? maximum : Math.max(0, Math.min(maximum, end - System.nanoTime()));
    }

    public static boolean expired() {
        return remainingNanos(Long.MAX_VALUE) == 0;
    }

    public static <T> T await(CompletableFuture<T> task, java.time.Duration maximum, T fallback) {
        try {
            return task.get(remainingNanos(maximum.toNanos()), java.util.concurrent.TimeUnit.NANOSECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return fallback;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback;
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            throw new IllegalStateException(e.getCause());
        }
    }

    private Io() {
    }

    public static <T> CompletableFuture<T> async(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, POOL);
    }

    public static CompletableFuture<Void> run(Runnable task) {
        return CompletableFuture.runAsync(task, POOL);
    }
}
