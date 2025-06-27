package com.bnovauto.api.service;

import com.bnovauto.api.model.Vehicle;
import com.bnovauto.api.model.Telemetry;
import com.bnovauto.api.model.Company;
import com.bnovauto.api.repository.VehicleRepository;
import com.bnovauto.api.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }
    
    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
    }
    
    @Transactional
    public Vehicle createVehicle(Vehicle vehicle) {
        // Проверка на существование транспортного средства
        if (vehicleRepository.existsByLicensePlate(vehicle.getLicensePlate())) {
            throw new RuntimeException("License plate is already registered!");
        }
        
        // Проверка существования компании
        Company company = companyRepository.findById(vehicle.getCompany().getId())
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + vehicle.getCompany().getId()));
        
        vehicle.setCompany(company);
        vehicle.setStatus("ACTIVE");
        
        return vehicleRepository.save(vehicle);
    }
    
    @Transactional
    public Vehicle updateVehicle(Long id, Vehicle vehicleDetails) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
        
        // Обновление данных транспортного средства
        vehicle.setName(vehicleDetails.getName());
        vehicle.setModel(vehicleDetails.getModel());
        
        // Если меняется номерной знак, проверяем на уникальность
        if (!vehicle.getLicensePlate().equals(vehicleDetails.getLicensePlate())) {
            if (vehicleRepository.existsByLicensePlate(vehicleDetails.getLicensePlate())) {
                throw new RuntimeException("License plate is already registered!");
            }
            vehicle.setLicensePlate(vehicleDetails.getLicensePlate());
        }
        
        vehicle.setYear(vehicleDetails.getYear());
        vehicle.setStatus(vehicleDetails.getStatus());
        
        // Если меняется компания, проверяем её существование
        if (!vehicle.getCompany().getId().equals(vehicleDetails.getCompany().getId())) {
            Company company = companyRepository.findById(vehicleDetails.getCompany().getId())
                    .orElseThrow(() -> new RuntimeException("Company not found with id: " + vehicleDetails.getCompany().getId()));
            
            vehicle.setCompany(company);
        }
        
        return vehicleRepository.save(vehicle);
    }
    
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
        
        vehicleRepository.delete(vehicle);
    }
    
    public List<Vehicle> getVehiclesByCompany(Long companyId) {
        return vehicleRepository.findByCompanyId(companyId);
    }
    
    @SuppressWarnings("unchecked")
    public List<Telemetry> getVehicleTelemetry(Long vehicleId, String startDate, String endDate) {
        // Проверка существования транспортного средства
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));
        
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT t FROM Telemetry t WHERE t.vehicle.id = :vehicleId");
        
        // Добавление фильтров по датам, если они указаны
        if (startDate != null && !startDate.isEmpty()) {
            queryBuilder.append(" AND t.timestamp >= :startDate");
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            queryBuilder.append(" AND t.timestamp <= :endDate");
        }
        
        queryBuilder.append(" ORDER BY t.timestamp DESC");
        
        Query query = entityManager.createQuery(queryBuilder.toString());
        query.setParameter("vehicleId", vehicleId);
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startDate, formatter);
            query.setParameter("startDate", start);
        }
        
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDateTime.parse(endDate, formatter);
            query.setParameter("endDate", end);
        }
        
        return query.getResultList();
    }
    
    @SuppressWarnings("unchecked")
    public Telemetry getLatestVehicleTelemetry(Long vehicleId) {
        // Проверка существования транспортного средства
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));
        
        Query query = entityManager.createQuery(
                "SELECT t FROM Telemetry t WHERE t.vehicle.id = :vehicleId ORDER BY t.timestamp DESC");
        query.setParameter("vehicleId", vehicleId);
        query.setMaxResults(1);
        
        List<Telemetry> results = query.getResultList();
        
        if (results.isEmpty()) {
            return null;
        }
        
        return results.get(0);
    }
    
    public Map<String, Object> getVehicleStatistics(Long vehicleId, String startDate, String endDate) {
        // Проверка существования транспортного средства
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + vehicleId));
        
        Map<String, Object> statistics = new HashMap<>();
        
        // Получение телеметрии за указанный период
        List<Telemetry> telemetryData = getVehicleTelemetry(vehicleId, startDate, endDate);
        
        // Расчет статистики
        if (!telemetryData.isEmpty()) {
            // Средняя скорость
            double avgSpeed = telemetryData.stream()
                    .filter(t -> t.getSpeed() != null)
                    .mapToDouble(t -> t.getSpeed())
                    .average()
                    .orElse(0.0);
            
            // Средний расход топлива
            double avgFuelLevel = telemetryData.stream()
                    .filter(t -> t.getFuelLevel() != null)
                    .mapToDouble(t -> t.getFuelLevel())
                    .average()
                    .orElse(0.0);
            
            // Максимальная скорость
            double maxSpeed = telemetryData.stream()
                    .filter(t -> t.getSpeed() != null)
                    .mapToDouble(t -> t.getSpeed())
                    .max()
                    .orElse(0.0);
            
            // Количество ошибок
            long errorCount = telemetryData.stream()
                    .filter(t -> t.getErrorCode() != null && !t.getErrorCode().isEmpty())
                    .count();
            
            statistics.put("avgSpeed", avgSpeed);
            statistics.put("avgFuelLevel", avgFuelLevel);
            statistics.put("maxSpeed", maxSpeed);
            statistics.put("errorCount", errorCount);
            statistics.put("dataPointsCount", telemetryData.size());
        }
        
        return statistics;
    }
}
