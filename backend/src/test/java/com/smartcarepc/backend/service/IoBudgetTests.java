package com.smartcarepc.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class IoBudgetTests {
    @Test
    void nestedVirtualTasksShareOneDeadlineAndCompletedResultsSurviveIt() {
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            Io.within(Duration.ofMillis(60), () -> {
                assertThat(Io.await(Io.async(() -> Io.await(new CompletableFuture<String>(), Duration.ofSeconds(5), "timed out")),
                        Duration.ofSeconds(5), "timed out")).isEqualTo("timed out");
                assertThat(Io.expired()).isTrue();
                assertThat(Io.await(CompletableFuture.completedFuture("checked result"), Duration.ofSeconds(5), "missing"))
                        .isEqualTo("checked result");
                return null;
            });
            assertThat(Io.expired()).isFalse();
        });
    }
}
