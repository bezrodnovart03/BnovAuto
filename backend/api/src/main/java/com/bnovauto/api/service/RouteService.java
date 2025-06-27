package com.bnovauto.api.service;

import com.bnovauto.api.model.Company;
import com.bnovauto.api.model.Route;
import com.bnovauto.api.repository.CompanyRepository;
import com.bnovauto.api.repository.RouteRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final CompanyRepository companyRepository;
    private final GeometryFactory geometryFactory;

    @Autowired
    public RouteService(RouteRepository routeRepository, CompanyRepository companyRepository) {
        this.routeRepository = routeRepository;
        this.companyRepository = companyRepository;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    }

    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }

    public Optional<Route> getRouteById(Long id) {
        return routeRepository.findById(id);
    }

    public List<Route> getRoutesByCompanyId(Long companyId) {
        return routeRepository.findByCompanyId(companyId);
    }

    public Route createRoute(Route route) {
        // Проверка существования компании
        if (route.getCompany() != null && route.getCompany().getId() != null) {
            Optional<Company> companyOptional = companyRepository.findById(route.getCompany().getId());
            if (!companyOptional.isPresent()) {
                throw new IllegalArgumentException("Company not found with id: " + route.getCompany().getId());
            }
            route.setCompany(companyOptional.get());
        }
        
        return routeRepository.save(route);
    }

    public Route updateRoute(Long id, Route routeDetails) {
        Optional<Route> routeOptional = routeRepository.findById(id);
        if (!routeOptional.isPresent()) {
            throw new IllegalArgumentException("Route not found with id: " + id);
        }

        Route route = routeOptional.get();
        
        // Обновление основных полей
        route.setName(routeDetails.getName());
        
        // Проверка и обновление компании
        if (routeDetails.getCompany() != null && routeDetails.getCompany().getId() != null) {
            if (route.getCompany() == null || !route.getCompany().getId().equals(routeDetails.getCompany().getId())) {
                Optional<Company> companyOptional = companyRepository.findById(routeDetails.getCompany().getId());
                if (!companyOptional.isPresent()) {
                    throw new IllegalArgumentException("Company not found with id: " + routeDetails.getCompany().getId());
                }
                route.setCompany(companyOptional.get());
            }
        }
        
        // Обновление геометрических данных
        if (routeDetails.getStartPoint() != null) {
            route.setStartPoint(routeDetails.getStartPoint());
        }
        
        if (routeDetails.getEndPoint() != null) {
            route.setEndPoint(routeDetails.getEndPoint());
        }
        
        if (routeDetails.getPath() != null) {
            route.setPath(routeDetails.getPath());
        }
        
        return routeRepository.save(route);
    }

    public void deleteRoute(Long id) {
        routeRepository.deleteById(id);
    }

    /**
     * Создает новый маршрут между двумя точками
     * 
     * @param name Название маршрута
     * @param companyId ID компании
     * @param startLat Широта начальной точки
     * @param startLng Долгота начальной точки
     * @param endLat Широта конечной точки
     * @param endLng Долгота конечной точки
     * @param waypoints Список промежуточных точек в формате [lat1,lng1,lat2,lng2,...]
     * @return Созданный маршрут
     */
    public Route createRouteBetweenPoints(String name, Long companyId, 
                                        double startLat, double startLng, 
                                        double endLat, double endLng, 
                                        List<Double> waypoints) {
        
        // Проверка существования компании
        Optional<Company> companyOptional = companyRepository.findById(companyId);
        if (!companyOptional.isPresent()) {
            throw new IllegalArgumentException("Company not found with id: " + companyId);
        }
        Company company = companyOptional.get();
        
        // Создание начальной и конечной точек
        Point startPoint = geometryFactory.createPoint(new Coordinate(startLng, startLat));
        Point endPoint = geometryFactory.createPoint(new Coordinate(endLng, endLat));
        
        // Создание списка координат для линии маршрута
        List<Coordinate> coordinates = new ArrayList<>();
        coordinates.add(new Coordinate(startLng, startLat));
        
        // Добавление промежуточных точек
        if (waypoints != null && waypoints.size() >= 2 && waypoints.size() % 2 == 0) {
            for (int i = 0; i < waypoints.size(); i += 2) {
                coordinates.add(new Coordinate(waypoints.get(i+1), waypoints.get(i)));
            }
        }
        
        coordinates.add(new Coordinate(endLng, endLat));
        
        // Создание линии маршрута
        LineString path = geometryFactory.createLineString(coordinates.toArray(new Coordinate[0]));
        
        // Создание и сохранение маршрута
        Route route = new Route();
        route.setName(name);
        route.setCompany(company);
        route.setStartPoint(startPoint);
        route.setEndPoint(endPoint);
        route.setPath(path);
        
        return routeRepository.save(route);
    }
    
}
