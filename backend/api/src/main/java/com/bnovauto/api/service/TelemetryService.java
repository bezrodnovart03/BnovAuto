package com.bnovauto.api.service;

import com.bnovauto.api.model.Telemetry;
import com.bnovauto.api.model.Vehicle;
import com.bnovauto.api.repository.TelemetryRepository;
import com.bnovauto.api.repository.VehicleRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final VehicleRepository vehicleRepository;
    private final GeometryFactory geometryFactory;

    @Autowired
    public TelemetryService(TelemetryRepository telemetryRepository, VehicleRepository vehicleRepository) {
        this.telemetryRepository = telemetryRepository;
        this.vehicleRepository = vehicleRepository;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    }

    public List<Telemetry> getAllTelemetry() {
        return telemetryRepository.findAll();
    }

    public Optional<Telemetry> getTelemetryById(Long id) {
        return telemetryRepository.findById(id);
    }

    public List<Telemetry> getTelemetryByVehicleId(Long vehicleId) {
        return telemetryRepository.findByVehicleId(vehicleId);
    }

    public List<Telemetry> getTelemetryByVehicleIdAndTimeRange(Long vehicleId, LocalDateTime startTime, LocalDateTime endTime) {
        return telemetryRepository.findByVehicleIdAndTimestampBetween(vehicleId, startTime, endTime);
    }

    public Telemetry createTelemetry(Telemetry telemetry) {
        return telemetryRepository.save(telemetry);
    }

    /**
     * Создает новую запись телеметрии для указанного транспортного средства
     * 
     * @param vehicleId ID транспортного средства
     * @param latitude Широта
     * @param longitude Долгота
     * @param speed Скорость (км/ч)
     * @param fuelLevel Уровень топлива (%)
     * @param engineTemperature Температура двигателя (°C)
     * @param engineRpm Обороты двигателя (об/мин)
     * @param batteryVoltage Напряжение аккумулятора (В)
     * @param errorCode Код ошибки (если есть)
     * @return Созданная запись телеметрии
     */
    public Telemetry recordTelemetry(Long vehicleId, double latitude, double longitude, 
                                    Double speed, Double fuelLevel, Double engineTemperature, 
                                    Integer engineRpm, Double batteryVoltage, String errorCode) {
        
        // Проверка существования транспортного средства
        Optional<Vehicle> vehicleOptional = vehicleRepository.findById(vehicleId);
        if (!vehicleOptional.isPresent()) {
            throw new IllegalArgumentException("Vehicle not found with id: " + vehicleId);
        }
        Vehicle vehicle = vehicleOptional.get();
        
        // Создание точки местоположения
        Point location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        
        // Создание и сохранение записи телеметрии
        Telemetry telemetry = new Telemetry();
        telemetry.setVehicle(vehicle);
        telemetry.setTimestamp(LocalDateTime.now());
        telemetry.setLocation(location);
        telemetry.setSpeed(speed);
        telemetry.setFuelLevel(fuelLevel);
        telemetry.setEngineTemperature(engineTemperature);
        telemetry.setEngineRpm(engineRpm);
        telemetry.setBatteryVoltage(batteryVoltage);
        telemetry.setErrorCode(errorCode);
        
        return telemetryRepository.save(telemetry);
    }

    /**
     * Получает последнюю запись телеметрии для указанного транспортного средства
     * 
     * @param vehicleId ID транспортного средства
     * @return Последняя запись телеметрии или пустой Optional, если записей нет
     */
    public Optional<Telemetry> getLatestTelemetryForVehicle(Long vehicleId) {
        return telemetryRepository.findTopByVehicleIdOrderByTimestampDesc(vehicleId);
    }
}
