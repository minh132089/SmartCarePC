package com.smartcarepc.backend.service.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class PriceLookupDeadlineTests {
    private final PriceEngine engine = new PriceEngine(mock(PoliteFetcher.class), mock(Searxng.class), true);

    @Test
    void anotherQueryReusesTheSameVerifiedProductWithoutSearchingAgain() {
        var fetcher = mock(PoliteFetcher.class);
        var prices = new PriceEngine(fetcher, mock(Searxng.class), true);
        String url = "https://store.test/products/kingston-16gb-ddr4";
        when(fetcher.page(any(), any())).thenReturn(new PoliteFetcher.Page("""
                <html><script type="application/ld+json">{"@type":"Product","name":"RAM Kingston Fury 16GB DDR4 3200",
                "offers":{"@type":"Offer","price":"900000","priceCurrency":"VND","availability":"https://schema.org/InStock"}}
                </script></html>
                """, url));
        assertThat(prices.readPage(url, "RAM Kingston Fury 16GB DDR4 3200")).isNotNull();
        assertThat(prices.search("RAM 16GB DDR4")).extracting(Offer::url).containsExactly(url);
        verify(fetcher, never()).get(any(), any());
    }

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
