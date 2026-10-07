package com.smartcarepc.backend.entity.component;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** Thứ không có thông số riêng: bộ đồ nghề lắp máy, dây cáp, hub quạt... */
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "accessory")
public class Accessory extends Component {
}
