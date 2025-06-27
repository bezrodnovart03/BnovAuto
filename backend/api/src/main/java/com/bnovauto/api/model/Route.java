package com.bnovauto.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "start_point", columnDefinition = "geometry(Point,4326)")
    private Point startPoint;

    @Column(name = "end_point", columnDefinition = "geometry(Point,4326)")
    private Point endPoint;

    @Column(name = "path", columnDefinition = "geometry(LineString,4326)")
    private LineString path;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
