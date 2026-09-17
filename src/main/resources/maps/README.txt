# Ether Cartographic Specifications & Map Repositories

This directory contains the reference rasters for all planetary presets (Earth, Moon, Mars, Venus, Mercury).

## Standard Projection & Resolution
- **Projection**: Equirectangular (Plate Carrée, EPSG:4326)
- **Aspect Ratio**: 2:1 (Standard: 2048 x 1024 px)
- **Format**: PNG (RGB or RGBA 8-bit per channel)

## 1. Elevation Maps (`<body_key>_elevation.png`)
Rendered with unified hypsometric/bathymetric false colors across all planetary bodies:
- Deep Trenches / Low Basins: Deep Navy (#0F172A to #1E3A8A)
- Continental Shelves / Lowlands: Cyan / Teal to Emerald Green (#065F46 to #10B981)
- Mid-altitudes / Plains: Light Green to Golden Amber (#84CC16 to #F59E0B)
- High Plateaus / Volcanic Highlands: Rust / Warm Terracotta (#B45309 to #DC2626)
- Extreme Peaks / Mons / Himalayas: Snow White / Bright Core (#F8FAFC)

## 2. Discrete Polychrome Biomes (`<body_key>_biomes.png`)
Exact categorical RGB mapping:
- Deep Ocean: RGB(0, 0, 100)
- Ocean / Shelf: RGB(0, 50, 200)
- Beach / Littoral: RGB(240, 220, 150)
- Plains / Grasslands: RGB(100, 200, 50)
- Forest (Temperate / Boreal): RGB(20, 120, 20)
- Jungle / Tropical Rainforest: RGB(0, 80, 0)
- Desert / Regolith: RGB(255, 200, 50)
- Hills / Badlands: RGB(150, 150, 100)
- Mountains / Rugged Uplands: RGB(100, 100, 100)
- Tundra: RGB(150, 200, 220)
- Snow / Glaciers / Polar Ice: RGB(255, 255, 255)

## 3. Climate & Atmospheric Tensors
- **Temperature** (`<body_key>_temperature.png`): Thermal colormap (-50°C blue to +50°C red).
- **Precipitation** (`<body_key>_precipitation.png`): Moisture gradient (0 mm/yr dry to 3000+ mm/yr dark blue/white).
- **Seasonality** (`<body_key>_seasonality.png`): Annual thermal variance.

## 4. Geological & Energy Resources (Unified Thematic Color Scheme)
- **Coal**: Amber / Warm Ochre (#92400E -> #FBBF24)
- **Crude Oil**: Ruby / Crimson (#991B1B -> #F87171)
- **Natural Gas**: Cyan / Sky Blue (#0E7490 -> #38BDF8)
- **Uranium / Thorium**: Emerald Green (#22C55E)
- **Helium-3**: Electric Violet / Purple (#A855F7)
- **Iron & Copper**: Rust / Copper Orange (#8B4513 -> #F97316)
- **Precious Metals & REE**: Gold / Radiant Yellow (#EAB308)
- **Geothermal / Mantle Heat**: Incandescent Orange-Red (#EF4444)
- **Aquifers / Subsurface Ice**: Azure Blue (#3B82F6)

