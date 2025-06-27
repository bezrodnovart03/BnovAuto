package com.bnovauto.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "telemetry")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Telemetry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "location", columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "speed")
    private Double speed;

    @Column(name = "fuel_level")
    private Double fuelLevel;

    @Column(name = "engine_temperature")
    private Double engineTemperature;

    @Column(name = "engine_rpm")
    private Integer engineRpm;

    @Column(name = "battery_voltage")
    private Double batteryVoltage;

    @Column(name = "error_code")
    private String errorCode;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}