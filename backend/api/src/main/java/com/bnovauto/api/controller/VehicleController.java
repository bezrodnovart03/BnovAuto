package com.bnovauto.api.controller;

import com.bnovauto.api.model.Vehicle;
import com.bnovauto.api.model.Telemetry;
import com.bnovauto.api.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @GetMapping
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        return new ResponseEntity<>(vehicles, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT') or hasRole('DRIVER')")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable Long id) {
        Vehicle vehicle = vehicleService.getVehicleById(id);
        return new ResponseEntity<>(vehicle, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<Vehicle> createVehicle(@RequestBody Vehicle vehicle) {
        Vehicle newVehicle = vehicleService.createVehicle(vehicle);
        return new ResponseEntity<>(newVehicle, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable Long id, @RequestBody Vehicle vehicleDetails) {
        Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicleDetails);
        return new ResponseEntity<>(updatedVehicle, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<HttpStatus> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<List<Vehicle>> getVehiclesByCompany(@PathVariable Long companyId) {
        List<Vehicle> vehicles = vehicleService.getVehiclesByCompany(companyId);
        return new ResponseEntity<>(vehicles, HttpStatus.OK);
    }

    @GetMapping("/{id}/telemetry")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT') or hasRole('DRIVER')")
    public ResponseEntity<List<Telemetry>> getVehicleTelemetry(
            @PathVariable Long id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<Telemetry> telemetry = vehicleService.getVehicleTelemetry(id, startDate, endDate);
        return new ResponseEntity<>(telemetry, HttpStatus.OK);
    }

    @GetMapping("/{id}/telemetry/latest")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT') or hasRole('DRIVER')")
    public ResponseEntity<Telemetry> getLatestVehicleTelemetry(@PathVariable Long id) {
        Telemetry telemetry = vehicleService.getLatestVehicleTelemetry(id);
        return new ResponseEntity<>(telemetry, HttpStatus.OK);
    }

    @GetMapping("/{id}/statistics")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<Map<String, Object>> getVehicleStatistics(
            @PathVariable Long id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> statistics = vehicleService.getVehicleStatistics(id, startDate, endDate);
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }
}
