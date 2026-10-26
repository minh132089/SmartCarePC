package com.smartcarepc.backend.service.ai;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.smartcarepc.backend.dto.ChatRequest;
import com.smartcarepc.backend.dto.ChatResponse;
import com.smartcarepc.backend.exception.AiUnavailableException;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Câu hỏi tư vấn chạy nền: trả mã việc ngay, trình duyệt hỏi lại vài giây một lần. Một yêu cầu dài cả phút dễ bị cắt
 * giữa chừng (link demo qua Cloudflare cắt ở 100 giây); mỗi lần hỏi lại thì chỉ mất vài mili giây.
 */
@Slf4j
@Component
public class ChatJobs {

    /** Việc xong giữ lại từng này để trình duyệt kịp lấy kết quả, rồi xoá */
    static final Duration KEEP = Duration.ofMinutes(15);

    public enum Status { running, done, error }

    /** Trạng thái gửi cho trình duyệt: stage là bước đang làm, result khi xong, error khi lỗi */
    public record JobView(String id, Status status, String stage, ChatResponse result, String error) { }

    private static final class Job {
        final String id = UUID.randomUUID().toString();
        final Instant created = Instant.now();
        volatile Status status = Status.running;
        volatile String stage;
        volatile ChatResponse result;
        volatile String error;

        JobView view() {
            return new JobView(id, status, stage, result, error);
        }
    }

    private final ChatService chat;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    /** Vài câu hỏi cùng lúc là đủ cho demo; mỗi câu đã tự chạy song song các lượt tìm kiếm bên trong */
    private final ExecutorService pool = Executors.newFixedThreadPool(4);

    public ChatJobs(ChatService chat) {
        this.chat = chat;
    }

    public JobView start(ChatRequest request) {
        Instant old = Instant.now().minus(KEEP);
        jobs.values().removeIf(j -> j.created.isBefore(old));
        var job = new Job();
        jobs.put(job.id, job);
        pool.execute(() -> run(job, request));
        return job.view();
    }

    public JobView get(String id) {
        var job = jobs.get(id);
        if (job == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This question was not found or has expired. Please submit it again.");
        return job.view();
    }

    private void run(Job job, ChatRequest request) {
        try {
            job.result = chat.reply(request, stage -> job.stage = stage);
            job.status = Status.done;
        } catch (AiUnavailableException | ResponseStatusException e) {
            job.error = e instanceof ResponseStatusException r && r.getReason() != null ? r.getReason() : e.getMessage();
            job.status = Status.error;
        } catch (RuntimeException | Error e) {
            log.error("Question {} failed", job.id, e);
            job.error = "The assistant encountered an error. Please try again.";
            job.status = Status.error;
            if (e instanceof Error fatal) throw fatal;
        }
    }

    @PreDestroy
    void stop() {
        pool.shutdownNow();
    }

    /** Chỉ cho test: đợi việc chạy xong */
    JobView await(String id, Duration max) throws InterruptedException {
        Instant end = Instant.now().plus(max);
        while (get(id).status() == Status.running && Instant.now().isBefore(end)) Thread.sleep(10);
        return get(id);
    }
}
