package com.example.radcityapp;

/**
 * Battery level estimator for 48v batteries
 */
public class FortyEightVoltBatteryLevelEstimator {
    // numbers derived from chart posted at this site: https://electricbikereview.com/forums/threads/battery-voltages.26398/
    private static final double MAX_VOLTAGE = 54.6; // max voltage for 48v ebike battery
    private static final double LOSS_FACTOR = 0.16; // voltage loss per percent of battery

    /**
     * get the battery level estimate based on a passed in measured voltage
     * @param measuredVoltage
     * @return the battery level estimate
     */
    public static double getBatteryLevelEstimate(double measuredVoltage){
        double voltageDrop = Math.abs(MAX_VOLTAGE - measuredVoltage);
        double percentLoss = voltageDrop / LOSS_FACTOR;
        return (100 - percentLoss);
    }
}
