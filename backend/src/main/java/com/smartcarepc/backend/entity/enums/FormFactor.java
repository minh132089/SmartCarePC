package com.smartcarepc.backend.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Chuẩn kích thước bo mạch chủ. rank lớn hơn = bo lớn hơn; vỏ nhận bo lớn thì nhận được bo nhỏ hơn. */
@Getter
@RequiredArgsConstructor
public enum FormFactor {
    ATX("ATX (305 × 244 mm)", 305, 244, 3),
    MATX("Micro-ATX (244 × 244 mm)", 244, 244, 2),
    ITX("Mini-ITX (170 × 170 mm)", 170, 170, 1);

    private final String label;
    private final int heightMm;
    private final int widthMm;
    private final int rank;

    public boolean fitsIn(FormFactor caseMax) {
        return rank <= caseMax.rank;
    }
}
