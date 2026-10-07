package com.smartcarepc.backend.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;

import com.smartcarepc.backend.entity.component.CaseFan;
import com.smartcarepc.backend.entity.component.Component;
import com.smartcarepc.backend.entity.component.Cooler;
import com.smartcarepc.backend.entity.component.Cpu;
import com.smartcarepc.backend.entity.component.Gpu;
import com.smartcarepc.backend.entity.component.Motherboard;
import com.smartcarepc.backend.entity.component.PcCase;
import com.smartcarepc.backend.entity.component.Psu;
import com.smartcarepc.backend.entity.component.Ram;
import com.smartcarepc.backend.entity.component.Ssd;

/** Một linh kiện trả về cho frontend. specs chứa thông số riêng của từng loại. */
public record ComponentResponse(
        Long id,
        String code,
        String category,
        String name,
        String description,
        String modelUrl,
        String imageUrl,
        BigDecimal price,
        Map<String, Object> specs,
        List<HotspotResponse> hotspots) {

    /** Dùng cho danh sách: không kèm chú thích để khỏi tải thêm */
    public static ComponentResponse from(Component c) {
        return of(c, List.of());
    }

    /** Dùng cho trang chi tiết / "Xem cận cảnh" */
    public static ComponentResponse withHotspots(Component c) {
        return of(c, c.getHotspots().stream().map(HotspotResponse::from).toList());
    }

    private static ComponentResponse of(Component c, List<HotspotResponse> hotspots) {
        Component real = (Component) Hibernate.unproxy(c);
        return new ComponentResponse(real.getId(), real.getCode(), real.getCategory().getCode(), real.getName(),
                real.getDescription(), real.getModelUrl(), real.getImageUrl(), real.getPrice(), specs(real), hotspots);
    }

    private static Map<String, Object> specs(Component c) {
        Map<String, Object> m = new LinkedHashMap<>();
        switch (c) {
            case PcCase x -> {
                m.put("widthMm", x.getWidthMm());
                m.put("heightMm", x.getHeightMm());
                m.put("depthMm", x.getDepthMm());
                m.put("maxFormFactor", x.getMaxFormFactor());
                m.put("sidePanel", x.getSidePanel());
                m.put("psuType", x.getPsuType());
                m.put("maxGpuLengthMm", x.getMaxGpuLengthMm());
                m.put("maxCoolerHeightMm", x.getMaxCoolerHeightMm());
                m.put("maxRadiatorMm", x.getMaxRadiatorMm());
                m.put("gpuMount", x.getGpuMount());
                m.put("fanSlots", x.getFanSlots());
                m.put("hasFanHub", x.isHasFanHub());
            }
            case Motherboard x -> {
                m.put("formFactor", x.getFormFactor());
                m.put("formFactorLabel", x.getFormFactor().getLabel());
                m.put("socket", x.getSocket());
                m.put("ramType", x.getRamType());
                m.put("ramSlots", x.getRamSlots());
                m.put("pcieX16Slots", x.getPcieX16Slots());
                m.put("m2Slots", x.getM2Slots());
                m.put("sysFanHeaders", x.getSysFanHeaders());
                m.put("hasArgbHeader", x.isHasArgbHeader());
                m.put("hasPumpHeader", x.isHasPumpHeader());
                m.put("hasUsbCHeader", x.isHasUsbCHeader());
            }
            case Cpu x -> {
                m.put("socket", x.getSocket());
                m.put("cores", x.getCores());
                m.put("threads", x.getThreads());
                m.put("baseGhz", x.getBaseGhz());
                m.put("boostGhz", x.getBoostGhz());
                m.put("tdpW", x.getTdpW());
                m.put("hasIgpu", x.isHasIgpu());
            }
            case Cooler x -> {
                m.put("coolerType", x.getCoolerType());
                m.put("heightMm", x.getHeightMm());
                m.put("radiatorMm", x.getRadiatorMm());
                m.put("fanCount", x.getFanCount());
                m.put("maxTdpW", x.getMaxTdpW());
            }
            case Ram x -> {
                m.put("ramType", x.getRamType());
                m.put("capacityGb", x.getCapacityGb());
                m.put("speedMts", x.getSpeedMts());
                m.put("heightMm", x.getHeightMm());
                m.put("hasHeatsink", x.isHasHeatsink());
            }
            case Gpu x -> {
                m.put("lengthMm", x.getLengthMm());
                m.put("heightMm", x.getHeightMm());
                m.put("thicknessMm", x.getThicknessMm());
                m.put("fanCount", x.getFanCount());
                m.put("tdpW", x.getTdpW());
                m.put("powerConnector", x.getPowerConnector());
            }
            case Ssd x -> {
                m.put("interfaceType", x.getInterfaceType());
                m.put("formSize", x.getFormSize());
                m.put("capacityGb", x.getCapacityGb());
            }
            case Psu x -> {
                m.put("psuType", x.getPsuType());
                m.put("wattage", x.getWattage());
                m.put("efficiency", x.getEfficiency());
                m.put("modular", x.isModular());
            }
            case CaseFan x -> {
                m.put("sizeMm", x.getSizeMm());
                m.put("argb", x.isArgb());
            }
            default -> {
                // Accessory: không có thông số riêng
            }
        }
        return m;
    }
}
