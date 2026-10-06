-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Create H3 cells table with PostGIS geometry
CREATE TABLE IF NOT EXISTS h3_cells_l8 (
    id BIGSERIAL PRIMARY KEY,
    h3_index BIGINT NOT NULL UNIQUE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    elevation DOUBLE PRECISION NOT NULL DEFAULT 0,
    temperature DOUBLE PRECISION NOT NULL DEFAULT 15,
    rainfall DOUBLE PRECISION NOT NULL DEFAULT 500,
    biome VARCHAR(20) NOT NULL DEFAULT 'PLAINS',
    population INTEGER NOT NULL DEFAULT 0,
    food_resource DOUBLE PRECISION NOT NULL DEFAULT 0,
    water_resource DOUBLE PRECISION NOT NULL DEFAULT 0,
    wood_resource DOUBLE PRECISION NOT NULL DEFAULT 0,
    geometry GEOMETRY(POINT, 4326),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_h3_index ON h3_cells_l8(h3_index);
CREATE INDEX IF NOT EXISTS idx_lat_lng ON h3_cells_l8(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_biome ON h3_cells_l8(biome);
CREATE INDEX IF NOT EXISTS idx_geometry ON h3_cells_l8 USING GIST(geometry);

-- Create aggregation tables for coarser resolutions
CREATE TABLE IF NOT EXISTS h3_cells_l7 (
    id BIGSERIAL PRIMARY KEY,
    h3_index BIGINT NOT NULL UNIQUE,
    mean_elevation DOUBLE PRECISION,
    total_population INTEGER,
    dominant_biome VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS h3_cells_l4 (
    id BIGSERIAL PRIMARY KEY,
    h3_index BIGINT NOT NULL UNIQUE,
    mean_elevation DOUBLE PRECISION,
    total_population INTEGER,
    dominant_biome VARCHAR(20)
);

-- Climate history table
CREATE TABLE IF NOT EXISTS climate_history (
    id BIGSERIAL PRIMARY KEY,
    year INTEGER NOT NULL,
    h3_index BIGINT NOT NULL,
    temperature DOUBLE PRECISION,
    rainfall DOUBLE PRECISION,
    UNIQUE(year, h3_index)
);

CREATE INDEX IF NOT EXISTS idx_climate_year ON climate_history(year);
CREATE INDEX IF NOT EXISTS idx_climate_h3 ON climate_history(h3_index);

-- Update trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_h3_cells_l8_updated_at BEFORE UPDATE ON h3_cells_l8
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
