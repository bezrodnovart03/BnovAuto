package com.bnovauto.api.controller;

import com.bnovauto.api.model.Route;
import com.bnovauto.api.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @GetMapping
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<List<Route>> getAllRoutes() {
        List<Route> routes = routeService.getAllRoutes();
        return new ResponseEntity<>(routes, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT') or hasRole('DRIVER')")
    public ResponseEntity<Route> getRouteById(@PathVariable Long id) {
        Route route = routeService.getRouteById(id);
        return new ResponseEntity<>(route, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<Route> createRoute(@RequestBody Route route) {
        Route newRoute = routeService.createRoute(route);
        return new ResponseEntity<>(newRoute, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<Route> updateRoute(@PathVariable Long id, @RequestBody Route routeDetails) {
        Route updatedRoute = routeService.updateRoute(id, routeDetails);
        return new ResponseEntity<>(updatedRoute, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<HttpStatus> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<List<Route>> getRoutesByCompany(@PathVariable Long companyId) {
        List<Route> routes = routeService.getRoutesByCompany(companyId);
        return new ResponseEntity<>(routes, HttpStatus.OK);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<Map<String, Object>> assignRoute(
            @PathVariable Long id,
            @RequestBody Map<String, Object> assignmentDetails) {
        Map<String, Object> result = routeService.assignRoute(id, assignmentDetails);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT') or hasRole('DRIVER')")
    public ResponseEntity<List<Route>> getRoutesByVehicle(@PathVariable Long vehicleId) {
        List<Route> routes = routeService.getRoutesByVehicle(vehicleId);
        return new ResponseEntity<>(routes, HttpStatus.OK);
    }

    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT') or @userSecurity.isCurrentUser(#driverId)")
    public ResponseEntity<List<Route>> getRoutesByDriver(@PathVariable Long driverId) {
        List<Route> routes = routeService.getRoutesByDriver(driverId);
        return new ResponseEntity<>(routes, HttpStatus.OK);
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<List<Route>> getActiveRoutes() {
        List<Route> routes = routeService.getActiveRoutes();
        return new ResponseEntity<>(routes, HttpStatus.OK);
    }
}
