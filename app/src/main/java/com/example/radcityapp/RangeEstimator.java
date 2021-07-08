package com.example.radcityapp;

/**
 * Estimate range for a 672 watt/hr battery
 */
public class RangeEstimator {
    // values on this page were determined experimentally or from the RadBike spec sheet.
    // Micah Toll's wonderful article was consulted as a starting point: https://electrek.co/2020/06/12/how-far-can-an-electric-bicycle-really-go-on-a-charge/
    private static final int BATTERY_CAPACITY = 672; // battery capacity in watt hours
    private static final double PAS_LEVEL_0_ESTIMATED_USAGE = 0.5; // PAS estimated usage levels in watt hours
    private static final double PAS_LEVEL_1_ESTIMATED_USAGE = 10;
    private static final double PAS_LEVEL_2_ESTIMATED_USAGE = 15.0;
    private static final double PAS_LEVEL_3_ESTIMATED_USAGE = 20.0;
    private static final double PAS_LEVEL_4_ESTIMATED_USAGE = 25.0;
    private static final double PAS_LEVEL_5_ESTIMATED_USAGE = 30.0;

    /**
     * Estimate the remaining range of the battery
     * @param PASLevel
     * @param chargeLevel
     * @return
     */
    public static double EstimateRange( int PASLevel, double chargeLevel){
        double remainingCapacity = chargeLevel/100 * BATTERY_CAPACITY;
        double usageRate;
        switch(PASLevel){
            case 0:
                usageRate = PAS_LEVEL_0_ESTIMATED_USAGE;
                break;
            case 1:
                usageRate = PAS_LEVEL_1_ESTIMATED_USAGE;
                break;
            case 2:
                usageRate = PAS_LEVEL_2_ESTIMATED_USAGE;
                break;
            case 3:
                usageRate = PAS_LEVEL_3_ESTIMATED_USAGE;
                break;
            case 4:
                usageRate = PAS_LEVEL_4_ESTIMATED_USAGE;
                break;
            case 5:
                usageRate = PAS_LEVEL_5_ESTIMATED_USAGE;
                break;
            default:
                usageRate = 0;
                break;
        }
        return (remainingCapacity/usageRate);
    }

    /**
     * get the remaining battery capacity given the charge level
     * @param chargeLevel
     * @return
     */
    public static double getRemainingCapacity(double chargeLevel){
        return chargeLevel/100 * BATTERY_CAPACITY;
    }
}
