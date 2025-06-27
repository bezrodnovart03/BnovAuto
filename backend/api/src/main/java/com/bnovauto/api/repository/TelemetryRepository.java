package com.bnovauto.api.repository;

import com.bnovauto.api.model.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    List<Telemetry> findByVehicleId(Long vehicleId);
    List<Telemetry> findByVehicleIdAndTimestampBetween(Long vehicleId, LocalDateTime startTime, LocalDateTime endTime);
    Optional<Telemetry> findTopByVehicleIdOrderByTimestampDesc(Long vehicleId);
}
