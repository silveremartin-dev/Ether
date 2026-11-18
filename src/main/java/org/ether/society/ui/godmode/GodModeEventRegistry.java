/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.godmode;

import org.ether.society.i18n.I18n;
import org.ether.society.model.EventCategory;

import java.util.List;

/**
 * Catalog of configurable disaster, climatic, sociological, and archetypal leader events for God Mode.
 *
 * <p>Encompasses natural geophysical catastrophes (volcanoes, earthquakes, tsunamis),
 * extraterrestrial habitat hazards (dome breach, ECLSS failure), and Great Man cliodynamic
 * leader archetypes (military conqueror, agrarian reformer).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class GodModeEventRegistry {

    private GodModeEventRegistry() {}

    /**
     * Model item representing a selectable God Mode event type.
     *
     * @param id internal unique string identifier
     * @param category ontological event category (NATURAL vs ANTHROPIC)
     * @param typeI18nKey localization key for event type name
     * @param nameI18nKey localization key for default event title
     * @param descI18nKey localization key for description
     * @param defaultDurationDays default duration in simulation days
     * @param defaultMagnitude default magnitude / intensity
     * @param isLeader true if event spawns a Great Man historical leader archetype
     */
    public record EventTypeItem(
        String id,
        EventCategory category,
        String typeI18nKey,
        String nameI18nKey,
        String descI18nKey,
        int defaultDurationDays,
        double defaultMagnitude,
        boolean isLeader
    ) {
        public String getDisplayName() {
            return I18n.getOrDefault(typeI18nKey, id);
        }

        public String getDefaultTitle() {
            return I18n.getOrDefault(nameI18nKey, id);
        }

        public String getDescription() {
            return I18n.getOrDefault(descI18nKey, "");
        }

        @Override
        public String toString() {
            return "[" + category.getDisplayName() + "] " + getDisplayName();
        }
    }

    /**
     * Exhaustive catalogue of supported divine interventions and macro-historical disruptions.
     */
    public static final List<EventTypeItem> EVENT_TYPES = List.of(
        // Category 1: Geophysical Events & Natural Catastrophes
        new EventTypeItem("VOLCANO", EventCategory.NATURAL, "godmode.type.volcano", "godmode.event.volcano", "godmode.event.volcano.desc", 365, 7.0, false),
        new EventTypeItem("EARTHQUAKE", EventCategory.NATURAL, "godmode.type.earthquake", "godmode.event.earthquake", "godmode.event.earthquake.desc", 3, 8.0, false),
        new EventTypeItem("TSUNAMI", EventCategory.NATURAL, "godmode.type.tsunami", "godmode.event.tsunami", "godmode.event.tsunami.desc", 1, 8.5, false),
        new EventTypeItem("FLOOD", EventCategory.NATURAL, "godmode.type.flood", "godmode.event.flood", "godmode.event.flood.desc", 30, 6.0, false),
        new EventTypeItem("DROUGHT", EventCategory.NATURAL, "godmode.type.drought", "godmode.event.drought", "godmode.event.drought.desc", 180, 5.5, false),
        new EventTypeItem("HEATWAVE", EventCategory.NATURAL, "godmode.type.heatwave", "godmode.event.heatwave", "godmode.event.heatwave.desc", 30, 5.0, false),
        new EventTypeItem("ICE_AGE", EventCategory.NATURAL, "godmode.type.ice_age", "godmode.event.ice_age", "godmode.event.ice_age.desc", 18250, 5.0, false),
        new EventTypeItem("METEOR", EventCategory.NATURAL, "godmode.type.meteor", "godmode.event.meteor", "godmode.event.meteor.desc", 7, 9.0, false),
        new EventTypeItem("SOLAR_EMP", EventCategory.NATURAL, "godmode.type.solar_emp", "godmode.event.solar_emp", "godmode.event.solar_emp.desc", 30, 8.5, false),
        new EventTypeItem("PANDEMIC", EventCategory.NATURAL, "godmode.type.pandemic", "godmode.event.pandemic", "godmode.event.pandemic.desc", 730, 8.0, false),
        new EventTypeItem("LOCUST_SWARM", EventCategory.NATURAL, "godmode.type.locust_swarm", "godmode.event.locust_swarm", "godmode.event.locust_swarm.desc", 60, 6.5, false),
        new EventTypeItem("CROP_BLIGHT", EventCategory.NATURAL, "godmode.type.crop_blight", "godmode.event.crop_blight", "godmode.event.crop_blight.desc", 90, 6.5, false),
        new EventTypeItem("WILDLIFE_DIE_OFF", EventCategory.NATURAL, "godmode.type.wildlife_die_off", "godmode.event.wildlife_die_off", "godmode.event.wildlife_die_off.desc", 180, 6.0, false),
        new EventTypeItem("FISHERY_COLLAPSE", EventCategory.NATURAL, "godmode.type.fishery_collapse", "godmode.event.fishery_collapse", "godmode.event.fishery_collapse.desc", 365, 6.5, false),
        new EventTypeItem("TROPICAL_CYCLONE", EventCategory.NATURAL, "godmode.type.tropical_cyclone", "godmode.event.tropical_cyclone", "godmode.event.tropical_cyclone.desc", 7, 7.5, false),
        new EventTypeItem("WILDFIRE", EventCategory.NATURAL, "godmode.type.wildfire", "godmode.event.wildfire", "godmode.event.wildfire.desc", 30, 6.5, false),
        new EventTypeItem("BIODIVERSITY_COLLAPSE", EventCategory.NATURAL, "godmode.type.biodiversity_collapse", "godmode.event.biodiversity_collapse", "godmode.event.biodiversity_collapse.desc", 3650, 6.0, false),
        new EventTypeItem("LAVA_TUBE_COLLAPSE", EventCategory.NATURAL, "godmode.type.lava_tube_collapse", "godmode.event.lava_tube_collapse", "godmode.event.lava_tube_collapse.desc", 1, 9.0, false),

        // Category 2: Anthropic Catastrophes, Technology & Warfare
        new EventTypeItem("NUCLEAR_STRIKE", EventCategory.ANTHROPIC, "godmode.type.nuclear_strike", "godmode.event.nuclear_strike", "godmode.event.nuclear_strike.desc", 0, 7.5, false),
        new EventTypeItem("NUCLEAR_WINTER", EventCategory.ANTHROPIC, "godmode.type.nuclear_winter", "godmode.event.nuclear_winter", "godmode.event.nuclear_winter.desc", 3650, 7.5, false),
        new EventTypeItem("INDUSTRIAL_ACCIDENT", EventCategory.ANTHROPIC, "godmode.type.industrial_accident", "godmode.event.industrial_accident", "godmode.event.industrial_accident.desc", 365, 7.0, false),
        new EventTypeItem("URBAN_FIRE", EventCategory.ANTHROPIC, "godmode.type.urban_fire", "godmode.event.urban_fire", "godmode.event.urban_fire.desc", 14, 7.5, false),
        new EventTypeItem("DAM_FAILURE", EventCategory.ANTHROPIC, "godmode.type.dam_failure", "godmode.event.dam_failure", "godmode.event.dam_failure.desc", 30, 8.0, false),
        new EventTypeItem("CYBER_ATTACK", EventCategory.ANTHROPIC, "godmode.type.cyber_attack", "godmode.event.cyber_attack", "godmode.event.cyber_attack.desc", 14, 6.5, false),
        new EventTypeItem("ECONOMIC_CRASH", EventCategory.ANTHROPIC, "godmode.type.economic_crash", "godmode.event.economic_crash", "godmode.event.economic_crash.desc", 365, 7.0, false),
        new EventTypeItem("GEOENGINEERING", EventCategory.ANTHROPIC, "godmode.type.geoengineering", "godmode.event.geoengineering", "godmode.event.geoengineering.desc", 730, 4.0, false),
        new EventTypeItem("DOME_BREACH", EventCategory.ANTHROPIC, "godmode.type.dome_breach", "godmode.event.dome_breach", "godmode.event.dome_breach.desc", 7, 8.5, false),
        new EventTypeItem("ECLSS_FAILURE", EventCategory.ANTHROPIC, "godmode.type.eclss_failure", "godmode.event.eclss_failure", "godmode.event.eclss_failure.desc", 30, 7.5, false),
        new EventTypeItem("AEROSTAT_PUNCTURE", EventCategory.ANTHROPIC, "godmode.type.aerostat_puncture", "godmode.event.aerostat_puncture", "godmode.event.aerostat_puncture.desc", 14, 8.0, false),
        new EventTypeItem("ALIEN_CONTACT", EventCategory.ANTHROPIC, "godmode.type.alien_contact", "godmode.event.alien_contact", "godmode.event.alien_contact.desc", 1825, 10.0, false),

        // Category 3: Historical Leader Archetypes
        new EventTypeItem("LEADER_MILITARY_CONQUEROR", EventCategory.ANTHROPIC, "leader.archetype.military_conqueror", "leader.archetype.military_conqueror", "leader.archetype.military_conqueror.desc", 7300, 8.0, true),
        new EventTypeItem("LEADER_INFRASTRUCTURE_BUILDER", EventCategory.ANTHROPIC, "leader.archetype.infrastructure_builder", "leader.archetype.infrastructure_builder", "leader.archetype.infrastructure_builder.desc", 10950, 7.5, true),
        new EventTypeItem("LEADER_INSTITUTIONAL_REFORMER", EventCategory.ANTHROPIC, "leader.archetype.institutional_reformer", "leader.archetype.institutional_reformer", "leader.archetype.institutional_reformer.desc", 9125, 7.0, true),
        new EventTypeItem("LEADER_HYDRAULIC_AGRARIAN_INNOVATOR", EventCategory.ANTHROPIC, "leader.archetype.hydraulic_agrarian_innovator", "leader.archetype.hydraulic_agrarian_innovator", "leader.archetype.hydraulic_agrarian_innovator.desc", 12775, 7.5, true),
        new EventTypeItem("LEADER_MORAL_RELIGIOUS_SAGE", EventCategory.ANTHROPIC, "leader.archetype.moral_religious_sage", "leader.archetype.moral_religious_sage", "leader.archetype.moral_religious_sage.desc", 14600, 8.0, true),
        new EventTypeItem("LEADER_TOTALITARIAN_PURGER", EventCategory.ANTHROPIC, "leader.archetype.totalitarian_purger", "leader.archetype.totalitarian_purger", "leader.archetype.totalitarian_purger.desc", 5475, 8.5, true)
    );
}
