package com.bnovauto.api.repository;

import com.bnovauto.api.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByCompanyId(Long companyId);
    
    // Добавленные методы
    @Query("SELECT r FROM Route r JOIN r.vehicles v WHERE v.id = ?1")
    List<Route> findByVehicleId(Long vehicleId);
    
    @Query("SELECT r FROM Route r JOIN r.drivers d WHERE d.id = ?1")
    List<Route> findByDriverId(Long driverId);
    
    @Query("SELECT r FROM Route r WHERE r.status = 'ACTIVE'")
    List<Route> findByStatusActive();
}

