package com.smartcarepc.backend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.smartcarepc.backend.dto.ChatRequest.Message;

class EnglishInputTests {
    @Test
    void englishRequirementsKeepVndBudgetMemoryAndGpuPreferences() {
        var text = List.of("A PC for Photoshop and Illustrator 2D, budget 20 million VND, RAM 32GB and SSD 1TB");
        var needs = ChatService.needs(text);
        assertThat(ChatService.amounts(text)).contains(20_000_000L);
        assertThat(needs.creator()).isTrue();
        assertThat(needs.vramMin()).isEqualTo(6);
        assertThat(needs.ramMin()).isEqualTo(32);
        assertThat(needs.ssdMin()).isEqualTo(1000);
        assertThat(ChatService.noDiscreteGpu(List.of("Office PC, no dedicated GPU"))).isTrue();
        assertThat(ChatService.noDiscreteGpu(List.of("Do not remove the graphics card"))).isFalse();
    }

    @Test
    void aShortEnglishFollowupReplacesTheOldGamingWorkload() {
        var text = ChatService.adviceUserTexts(List.of(
                new Message("user", "A gaming PC, budget 25 million VND, demanding AAA games"),
                new Message("assistant", "Do you play light games or demanding AAA games?"),
                new Message("user", "Only light games now")));
        assertThat(ChatService.needs(text).vramMin()).isEqualTo(6);
        assertThat(ChatService.amounts(text)).contains(25_000_000L);
    }

    @Test
    void englishReasonsCannotInventWarrantyOrTheRamLayout() {
        assertThat(ChatService.safeReason("ram", 1, "Two modules for dual channel")).isNull();
        assertThat(ChatService.safeReason("cpu", 1, "36 months warranty")).isNull();
        assertThat(ChatService.safeReason("ssd", 1, "Space for your documents")).isEqualTo("Space for your documents");
    }
}
