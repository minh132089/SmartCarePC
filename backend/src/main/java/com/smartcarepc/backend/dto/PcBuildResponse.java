package com.smartcarepc.backend.dto;

import java.util.List;

import com.smartcarepc.backend.entity.PcBuild;
import com.smartcarepc.backend.entity.enums.BuildType;

/** Một bộ máy kèm toàn bộ linh kiện */
public record PcBuildResponse(
        Long id,
        String code,
        String name,
        String tagline,
        BuildType buildType,
        String thumbnailUrl,
        List<Item> items) {

    public record Item(int quantity, ComponentResponse component) {
    }

    public static PcBuildResponse from(PcBuild b) {
        List<Item> items = b.getItems().stream()
                .map(i -> new Item(i.getQuantity(), ComponentResponse.from(i.getComponent())))
                .toList();
        return new PcBuildResponse(b.getId(), b.getCode(), b.getName(), b.getTagline(), b.getBuildType(),
                b.getThumbnailUrl(), items);
    }
}
