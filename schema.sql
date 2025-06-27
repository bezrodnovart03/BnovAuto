-- Создание расширений TimescaleDB и PostGIS
CREATE EXTENSION IF NOT EXISTS timescaledb;
CREATE EXTENSION IF NOT EXISTS postgis;

-- Создание схемы для пользователей и аутентификации
CREATE SCHEMA IF NOT EXISTS auth;

-- Таблица ролей
CREATE TABLE auth.roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    description TEXT
);

-- Таблица компаний/предприятий
CREATE TABLE auth.companies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address TEXT,
    contact_person VARCHAR(100),
    contact_email VARCHAR(100),
    contact_phone VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Таблица пользователей
CREATE TABLE auth.users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    company_id INTEGER REFERENCES auth.companies(id),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Таблица связи пользователей и ролей (многие ко многим)
CREATE TABLE auth.user_roles (
    user_id INTEGER NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES auth.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Создание схемы для транспортных средств и мониторинга
CREATE SCHEMA IF NOT EXISTS fleet;

-- Таблица типов транспортных средств
CREATE TABLE fleet.vehicle_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    max_weight NUMERIC(10, 2), -- в кг
    max_capacity INTEGER, -- количество автомобилей
    dimensions_length NUMERIC(10, 2), -- в метрах
    dimensions_width NUMERIC(10, 2), -- в метрах
    dimensions_height NUMERIC(10, 2) -- в метрах
);

-- Таблица транспортных средств
CREATE TABLE fleet.vehicles (
    id SERIAL PRIMARY KEY,
    vin VARCHAR(17) NOT NULL UNIQUE,
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    year INTEGER NOT NULL,
    color VARCHAR(30),
    weight NUMERIC(10, 2), -- в кг
    company_id INTEGER REFERENCES auth.companies(id),
    vehicle_type_id INTEGER REFERENCES fleet.vehicle_types(id),
    status VARCHAR(20) NOT NULL DEFAULT 'available', -- available, in_transit, maintenance, inactive
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Таблица водителей (связь с пользователями)
CREATE TABLE fleet.drivers (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES auth.users(id),
    license_number VARCHAR(50) NOT NULL,
    license_expiry DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active', -- active, inactive, on_leave
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Таблица маршрутов
CREATE TABLE fleet.routes (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    origin VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    distance NUMERIC(10, 2), -- в км
    estimated_time NUMERIC(5, 2), -- в часах
    company_id INTEGER REFERENCES auth.companies(id),
    status VARCHAR(20) NOT NULL DEFAULT 'planned', -- planned, in_progress, completed, cancelled
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Таблица геометрии маршрутов (PostGIS)
CREATE TABLE fleet.route_geometries (
    id SERIAL PRIMARY KEY,
    route_id INTEGER NOT NULL REFERENCES fleet.routes(id) ON DELETE CASCADE,
    geometry GEOMETRY(LINESTRING, 4326) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Таблица назначений транспорта на маршруты
CREATE TABLE fleet.route_assignments (
    id SERIAL PRIMARY KEY,
    route_id INTEGER NOT NULL REFERENCES fleet.routes(id),
    vehicle_id INTEGER NOT NULL REFERENCES fleet.vehicles(id),
    driver_id INTEGER REFERENCES fleet.drivers(id),
    departure_date TIMESTAMP WITH TIME ZONE NOT NULL,
    arrival_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'assigned', -- assigned, in_progress, completed, cancelled
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Создание гипертаблицы для телеметрии транспорта (TimescaleDB)
CREATE TABLE fleet.vehicle_telemetry (
    time TIMESTAMP WITH TIME ZONE NOT NULL,
    vehicle_id INTEGER NOT NULL REFERENCES fleet.vehicles(id),
    route_assignment_id INTEGER REFERENCES fleet.route_assignments(id),
    location GEOGRAPHY(POINT, 4326) NOT NULL, -- PostGIS географическая точка
    speed NUMERIC(5, 2), -- в км/ч
    fuel_level NUMERIC(5, 2), -- в процентах
    engine_temperature NUMERIC(5, 2), -- в градусах Цельсия
    battery_voltage NUMERIC(5, 2), -- в вольтах
    engine_rpm INTEGER,
    odometer NUMERIC(10, 2), -- в км
    fuel_consumption NUMERIC(5, 2), -- л/100км
    status VARCHAR(20) -- running, idle, stopped, error
);

-- Преобразование в гипертаблицу TimescaleDB
SELECT create_hypertable('fleet.vehicle_telemetry', 'time');

-- Создание индексов для оптимизации запросов
CREATE INDEX idx_vehicle_telemetry_vehicle_id ON fleet.vehicle_telemetry(vehicle_id);
CREATE INDEX idx_vehicle_telemetry_route_assignment_id ON fleet.vehicle_telemetry(route_assignment_id);
CREATE INDEX idx_vehicle_telemetry_time_vehicle_id ON fleet.vehicle_telemetry(time, vehicle_id);
CREATE INDEX idx_vehicle_telemetry_location ON fleet.vehicle_telemetry USING GIST(location);

-- Таблица для событий транспорта
CREATE TABLE fleet.vehicle_events (
    id SERIAL PRIMARY KEY,
    time TIMESTAMP WITH TIME ZONE NOT NULL,
    vehicle_id INTEGER NOT NULL REFERENCES fleet.vehicles(id),
    route_assignment_id INTEGER REFERENCES fleet.route_assignments(id),
    event_type VARCHAR(50) NOT NULL, -- start, stop, refuel, maintenance, error, etc.
    description TEXT,
    location GEOGRAPHY(POINT, 4326),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Преобразование в гипертаблицу TimescaleDB
SELECT create_hypertable('fleet.vehicle_events', 'time');

-- Создание индексов для оптимизации запросов
CREATE INDEX idx_vehicle_events_vehicle_id ON fleet.vehicle_events(vehicle_id);
CREATE INDEX idx_vehicle_events_time_vehicle_id ON fleet.vehicle_events(time, vehicle_id);
CREATE INDEX idx_vehicle_events_event_type ON fleet.vehicle_events(event_type);

-- Таблица для заправок и расхода топлива
CREATE TABLE fleet.fuel_records (
    id SERIAL PRIMARY KEY,
    vehicle_id INTEGER NOT NULL REFERENCES fleet.vehicles(id),
    driver_id INTEGER REFERENCES fleet.drivers(id),
    time TIMESTAMP WITH TIME ZONE NOT NULL,
    fuel_amount NUMERIC(10, 2) NOT NULL, -- в литрах
    fuel_type VARCHAR(30) NOT NULL,
    cost NUMERIC(10, 2), -- в валюте
    location GEOGRAPHY(POINT, 4326),
    odometer NUMERIC(10, 2), -- в км
    is_refill BOOLEAN NOT NULL, -- true для заправки, false для слива/расхода
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Преобразование в гипертаблицу TimescaleDB
SELECT create_hypertable('fleet.fuel_records', 'time');

-- Создание индексов для оптимизации запросов
CREATE INDEX idx_fuel_records_vehicle_id ON fleet.fuel_records(vehicle_id);
CREATE INDEX idx_fuel_records_time_vehicle_id ON fleet.fuel_records(time, vehicle_id);

-- Создание схемы для поддержки
CREATE SCHEMA IF NOT EXISTS support;

-- Таблица для обращений в поддержку
CREATE TABLE support.issues (
    id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'open', -- open, in_progress, resolved, closed
    priority VARCHAR(20) NOT NULL DEFAULT 'medium', -- low, medium, high, critical
    reported_by INTEGER REFERENCES auth.users(id),
    assigned_to INTEGER REFERENCES auth.users(id),
    company_id INTEGER REFERENCES auth.companies(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Таблица для комментариев к обращениям
CREATE TABLE support.issue_comments (
    id SERIAL PRIMARY KEY,
    issue_id INTEGER NOT NULL REFERENCES support.issues(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES auth.users(id),
    comment TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Создание схемы для отчетов и аналитики
CREATE SCHEMA IF NOT EXISTS analytics;

-- Таблица для сохраненных отчетов
CREATE TABLE analytics.reports (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    report_type VARCHAR(50) NOT NULL,
    parameters JSONB,
    created_by INTEGER REFERENCES auth.users(id),
    company_id INTEGER REFERENCES auth.companies(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Вставка начальных данных для ролей
INSERT INTO auth.roles (name, description) VALUES
('ROLE_DRIVER', 'Водитель/транспорт - передает данные о машине, имеет доступ только к карте с маршрутом'),
('ROLE_DIRECTOR', 'Директор/предприятие - имеет полный доступ в приложении'),
('ROLE_SUPPORT', 'Поддержка - помогает при проблемах и имеет доступ к редактированию рабочих столов');

-- Создание материализованных представлений для аналитики
CREATE MATERIALIZED VIEW analytics.daily_vehicle_stats AS
SELECT
    time_bucket('1 day', time) AS day,
    vehicle_id,
    AVG(speed) AS avg_speed,
    MAX(speed) AS max_speed,
    MIN(fuel_level) AS min_fuel_level,
    MAX(fuel_level) AS max_fuel_level,
    AVG(fuel_level) AS avg_fuel_level,
    MAX(odometer) - MIN(odometer) AS distance_traveled,
    AVG(fuel_consumption) AS avg_fuel_consumption
FROM
    fleet.vehicle_telemetry
GROUP BY
    day, vehicle_id;

-- Создание индекса для оптимизации запросов к материализованному представлению
CREATE INDEX idx_daily_vehicle_stats_day_vehicle ON analytics.daily_vehicle_stats(day, vehicle_id);

-- Создание функции для обновления материализованного представления
CREATE OR REPLACE FUNCTION refresh_daily_vehicle_stats()
RETURNS VOID AS $$
BEGIN
    REFRESH MATERIALIZED VIEW analytics.daily_vehicle_stats;
END;
$$ LANGUAGE plpgsql;

-- Создание триггерной функции для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Создание триггеров для автоматического обновления updated_at
CREATE TRIGGER update_auth_users_timestamp
BEFORE UPDATE ON auth.users
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER update_auth_companies_timestamp
BEFORE UPDATE ON auth.companies
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER update_fleet_vehicles_timestamp
BEFORE UPDATE ON fleet.vehicles
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER update_fleet_drivers_timestamp
BEFORE UPDATE ON fleet.drivers
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER update_fleet_routes_timestamp
BEFORE UPDATE ON fleet.routes
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER update_fleet_route_assignments_timestamp
BEFORE UPDATE ON fleet.route_assignments
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER update_support_issues_timestamp
BEFORE UPDATE ON support.issues
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER update_analytics_reports_timestamp
BEFORE UPDATE ON analytics.reports
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- Создание функции для расчета расстояния между точками маршрута
CREATE OR REPLACE FUNCTION fleet.calculate_route_distance(route_id INTEGER)
RETURNS NUMERIC AS $$
DECLARE
    distance NUMERIC;
BEGIN
    SELECT ST_Length(geometry::geography)/1000 INTO distance
    FROM fleet.route_geometries
    WHERE route_id = $1;
    
    RETURN distance;
END;
$$ LANGUAGE plpgsql;

-- Создание функции для проверки выхода транспорта за пределы маршрута
CREATE OR REPLACE FUNCTION fleet.check_vehicle_route_deviation(vehicle_id INTEGER, route_id INTEGER, threshold_meters NUMERIC DEFAULT 100)
RETURNS BOOLEAN AS $$
DECLARE
    is_deviation BOOLEAN;
    route_geom GEOMETRY;
    vehicle_point GEOMETRY;
BEGIN
    -- Получаем геометрию маршрута
    SELECT geometry INTO route_geom
    FROM fleet.route_geometries
    WHERE route_id = $1;
    
    -- Получаем последнюю известную позицию транспорта
    SELECT ST_SetSRID(ST_MakePoint(ST_X(location::geometry), ST_Y(location::geometry)), 4326) INTO vehicle_point
    FROM fleet.vehicle_telemetry
    WHERE vehicle_id = $1
    ORDER BY time DESC
    LIMIT 1;
    
    -- Проверяем, находится ли точка в пределах буфера маршрута
    SELECT ST_Distance(vehicle_point::geography, route_geom::geography) > threshold_meters INTO is_deviation;
    
    RETURN is_deviation;
END;
$$ LANGUAGE plpgsql;

-- Создание представления для текущего местоположения всех транспортных средств
CREATE OR REPLACE VIEW fleet.current_vehicle_locations AS
SELECT DISTINCT ON (vehicle_id)
    vehicle_id,
    time,
    location,
    speed,
    fuel_level,
    status
FROM
    fleet.vehicle_telemetry
ORDER BY
    vehicle_id, time DESC;

-- Создание представления для статистики по маршрутам
CREATE OR REPLACE VIEW analytics.route_statistics AS
SELECT
    r.id AS route_id,
    r.name AS route_name,
    r.origin,
    r.destination,
    r.distance,
    r.estimated_time,
    COUNT(ra.id) AS total_assignments,
    SUM(CASE WHEN ra.status = 'completed' THEN 1 ELSE 0 END) AS completed_assignments,
    AVG(EXTRACT(EPOCH FROM (ra.arrival_date - ra.departure_date))/3600) AS avg_actual_time,
    MIN(EXTRACT(EPOCH FROM (ra.arrival_date - ra.departure_date))/3600) AS min_actual_time,
    MAX(EXTRACT(EPOCH FROM (ra.arrival_date - ra.departure_date))/3600) AS max_actual_time
FROM
    fleet.routes r
LEFT JOIN
    fleet.route_assignments ra ON r.id = ra.route_id
WHERE
    ra.status = 'completed'
GROUP BY
    r.id, r.name, r.origin, r.destination, r.distance, r.estimated_time;

-- Создание представления для статистики по транспортным средствам
CREATE OR REPLACE VIEW analytics.vehicle_statistics AS
SELECT
    v.id AS vehicle_id,
    v.make,
    v.model,
    v.vin,
    COUNT(ra.id) AS total_assignments,
    SUM(CASE WHEN ra.status = 'completed' THEN 1 ELSE 0 END) AS completed_assignments,
    AVG(dvs.avg_speed) AS avg_speed,
    AVG(dvs.avg_fuel_consumption) AS avg_fuel_consumption,
    SUM(dvs.distance_traveled) AS total_distance_traveled,
    COUNT(DISTINCT fe.id) AS total_errors
FROM
    fleet.vehicles v
LEFT JOIN
    fleet.route_assignments ra ON v.id = ra.vehicle_id
LEFT JOIN
    analytics.daily_vehicle_stats dvs ON v.id = dvs.vehicle_id
LEFT JOIN
    fleet.vehicle_events fe ON v.id = fe.vehicle_id AND fe.event_type = 'error'
GROUP BY
    v.id, v.make, v.model, v.vin;

-- Создание представления для статистики по водителям
CREATE OR REPLACE VIEW analytics.driver_statistics AS
SELECT
    d.id AS driver_id,
    u.first_name,
    u.last_name,
    d.license_number,
    COUNT(ra.id) AS total_assignments,
    SUM(CASE WHEN ra.status = 'completed' THEN 1 ELSE 0 END) AS completed_assignments,
    AVG(EXTRACT(EPOCH FROM (ra.arrival_date - ra.departure_date))/3600) AS avg_trip_time,
    COUNT(DISTINCT fe.id) AS total_incidents
FROM
    fleet.drivers d
JOIN
    auth.users u ON d.user_id = u.id
LEFT JOIN
    fleet.route_assignments ra ON d.id = ra.driver_id
LEFT JOIN
    fleet.vehicle_events fe ON ra.id = fe.route_assignment_id AND fe.event_type IN ('error', 'violation')
GROUP BY
    d.id, u.first_name, u.last_name, d.license_number;

-- Создание представления для статистики по компаниям
CREATE OR REPLACE VIEW analytics.company_statistics AS
SELECT
    c.id AS company_id,
    c.name AS company_name,
    COUNT(DISTINCT v.id) AS total_vehicles,
    COUNT(DISTINCT d.id) AS total_drivers,
    COUNT(DISTINCT r.id) AS total_routes,
    COUNT(DISTINCT ra.id) AS total_assignments,
    SUM(CASE WHEN ra.status = 'completed' THEN 1 ELSE 0 END) AS completed_assignments,
    AVG(dvs.avg_fuel_consumption) AS avg_fleet_fuel_consumption,
    SUM(dvs.distance_traveled) AS total_distance_traveled
FROM
    auth.companies c
LEFT JOIN
    fleet.vehicles v ON c.id = v.company_id
LEFT JOIN
    fleet.drivers d ON c.id = (SELECT company_id FROM auth.users WHERE id = d.user_id)
LEFT JOIN
    fleet.routes r ON c.id = r.company_id
LEFT JOIN
    fleet.route_assignments ra ON r.id = ra.route_id
LEFT JOIN
    analytics.daily_vehicle_stats dvs ON v.id = dvs.vehicle_id
GROUP BY
    c.id, c.name;

-- Создание функции для генерации отчета по расходу топлива
CREATE OR REPLACE FUNCTION analytics.generate_fuel_consumption_report(
    company_id INTEGER,
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE
)
RETURNS TABLE (
    vehicle_id INTEGER,
    make VARCHAR,
    model VARCHAR,
    vin VARCHAR,
    total_distance NUMERIC,
    total_fuel_consumed NUMERIC,
    avg_consumption NUMERIC,
    total_refills INTEGER,
    total_refill_amount NUMERIC,
    total_refill_cost NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        v.id AS vehicle_id,
        v.make,
        v.model,
        v.vin,
        COALESCE(MAX(vt.odometer) - MIN(vt.odometer), 0) AS total_distance,
        COALESCE(SUM(CASE WHEN fr.is_refill = false THEN fr.fuel_amount ELSE 0 END), 0) AS total_fuel_consumed,
        CASE
            WHEN COALESCE(MAX(vt.odometer) - MIN(vt.odometer), 0) > 0 THEN
                COALESCE(SUM(CASE WHEN fr.is_refill = false THEN fr.fuel_amount ELSE 0 END), 0) / 
                (COALESCE(MAX(vt.odometer) - MIN(vt.odometer), 0) / 100)
            ELSE 0
        END AS avg_consumption,
        COUNT(CASE WHEN fr.is_refill = true THEN 1 ELSE NULL END) AS total_refills,
        COALESCE(SUM(CASE WHEN fr.is_refill = true THEN fr.fuel_amount ELSE 0 END), 0) AS total_refill_amount,
        COALESCE(SUM(CASE WHEN fr.is_refill = true THEN fr.cost ELSE 0 END), 0) AS total_refill_cost
    FROM
        fleet.vehicles v
    LEFT JOIN
        fleet.vehicle_telemetry vt ON v.id = vt.vehicle_id AND vt.time BETWEEN start_date AND end_date
    LEFT JOIN
        fleet.fuel_records fr ON v.id = fr.vehicle_id AND fr.time BETWEEN start_date AND end_date
    WHERE
        v.company_id = company_id
    GROUP BY
        v.id, v.make, v.model, v.vin;
END;
$$ LANGUAGE plpgsql;

-- Создание функции для генерации отчета по эффективности маршрутов
CREATE OR REPLACE FUNCTION analytics.generate_route_efficiency_report(
    company_id INTEGER,
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE
)
RETURNS TABLE (
    route_id INTEGER,
    route_name VARCHAR,
    origin VARCHAR,
    destination VARCHAR,
    planned_distance NUMERIC,
    avg_actual_distance NUMERIC,
    distance_deviation_percent NUMERIC,
    planned_time NUMERIC,
    avg_actual_time NUMERIC,
    time_deviation_percent NUMERIC,
    completion_rate NUMERIC,
    avg_fuel_consumption NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        r.id AS route_id,
        r.name AS route_name,
        r.origin,
        r.destination,
        r.distance AS planned_distance,
        AVG(COALESCE(MAX(vt.odometer) - MIN(vt.odometer), 0)) AS avg_actual_distance,
        CASE
            WHEN r.distance > 0 THEN
                (AVG(COALESCE(MAX(vt.odometer) - MIN(vt.odometer), 0)) - r.distance) / r.distance * 100
            ELSE 0
        END AS distance_deviation_percent,
        r.estimated_time AS planned_time,
        AVG(EXTRACT(EPOCH FROM (ra.arrival_date - ra.departure_date))/3600) AS avg_actual_time,
        CASE
            WHEN r.estimated_time > 0 THEN
                (AVG(EXTRACT(EPOCH FROM (ra.arrival_date - ra.departure_date))/3600) - r.estimated_time) / r.estimated_time * 100
            ELSE 0
        END AS time_deviation_percent,
        COALESCE(SUM(CASE WHEN ra.status = 'completed' THEN 1 ELSE 0 END)::NUMERIC / NULLIF(COUNT(ra.id), 0), 0) * 100 AS completion_rate,
        AVG(dvs.avg_fuel_consumption) AS avg_fuel_consumption
    FROM
        fleet.routes r
    LEFT JOIN
        fleet.route_assignments ra ON r.id = ra.route_id AND ra.departure_date BETWEEN start_date AND end_date
    LEFT JOIN
        fleet.vehicle_telemetry vt ON ra.vehicle_id = vt.vehicle_id AND vt.time BETWEEN ra.departure_date AND COALESCE(ra.arrival_date, end_date)
    LEFT JOIN
        analytics.daily_vehicle_stats dvs ON ra.vehicle_id = dvs.vehicle_id AND dvs.day BETWEEN start_date AND end_date
    WHERE
        r.company_id = company_id
    GROUP BY
        r.id, r.name, r.origin, r.destination, r.distance, r.estimated_time;
END;
$$ LANGUAGE plpgsql;

-- Создание функции для генерации отчета по нарушениям
CREATE OR REPLACE FUNCTION analytics.generate_violations_report(
    company_id INTEGER,
    start_date TIMESTAMP WITH TIME ZONE,
    end_date TIMESTAMP WITH TIME ZONE
)
RETURNS TABLE (
    vehicle_id INTEGER,
    driver_id INTEGER,
    driver_name TEXT,
    route_id INTEGER,
    route_name VARCHAR,
    violation_count INTEGER,
    speeding_violations INTEGER,
    route_deviation_violations INTEGER,
    unauthorized_stops INTEGER,
    other_violations INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        v.id AS vehicle_id,
        d.id AS driver_id,
        CONCAT(u.first_name, ' ', u.last_name) AS driver_name,
        r.id AS route_id,
        r.name AS route_name,
        COUNT(ve.id) AS violation_count,
        SUM(CASE WHEN ve.event_type = 'speeding' THEN 1 ELSE 0 END) AS speeding_violations,
        SUM(CASE WHEN ve.event_type = 'route_deviation' THEN 1 ELSE 0 END) AS route_deviation_violations,
        SUM(CASE WHEN ve.event_type = 'unauthorized_stop' THEN 1 ELSE 0 END) AS unauthorized_stops,
        SUM(CASE WHEN ve.event_type NOT IN ('speeding', 'route_deviation', 'unauthorized_stop') THEN 1 ELSE 0 END) AS other_violations
    FROM
        fleet.vehicles v
    JOIN
        fleet.route_assignments ra ON v.id = ra.vehicle_id
    JOIN
        fleet.routes r ON ra.route_id = r.id
    LEFT JOIN
        fleet.drivers d ON ra.driver_id = d.id
    LEFT JOIN
        auth.users u ON d.user_id = u.id
    LEFT JOIN
        fleet.vehicle_events ve ON v.id = ve.vehicle_id AND ve.time BETWEEN start_date AND end_date AND ve.event_type IN ('speeding', 'route_deviation', 'unauthorized_stop', 'violation')
    WHERE
        v.company_id = company_id AND
        ra.departure_date BETWEEN start_date AND end_date
    GROUP BY
        v.id, d.id, driver_name, r.id, r.name;
END;
$$ LANGUAGE plpgsql;
