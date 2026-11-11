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

    /**
     * Computes authentic directional asymmetric cultural affinity $A(c_{\text{this}} \to c_{\text{other}}) \in [0.0, 1.0]$.
     *
     * <p>Accounts for:
     * <ul>
     *   <li>Base isotropic Mahalanobis/Euclidean distance decay across 4D ethnographic traits:
     *       $A_0 = \exp(-3.5 \cdot d)$.</li>
     *   <li>Social hierarchy and institutional prestige asymmetry ($\Delta t_1 = t_{\text{other}}[1] - t_{\text{this}}[1]$):
     *       peripheral and emerging polities exhibit higher cultural aspiration towards complex imperial centers,
     *       whereas dominant hegemonic polities exhibit an ethnocentric bias discount towards peripheries.</li>
     *   <li>Ritual exclusivism and dogmatic intolerance penalty ($\Delta t_3 = t_{\text{this}}[3] \cdot |t_{\text{this}}[3] - t_{\text{other}}[3]|$):
     *       strongly doctrinal or orthodox ritual traditions incur directional friction against foreign traditions,
     *       whereas syncretic traditions maintain higher bilateral openness.</li>
     * </ul>
     *
     * @param other target cultural entity
     * @return directional affinity in $[0.0, 1.0]$
     */
    public double affinityTowards(CulturalEntity other) {
        if (other == null) return 0.0;
        if (this.id != null && this.id.equals(other.id())) return 1.0;
        if (this.traits == null || other.traits() == null) return 0.5;

        double dist = distanceTo(other);
        double baseAffinity = Math.exp(-3.5 * dist);

        // Trait dimensions: [0: linguisticBranch, 1: socialStructure, 2: subsistenceMode, 3: ritualTradition]
        double t1This = this.traits.length > 1 ? this.traits[1] : 0.5;
        double t1Other = other.traits().length > 1 ? other.traits()[1] : 0.5;
        double statusDelta = t1Other - t1This; // positive if other is more stratified/imperial

        double t3This = this.traits.length > 3 ? this.traits[3] : 0.5;
        double t3Other = other.traits().length > 3 ? other.traits()[3] : 0.5;
        double dogmaPenalty = t3This * Math.abs(t3This - t3Other);

        // Asymmetric modulation: status attraction/discount + doctrinal intolerance discount
        double prestigeBias = 0.12 * Math.tanh(2.5 * statusDelta);
        double intoleranceDiscount = 0.15 * dogmaPenalty;

        double directed = baseAffinity + prestigeBias - intoleranceDiscount;
        return Math.clamp(directed, 0.01, 1.0);
    }

    /**
     * Backward-compatible alias for directional cultural affinity.
     *
     * @param other target cultural entity
     * @return affinity value in $[0.0, 1.0]$
     */
    public double affinityWith(CulturalEntity other) {
        return affinityTowards(other);
    }
}
