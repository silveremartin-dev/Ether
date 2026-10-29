package org.ether.society.core.dod;

import org.ether.society.analytics.HeadlessBatchRunner;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.Test;

public class Demographic1914PopulationTest {

    @Test
    public void test1914PopulationDrop() {
        Scenario ww1Scenario = null;
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            if ("world_wars_totalitarian_1914".equals(s.getPresetKey())) {
                ww1Scenario = s;
                break;
            }
        }
        ww1Scenario.setEndDateYear(1915);
        ww1Scenario.setH3Resolution(2);
        ww1Scenario.setTargetCohortSize(50000);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(ww1Scenario);
        for (var entry : record.getTimeSeriesData().entrySet()) {
            int year = entry.getKey();
            var snap = entry.getValue();
            System.out.println("Year: " + year + ", Pop: " + snap.getPopulation() + ", MetricsMap Pop: " + snap.getMetricsMap().get("worldPopulation"));
        }
    }
}
