package de.hysky.skyblocker.config.configs;

import de.hysky.skyblocker.utils.waypoint.Waypoint;
import dev.isxander.yacl3.config.v2.api.SerialEntry;

public class CrimsonIsleConfig {
    @SerialEntry
    public Kuudra kuudra = new Kuudra();

    public static class Kuudra {
        @SerialEntry
        public boolean supplyWaypoints = true;

        @SerialEntry
        public boolean fuelWaypoints = true;

        @SerialEntry
        public Waypoint.Type suppliesAndFuelWaypointType = Waypoint.Type.WAYPOINT;

        @SerialEntry
        public boolean ballistaBuildWaypoints = true;

        @SerialEntry
        public boolean safeSpotWaypoints = true;

        @SerialEntry
        public boolean pearlWaypoints = true;

        @SerialEntry
        public boolean noArrowPoisonWarning = true;

        @SerialEntry
        public int arrowPoisonThreshold = 32;
    }
}
