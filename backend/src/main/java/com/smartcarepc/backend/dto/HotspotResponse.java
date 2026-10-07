package com.smartcarepc.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import com.smartcarepc.backend.entity.Hotspot;

/** at = [x, y, z] theo gốc model, giống kiểu Hotspot của frontend */
public record HotspotResponse(List<BigDecimal> at, String title, String text) {

    public static HotspotResponse from(Hotspot h) {
        return new HotspotResponse(List.of(h.getPosX(), h.getPosY(), h.getPosZ()), h.getTitle(), h.getContent());
    }
}
