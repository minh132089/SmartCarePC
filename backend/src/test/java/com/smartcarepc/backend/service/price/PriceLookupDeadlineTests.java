package com.smartcarepc.backend.service.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class PriceLookupDeadlineTests {
    private final PriceEngine engine = new PriceEngine(mock(PoliteFetcher.class), mock(Searxng.class), true);

    @Test
    void slowSourcesShareOneWaitAndCompletedSourcesAreStillCollected() {
        var incomplete = new AtomicBoolean();
        var completed = CompletableFuture.completedFuture(List.of("available product"));
        assertTimeoutPreemptively(Duration.ofMillis(500), () -> {
            long deadline = System.nanoTime() + Duration.ofMillis(60).toNanos();
            for (int i = 0; i < 4; i++) {
                assertThat(engine.join(new CompletableFuture<List<String>>(), incomplete, deadline)).isEmpty();
            }
            assertThat(engine.join(completed, incomplete, deadline)).containsExactly("available product");
        });
        assertThat(incomplete).isTrue();
    }

    @Test
    void timedOutPageDoesNotDiscardAnAlreadyCompletedVerifiedPage() {
        var offer = new Offer("RAM DDR4 16GB", "Store", 900_000L, null,
                "https://store.test/ram", null, true, 36, Instant.now());
        long expired = System.nanoTime() - 1;
        assertThat(engine.joinOne(new CompletableFuture<>(), expired)).isNull();
        assertThat(engine.joinOne(CompletableFuture.completedFuture(offer), expired)).isSameAs(offer);
    }

    @Test
    void failedSourceIsMarkedIncompleteInsteadOfTreatingItAsNoStock() {
        var incomplete = new AtomicBoolean();
        assertThat(engine.join(CompletableFuture.<List<String>>failedFuture(new IllegalStateException("offline")),
                incomplete, System.nanoTime() + Duration.ofSeconds(1).toNanos())).isEmpty();
        assertThat(incomplete).isTrue();
    }
}
