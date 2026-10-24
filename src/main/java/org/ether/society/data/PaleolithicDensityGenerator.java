package org.ether.society.data;

import java.awt.image.BufferedImage;
import static org.ether.society.data.PaleoGeographyCoordinatesCatalog.*;

/**
 * Generates high-fidelity archaeological and paleolithic synthetic population density fields
 * spanning deep prehistory from -100,000 BP through early agricultural transitions (-4,500 BP).
 *
 * <p>Integrates multi-clade hominin dispersal models (Homo sapiens, Neanderthal, Denisovan),
 * glacial refugia (LGM, Fennoscandian / Laurentide ice sheets), and archaeological hearths.</p>
 */
public final class PaleolithicDensityGenerator {

    private PaleolithicDensityGenerator() {
        // Utility class
    }

    /**
     * Apply prehistoric geographic mask.
     *
     * @param scenarioKey the scenario key parameter (String)
     * @param year the year parameter (long)
     * @param src the src parameter (BufferedImage)
     * @return the resulting computation or state reference
     */
    public static BufferedImage applyPrehistoricGeographicMask(String scenarioKey, long year, BufferedImage src) {

        if (src == null) return null;
        if (year >= -10000) return src;

        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage masked = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y + 0.5) * 180.0 / h;
            for (int x = 0; x < w; x++) {
                double lon = -180.0 + (x + 0.5) * 360.0 / w;
                int rgb = src.getRGB(x, y);

                double weight = HistoricalMapGenerator.getHomininOccupancyWeight(lon, lat, year);
                if (weight > 0.001 && HistoricalMapGenerator.isLand(lon, lat)) {
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    int lum = (int) ((0.299 * r + 0.587 * g + 0.114 * b) * weight);
                    lum = Math.clamp(lum, 0, 255);
                    masked.setRGB(x, y, (lum << 16) | (lum << 8) | lum);
                } else {
                    masked.setRGB(x, y, 0x000000); // Pure black for uninhabited regions & oceans
                }
            }
        }
        return masked;
    }

    /**
     * Generate prehistoric synthetic density map.
     *
     * @param year the year parameter (long)
     * @param type the type parameter (String)
     * @return the resulting computation or state reference
     */
    public static BufferedImage generatePrehistoricSyntheticDensityMap(long year, String type) {
        BufferedImage img = new BufferedImage(HistoricalMapGenerator.WIDTH, HistoricalMapGenerator.HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HistoricalMapGenerator.HEIGHT; y++) {
            double lat = 90.0 - (y + 0.5) / HistoricalMapGenerator.HEIGHT * 180.0;
            for (int x = 0; x < HistoricalMapGenerator.WIDTH; x++) {
                double lng = (x + 0.5) / HistoricalMapGenerator.WIDTH * 360.0 - 180.0;
                boolean isLand = HistoricalMapGenerator.isLand(lng, lat);
                double weight = HistoricalMapGenerator.getHomininOccupancyWeight(lng, lat, year);
                if (!isLand || weight <= 0.001) {
                    img.setRGB(x, y, 0x000000);
                    continue;
                }

                double dens = 0.0;
                if (year <= -85000) {
                    // -100,000 BP: Sapiens in Africa & Levant, Mousterian Neanderthals, Denisovans (hab/km2)
                    double densSapiens = 0.0006 +
                        0.0100 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 36.0, 0.0) / 300.0) +      // East African Rift
                        0.0080 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 22.0, -34.0) / 250.0) +    // South African Coast
                        0.0065 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 32.0, 26.0) / 250.0) +     // Nile Corridor
                        0.0045 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 10.0, 14.0) / 300.0) +     // Sahel / West Africa
                        0.0055 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.0, 31.5) / 200.0) +     // Levant
                        0.0040 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 50.0, 16.0) / 250.0);      // Southern Arabia

                    double densNeanderthal = 0.0005 +
                        0.0060 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 2.0, 44.0) / 250.0) +     // France / Cantabria
                        0.0050 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -4.0, 40.0) / 200.0) +    // Iberia
                        0.0045 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 16.0, 47.0) / 250.0) +    // Central Europe / Balkans
                        0.0035 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 40.0, 44.0) / 200.0) +    // Caucasus / Crimea
                        0.0035 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 44.0, 36.0) / 200.0) +    // Zagros
                        0.0025 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 85.0, 51.0) / 180.0);     // Altai

                    double densDenisovan = 0.0004 +
                        0.0055 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 112.0, 34.0) / 300.0) +   // Yellow River / North China
                        0.0045 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 110.0, 26.0) / 300.0) +   // South China
                        0.0040 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 105.0, -2.0) / 350.0) +   // Sundaland
                        0.0035 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 102.0, 16.0) / 250.0) +   // Indochina
                        0.0040 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 78.0, 20.0) / 300.0) +    // Indian Subcontinent
                        0.0020 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 102.0, 35.0) / 180.0);    // Tibetan Plateau

                    dens = HistoricalMapGenerator.blendPaleoTraits(lng, lat, densSapiens, densNeanderthal, densDenisovan);
                } else if (year <= -65000) {
                    // -74,000 BP (Toba Super-Eruption Bottleneck): Sapiens contracted to African Refugia, South Asian depopulation
                    double densSapiensRefugia = 0.0003 +
                        0.0065 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 22.1, -34.2) / 100.0) +    // Pinnacle Point / Mossel Bay coastal refuge
                        0.0055 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 24.0, -34.0) / 100.0) +    // Klasies River Mouth
                        0.0045 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 36.0, 0.5) / 140.0) +      // East African Equatorial Highlands
                        0.0030 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -2.4, 34.8) / 120.0) +     // Maghreb Taforalt refuge
                        0.0025 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.3, 32.7) / 100.0);     // Skhul / Qafzeh Levant contact

                    double densNeanderthalRefugia = 0.0002 +
                        0.0030 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -5.5, 36.1) / 120.0) +    // Gibraltar Gorham's Cave refuge
                        0.0025 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 1.5, 43.5) / 140.0) +     // Aquitaine / Pyrenees refuge
                        0.0020 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 15.0, 40.5) / 120.0) +    // Southern Italy / Mediterranean
                        0.0020 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 44.0, 36.0) / 120.0);     // Zagros Shanidar

                    double densDenisovanRefugia = 0.0002 +
                        0.0025 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 84.5, 51.4) / 100.0) +    // Denisova Cave Altai
                        0.0020 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 102.5, 35.5) / 120.0) +   // Baishiya Tibetan Plateau
                        0.0016 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 110.0, -7.5) / 140.0);    // Sundaland Ngandong

                    dens = HistoricalMapGenerator.blendPaleoTraits(lng, lat, densSapiensRefugia, densNeanderthalRefugia, densDenisovanRefugia);
                } else if (year <= -52000) {
                    // -60,000 to -55,000 BP (MIS 4/3): Out-of-Africa Indian Ocean Coastal Highway & Northern Sahul Pioneer Landfall
                    double densAfrica = 0.0010 +
                        0.0140 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 36.0, 0.5) / 300.0) +      // East Africa
                        0.0110 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 21.5, -34.0) / 250.0) +    // South Africa (Blombos/Klasies)
                        0.0090 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 32.5, 26.0) / 220.0) +     // Nile
                        0.0070 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 8.0, 12.0) / 250.0) +      // West Africa
                        0.0080 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -7.0, 32.0) / 220.0);      // Maghreb (Taforalt)

                    // Active Indian Ocean Southern Coastal Highway
                    double densIndianOceanHighway = 0.0010 +
                        0.0145 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 50.0, 15.0) / 120.0) +     // Southern Arabia (Jebel Faya / Dhofar)
                        0.0150 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 65.0, 25.0) / 140.0) +     // Makran / Indus Delta
                        0.0180 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 78.0, 15.0) / 180.0) +     // South India / Jwalapuram / Attirampakkam
                        0.0150 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 80.5, 7.5) / 120.0) +      // Sri Lanka Fa Hien
                        0.0140 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 90.0, 22.0) / 150.0) +     // Bengal Delta
                        0.0150 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 105.0, -2.0) / 200.0) +    // Sundaland (Niah, Sumatra, Java)
                        0.0120 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 120.0, -4.5) / 140.0);     // Wallacea / Sulawesi (Maros-Pangkep)

                    double densSahulNorth =
                        0.0120 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 132.9, -12.5) / 80.0) +    // Madjedbebe / Arnhem Land pioneer landfall
                        0.0100 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 125.0, -16.0) / 80.0);     // Kimberley Carpenter's Gap

                    double densEurasiaInland = 0.0006 +
                        0.0060 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.5, 32.5) / 140.0) +     // Levant
                        0.0050 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 2.0, 44.0) / 180.0) +      // Neanderthal Europe
                        0.0050 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 112.0, 32.0) / 200.0);     // Tianyuan China

                    dens = Math.max(densAfrica, Math.max(densIndianOceanHighway, Math.max(densSahulNorth, densEurasiaInland)));
                } else if (year <= -42000) {
                    // -50,000 BP (MIS 3): Full Sahul Radiation across interior & Initial Upper Paleolithic (IUP)
                    double densAfrica = 0.0018 +
                        0.0200 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 36.0, 0.5) / 300.0) +
                        0.0160 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 21.5, -34.0) / 220.0) +
                        0.0140 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 32.5, 26.0) / 200.0) +
                        0.0090 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 8.0, 12.0) / 250.0);

                    double densSahul = 0.0016 +
                        0.0180 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 132.9, -12.5) / 90.0) +   // Madjedbebe
                        0.0150 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 125.0, -16.0) / 100.0) +  // Kimberley
                        0.0130 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 115.3, -20.8) / 90.0) +   // Barrow Island Boodie Cave
                        0.0140 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 143.0, -33.7) / 110.0) +  // Lake Mungo / Willandra Lakes
                        0.0120 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 116.0, -32.0) / 100.0) +  // Swan River / Devil's Lair
                        0.0120 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 143.0, -5.5) / 110.0);    // New Guinea Highlands

                    double densEurasiaIUP = 0.0016 +
                        0.0140 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.5, 32.5) / 130.0) +     // Levant (Ksar Akil / Boker Tachtit)
                        0.0110 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 25.0, 43.0) / 140.0) +     // Bacho Kiro Balkans IUP
                        0.0100 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 1.5, 45.0) / 150.0) +      // Franco-Cantabrian
                        0.0160 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 78.0, 20.0) / 200.0) +     // India
                        0.0170 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 105.0, -2.0) / 220.0) +    // Sundaland
                        0.0110 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 112.0, 32.0) / 180.0) +    // Tianyuan China
                        0.0070 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 84.5, 51.4) / 120.0);      // Denisova Altai

                    dens = Math.max(densAfrica, Math.max(densSahul, densEurasiaIUP));
                } else if (year <= -32000) {
                    // -40,000 to -35,000 BP (Late MIS 3 / Aurignacian / Tasmania Settled)
                    double densEuropeAurignacian = 0.0026 +
                        0.0260 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 4.4, 44.4) / 80.0) +      // Chauvet Cave / Ardèche
                        0.0220 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 10.2, 48.6) / 80.0) +     // Vogelherd / Swabian Jura
                        0.0200 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 1.0, 45.0) / 90.0) +      // Dordogne Aurignacian
                        0.0180 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 24.5, 45.5) / 100.0);     // Peștera cu Oase Romania

                    double densSahulTasmania = 0.0024 +
                        0.0180 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 132.9, -12.5) / 90.0) +
                        0.0170 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 143.0, -33.7) / 110.0) +
                        0.0150 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 116.0, -32.0) / 100.0) +
                        0.0130 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 145.8, -42.5) / 70.0);    // Tasmania Warreen Cave / Parmerpar Meethaner

                    double densAsiaAfricaLateMIS3 = 0.0026 +
                        0.0200 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.5, 32.5) / 100.0) +
                        0.0200 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 78.0, 20.0) / 180.0) +
                        0.0200 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 105.0, -2.0) / 220.0) +
                        0.0170 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 114.0, 34.0) / 180.0) +
                        0.0220 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 36.0, 0.5) / 250.0);

                    dens = Math.max(densEuropeAurignacian, Math.max(densSahulTasmania, densAsiaAfricaLateMIS3));
                } else if (year <= -22000) {
                    // -25,000 BP: Gravettian Horizon, Beringian Standstill, Early Americas Pioneers
                    double densEurope = 0.0035 +
                        0.0380 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 16.5, 48.8) / 80.0) +     // Pavlovian / Dolní Věstonice / Willendorf
                        0.0340 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 1.0, 45.0) / 90.0) +      // Franco-Cantabrian Gravettian (Laugerie, Abri Pataud)
                        0.0300 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 39.0, 51.4) / 100.0) +    // Kostenki-Borshchevo / Don
                        0.0260 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 40.5, 56.2) / 80.0) +     // Sungir / Upper Volga
                        0.0240 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 15.5, 41.7) / 80.0) +     // Paglicci / Italian Gravettian
                        0.0200 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -7.5, 37.5) / 80.0);      // Vale Boi / Iberian Gravettian

                    double densBeringia = 0.0;
                    double dBeringia = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_BERINGIA_STEPPE_CORRIDOR);
                    if (dBeringia <= 5.0) {
                        densBeringia = 0.0180 * Math.exp(-dBeringia / 2.0);
                    }

                    double densAmericasPreLGM = 0.0;
                    double dPac = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_PACIFIC_KELP_HIGHWAY);
                    if (dPac <= 4.0) densAmericasPreLGM = Math.max(densAmericasPreLGM, 0.0150 * Math.exp(-dPac / 2.0));
                    densAmericasPreLGM = Math.max(densAmericasPreLGM,
                        0.0180 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -106.3, 32.8) / 40.0) + // White Sands precursor
                        0.0140 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -103.5, 24.2) / 40.0) + // Chiquihuite
                        0.0120 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -80.4, 40.3) / 40.0)    // Meadowcroft
                    );

                    double densAsiaAfrica = 0.0035 +
                        0.0280 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.5, 32.7) / 100.0) +     // Ohalo II / Early Epipaleolithic Levant
                        0.0220 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 32.0, 26.0) / 150.0) +     // Nile Valley
                        0.0180 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 22.0, -34.0) / 180.0) +    // South African LSA
                        0.0200 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 78.0, 22.0) / 180.0) +     // India
                        0.0220 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 115.0, 30.0) / 180.0) +    // Yangtze / South China
                        0.0160 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 135.0, -25.0) / 220.0);    // Sahul forager network

                    dens = Math.max(densEurope, Math.max(densBeringia, Math.max(densAmericasPreLGM, densAsiaAfrica)));
                } else if (year <= -15000) {
                    // -20,000 BP: LGM Paroxysm Refugia & White Sands Peak
                    double densSolutrean =
                        0.0450 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 0.5, 44.8) / 60.0) +    // Dordogne & Aquitaine Solutrean Core (Laugerie-Haute, Solutré)
                        0.0380 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -4.5, 43.4) / 50.0) +   // Cantabrian / Altamira Solutrean
                        0.0320 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -3.5, 38.0) / 60.0) +   // Iberian Mediterranean (Parpalló)
                        0.0280 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -8.5, 37.1) / 60.0);    // Portuguese Estremadura (Vale Almoinha)

                    double densMedEpigravettian =
                        0.0350 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 15.5, 41.7) / 60.0) +   // Grotta Paglicci / Italian Epigravettian
                        0.0280 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 23.0, 38.5) / 60.0);    // Franchthi / Greek refuge

                    double densEasternRefugia =
                        0.0300 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.0, 50.5) / 70.0) +   // Mezhirich / Dnepr mammoth bone settlements
                        0.0260 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 39.0, 51.4) / 70.0);    // Kostenki

                    double densLevantAfrica = 0.0042 +
                        0.0450 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.5, 32.7) / 60.0) +      // Kebaran Levant (Ohalo II / Sea of Galilee)
                        0.0320 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 32.5, 25.5) / 120.0) +     // Nile valley (Wadi Kubbaniya)
                        0.0180 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 22.0, -34.0) / 150.0) +    // South Africa
                        0.0180 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 80.0, 22.0) / 180.0) +     // India
                        0.0220 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 114.0, 28.0) / 180.0);     // South China

                    double densAmericasLGM = 0.0;
                    double dPac = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_PACIFIC_KELP_HIGHWAY);
                    if (dPac <= 4.0) densAmericasLGM = Math.max(densAmericasLGM, 0.0180 * Math.exp(-dPac / 2.0));
                    double dAndes = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_ANDES_CORRIDOR);
                    if (dAndes <= 3.5) densAmericasLGM = Math.max(densAmericasLGM, 0.0150 * Math.exp(-dAndes / 2.0));
                    double dAmazon = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_AMAZON_MAINSTEM);
                    if (dAmazon <= 3.5) densAmericasLGM = Math.max(densAmericasLGM, 0.0130 * Math.exp(-dAmazon / 2.2));
                    double dSavanna = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_BRAZIL_SAVANNA);
                    if (dSavanna <= 3.5) densAmericasLGM = Math.max(densAmericasLGM, 0.0140 * Math.exp(-dSavanna / 2.2));

                    densAmericasLGM = Math.max(densAmericasLGM,
                        0.0220 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -106.3, 32.8) / 35.0) + // White Sands trackways (attested 23k-21k BP)
                        0.0150 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -77.3, 36.8) / 35.0) +   // Cactus Hill
                        0.0150 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -83.9, 30.1) / 35.0)    // Page-Ladson
                    );

                    double densBeringiaLGM = 0.0;
                    double dBeringia = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_BERINGIA_STEPPE_CORRIDOR);
                    if (dBeringia <= 5.0) {
                        densBeringiaLGM = 0.0160 * Math.exp(-dBeringia / 2.0);
                    }

                    double densSahulLGM =
                        0.0220 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 132.9, -12.5) / 100.0) + // Madjedbebe
                        0.0200 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 143.0, -33.7) / 120.0) + // Lake Mungo
                        0.0160 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 145.8, -42.5) / 70.0);   // Tasmania

                    dens = Math.max(densSolutrean, Math.max(densMedEpigravettian,
                           Math.max(densEasternRefugia, Math.max(densLevantAfrica,
                           Math.max(densAmericasLGM, Math.max(densBeringiaLGM, densSahulLGM))))));
                } else if (year <= -12500) {
                    // -14,000 BP: Deglaciation, Active Pacific Kelp Highway & Pre-Clovis Landfalls (Monte Verde II ~14.5k BP)
                    double densKelpHighway = 0.0;
                    double dPac = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_PACIFIC_KELP_HIGHWAY);
                    if (dPac <= 4.5) densKelpHighway = Math.max(densKelpHighway, 0.075 * Math.exp(-dPac / 2.0));
                    double dAndes = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_ANDES_CORRIDOR);
                    if (dAndes <= 3.5) densKelpHighway = Math.max(densKelpHighway, 0.060 * Math.exp(-dAndes / 2.0));
                    double dAmazon = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_AMAZON_MAINSTEM);
                    if (dAmazon <= 3.5) densKelpHighway = Math.max(densKelpHighway, 0.045 * Math.exp(-dAmazon / 2.2));
                    double dSavanna = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_BRAZIL_SAVANNA);
                    if (dSavanna <= 3.5) densKelpHighway = Math.max(densKelpHighway, 0.050 * Math.exp(-dSavanna / 2.2));

                    densKelpHighway = Math.max(densKelpHighway,
                        0.080 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -73.8, -41.8) / 30.0) +   // Monte Verde II Chile (14.5k BP)
                        0.065 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -79.0, -8.0) / 30.0) +    // Huaca Prieta / Paiján Peru
                        0.060 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -120.5, 42.7) / 30.0) +   // Paisley Caves OR
                        0.065 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -119.8, 34.2) / 30.0) +   // Channel Islands CA
                        0.045 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -80.4, 40.3) / 35.0) +    // Meadowcroft PA
                        0.045 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -77.3, 36.8) / 35.0)     // Cactus Hill VA
                    );

                    double densEuropeLateGlacial = 0.016 +
                        0.120 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 1.0, 45.0) / 80.0) +      // Magdalenian France
                        0.095 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 15.5, 41.7) / 70.0) +     // Epigravettian Italy
                        0.080 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 22.0, 44.5) / 70.0);     // Iron Gates

                    double densLevantLate = 0.018 +
                        0.180 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.58, 33.08) / 40.0) +    // Early Natufian pioneers
                        0.100 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 32.5, 25.5) / 100.0);

                    double densAsiaAfricaLate = 0.016 +
                        0.100 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 114.0, 34.5) / 120.0) +
                        0.090 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 139.5, 35.7) / 90.0) +
                        0.080 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 77.6, 22.9) / 120.0);

                    dens = Math.max(densKelpHighway, Math.max(densEuropeLateGlacial, Math.max(densLevantLate, densAsiaAfricaLate)));
                } else if (year <= -10500) {
                    // -10,900 BP: Younger Dryas & Clovis Horizon / Natufian Epipaleolithic
                    double densNatufian = 0.014 +
                        0.150 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.58, 33.08) / 20.0) +    // Ain Mallaha / Hula Valley Natufian core
                        0.120 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.22, 32.92) / 20.0) +    // Hayonim Cave
                        0.110 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 37.00, 32.00) / 20.0) +    // Shubayqa 1 (Black Desert / early bread)
                        0.090 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 41.50, 38.10) / 20.0);   // Hallan Çemi / Upper Tigris proto-sedentism

                    // Americas: Structured riverine networks & Ice-Free Corridor
                    double densAmericasYD = 0.0;
                    double dPac = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_PACIFIC_KELP_HIGHWAY);
                    if (dPac <= 4.0) densAmericasYD = Math.max(densAmericasYD, 0.070 * Math.exp(-dPac / 2.0));
                    double dIFC = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_ICE_FREE_CORRIDOR);
                    if (dIFC <= 3.5) densAmericasYD = Math.max(densAmericasYD, 0.075 * Math.exp(-dIFC / 2.0));
                    double dCol = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_COLUMBIA_SNAKE);
                    if (dCol <= 3.0) densAmericasYD = Math.max(densAmericasYD, 0.075 * Math.exp(-dCol / 2.0));
                    double dMiss = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_MISSISSIPPI_OHIO);
                    if (dMiss <= 3.5) densAmericasYD = Math.max(densAmericasYD, 0.080 * Math.exp(-dMiss / 2.0));
                    double dLowMiss = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_LOWER_MISSISSIPPI);
                    if (dLowMiss <= 3.0) densAmericasYD = Math.max(densAmericasYD, 0.080 * Math.exp(-dLowMiss / 2.0));
                    double dSE = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_SOUTHEAST_FLORIDA);
                    if (dSE <= 3.5) densAmericasYD = Math.max(densAmericasYD, 0.075 * Math.exp(-dSE / 2.0));

                    double dAndes = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_ANDES_CORRIDOR);
                    if (dAndes <= 3.5) densAmericasYD = Math.max(densAmericasYD, 0.080 * Math.exp(-dAndes / 2.0));
                    double dAmazon = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_AMAZON_MAINSTEM);
                    if (dAmazon <= 3.5) densAmericasYD = Math.max(densAmericasYD, 0.070 * Math.exp(-dAmazon / 2.2));
                    double dMadeira = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_AMAZON_MADEIRA);
                    if (dMadeira <= 3.5) densAmericasYD = Math.max(densAmericasYD, 0.065 * Math.exp(-dMadeira / 2.2));
                    double dSavanna = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_BRAZIL_SAVANNA);
                    if (dSavanna <= 3.5) densAmericasYD = Math.max(densAmericasYD, 0.070 * Math.exp(-dSavanna / 2.2));

                    densAmericasYD = Math.max(densAmericasYD,
                        0.090 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -103.3, 34.3) / 35.0) +   // Blackwater Draw Clovis type site
                        0.080 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -97.7, 30.9) / 35.0) +    // Gault site Texas
                        0.070 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -80.4, 40.3) / 35.0) +    // Meadowcroft PA
                        0.070 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -77.3, 36.8) / 35.0) +    // Cactus Hill VA
                        0.070 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -71.2, 42.6) / 35.0) +    // Bull Brook MA
                        0.065 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -104.9, 41.2) / 35.0) +   // Lindenmeier Folsom site
                        0.065 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -73.8, -41.8) / 30.0) +   // Monte Verde Chile
                        0.060 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -77.7, -9.2) / 30.0) +    // Guitarrero Cave Peru
                        0.060 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -54.5, -2.4) / 30.0) +    // Caverna da Pedra Pintada Amazonia
                        0.055 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -44.0, -19.5) / 30.0) +   // Lapa do Santo Brazil
                        0.050 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -70.0, -52.0) / 30.0)     // Fell's Cave Patagonia
                    );

                    double densEuropeYD = 0.014 +
                        0.110 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 1.0, 45.0) / 80.0) +      // Franco-Cantabrian Late Magdalenian/Azilian
                        0.090 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 3.0, 49.5) / 80.0) +      // Federmesser / Ahrensburgian Paris/Rhine
                        0.085 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 15.5, 41.7) / 70.0) +     // Epigravettian Italy
                        0.070 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.0, 50.5) / 80.0);     // Dnepr mammoth/reindeer camp

                    double densAsiaAfricaYD = 0.014 +
                        0.095 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 114.0, 34.5) / 100.0) +    // Yellow River Late Paleolithic
                        0.090 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 117.2, 28.7) / 90.0) +     // Xianrendong early pottery
                        0.080 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 139.5, 35.7) / 80.0) +     // Incipient Jomon Japan
                        0.075 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 77.6, 22.9) / 100.0) +     // Bhimbetka India
                        0.105 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 32.5, 25.5) / 100.0) +     // Nile Valley
                        0.070 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -2.4, 34.8) / 80.0) +      // Taforalt Maghreb
                        0.050 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 143.0, -33.7) / 120.0);   // Sahul

                    dens = Math.max(densNatufian, Math.max(densAmericasYD,
                           Math.max(densEuropeYD, densAsiaAfricaYD)));
                } else if (year <= -9000) {
                    // -10,000 BP: Early Holocene / Pre-Pottery Neolithic A (Göbekli Tepe, Jericho, Folsom)
                    double densPPNA = 0.018 +
                        0.45 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 38.92, 37.22) / 30.0) +    // Göbekli Tepe / Karahan Tepe Monumental Core
                        0.38 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.44, 31.87) / 30.0) +    // Jericho PPNA (Tell es-Sultan)
                        0.32 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 38.10, 35.90) / 35.0) +    // Mureybet / Jerf el Ahmar
                        0.30 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 39.70, 38.20) / 35.0) +    // Çayönü Tepesi
                        0.25 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 47.40, 32.50) / 40.0);     // Ali Kosh Zagros

                    double densChinaPPN = 0.014 +
                        0.22 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 113.6, 34.4) / 60.0) +    // Peiligang / Yellow River proto-millet
                        0.20 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 120.0, 29.5) / 60.0) +    // Shangshan Yangtze rice foragers
                        0.16 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 111.5, 25.5) / 60.0) +    // Yuchanyan
                        0.16 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 117.2, 28.7) / 60.0);     // Xianrendong

                    double densAmericasPaleo = 0.0;
                    double dPac = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_PACIFIC_KELP_HIGHWAY);
                    if (dPac <= 4.0) densAmericasPaleo = Math.max(densAmericasPaleo, 0.070 * Math.exp(-dPac / 2.0));
                    double dIFC = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_ICE_FREE_CORRIDOR);
                    if (dIFC <= 3.5) densAmericasPaleo = Math.max(densAmericasPaleo, 0.075 * Math.exp(-dIFC / 2.0));
                    double dCol = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_COLUMBIA_SNAKE);
                    if (dCol <= 3.0) densAmericasPaleo = Math.max(densAmericasPaleo, 0.075 * Math.exp(-dCol / 2.0));
                    double dMiss = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_MISSISSIPPI_OHIO);
                    if (dMiss <= 3.5) densAmericasPaleo = Math.max(densAmericasPaleo, 0.080 * Math.exp(-dMiss / 2.0));
                    double dLowMiss = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_LOWER_MISSISSIPPI);
                    if (dLowMiss <= 3.0) densAmericasPaleo = Math.max(densAmericasPaleo, 0.080 * Math.exp(-dLowMiss / 2.0));
                    double dSE = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_SOUTHEAST_FLORIDA);
                    if (dSE <= 3.5) densAmericasPaleo = Math.max(densAmericasPaleo, 0.075 * Math.exp(-dSE / 2.0));

                    double dAndes = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_ANDES_CORRIDOR);
                    if (dAndes <= 3.5) densAmericasPaleo = Math.max(densAmericasPaleo, 0.080 * Math.exp(-dAndes / 2.0));
                    double dAmazon = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_AMAZON_MAINSTEM);
                    if (dAmazon <= 3.5) densAmericasPaleo = Math.max(densAmericasPaleo, 0.070 * Math.exp(-dAmazon / 2.2));
                    double dMadeira = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_AMAZON_MADEIRA);
                    if (dMadeira <= 3.5) densAmericasPaleo = Math.max(densAmericasPaleo, 0.065 * Math.exp(-dMadeira / 2.2));
                    double dSavanna = HistoricalMapGenerator.distToPolyline(lng, lat, ROUTE_BRAZIL_SAVANNA);
                    if (dSavanna <= 3.5) densAmericasPaleo = Math.max(densAmericasPaleo, 0.070 * Math.exp(-dSavanna / 2.2));

                    densAmericasPaleo = Math.max(densAmericasPaleo,
                        0.090 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -104.9, 41.2) / 35.0) +   // Lindenmeier Folsom
                        0.085 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -97.7, 30.9) / 35.0) +    // Gault TX
                        0.075 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -80.4, 40.3) / 35.0) +    // Meadowcroft PA
                        0.075 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -71.2, 42.6) / 35.0) +    // Bull Brook MA
                        0.070 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -73.8, -41.8) / 30.0) +   // Monte Verde Chile
                        0.065 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -77.7, -9.2) / 30.0) +    // Guitarrero Cave Peru
                        0.065 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -54.5, -2.4) / 30.0) +    // Caverna da Pedra Pintada Amazonia
                        0.060 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -44.0, -19.5) / 30.0) +   // Lapa do Santo Brazil
                        0.050 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -70.0, -52.0) / 30.0)     // Fell's Cave Patagonia
                    );

                    double densEuropeMesolithic = 0.014 +
                        0.120 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 1.0, 45.0) / 80.0) +      // Sauveterrian / Azilian France
                        0.100 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 10.0, 52.0) / 80.0) +     // Maglemosian Northern Europe
                        0.095 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 22.0, 44.5) / 70.0) +     // Lepenski Vir / Iron Gates Mesolithic
                        0.090 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 15.5, 41.7) / 70.0);     // Italian Mesolithic

                    double densIndiaAfrica = 0.014 +
                        0.105 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 77.6, 22.9) / 100.0) +    // Bhimbetka Mesolithic rock art
                        0.095 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 80.0, 25.0) / 100.0) +    // Sarai Nahar Rai / Damdama
                        0.125 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 32.5, 25.5) / 100.0) +    // Nile Valley Epipaleolithic
                        0.085 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -2.4, 34.8) / 80.0) +     // Capsian Maghreb
                        0.070 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 36.0, 0.5) / 180.0) +     // East Africa
                        0.055 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 143.0, -33.7) / 120.0);   // Sahul

                    dens = Math.max(densPPNA, Math.max(densChinaPPN,
                           Math.max(densAmericasPaleo, Math.max(densEuropeMesolithic, densIndiaAfrica))));
                } else if (year <= -7000) {
                    // -8,000 BP: Early Neolithic / 8.2 ka Event (Çatalhöyük, Jiahu, Mehrgarh, Early European Farmers)
                    double densAnatoliaNeolithic = 0.08 +
                        3.20 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 32.83, 37.67) / 40.0) +   // Çatalhöyük Mega-Village Core
                        2.50 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 30.10, 37.60) / 40.0) +   // Hacilar
                        2.40 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 35.95, 31.98) / 45.0) +   // Ain Ghazal Jordan
                        2.20 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 39.10, 36.50) / 50.0) +   // Tell Sabi Abyad / Halaf
                        2.00 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 44.90, 35.60) / 50.0) +   // Jarmo Zagros
                        1.80 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 47.20, 34.40) / 50.0);   // Ganj Dareh

                    double densChinaNeolithic = 0.06 +
                        2.60 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 113.6, 33.6) / 50.0) +    // Jiahu (Henan - flutes, fermented rice)
                        2.20 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 114.2, 36.7) / 60.0) +    // Cishan / Peiligang millet
                        2.00 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 120.2, 30.1) / 50.0) +    // Kuahuqiao wet rice
                        1.80 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 105.9, 35.0) / 60.0);     // Dadiwan Gansu

                    double densEuropeEEF = 0.05 +
                        1.80 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 22.8, 39.3) / 50.0) +      // Sesklo / Thessaly Early Neolithic
                        1.60 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 20.5, 44.8) / 55.0) +      // StarÄevo-KÃ¶rÃ¶s-CriÅŸ Danube basin
                        1.40 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 9.0, 44.0) / 55.0) +       // Cardial / Impressed Ware Liguria/Provence
                        1.30 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 16.0, 48.5) / 60.0) +      // Early LBK pioneers Austria/Moravia
                        1.20 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 3.0, 41.5) / 55.0);       // Cardial Catalonia

                    double densMehrgarhIndus = 0.05 +
                        2.10 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 68.05, 29.28) / 45.0) +    // Mehrgarh Period I-II Neolithic
                        1.00 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 81.5, 25.0) / 70.0);     // Vindhya / Ganges early farming

                    double densGreenSaharaPastoral = 0.04 +
                        1.50 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 30.58, 22.53) / 55.0) +    // Nabta Playa Megalithic Calendar Center
                        1.40 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 30.85, 29.35) / 55.0) +    // Faiyum A Early Agriculture
                        1.10 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 14.5, 13.5) / 80.0) +     // Lake Mega-Chad Pastoralists
                        0.95 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 21.5, 17.0) / 80.0);     // Ennedi / Tibesti Cattle Herders

                    double densAmericasArchaic = 0.02 +
                        0.25 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -79.2, -6.9) / 55.0) +     // Nanchoc Valley Peru (early irrigation)
                        0.20 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -96.4, 16.9) / 55.0) +     // Guila Naquitz Oaxaca
                        0.18 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -97.4, 18.4) / 55.0) +     // Tehuacan Valley
                        0.15 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -88.0, 37.0) / 80.0);    // Eastern North America Archaic

                    dens = Math.max(densAnatoliaNeolithic, Math.max(densChinaNeolithic,
                           Math.max(densEuropeEEF, Math.max(densMehrgarhIndus,
                           Math.max(densGreenSaharaPastoral, densAmericasArchaic)))));
                } else if (year <= -4500) {
                    // -6,000 BP: Middle Neolithic / Ubaid Period & Green Sahara Optimum (Eridu, Yangshao, Vinča)
                    double densUbaid = 0.12 +
                        6.50 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 45.99, 30.82) / 40.0) +   // Eridu Temple Core (Proto-Urban Ubaid)
                        5.50 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 45.88, 31.25) / 40.0) +   // Tell el-'Oueili
                        5.00 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 48.26, 32.19) / 45.0) +   // Susa I Susiana
                        4.50 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 43.27, 36.52) / 45.0);   // Tepe Gawra Northern Ubaid

                    double densEgyptPredynastic = 0.10 +
                        5.00 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 31.37, 26.99) / 45.0) +   // Badari Upper Egypt
                        4.50 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 30.82, 30.33) / 45.0) +   // Merimde Beni Salama Delta
                        4.00 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 32.78, 25.10) / 45.0);   // Hierakonpolis precursor

                    double densChinaYangshao = 0.10 +
                        5.00 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 109.06, 34.27) / 45.0) +  // Banpo / Yangshao painted pottery
                        4.50 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 111.30, 34.70) / 45.0) +  // Miaodigou Core
                        4.80 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 121.38, 29.96) / 45.0) +  // Hemudu mature rice agriculture
                        3.80 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 120.60, 30.80) / 50.0) +  // Majiabang
                        3.20 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 119.50, 41.30) / 60.0);   // Hongshan Niuheliang

                    double densEuropeVinca = 0.08 +
                        4.20 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 20.62, 44.76) / 40.0) +   // Vinča-Belo Brdo proto-urban tell
                        3.80 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 21.36, 43.20) / 40.0) +   // Pločnik copper metallurgy
                        3.60 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 27.00, 47.00) / 50.0) +   // Cucuteni-Trypillia early mega-sites
                        3.40 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 16.50, 48.20) / 50.0) +   // Lengyel / mature LBK
                        3.00 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -3.00, 47.60) / 45.0);   // Carnac / Atlantic Megalithic builders

                    double densIndusMehrgarh = 0.08 +
                        4.00 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 68.05, 29.28) / 45.0) +   // Mehrgarh Period III-IV
                        3.20 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 71.50, 28.50) / 50.0);   // Hakra Ware proto-Harappan

                    double densGreenSaharaOptimum = 0.06 +
                        2.80 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 14.00, 13.50) / 65.0) +  // Lake Mega-Chad fishing & pastoralism
                        2.40 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 9.00, 25.50) / 65.0) +   // Tassili n'Ajjer Bovidian rock art
                        2.00 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, 8.50, 18.00) / 65.0) +    // Air Mountains
                        1.80 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -9.50, 18.50) / 65.0);   // Dhar Tichitt precursor

                    double densAmericasMiddle = 0.03 +
                        0.45 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -77.50, -10.90) / 45.0) +  // Norte Chico / Caral precursor Peru
                        0.35 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -92.10, 32.30) / 55.0) +   // Watson Brake earthen mounds Louisiana
                        0.30 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -92.80, 15.20) / 55.0) +   // Chantuto shellmounds Chiapas
                        0.30 * Math.exp(-HistoricalScenarioFeaturesCatalog.distSq(lng, lat, -80.70, -2.00) / 55.0);   // Valdivia precursor Ecuador

                    dens = Math.max(densUbaid, Math.max(densEgyptPredynastic,
                           Math.max(densChinaYangshao, Math.max(densEuropeVinca,
                           Math.max(densIndusMehrgarh, Math.max(densGreenSaharaOptimum, densAmericasMiddle))))));
                }

                dens *= weight;
                if (dens > 0.0005) {
                    double logNorm = Math.log1p(2.5 * dens) / Math.log1p(2.5 * 35.0); // reference 35.0 hab/km2
                    int gray = (int) Math.clamp(12.0 + logNorm * 243.0, 12.0, 255.0);
                    img.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
                } else {
                    img.setRGB(x, y, 0x000000);
                }
            }
        }
        return img;
    }

    /**
     * Save images to disk cache.
     *
     * @param scenarioName the scenario name parameter (String)
     * @param imgDensity the img density parameter (BufferedImage)
     * @param imgSovereignty the img sovereignty parameter (BufferedImage)
     * @param imgIsogloss the img isogloss parameter (BufferedImage)
     * @param imgKinship the img kinship parameter (BufferedImage)
     * @param imgRituals the img rituals parameter (BufferedImage)
     * @param imgTech the img tech parameter (BufferedImage)
     * @param imgTrade the img trade parameter (BufferedImage)
     * @param imgInst the img inst parameter (BufferedImage)
     * @param imgEco the img eco parameter (BufferedImage)
     * @param imgPathogen the img pathogen parameter (BufferedImage)
     */

}
