package com.smartcarepc.backend.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smartcarepc.backend.entity.MaintenanceEvent;
public interface MaintenanceEventRepository extends JpaRepository<MaintenanceEvent, Long> {
    Optional<MaintenanceEvent> findFirstByEventId(String eventId);
    List<MaintenanceEvent> findAllByOrderByIdDesc();
}
