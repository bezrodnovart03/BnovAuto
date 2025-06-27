package com.bnovauto.api.repository;

import com.bnovauto.api.model.Vehicle;
import com.bnovauto.api.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByCompany(Company company);
    List<Vehicle> findByCompanyId(Long companyId);
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    boolean existsByLicensePlate(String licensePlate);
    
    @Query("SELECT v FROM Vehicle v WHERE v.status = :status")
    List<Vehicle> findByStatus(String status);
    
    @Query("SELECT v FROM Vehicle v JOIN v.company c WHERE c.id = :companyId AND v.status = :status")
    List<Vehicle> findByCompanyIdAndStatus(Long companyId, String status);
}
