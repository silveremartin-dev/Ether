/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.i18n.I18n;

/**
 * Immutable representation of a discrete cultural entity at a historical epoch.
 *
 * @param id                  canonical entity identifier
 * @param colorHex            24-bit hex badge color
 * @param colorRgb            RGB components
 * @param traits              normalized ethnographic feature vector
 * @param nameEn              English display name
 * @param nameFr              French display name
 * @param nameDe              German display name
 * @param nameEs              Spanish display name
 * @param nameZh              Chinese display name
 * @param kinshipType         kinship & social structure classification
 * @param lithicTechnocomplex lithic or metallurgical technocomplex classification
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public record CulturalEntity(
        String id,
        String colorHex,
        int[] colorRgb,
        double[] traits,
        String nameEn,
        String nameFr,
        String nameDe,
        String nameEs,
        String nameZh,
        String kinshipType,
        String lithicTechnocomplex
) {
    public String getLocalizedName() {
        String lang = I18n.getCurrentLanguage() != null ? I18n.getCurrentLanguage().getCode() : "en";
        if ("fr".equalsIgnoreCase(lang)) return nameFr != null ? nameFr : nameEn;
        if ("de".equalsIgnoreCase(lang)) return nameDe != null ? nameDe : nameEn;
        if ("es".equalsIgnoreCase(lang)) return nameEs != null ? nameEs : nameEn;
        if ("zh".equalsIgnoreCase(lang)) return nameZh != null ? nameZh : nameEn;
        return nameEn != null ? nameEn : id;
    }

    public double distanceTo(CulturalEntity other) {
        if (other == null || this.traits == null || other.traits == null) return 1.0;
        double sumSq = 0.0;
        int n = Math.min(this.traits.length, other.traits.length);
        for (int i = 0; i < n; i++) {
            double diff = this.traits[i] - other.traits[i];
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq);
    }

    public double affinityWith(CulturalEntity other) {
        if (other != null && this.id.equals(other.id)) return 1.0;
        double dist = distanceTo(other);
        return Math.exp(-3.5 * dist);
    }
}
