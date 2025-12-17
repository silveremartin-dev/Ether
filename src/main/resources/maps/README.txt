# Custom Map Import

Place your map images in this folder to be loaded by the "Real Data" option in Ether.

## Requirements
- Projection: Equirectangular (Plate Carrée)
- Format: PNG, JPG

## Files
1. **Elevation Map**: `earth_elevation.png`
   - Grayscale
   - Black = -10,000m (Deep Ocean)
   - 50% Gray = 0m (Sea Level)
   - White = +10,000m (High Peaks)

2. **Biome Map**: `earth_biomes.png`
   - Use the following colors to define biomes:
   - Deep Ocean: RGB(0, 0, 100)
   - Ocean: RGB(0, 50, 200)
   - Beach: RGB(240, 220, 150)
   - Plains: RGB(100, 200, 50)
   - Forest: RGB(20, 120, 20)
   - Jungle: RGB(0, 80, 0)
   - Desert: RGB(255, 200, 50)
   - Hills: RGB(150, 150, 100)
   - Mountains: RGB(100, 100, 100)
   - Tundra: RGB(150, 200, 220)
   - Snow: RGB(255, 255, 255)
